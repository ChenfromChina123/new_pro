package com.aispring.service.impl;

import com.aispring.config.StorageProperties;
import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.dto.response.TerminalFileDto;
import com.aispring.service.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalServiceImpl implements TerminalService {

    private final StorageProperties storageProperties;
    private static final long MAX_QUOTA_BYTES = 1024 * 1024 * 1024; // 1GB
    private static final int MAX_DEPTH = 10;

    @Override
    public String getUserTerminalRoot(Long userId) {
        String base = storageProperties.getAiTerminalAbsolute();
        return Paths.get(base, String.valueOf(userId)).normalize().toString();
    }

    private void checkQuota(Path rootPath) throws IOException {
        if (!Files.exists(rootPath)) return;
        try (Stream<Path> walk = Files.walk(rootPath)) {
            long size = walk.filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
            if (size > MAX_QUOTA_BYTES) {
                throw new IOException("Storage quota exceeded (1GB limit).");
            }
        }
    }

    private void checkDepth(Path rootPath, Path targetPath) throws IOException {
        Path relative = rootPath.relativize(targetPath);
        if (relative.getNameCount() > MAX_DEPTH) {
            throw new IOException("Directory depth limit exceeded (Max 10 levels).");
        }
    }

    private String getRelativePath(Path rootPath, Path cwdPath) {
        if (cwdPath.equals(rootPath)) {
            return "/";
        }
        try {
            return "/" + rootPath.relativize(cwdPath).toString().replace("\\", "/");
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }

    private Path resolvePath(Path rootPath, String relativeCwd) {
        Path cwdPath = rootPath;
        if (relativeCwd != null && !relativeCwd.isEmpty()) {
            String safeRel = relativeCwd.replace("..", "");
            if (safeRel.startsWith("/") || safeRel.startsWith("\\")) {
                if (safeRel.length() > 1) {
                     safeRel = safeRel.substring(1);
                } else {
                     safeRel = "";
                }
            }
            cwdPath = rootPath.resolve(safeRel).normalize();
        }
        if (!cwdPath.startsWith(rootPath)) {
            cwdPath = rootPath;
        }
        return cwdPath;
    }

    @Override
    public List<TerminalFileDto> listFiles(Long userId, String relativePath) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path targetPath = resolvePath(rootPath, relativePath);

        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            return new ArrayList<>();
        }

        List<TerminalFileDto> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(targetPath)) {
            stream.forEach(path -> {
                File file = path.toFile();
                files.add(TerminalFileDto.builder()
                        .name(file.getName())
                        .path(getRelativePath(rootPath, path))
                        .isDirectory(file.isDirectory())
                        .size(file.length())
                        .lastModified(file.lastModified())
                        .build());
            });
        } catch (IOException e) {
            log.error("Error listing files", e);
        }
        return files;
    }

    @Override
    public TerminalCommandResponse writeFile(Long userId, String relativePath, String content, String relativeCwd, boolean overwrite) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd);

        try {
            if (!Files.exists(cwdPath)) {
                Files.createDirectories(cwdPath);
            }
            checkQuota(rootPath);
            checkDepth(rootPath, cwdPath);
        } catch (IOException e) {
            return new TerminalCommandResponse("", "Error: " + e.getMessage(), 1, getRelativePath(rootPath, cwdPath));
        }

        if (relativePath == null || relativePath.isEmpty()) {
            return new TerminalCommandResponse("", "File path is required", 1, getRelativePath(rootPath, cwdPath));
        }

        String safePath = relativePath.replace("..", "");
        if (safePath.startsWith("/") || safePath.startsWith("\\")) {
            safePath = safePath.substring(1);
        }

        Path target = cwdPath.resolve(safePath).normalize();
        if (!target.startsWith(rootPath)) {
            return new TerminalCommandResponse("", "Invalid path (out of sandbox)", 1, getRelativePath(rootPath, cwdPath));
        }

        try {
            checkDepth(rootPath, target);
            
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (Files.exists(target) && !overwrite) {
                return new TerminalCommandResponse("", "File already exists: " + target.getFileName(), 1, getRelativePath(rootPath, cwdPath));
            }

            Files.writeString(target, content != null ? content : "", StandardCharsets.UTF_8);
            return new TerminalCommandResponse("Wrote file: " + rootPath.relativize(target).toString(), "", 0, getRelativePath(rootPath, cwdPath));
        } catch (Exception e) {
            log.error("Write file failed", e);
            return new TerminalCommandResponse("", "Write file failed: " + e.getMessage(), -1, getRelativePath(rootPath, cwdPath));
        }
    }

    @Override
    public TerminalCommandResponse executeCommand(Long userId, String command, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd);

        // Ensure directory exists
        if (!Files.exists(cwdPath)) {
            try {
                Files.createDirectories(cwdPath);
            } catch (IOException e) {
                return new TerminalCommandResponse("", "Error creating directory: " + e.getMessage(), 1, getRelativePath(rootPath, cwdPath));
            }
        }

        // Detect OS
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        List<String> cmdList = new ArrayList<>();
        if (isWindows) {
            cmdList.add("powershell.exe");
            cmdList.add("-Command");
            cmdList.add(command);
        } else {
            cmdList.add("/bin/bash");
            cmdList.add("-c");
            cmdList.add(command);
        }

        ProcessBuilder pb = new ProcessBuilder(cmdList);
        pb.directory(cwdPath.toFile());
        pb.redirectErrorStream(false); // Split stdout and stderr

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            Process process = pb.start();
            
            // Read output in threads to avoid blocking
            Thread outThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), isWindows ? "GBK" : "UTF-8"))) { 
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading stdout", e);
                }
            });

            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), isWindows ? "GBK" : "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.error("Error reading stderr", e);
                }
            });

            outThread.start();
            errThread.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS); // Timeout
            if (!finished) {
                process.destroyForcibly();
                return new TerminalCommandResponse(stdout.toString(), "Command timed out after 30s.\n" + stderr.toString(), 124, getRelativePath(rootPath, cwdPath));
            }
            
            outThread.join();
            errThread.join();

            return new TerminalCommandResponse(stdout.toString(), stderr.toString(), process.exitValue(), getRelativePath(rootPath, cwdPath));

        } catch (Exception e) {
            log.error("Command execution failed", e);
            return new TerminalCommandResponse("", "Execution failed: " + e.getMessage(), -1, getRelativePath(rootPath, cwdPath));
        }
    }
}
