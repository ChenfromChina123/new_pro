package com.aispring.service;

import com.aispring.entity.User;
import com.aispring.entity.UserFile;
import com.aispring.entity.UserFolder;
import com.aispring.repository.UserFileRepository;
import com.aispring.repository.UserFolderRepository;
import com.aispring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.aispring.config.StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 云盘服务
 * 对应Python: app.py中的云盘相关功能
 */
@Service
@RequiredArgsConstructor
public class CloudDiskService {
    
    private final UserFileRepository userFileRepository;
    private final UserFolderRepository userFolderRepository;
    private final UserRepository userRepository;
    private final StorageProperties storageProperties;

    private String getCloudDiskAbsolutePath() {
        return storageProperties.getCloudDiskAbsolute();
    }

    private void ensureUserDirectoryExists(Long userId) {
        try {
            Path userBase = Paths.get(getCloudDiskAbsolutePath()).resolve(String.valueOf(userId)).normalize();
            Files.createDirectories(userBase);
        } catch (Exception ignore) {}
    }
    
    private String normalizePath(String p) {
        if (p == null) return "";
        String s = p.trim();
        if (s.equals("/") || s.equals("")) return "";
        // 去掉首尾斜杠
        while (s.startsWith("/")) s = s.substring(1);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
    
    /**
     * 初始化用户文件夹结构
     */
    @Transactional(rollbackFor = Exception.class)
    public void initUserFolderStructure(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        // 检查根目录是否已存在，不存在则创建
        if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, "/")) {
            UserFolder rootFolder = new UserFolder();
            rootFolder.setUser(user);
            rootFolder.setFolderName("根目录");
            rootFolder.setFolderPath("/");
            rootFolder.setParentPath(null);
            userFolderRepository.save(rootFolder);
        }
        
