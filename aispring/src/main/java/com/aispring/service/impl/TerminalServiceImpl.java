package com.aispring.service.impl;

import com.aispring.config.StorageProperties;
import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.service.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalServiceImpl implements TerminalService {

    private final StorageProperties storageProperties;

    @Override
    public String getUserTerminalRoot(Long userId) {
        String base = storageProperties.getCloudDiskAbsolute();
        return Paths.get(base, String.valueOf(userId)).normalize().toString();
    }

    @Override
    public TerminalCommandResponse executeCommand(Long userId, String command, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);

        // Determine working directory
        Path cwdPath = rootPath;
        if (relativeCwd != null && !relativeCwd.isEmpty()) {
            // Sanitize relative path
            String safeRel = relativeCwd.replace("..", ""); // Basic protection
            if (safeRel.startsWith("/") || safeRel.startsWith("\\")) {
                safeRel = safeRel.substring(1);
            }
            cwdPath = rootPath.resolve(safeRel).normalize();
        }

        // Security check: ensure cwd is within user root
        if (!cwdPath.startsWith(rootPath)) {
            cwdPath = rootPath;
        }

        // Ensure directory exists
        if (!Files.exists(cwdPath)) {
            try {
                Files.createDirectories(cwdPath);
            } catch (IOException e) {
                return new TerminalCommandResponse("", "Error creating directory: " + e.getMessage(), 1, cwdPath.toString());
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
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), isWindows ? "GBK" : "UTF-8"))) { // Windows often uses GBK for console
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
                return new TerminalCommandResponse(stdout.toString(), "Command timed out after 30s.\n" + stderr.toString(), 124, cwdPath.toString());
            }
            
            outThread.join();
            errThread.join();

            return new TerminalCommandResponse(stdout.toString(), stderr.toString(), process.exitValue(), cwdPath.toString());

        } catch (Exception e) {
            log.error("Command execution failed", e);
            return new TerminalCommandResponse("", "Execution failed: " + e.getMessage(), -1, cwdPath.toString());
        }
    }
}
