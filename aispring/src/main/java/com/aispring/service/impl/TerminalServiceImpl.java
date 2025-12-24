package com.aispring.service.impl;

import com.aispring.config.StorageProperties;
import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.dto.response.TerminalFileDto;
import com.aispring.entity.terminal.FileSearchRequest;
import com.aispring.entity.terminal.FileContextRequest;
import com.aispring.entity.terminal.FileModifyRequest;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
        Path root = Paths.get(base, String.valueOf(userId)).normalize();
        
        // Ensure root and requirements directory exist
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            Path reqDir = root.resolve("requirements");
            if (!Files.exists(reqDir)) {
                Files.createDirectories(reqDir);
            }
        } catch (IOException e) {
            log.error("Failed to initialize user terminal root", e);
        }
        
        return root.toString();
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
    public String readFile(Long userId, String relativePath) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        
        // Use resolvePath logic to handle the path safely
        Path targetPath = resolvePath(rootPath, relativePath);

        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            throw new RuntimeException("File not found: " + relativePath);
        }

        try {
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading file: " + relativePath, e);
            throw new RuntimeException("Could not read file: " + e.getMessage());
        }
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

        // 严格指令检测：禁止目录穿越和访问其他盘符
        if (command.contains("..")) {
            return new TerminalCommandResponse("", "Security Error: Directory traversal (..) is prohibited.", 1, getRelativePath(rootPath, cwdPath));
        }
        if (isWindows && command.matches("(?i).*[a-z]:[\\\\/].*")) {
            // 允许访问用户根目录，但为了脱敏，建议用户/AI始终使用相对路径
            // 如果指令中包含非用户根目录的盘符路径，直接拦截
            String normalizedCommand = command.replace("/", "\\");
            String normalizedRoot = userRoot.replace("/", "\\");
            if (!normalizedCommand.contains(normalizedRoot)) {
                return new TerminalCommandResponse("", "Security Error: Access to absolute paths outside sandbox is prohibited.", 1, getRelativePath(rootPath, cwdPath));
            }
        }

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

            String sanitizedStdout = sanitizeOutput(stdout.toString(), userRoot, rootPath);
            String sanitizedStderr = sanitizeOutput(stderr.toString(), userRoot, rootPath);

            return new TerminalCommandResponse(sanitizedStdout, sanitizedStderr, process.exitValue(), getRelativePath(rootPath, cwdPath));

        } catch (Exception e) {
            log.error("Command execution failed", e);
            return new TerminalCommandResponse("", "Execution failed: " + sanitizeOutput(e.getMessage(), userRoot, rootPath), -1, getRelativePath(rootPath, cwdPath));
        }
    }

    /**
     * 脱敏输出内容，将物理路径替换为虚拟路径
     */
    private String sanitizeOutput(String output, String userRoot, Path rootPath) {
        if (output == null || output.isEmpty()) return output;
        
        String sanitized = output;
        
        // 1. 替换物理根路径为 ~
        sanitized = sanitized.replace(userRoot, "~");
        
        // 2. 尝试替换正斜杠版本的物理路径（Windows环境下常见）
        String forwardRoot = userRoot.replace("\\", "/");
        if (!forwardRoot.equals(userRoot)) {
            sanitized = sanitized.replace(forwardRoot, "~");
        }
        
        // 3. 尝试替换 Path 对象生成的字符串
        String rootPathStr = rootPath.toString();
        if (!rootPathStr.equals(userRoot)) {
            sanitized = sanitized.replace(rootPathStr, "~");
        }
        
        // 4. 屏蔽任何其他 Windows 盘符路径，防止探测系统目录
        // 匹配如 C:\Windows, D:\Data 等，但不匹配已经替换过的 ~
        sanitized = sanitized.replaceAll("(?i)[a-z]:\\\\[^\\s\\n\\r]*", "[RESTRICTED PATH]");
        sanitized = sanitized.replaceAll("(?i)[a-z]:/[^\\s\\n\\r]*", "[RESTRICTED PATH]");
        
        // 5. 屏蔽 Linux 风格的绝对路径 (以 / 开头且不是我们的虚拟路径开头)
        // 注意：这可能会误伤，但为了严格性，可以尝试屏蔽常见的敏感路径
        sanitized = sanitized.replaceAll("/(etc|var|usr|bin|sbin|lib|root|home)(/[^\\s\\n\\r]*)?", "[RESTRICTED PATH]");
        
        return sanitized;
    }

    @Override
    public TerminalCommandResponse searchFiles(Long userId, FileSearchRequest request, String relativeCwd) {
        log.info("=== Search Files ===");
        log.info("User: {}, Pattern: {}, FilePattern: {}, ContextLines: {}", 
            userId, request.getPattern(), request.getFilePattern(), request.getContextLines());
        
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd);
        
        try {
            Pattern pattern = Pattern.compile(
                request.getPattern(),
                request.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE
            );
            
            List<String> results = new ArrayList<>();
            int contextLines = request.getContextLines();
            
            try (Stream<Path> stream = Files.walk(cwdPath, 10)) {
                stream.filter(path -> {
                    if (Files.isDirectory(path)) return false;
                    String fileName = path.getFileName().toString();
                    
                    // 文件模式匹配
                    if (request.getFilePattern() != null && !request.getFilePattern().isEmpty()) {
                        String filePattern = request.getFilePattern().replace("*", ".*");
                        if (!fileName.matches(filePattern)) return false;
                    }
                    return true;
                }).forEach(filePath -> {
                    try {
                        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                        String relativePath = getRelativePath(rootPath, filePath);
                        
                        for (int i = 0; i < lines.size(); i++) {
                            Matcher matcher = pattern.matcher(lines.get(i));
                            if (matcher.find()) {
                                int lineNum = i + 1;
                                
                                // 添加上下文
                                results.add(String.format("\n=== %s:%d ===\n", relativePath, lineNum));
                                
                                int start = Math.max(0, i - contextLines);
                                int end = Math.min(lines.size(), i + contextLines + 1);
                                
                                for (int j = start; j < end; j++) {
                                    String prefix = (j == i) ? ">>> " : "    ";
                                    results.add(String.format("%s%d: %s", prefix, j + 1, lines.get(j)));
                                }
                                results.add(""); // 空行分隔
                            }
                        }
                    } catch (IOException e) {
                        log.warn("Error reading file: {}", filePath, e);
                    }
                });
            }
            
            String output = String.join("\n", results);
            if (output.isEmpty()) {
                output = "No matches found for pattern: " + request.getPattern();
            }
            
            log.info("Search completed, found {} matches", results.size());
            return new TerminalCommandResponse(output, "", 0, getRelativePath(rootPath, cwdPath));
            
        } catch (Exception e) {
            log.error("Search failed", e);
            return new TerminalCommandResponse("", "Search failed: " + e.getMessage(), -1, getRelativePath(rootPath, cwdPath));
        }
    }

    @Override
    public TerminalCommandResponse readFileContext(Long userId, FileContextRequest request, String relativeCwd) {
        log.info("=== Read File Context ===");
        log.info("User: {}, Files: {}", userId, request.getFiles().size());
        
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd);
        
        List<String> results = new ArrayList<>();
        
        for (FileContextRequest.FileRange fileRange : request.getFiles()) {
            try {
                Path targetPath = resolvePath(rootPath, fileRange.getPath());
                
                if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
                    results.add(String.format("\n=== %s (NOT FOUND) ===\n", fileRange.getPath()));
                    continue;
                }
                
                List<String> lines = Files.readAllLines(targetPath, StandardCharsets.UTF_8);
                int startLine = Math.max(1, fileRange.getStartLine()) - 1; // 转换为0索引
                int endLine = Math.min(lines.size(), fileRange.getEndLine());
                
                results.add(String.format("\n=== %s (lines %d-%d) ===\n", 
                    getRelativePath(rootPath, targetPath), 
                    fileRange.getStartLine(), 
                    endLine));
                
                for (int i = startLine; i < endLine; i++) {
                    results.add(String.format("%d: %s", i + 1, lines.get(i)));
                }
                
                log.info("Read file: {}, lines {}-{}", fileRange.getPath(), fileRange.getStartLine(), endLine);
                
            } catch (Exception e) {
                log.error("Error reading file context: {}", fileRange.getPath(), e);
                results.add(String.format("\n=== %s (ERROR) ===\n%s\n", fileRange.getPath(), e.getMessage()));
            }
        }
        
        String output = String.join("\n", results);
        return new TerminalCommandResponse(output, "", 0, getRelativePath(rootPath, cwdPath));
    }

    @Override
    public TerminalCommandResponse modifyFile(Long userId, FileModifyRequest request, String relativeCwd) {
        log.info("=== Modify File ===");
        log.info("User: {}, Path: {}, Operations: {}", userId, request.getPath(), request.getOperations().size());
        
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd);
        Path targetPath = resolvePath(rootPath, request.getPath());
        
        if (!targetPath.startsWith(rootPath)) {
            return new TerminalCommandResponse("", "Invalid path (out of sandbox)", 1, getRelativePath(rootPath, cwdPath));
        }
        
        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            return new TerminalCommandResponse("", "File not found: " + request.getPath(), 1, getRelativePath(rootPath, cwdPath));
        }
        
        try {
            List<String> lines = new ArrayList<>(Files.readAllLines(targetPath, StandardCharsets.UTF_8));
            
            // 按行号倒序处理操作，避免索引偏移
            request.getOperations().sort((a, b) -> Integer.compare(b.getStartLine(), a.getStartLine()));
            
            for (FileModifyRequest.ModifyOperation op : request.getOperations()) {
                int startIdx = op.getStartLine() - 1; // 转换为0索引
                
                log.info("Operation: type={}, startLine={}, endLine={}", 
                    op.getType(), op.getStartLine(), op.getEndLine());
                
                switch (op.getType()) {
                    case "delete":
                        if (op.getEndLine() != null && op.getEndLine() >= op.getStartLine()) {
                            int endIdx = Math.min(op.getEndLine(), lines.size());
                            lines.subList(startIdx, endIdx).clear();
                            log.info("Deleted lines {}-{}", op.getStartLine(), endIdx);
                        }
                        break;
                        
                    case "insert":
                        if (op.getContent() != null) {
                            String[] insertLines = op.getContent().split("\n");
                            for (int i = insertLines.length - 1; i >= 0; i--) {
                                lines.add(startIdx, insertLines[i]);
                            }
                            log.info("Inserted {} lines at line {}", insertLines.length, op.getStartLine());
                        }
                        break;
                        
                    case "replace":
                        if (op.getEndLine() != null && op.getEndLine() >= op.getStartLine() && op.getContent() != null) {
                            int endIdx = Math.min(op.getEndLine(), lines.size());
                            lines.subList(startIdx, endIdx).clear();
                            
                            String[] replaceLines = op.getContent().split("\n");
                            for (int i = replaceLines.length - 1; i >= 0; i--) {
                                lines.add(startIdx, replaceLines[i]);
                            }
                            log.info("Replaced lines {}-{} with {} lines", 
                                op.getStartLine(), endIdx, replaceLines.length);
                        }
                        break;
                        
                    default:
                        log.warn("Unknown operation type: {}", op.getType());
                }
            }
            
            Files.write(targetPath, lines, StandardCharsets.UTF_8);
            String output = String.format("File modified: %s (%d operations applied)", 
                getRelativePath(rootPath, targetPath), request.getOperations().size());
            
            log.info("File modification completed successfully");
            return new TerminalCommandResponse(output, "", 0, getRelativePath(rootPath, cwdPath));
            
        } catch (Exception e) {
            log.error("File modification failed", e);
            return new TerminalCommandResponse("", "Modification failed: " + e.getMessage(), -1, getRelativePath(rootPath, cwdPath));
        }
    }
    
    // Phase 4: ToolsService 需要的辅助方法实现
    
    @Override
    public List<String> listDirectory(Long userId, String relativePath, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path targetPath = resolvePath(rootPath, relativeCwd != null ? relativeCwd : "/");
        
        if (relativePath != null && !relativePath.isEmpty()) {
            String safePath = relativePath.replace("..", "");
            if (safePath.startsWith("/") || safePath.startsWith("\\")) {
                safePath = safePath.substring(1);
            }
            targetPath = targetPath.resolve(safePath).normalize();
        }
        
        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            throw new RuntimeException("Directory not found: " + relativePath);
        }
        
        List<String> files = new ArrayList<>();
        try {
            Files.list(targetPath).forEach(path -> {
                files.add(path.getFileName().toString());
            });
        } catch (IOException e) {
            log.error("Error listing directory", e);
            throw new RuntimeException("Could not list directory: " + e.getMessage());
        }
        return files;
    }
    
    @Override
    public String getDirectoryTree(Long userId, String relativePath, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path targetPath = resolvePath(rootPath, relativeCwd != null ? relativeCwd : "/");
        
        if (relativePath != null && !relativePath.isEmpty()) {
            String safePath = relativePath.replace("..", "");
            if (safePath.startsWith("/") || safePath.startsWith("\\")) {
                safePath = safePath.substring(1);
            }
            targetPath = targetPath.resolve(safePath).normalize();
        }
        
        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            throw new RuntimeException("Directory not found: " + relativePath);
        }
        
        StringBuilder tree = new StringBuilder();
        try {
            buildTree(targetPath, rootPath, tree, "", true);
        } catch (IOException e) {
            log.error("Error building directory tree", e);
            throw new RuntimeException("Could not build directory tree: " + e.getMessage());
        }
        return tree.toString();
    }
    
    private void buildTree(Path dir, Path rootPath, StringBuilder tree, String prefix, boolean isLast) throws IOException {
        String name = dir.getFileName() != null ? dir.getFileName().toString() : dir.toString();
        tree.append(prefix).append(isLast ? "└── " : "├── ").append(name).append("\n");
        
        String newPrefix = prefix + (isLast ? "    " : "│   ");
        if (Files.isDirectory(dir)) {
            List<Path> children = Files.list(dir).sorted().collect(java.util.stream.Collectors.toList());
            for (int i = 0; i < children.size(); i++) {
                boolean last = (i == children.size() - 1);
                buildTree(children.get(i), rootPath, tree, newPrefix, last);
            }
        }
    }
    
    @Override
    public void createDirectory(Long userId, String relativePath, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd != null ? relativeCwd : "/");
        
        String safePath = relativePath.replace("..", "");
        if (safePath.startsWith("/") || safePath.startsWith("\\")) {
            safePath = safePath.substring(1);
        }
        
        Path target = cwdPath.resolve(safePath).normalize();
        if (!target.startsWith(rootPath)) {
            throw new RuntimeException("Invalid path (out of sandbox)");
        }
        
        try {
            checkDepth(rootPath, target);
            Files.createDirectories(target);
        } catch (IOException e) {
            log.error("Error creating directory", e);
            throw new RuntimeException("Could not create directory: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteFileOrFolder(Long userId, String relativePath, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd != null ? relativeCwd : "/");
        
        String safePath = relativePath.replace("..", "");
        if (safePath.startsWith("/") || safePath.startsWith("\\")) {
            safePath = safePath.substring(1);
        }
        
        Path target = cwdPath.resolve(safePath).normalize();
        if (!target.startsWith(rootPath)) {
            throw new RuntimeException("Invalid path (out of sandbox)");
        }
        
        if (!Files.exists(target)) {
            throw new RuntimeException("File or folder not found: " + relativePath);
        }
        
        try {
            if (Files.isDirectory(target)) {
                deleteDirectory(target);
            } else {
                Files.delete(target);
            }
        } catch (IOException e) {
            log.error("Error deleting file/folder", e);
            throw new RuntimeException("Could not delete: " + e.getMessage());
        }
    }
    
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> entries = Files.list(dir)) {
                entries.forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            deleteDirectory(path);
                        } else {
                            Files.delete(path);
                        }
                    } catch (IOException e) {
                        log.error("Error deleting path: " + path, e);
                    }
                });
            }
        }
        Files.delete(dir);
    }
    
    @Override
    public List<String> searchFileNames(Long userId, String pattern, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd != null ? relativeCwd : "/");
        
        List<String> results = new ArrayList<>();
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        
        try {
            Files.walk(cwdPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (regex.matcher(fileName).find()) {
                        String relative = rootPath.relativize(path).toString().replace("\\", "/");
                        results.add("/" + relative);
                    }
                });
        } catch (IOException e) {
            log.error("Error searching file names", e);
            throw new RuntimeException("Could not search file names: " + e.getMessage());
        }
        
        return results;
    }
    
    @Override
    public java.util.Map<String, List<String>> searchInFiles(Long userId, String pattern, String relativeCwd) {
        String userRoot = getUserTerminalRoot(userId);
        Path rootPath = Paths.get(userRoot);
        Path cwdPath = resolvePath(rootPath, relativeCwd != null ? relativeCwd : "/");
        
        java.util.Map<String, List<String>> results = new java.util.HashMap<>();
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        
        try {
            Files.walk(cwdPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String content = Files.readString(path, StandardCharsets.UTF_8);
                        String[] lines = content.split("\n");
                        List<String> matchedLines = new ArrayList<>();
                        
                        for (int i = 0; i < lines.length; i++) {
                            if (regex.matcher(lines[i]).find()) {
                                matchedLines.add(String.format("Line %d: %s", i + 1, lines[i]));
                            }
                        }
                        
                        if (!matchedLines.isEmpty()) {
                            String relative = rootPath.relativize(path).toString().replace("\\", "/");
                            results.put("/" + relative, matchedLines);
                        }
                    } catch (IOException e) {
                        log.error("Error reading file: " + path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error searching in files", e);
            throw new RuntimeException("Could not search in files: " + e.getMessage());
        }
        
        return results;
    }
}