        // 检查笔记文件夹是否已存在，不存在则创建
        if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, "/笔记")) {
            UserFolder notesFolder = new UserFolder();
            notesFolder.setUser(user);
            notesFolder.setFolderName("笔记");
            notesFolder.setFolderPath("/笔记");
            notesFolder.setParentPath("/");
            userFolderRepository.save(notesFolder);
        }
        
        // 创建物理文件夹
        ensureUserDirectoryExists(userId);
        String userDiskPath = getCloudDiskAbsolutePath() + "/" + userId;
        new File(userDiskPath + "/笔记").mkdirs();

        // 同步文件和文件夹结构
        synchronizeUserFilesAndFolders(userId);
    }
    
    /**
     * 获取用户的文件夹树
     */
    public List<UserFolder> getUserFolders(Long userId) {
        // 先通过JPA方法获取文件夹列表，再添加额外过滤确保只返回当前用户的文件夹
        return userFolderRepository.findByUser_IdOrderByIdAsc(userId)
                .stream()
                // 双重验证：确保文件夹的user_id与当前用户ID匹配
                .filter(folder -> folder.getUser().getId().equals(userId))
                .toList();
    }
    
    /**
     * 创建文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public UserFolder createFolder(Long userId, String folderName, String folderPath, Long parentId) {
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件夹名称不能为空");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        // 清理和验证folderPath
        String normalizedFolderPath;
        if (folderPath == null || folderPath.isEmpty() || folderPath.equals("-") || folderPath.equals("null")) {
            // 无效或空的folderPath，使用根目录
            normalizedFolderPath = "/";
        } else {
            // 确保路径以/开头
            normalizedFolderPath = folderPath.startsWith("/") ? folderPath : "/" + folderPath;
        }
        
        // 构建新文件夹的完整路径
        String fullFolderPath;
        if (normalizedFolderPath.equals("/")) {
            // 根目录下创建文件夹
            fullFolderPath = "/" + folderName;
        } else {
            // 子目录下创建文件夹
            fullFolderPath = normalizedFolderPath.endsWith("/") ? normalizedFolderPath + folderName : normalizedFolderPath + "/" + folderName;
        }
        
        // 检查是否已存在同名文件夹
        if (userFolderRepository.existsByUser_IdAndFolderPath(userId, fullFolderPath)) {
            throw new IllegalArgumentException("该文件夹已存在");
        }
        
        UserFolder folder = new UserFolder();
        folder.setUser(user);
        folder.setFolderName(folderName);
        folder.setFolderPath(fullFolderPath);
        
        // 构建父路径
        String parentPath = fullFolderPath.substring(0, fullFolderPath.lastIndexOf('/'));
        if (parentPath.isEmpty()) {
            parentPath = "/";
        }
        folder.setParentPath(parentPath);
        
        folder = userFolderRepository.save(folder);
        
        // 创建物理文件夹
        String physicalPath = getCloudDiskAbsolutePath() + "/" + userId + fullFolderPath;
        new File(physicalPath).mkdirs();
        
        return folder;
    }
    
    /**
     * 删除文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long userId, Long folderId) throws IOException {
        UserFolder targetFolder = userFolderRepository.findByIdAndUser_Id(folderId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件夹不存在"));
            
        if (targetFolder.getFolderPath().equals("/")) {
            throw new IllegalArgumentException("不能删除根目录");
        }
        
        // 1. 找出所有子文件夹
        String prefix = targetFolder.getFolderPath().endsWith("/") ? targetFolder.getFolderPath() : targetFolder.getFolderPath() + "/";
        List<UserFolder> subFolders = userFolderRepository.findByUser_IdAndFolderPathStartingWith(userId, prefix);
        
        // 2. 将目标文件夹也加入列表
        List<UserFolder> allFoldersToDelete = new ArrayList<>(subFolders);
        allFoldersToDelete.add(targetFolder);
        
        // 3. 按路径长度倒序排序，确保先删除子文件夹（深层路径更长）
        allFoldersToDelete.sort((f1, f2) -> f2.getFolderPath().length() - f1.getFolderPath().length());
        
        // 4. 依次删除
        for (UserFolder folder : allFoldersToDelete) {
            // 删除文件夹中的所有文件
            List<UserFile> files = userFileRepository.findByUser_IdAndFolderPathOrderByUploadTimeDesc(userId, folder.getFolderPath());
            for (UserFile file : files) {
                deleteFile(userId, file.getId());
            }
            
            // 删除物理文件夹
            String physicalPath = getCloudDiskAbsolutePath() + "/" + userId + folder.getFolderPath();
            Path path = Paths.get(physicalPath);
            if (Files.exists(path)) {
                // 递归删除物理文件夹（包含可能存在的残留文件）
                try (java.util.stream.Stream<Path> walk = Files.walk(path)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                }
            }
            
            // 删除数据库记录
            userFolderRepository.delete(folder);
        }
    }
    
    /**
     * 上传文件
     * @param conflictStrategy 冲突处理策略: RENAME (默认), OVERWRITE
     */
    @Transactional(rollbackFor = Exception.class)
    public UserFile uploadFile(Long userId, Long folderId, String folderPath, 
                               MultipartFile file, String conflictStrategy) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "unknown_file";
        
        // 规范化 folderPath (用于保存到数据库)
        String saveFolderPath = (folderPath != null && !folderPath.isEmpty()) 
            ? (folderPath.startsWith("/") ? folderPath : "/" + folderPath) 
            : "/";
        // 确保不以 / 结尾（除了根目录）
        if (saveFolderPath.length() > 1 && saveFolderPath.endsWith("/")) {
            saveFolderPath = saveFolderPath.substring(0, saveFolderPath.length() - 1);
        }

        // 检查文件是否存在 (使用更稳健的内存匹配，兼容不同的路径格式)
        String checkPath = normalizePath(folderPath);
        List<UserFile> allFiles = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
        
        String currentFilename = originalFilename;
        Optional<UserFile> existingFileOpt = allFiles.stream()
            .filter(f -> normalizePath(f.getFolderPath()).equals(checkPath) && 
                         (f.getFilename() != null && f.getFilename().equals(currentFilename)))
            .findFirst();

        if (existingFileOpt.isPresent()) {
            if ("OVERWRITE".equalsIgnoreCase(conflictStrategy)) {
                // 覆盖模式
                UserFile existingFile = existingFileOpt.get();
                
                // 删除旧的物理文件
                String oldRelPath = existingFile.getFilepath();
                String oldPhysicalPath = getCloudDiskAbsolutePath() + "/" + userId + (oldRelPath.startsWith("/") ? oldRelPath : "/" + oldRelPath);
                try { Files.deleteIfExists(Paths.get(oldPhysicalPath)); } catch (Exception ignore) {}
                
                // 保存新物理文件
                String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
                String uniqueFilename = UUID.randomUUID().toString() + extension;
                
                String userDiskPath = getCloudDiskAbsolutePath() + "/" + userId + saveFolderPath;
                new File(userDiskPath).mkdirs();
                
                String filePath = userDiskPath + "/" + uniqueFilename;
                Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
                
                // 更新记录
                String fileRelPath = saveFolderPath.equals("/") ? ("/" + uniqueFilename) : (saveFolderPath + "/" + uniqueFilename);
                existingFile.setFilepath(fileRelPath);
                existingFile.setFileSize(file.getSize());
                existingFile.setUploadTime(LocalDateTime.now());
                existingFile.setFolderPath(saveFolderPath); // 确保更新folderPath
                
                String mime = file.getContentType();
                if (mime == null || mime.isEmpty()) {
                    try { mime = Files.probeContentType(Paths.get(filePath)); } catch (Exception ignore) {}
                }
                existingFile.setFileType(mime);
                
                return userFileRepository.save(existingFile);
                
            } else {
                // 智能重命名模式 (默认)
                String nameWithoutExt = originalFilename.contains(".") ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) : originalFilename;
                String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
                
                int counter = 1;
                String newName = originalFilename;
                
                // 在内存中检查重名
                while (true) {
                    String candidate = newName;
                    boolean exists = allFiles.stream().anyMatch(f -> 
                        normalizePath(f.getFolderPath()).equals(checkPath) && 
                        (f.getFilename() != null && f.getFilename().equals(candidate))
                    );
                    if (!exists) break;
                    
                    newName = nameWithoutExt + "(" + counter + ")" + ext;
                    counter++;
                }
                originalFilename = newName;
                // 继续执行下方的保存逻辑，使用新的 originalFilename
            }
        }

        // 生成唯一文件名
        String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // 保存物理文件
        String userDiskPath = getCloudDiskAbsolutePath() + "/" + userId + saveFolderPath;
        new File(userDiskPath).mkdirs();
        
        String filePath = userDiskPath + "/" + uniqueFilename;
        Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        UserFile userFile = new UserFile();
        userFile.setUser(user);
        userFile.setFilename(originalFilename);
        userFile.setOriginalFilename(originalFilename);
        String fileRelPath = saveFolderPath.equals("/") ? ("/" + uniqueFilename) : (saveFolderPath + "/" + uniqueFilename);
        userFile.setFilepath(fileRelPath);
        userFile.setFolderPath(saveFolderPath); // 显式设置 folderPath

        userFile.setFileSize(file.getSize());
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            try {
                contentType = Files.probeContentType(Paths.get(filePath));
            } catch (Exception ignore) {}
        }
        if (contentType != null) {
            int semi = contentType.indexOf(';');
            if (semi > 0) {
                contentType = contentType.substring(0, semi).trim();
            }
            if (contentType.length() > 50) {
                contentType = contentType.substring(0, 50);
            }
        }
        userFile.setFileType(contentType);
        userFile.setFolderPath(folderPath);
        return userFileRepository.save(userFile);
    }
    
    /**
     * 获取用户的所有文件
     */
    public List<UserFile> getUserFiles(Long userId, Long folderId, String folderPath) {
        if (folderId != null) {
            UserFolder folder = userFolderRepository.findByIdAndUser_Id(folderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("文件夹不存在"));
            String targetPath = folder.getFolderPath();
            List<UserFile> all = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
            return all.stream().filter(f -> targetPath.equals(f.getFolderPath())).toList();
        } else if (folderPath != null) {
            // 直接根据folderPath过滤文件
            String norm = normalizePath(folderPath);
            List<UserFile> all = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
            return all.stream().filter(f -> {
                String fp = normalizePath(f.getFolderPath());
                return fp.equals(norm);
            }).toList();
        }
        return userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserFile renameFile(Long userId, Long fileId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("新文件名称不能为空");
        }
        UserFile file = userFileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        String folderPath = file.getFolderPath();
        List<UserFile> siblings = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
        boolean exists = siblings.stream().anyMatch(f -> {
            boolean sameFolder = normalizePath(f.getFolderPath()).equals(normalizePath(folderPath));
            String a = f.getFilename() != null ? f.getFilename().trim() : "";
            String b = newName.trim();
            boolean sameName = a.equalsIgnoreCase(b);
            return sameFolder && sameName && !f.getId().equals(file.getId());
        });
        if (exists) {
            String rel = file.getFilepath();
            String unique = rel != null && rel.contains("/") ? rel.substring(rel.lastIndexOf('/') + 1) : rel;
            String base = getCloudDiskAbsolutePath() + "/" + userId;
            java.nio.file.Path tempDir = java.nio.file.Paths.get(base + "/__temp").normalize();
            try { java.nio.file.Files.createDirectories(tempDir); } catch (Exception ignore) {}
            java.nio.file.Path oldPath = java.nio.file.Paths.get(base + (rel != null && rel.startsWith("/") ? rel : ("/" + rel))).normalize();
            java.nio.file.Path tempPath = tempDir.resolve(unique == null ? java.util.UUID.randomUUID().toString() : unique).normalize();
            try { java.nio.file.Files.move(oldPath, tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignore) {}
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("conflict", true);
            payload.put("tempRelPath", "/__temp/" + (unique == null ? tempPath.getFileName().toString() : unique));
            payload.put("originalRelPath", rel);
            payload.put("originalFolderPath", folderPath);
            payload.put("desiredName", newName.trim());
            payload.put("fileId", file.getId());
            throw new RuntimeException("RENAME_CONFLICT:" + new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(payload).toString());
        }
        file.setFilename(newName.trim());
        return userFileRepository.save(file);
    }

    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, Object> startRenameFile(Long userId, Long fileId, String newName) throws IOException {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("新文件名称不能为空");
        }
        UserFile file = userFileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        String folderPath = file.getFolderPath();
        List<UserFile> siblings = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
        boolean exists = siblings.stream().anyMatch(f -> {
            boolean sameFolder = normalizePath(f.getFolderPath()).equals(normalizePath(folderPath));
            String a = f.getFilename() != null ? f.getFilename().trim() : "";
            String b = newName.trim();
            boolean sameName = a.equalsIgnoreCase(b);
            return sameFolder && sameName && !f.getId().equals(file.getId());
        });
        if (!exists) {
            file.setFilename(newName.trim());
            UserFile saved = userFileRepository.save(file);
            java.util.Map<String, Object> ok = new java.util.HashMap<>();
            ok.put("conflict", false);
            ok.put("file", saved);
            return ok;
        }
        String rel = file.getFilepath();
        String unique = rel != null && rel.contains("/") ? rel.substring(rel.lastIndexOf('/') + 1) : rel;
        String base = getCloudDiskAbsolutePath() + "/" + userId;
        java.nio.file.Path tempDir = java.nio.file.Paths.get(base + "/__temp").normalize();
        Files.createDirectories(tempDir);
        java.nio.file.Path oldPath = java.nio.file.Paths.get(base + (rel != null && rel.startsWith("/") ? rel : ("/" + rel))).normalize();
        java.nio.file.Path tempPath = tempDir.resolve(unique == null ? java.util.UUID.randomUUID().toString() : unique).normalize();
        Files.move(oldPath, tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("conflict", true);
        payload.put("tempRelPath", "/__temp/" + (unique == null ? tempPath.getFileName().toString() : unique));
        payload.put("originalRelPath", rel);
        payload.put("originalFolderPath", folderPath);
        payload.put("desiredName", newName.trim());
        payload.put("fileId", file.getId());
        return payload;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserFile resolveRenameFile(Long userId, Long fileId, String action, String finalName) throws java.io.IOException {
        if (finalName == null || finalName.trim().isEmpty()) {
            throw new IllegalArgumentException("新文件名称不能为空");
        }
        UserFile file = userFileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        String folderPath = file.getFolderPath();
        String rel = file.getFilepath();
        String unique = rel != null && rel.contains("/") ? rel.substring(rel.lastIndexOf('/') + 1) : rel;
        String base = getCloudDiskAbsolutePath() + "/" + userId;
        java.nio.file.Path tempPath = java.nio.file.Paths.get(base + "/__temp/" + unique).normalize();
        java.nio.file.Path targetDir = java.nio.file.Paths.get(base + (folderPath != null && folderPath.startsWith("/") ? folderPath : ("/" + folderPath))).normalize();
        Files.createDirectories(targetDir);

        if ("override".equalsIgnoreCase(action)) {
            List<UserFile> all = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
            for (UserFile other : all) {
                boolean sameFolder = normalizePath(other.getFolderPath()).equals(normalizePath(folderPath));
                boolean sameName = (other.getFilename() != null ? other.getFilename().trim() : "").equals(finalName.trim());
                if (sameFolder && sameName && !other.getId().equals(file.getId())) {
                    String orel = other.getFilepath();
                    java.nio.file.Path oPath = java.nio.file.Paths.get(base + (orel != null && orel.startsWith("/") ? orel : ("/" + orel))).normalize();
                    if (java.nio.file.Files.exists(oPath)) java.nio.file.Files.delete(oPath);
                    userFileRepository.delete(other);
                    break;
                }
            }
        } else {
            List<UserFile> all = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
            String baseName = finalName.trim();
            boolean exists = all.stream().anyMatch(f -> {
                boolean sameFolder = normalizePath(f.getFolderPath()).equals(normalizePath(folderPath));
                String a = f.getFilename() != null ? f.getFilename().trim() : "";
                boolean sameName = a.equalsIgnoreCase(baseName);
                return sameFolder && sameName && !f.getId().equals(file.getId());
            });
            if (exists) {
                String name = baseName;
                int i = 1;
                while (true) {
                    String candidate = baseName + i;
                    boolean ok = all.stream().noneMatch(f -> {
                        boolean sameFolder = normalizePath(f.getFolderPath()).equals(normalizePath(folderPath));
                        String a = f.getFilename() != null ? f.getFilename().trim() : "";
                        boolean sameName = a.equalsIgnoreCase(candidate);
                        return sameFolder && sameName && !f.getId().equals(file.getId());
                    });
                    if (ok) { name = candidate; break; }
                    i++;
                }
                finalName = name;
            }
        }

        java.nio.file.Path targetPath = targetDir.resolve(unique).normalize();
        if (java.nio.file.Files.exists(tempPath)) java.nio.file.Files.move(tempPath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        file.setFilename(finalName.trim());
        return userFileRepository.save(file);
    }
    
    /**
     * 下载文件
     */
    public Path downloadFile(Long userId, Long fileId) throws IOException {
        UserFile file = userFileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        ensureUserDirectoryExists(userId);
        String rel = file.getFilepath();
        String relNorm = (rel != null && rel.startsWith("/")) ? rel.substring(1) : rel;
        Path cwd = Paths.get(System.getProperty("user.dir")).normalize();
        Path p1 = cwd.resolve("cloud_disk").resolve(String.valueOf(userId)).resolve(relNorm == null ? "" : relNorm).normalize();
        Path p2 = cwd.getParent() != null ? cwd.getParent().resolve("cloud_disk").resolve(String.valueOf(userId)).resolve(relNorm == null ? "" : relNorm).normalize() : p1;
        Path p3 = (cwd.getParent() != null && cwd.getParent().getParent() != null) ? cwd.getParent().getParent().resolve("cloud_disk").resolve(String.valueOf(userId)).resolve(relNorm == null ? "" : relNorm).normalize() : p2;
        Path chosen = Files.exists(p1) ? p1 : (Files.exists(p2) ? p2 : p3);
        if (!Files.exists(chosen) || !Files.isRegularFile(chosen)) {
            String physicalPath = getCloudDiskAbsolutePath() + "/" + userId + (rel != null && rel.startsWith("/") ? rel : ("/" + rel));
            Path fallback = Paths.get(physicalPath).normalize();
            if (!Files.exists(fallback) || !Files.isRegularFile(fallback)) {
                throw new IOException("文件不存在或已被删除: " + fallback);
            }
            return fallback;
        }
        return chosen;
    }
    
    /**
     * 删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long userId, Long fileId) throws IOException {
        UserFile file = userFileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        
        // 删除物理文件
        String relDel = file.getFilepath();
        String physicalPath = getCloudDiskAbsolutePath() + "/" + userId + (relDel != null && relDel.startsWith("/") ? relDel : ("/" + relDel));
        Path path = Paths.get(physicalPath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
        
        // 删除数据库记录
        userFileRepository.delete(file);
    }
    
    /**
     * 移动文件
     */
    @Transactional(rollbackFor = Exception.class)
    public UserFile moveFile(Long userId, Long fileId, Long targetFolderId, String targetPath) throws IOException {
        UserFile file = userFileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        
        String oldRel = file.getFilepath();
        String oldPhysicalPath = getCloudDiskAbsolutePath() + "/" + userId + (oldRel != null && oldRel.startsWith("/") ? oldRel : ("/" + oldRel));
        String filename = file.getFilepath().substring(file.getFilepath().lastIndexOf("/") + 1);
        String newFilePath = (targetPath != null && targetPath.endsWith("/")) ? (targetPath + filename) : (targetPath + "/" + filename);
        String newPhysicalPath = getCloudDiskAbsolutePath() + "/" + userId + (newFilePath.startsWith("/") ? newFilePath : ("/" + newFilePath));
        try { Files.createDirectories(Paths.get(newPhysicalPath).normalize().getParent()); } catch (Exception ignore) {}
        
        // 移动物理文件
        Files.move(Paths.get(oldPhysicalPath), Paths.get(newPhysicalPath), StandardCopyOption.REPLACE_EXISTING);
        
        // 更新数据库记录
        file.setFolderPath(targetPath);
        file.setFilepath(newFilePath);
        
        return userFileRepository.save(file);
    }

    public Path zipFolder(Long userId, String folderPath) throws IOException {
        ensureUserDirectoryExists(userId);
        String base = getCloudDiskAbsolutePath() + "/" + userId;
        String rel = (folderPath != null && !folderPath.isEmpty()) ? (folderPath.startsWith("/") ? folderPath : "/" + folderPath) : "";
        Path start = Paths.get(base + (rel.isEmpty() ? "" : rel)).normalize();
        if (!Files.exists(start) || !Files.isDirectory(start)) {
            throw new IOException("目录不存在: " + start);
        }
        Path tempZip = Files.createTempFile("cloud-folder-" + userId + "-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
            Path root = start;
            Files.walk(root).forEach(p -> {
                String entryName = root.relativize(p).toString().replace("\\", "/");
                try {
                    if (Files.isDirectory(p)) {
                        if (!entryName.isEmpty()) {
                            if (!entryName.endsWith("/")) {
                                entryName = entryName + "/";
                            }
                            zos.putNextEntry(new ZipEntry(entryName));
                            zos.closeEntry();
                        }
                    } else {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(p, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                }
            });
        }
        tempZip.toFile().deleteOnExit();
        return tempZip;
    }

    /**
     * 上传并解压文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public int uploadFolderZip(Long userId, String folderPath, MultipartFile zipFile) throws IOException {
        ensureUserDirectoryExists(userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        String base = getCloudDiskAbsolutePath() + "/" + userId;
        String normParam = (folderPath != null && !folderPath.isEmpty()) ? (folderPath.startsWith("/") ? folderPath : "/" + folderPath) : "/";
        Path targetBase = Paths.get(base + (normParam.equals("/") ? "" : normParam)).normalize();
        try { Files.createDirectories(targetBase); } catch (Exception ignore) {}

        // 确保目标文件夹在数据库中存在
        if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, normParam)) {
            String name = normParam.equals("/") ? "根目录" : normParam.substring(normParam.lastIndexOf('/') + 1);
            UserFolder f = new UserFolder();
            f.setUser(user);
            f.setFolderName(name);
            f.setFolderPath(normParam);
            String parentPath = normParam.equals("/") ? null : normParam.substring(0, normParam.lastIndexOf('/')).isEmpty() ? "/" : normParam.substring(0, normParam.lastIndexOf('/'));
            f.setParentPath(parentPath);
            userFolderRepository.save(f);
        }

        int count = 0;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(zipFile.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().replace("\\", "/");
                if (entry.isDirectory()) {
                    Path dir = targetBase.resolve(entryName).normalize();
                    try { Files.createDirectories(dir); } catch (Exception ignore) {}
                    String fullFolderPath = normParam.equals("/") ? "/" + entryName.replaceAll("/+", "/").replaceAll("/$", "") : (normParam + (normParam.endsWith("/") ? "" : "/") + entryName.replaceAll("/+", "/").replaceAll("/$", ""));
                    if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, fullFolderPath)) {
                        UserFolder sub = new UserFolder();
                        sub.setUser(user);
                        String folderName = entryName.replaceAll("/$", "");
                        folderName = folderName.contains("/") ? folderName.substring(folderName.lastIndexOf('/') + 1) : folderName;
                        sub.setFolderName(folderName);
                        sub.setFolderPath(fullFolderPath);
                        String parentPath = fullFolderPath.substring(0, fullFolderPath.lastIndexOf('/'));
                        if (parentPath.isEmpty()) parentPath = "/";
                        sub.setParentPath(parentPath);
                        userFolderRepository.save(sub);
                    }
                } else {
                    Path targetDir = targetBase.resolve(entryName).normalize().getParent();
                    if (targetDir != null) {
                        try { Files.createDirectories(targetDir); } catch (Exception ignore) {}
                    }
                    String originalFilename = entryName.contains("/") ? entryName.substring(entryName.lastIndexOf('/') + 1) : entryName;
                    String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
                    String uniqueFilename = java.util.UUID.randomUUID().toString() + (extension.isEmpty() ? "" : extension);
                    Path physicalTarget = (targetDir == null ? targetBase : targetDir).resolve(uniqueFilename).normalize();
                    Files.copy(zis, physicalTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    String relDir = entryName.contains("/") ? entryName.substring(0, entryName.lastIndexOf('/')) : "";
                    String fullFolderPath = normParam.equals("/") ? (relDir.isEmpty() ? "/" : "/" + relDir) : (normParam + (normParam.endsWith("/") ? "" : "/") + relDir);
                    fullFolderPath = fullFolderPath.replaceAll("/+", "/");
                    if (!fullFolderPath.startsWith("/")) fullFolderPath = "/" + fullFolderPath;
                    if (fullFolderPath.endsWith("/")) fullFolderPath = fullFolderPath.substring(0, fullFolderPath.length() - 1);
                    if (fullFolderPath.isEmpty()) fullFolderPath = "/";

                    if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, fullFolderPath)) {
                        UserFolder sub = new UserFolder();
                        sub.setUser(user);
                        String folderName = fullFolderPath.substring(fullFolderPath.lastIndexOf('/') + 1);
                        sub.setFolderName(folderName.isEmpty() ? "根目录" : folderName);
                        sub.setFolderPath(fullFolderPath);
                        String parentPath = fullFolderPath.substring(0, fullFolderPath.lastIndexOf('/'));
                        if (parentPath.isEmpty()) parentPath = "/";
                        sub.setParentPath(parentPath);
                        userFolderRepository.save(sub);
                    }

                    UserFile uf = new UserFile();
                    uf.setUser(user);
                    uf.setFilename(originalFilename);
                    uf.setOriginalFilename(originalFilename);
                    String relFilePath = fullFolderPath.endsWith("/") ? (fullFolderPath + uniqueFilename) : (fullFolderPath + "/" + uniqueFilename);
                    uf.setFilepath(relFilePath);
                    try { uf.setFileSize(Files.size(physicalTarget)); } catch (Exception e) { uf.setFileSize(0L); }
                    String mime = null;
                    try { mime = Files.probeContentType(physicalTarget); } catch (Exception ignore) {}
                    if (mime != null) {
                        int semi = mime.indexOf(';');
                        if (semi > 0) mime = mime.substring(0, semi).trim();
                        if (mime.length() > 50) mime = mime.substring(0, 50);
                    }
                    uf.setFileType(mime);
                    uf.setFolderPath(fullFolderPath);
                    userFileRepository.save(uf);
                    count++;
                }
                zis.closeEntry();
            }
        }
        return count;
    }

    @Transactional
    public int uploadFolderStream(Long userId, String folderPath, MultipartFile[] files, String[] paths) throws IOException {
        ensureUserDirectoryExists(userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String base = getCloudDiskAbsolutePath() + "/" + userId;
        String normParam = (folderPath != null && !folderPath.isEmpty()) ? (folderPath.startsWith("/") ? folderPath : "/" + folderPath) : "/";
        Path targetBase = Paths.get(base + (normParam.equals("/") ? "" : normParam)).normalize();
        try { Files.createDirectories(targetBase); } catch (Exception ignore) {}

        if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, normParam)) {
            String name = normParam.equals("/") ? "根目录" : normParam.substring(normParam.lastIndexOf('/') + 1);
            UserFolder f = new UserFolder();
            f.setUser(user);
            f.setFolderName(name);
            f.setFolderPath(normParam);
            String parentPath = normParam.equals("/") ? null : normParam.substring(0, normParam.lastIndexOf('/')).isEmpty() ? "/" : normParam.substring(0, normParam.lastIndexOf('/'));
            f.setParentPath(parentPath);
            userFolderRepository.save(f);
        }

        int count = 0;
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String entryName = (paths != null && paths.length > i && paths[i] != null) ? paths[i] : file.getOriginalFilename();
            entryName = entryName.replace("\\", "/");
            String relDir = entryName.contains("/") ? entryName.substring(0, entryName.lastIndexOf('/')) : "";
            Path targetDir = relDir.isEmpty() ? targetBase : targetBase.resolve(relDir).normalize();
            try { Files.createDirectories(targetDir); } catch (Exception ignore) {}

            String fullFolderPath = normParam.equals("/") ? (relDir.isEmpty() ? "/" : "/" + relDir) : (normParam + (normParam.endsWith("/") ? "" : "/") + relDir);
            fullFolderPath = fullFolderPath.replaceAll("/+", "/");
            if (!fullFolderPath.startsWith("/")) fullFolderPath = "/" + fullFolderPath;
            if (fullFolderPath.endsWith("/")) fullFolderPath = fullFolderPath.substring(0, fullFolderPath.length() - 1);
            if (fullFolderPath.isEmpty()) fullFolderPath = "/";

            if (!userFolderRepository.existsByUser_IdAndFolderPath(userId, fullFolderPath)) {
                UserFolder sub = new UserFolder();
                sub.setUser(user);
                String folderName = fullFolderPath.substring(fullFolderPath.lastIndexOf('/') + 1);
                sub.setFolderName(folderName.isEmpty() ? "根目录" : folderName);
                sub.setFolderPath(fullFolderPath);
                String parentPath = fullFolderPath.substring(0, fullFolderPath.lastIndexOf('/'));
                if (parentPath.isEmpty()) parentPath = "/";
                sub.setParentPath(parentPath);
                userFolderRepository.save(sub);
            }

            String originalFilename = entryName.contains("/") ? entryName.substring(entryName.lastIndexOf('/') + 1) : entryName;
            String extension = (originalFilename != null && originalFilename.contains(".")) ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
            String uniqueFilename = java.util.UUID.randomUUID().toString() + (extension.isEmpty() ? "" : extension);
            Path physicalTarget = targetDir.resolve(uniqueFilename).normalize();
            Files.copy(file.getInputStream(), physicalTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            UserFile uf = new UserFile();
            uf.setUser(user);
            uf.setFilename(originalFilename);
            uf.setOriginalFilename(originalFilename);
            String relFilePath = fullFolderPath.endsWith("/") ? (fullFolderPath + uniqueFilename) : (fullFolderPath + "/" + uniqueFilename);
            uf.setFilepath(relFilePath);
            try { uf.setFileSize(Files.size(physicalTarget)); } catch (Exception e) { uf.setFileSize(0L); }
            String mime = file.getContentType();
            if (mime == null || mime.isEmpty()) {
                try { mime = Files.probeContentType(physicalTarget); } catch (Exception ignore) {}
            }
            if (mime != null) {
                int semi = mime.indexOf(';');
                if (semi > 0) mime = mime.substring(0, semi).trim();
                if (mime.length() > 50) mime = mime.substring(0, 50);
            }
            uf.setFileType(mime);
            uf.setFolderPath(fullFolderPath);
            userFileRepository.save(uf);
            count++;
        }
        return count;
    }

    @Transactional
    public void migrateToUnifiedBase() {
        Path unified = Paths.get(getCloudDiskAbsolutePath());
        try { Files.createDirectories(unified); } catch (Exception ignore) {}
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path oldBase1 = cwd.resolve("cloud_disk").normalize();
        Path oldBase2 = cwd.resolve("aispring").resolve("cloud_disk").normalize();
        List<UserFile> files = userFileRepository.findAll();
        for (UserFile f : files) {
            Long uid = f.getUser().getId();
            String rel = f.getFilepath();
            String target = unified.resolve(String.valueOf(uid)).resolve(rel.startsWith("/") ? rel.substring(1) : rel).toString();
            Path targetPath = Paths.get(target).normalize();
            if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
                continue;
            }
            String normRel = rel != null && rel.startsWith("/") ? rel.substring(1) : rel;
            if (normRel == null || normRel.isEmpty()) {
                continue;
            }
            String first = normRel.split("/")[0];
            String remain = normRel.substring(first.length());
            Path altInUnified = unified.resolve(String.valueOf(uid) + first).resolve(remain.startsWith("/") ? remain.substring(1) : remain).normalize();
            Path sourcePath = null;
            if (Files.exists(altInUnified)) {
                sourcePath = altInUnified;
            } else {
                Path altInOld1 = oldBase1.resolve(String.valueOf(uid) + first).resolve(remain.startsWith("/") ? remain.substring(1) : remain).normalize();
                Path altInOld2 = oldBase2.resolve(String.valueOf(uid) + first).resolve(remain.startsWith("/") ? remain.substring(1) : remain).normalize();
                if (Files.exists(altInOld1)) {
                    sourcePath = altInOld1;
                } else if (Files.exists(altInOld2)) {
                    sourcePath = altInOld2;
                }
            }
            if (sourcePath != null && Files.isRegularFile(sourcePath)) {
                try {
                    Files.createDirectories(targetPath.getParent());
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignore) {}
            }
        }
    }
    /**
     * 重命名文件夹
     */
    @Transactional(rollbackFor = Exception.class)
    public UserFolder renameFolder(Long userId, Long folderId, String newName) throws IOException {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("新文件夹名称不能为空");
        }
        UserFolder folder = userFolderRepository.findByIdAndUser_Id(folderId, userId)
            .orElseThrow(() -> new IllegalArgumentException("文件夹不存在"));
        
        String oldPath = folder.getFolderPath();
        String newPath = oldPath.substring(0, oldPath.lastIndexOf("/") + 1) + newName;

        // 重名检测
        if (userFolderRepository.existsByUser_IdAndFolderPath(userId, newPath)) {
            throw new IllegalArgumentException("同级目录已存在同名文件夹");
        }
        
        String oldPhysicalPath = getCloudDiskAbsolutePath() + "/" + userId + oldPath;
        String newPhysicalPath = getCloudDiskAbsolutePath() + "/" + userId + newPath;
        try { Files.createDirectories(Paths.get(newPhysicalPath).normalize().getParent()); } catch (Exception ignore) {}
        
        // 重命名物理文件夹
            Files.move(Paths.get(oldPhysicalPath), Paths.get(newPhysicalPath), StandardCopyOption.REPLACE_EXISTING);
            
            // 更新当前文件夹记录
        folder.setFolderName(newName);
        folder.setFolderPath(newPath);
        folder = userFolderRepository.save(folder);

        // 级联更新子文件夹与文件路径
        String prefix = oldPath.endsWith("/") ? oldPath : oldPath + "/";
        String targetPrefix = newPath.endsWith("/") ? newPath : newPath + "/";

        // 子文件夹
        List<UserFolder> subFolders = userFolderRepository.findByUser_IdAndFolderPathStartingWith(userId, prefix);
        for (UserFolder sub : subFolders) {
            String p = sub.getFolderPath();
            String updated = targetPrefix + p.substring(prefix.length());
            sub.setFolderPath(updated);
            String parent = updated.substring(0, updated.lastIndexOf('/'));
            if (parent.isEmpty()) parent = "/";
            sub.setParentPath(parent);
            userFolderRepository.save(sub);
        }

        // 文件
        List<UserFile> allFiles = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
        for (UserFile f : allFiles) {
            String fp = f.getFolderPath();
            if (fp != null && (fp.equals(oldPath) || fp.startsWith(prefix))) {
                String newFolderPath;
                if (fp.equals(oldPath)) {
                    newFolderPath = newPath;
                } else {
                    newFolderPath = targetPrefix + fp.substring(prefix.length());
                }
                String rel = f.getFilepath();
                String newRel;
                if (rel != null) {
                    String relPrefix = oldPath.endsWith("/") ? oldPath : oldPath + "/";
                    String targetRelPrefix = newPath.endsWith("/") ? newPath : newPath + "/";
                    if (rel.startsWith(relPrefix)) {
                        newRel = targetRelPrefix + rel.substring(relPrefix.length());
                    } else {
                        // 兼容旧数据：若不以旧前缀开头，仅替换父路径部分
                        int idx = rel.lastIndexOf('/');
                        String filename = idx >= 0 ? rel.substring(idx + 1) : rel;
                        newRel = (newFolderPath.endsWith("/") ? newFolderPath + filename : newFolderPath + "/" + filename);
                    }
                } else {
                    newRel = null;
                }
                f.setFolderPath(newFolderPath);
                f.setFilepath(newRel);
                userFileRepository.save(f);
            }
        }

        return folder;
    }

    /**
     * 同步用户文件和文件夹结构，删除数据库中存在但物理文件/文件夹不存在的记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void synchronizeUserFilesAndFolders(Long userId) {
        // 1. 同步文件夹
        List<UserFolder> userFolders = userFolderRepository.findByUser_IdOrderByIdAsc(userId);
        String userBasePhysicalPath = getCloudDiskAbsolutePath() + "/" + userId;

        for (UserFolder folder : userFolders) {
            String folderPhysicalPath = userBasePhysicalPath + folder.getFolderPath();
            Path path = Paths.get(folderPhysicalPath);

            if (!Files.exists(path) || !Files.isDirectory(path)) {
                System.out.println("检测到数据库中存在但物理路径不存在的文件夹，正在删除: " + folderPhysicalPath);
                userFolderRepository.delete(folder);
            }
        }

        // 2. 同步文件
        List<UserFile> userFiles = userFileRepository.findByUser_IdOrderByUploadTimeDesc(userId);
        for (UserFile file : userFiles) {
            String filePhysicalPath = userBasePhysicalPath + file.getFilepath();
            Path path = Paths.get(filePhysicalPath);

            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                System.out.println("检测到数据库中存在但物理文件不存在的文件，正在删除: " + filePhysicalPath);
                userFileRepository.delete(file);
            }
        }
    }
}

