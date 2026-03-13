package com.aispring.sftp.service.impl;

import com.aispring.sftp.dto.FileInfo;
import com.aispring.sftp.dto.TransferProgress;
import com.aispring.sftp.pool.SFTPClientPool;
import com.aispring.sftp.service.SFTPService;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.xfer.FilePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * SFTP 服务实现类
 * 提供远程服务器文件操作的具体实现
 */
@Slf4j
@Service
public class SFTPServiceImpl implements SFTPService {

    private static final int BUFFER_SIZE = 8192;

    private SFTPClientPool sftpPool;

    @Autowired
    public void setSftpPool(SFTPClientPool sftpPool) {
        this.sftpPool = sftpPool;
    }

    /**
     * 列出目录下的文件和子目录
     * @param serverId 服务器 ID
     * @param path 目录路径
     * @return 文件信息列表
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public List<FileInfo> listFiles(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            List<RemoteResourceInfo> resources = sftp.ls(path);
            List<FileInfo> files = new ArrayList<>();

            for (RemoteResourceInfo info : resources) {
                if (".".equals(info.getName()) || "..".equals(info.getName())) {
                    continue;
                }

                FileInfo fileInfo = FileInfo.builder()
                    .name(info.getName())
                    .path(info.getPath())
                    .isDirectory(info.isDirectory())
                    .size(info.isDirectory() ? 0 : info.getAttributes().getSize())
                    .modifiedTime(convertToLocalDateTime((int) info.getAttributes().getMtime()))
                    .permissions(formatPermissions(info.getAttributes().getMode().getPermissions()))
                    .build();

                files.add(fileInfo);
            }

            files.sort((a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            log.debug("列出目录文件: serverId={}, path={}, count={}", serverId, path, files.size());
            return files;

        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 上传文件
     * @param serverId 服务器 ID
     * @param remotePath 远程文件路径
     * @param inputStream 文件输入流
     * @param fileSize 文件大小
     * @param progressCallback 进度回调（可选）
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public void uploadFile(Long serverId, String remotePath, InputStream inputStream,
                          long fileSize, Consumer<TransferProgress> progressCallback) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);

            Set<OpenMode> modes = EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC);
            RemoteFile file = sftp.open(remotePath, modes);

            long transferred = 0;
            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long lastProgressTime = startTime;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                file.write(transferred, buffer, 0, bytesRead);
                transferred += bytesRead;

                long currentTime = System.currentTimeMillis();
                if (progressCallback != null && currentTime - lastProgressTime >= 200) {
                    TransferProgress progress = createProgress(
                        remotePath, fileSize, transferred, startTime, currentTime);
                    progressCallback.accept(progress);
                    lastProgressTime = currentTime;
                }
            }

            file.close();

            if (progressCallback != null) {
                TransferProgress progress = createProgress(
                    remotePath, fileSize, transferred, startTime, System.currentTimeMillis());
                progress.setStatus(TransferProgress.TransferStatus.COMPLETED);
                progressCallback.accept(progress);
            }

            log.info("文件上传完成: serverId={}, path={}, size={}", serverId, remotePath, transferred);

        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 下载文件
     * @param serverId 服务器 ID
     * @param remotePath 远程文件路径
     * @param outputStream 输出流
     * @param progressCallback 进度回调（可选）
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public void downloadFile(Long serverId, String remotePath, OutputStream outputStream,
                            Consumer<TransferProgress> progressCallback) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);

            long fileSize = sftp.size(remotePath);
            RemoteFile file = sftp.open(remotePath);

            long transferred = 0;
            long startTime = System.currentTimeMillis();
            byte[] buffer = new byte[BUFFER_SIZE];
            long lastProgressTime = startTime;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead;

            while (transferred < fileSize) {
                bytesRead = file.read(transferred, buffer, 0,
                    (int) Math.min(BUFFER_SIZE, fileSize - transferred));
                if (bytesRead == -1) break;

                outputStream.write(buffer, 0, bytesRead);
                transferred += bytesRead;

                long currentTime = System.currentTimeMillis();
                if (progressCallback != null && currentTime - lastProgressTime >= 200) {
                    TransferProgress progress = createProgress(
                        remotePath, fileSize, transferred, startTime, currentTime);
                    progressCallback.accept(progress);
                    lastProgressTime = currentTime;
                }
            }

            file.close();
            outputStream.flush();

            if (progressCallback != null) {
                TransferProgress progress = createProgress(
                    remotePath, fileSize, transferred, startTime, System.currentTimeMillis());
                progress.setStatus(TransferProgress.TransferStatus.COMPLETED);
                progressCallback.accept(progress);
            }

            log.info("文件下载完成: serverId={}, path={}, size={}", serverId, remotePath, transferred);

        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 删除文件
     * @param serverId 服务器 ID
     * @param path 文件路径
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public void deleteFile(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            sftp.rm(path);
            log.info("文件已删除: serverId={}, path={}", serverId, path);
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 删除目录（递归）
     * @param serverId 服务器 ID
     * @param path 目录路径
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public void deleteDirectory(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            deleteDirectoryRecursive(sftp, path);
            log.info("目录已删除: serverId={}, path={}", serverId, path);
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 递归删除目录
     * @param sftp SFTP 客户端
     * @param path 目录路径
     * @throws Exception 操作失败时抛出异常
     */
    private void deleteDirectoryRecursive(SFTPClient sftp, String path) throws Exception {
        List<RemoteResourceInfo> resources = sftp.ls(path);

        for (RemoteResourceInfo info : resources) {
            if (".".equals(info.getName()) || "..".equals(info.getName())) {
                continue;
            }

            String fullPath = info.getPath();
            if (info.isDirectory()) {
                deleteDirectoryRecursive(sftp, fullPath);
            } else {
                sftp.rm(fullPath);
            }
        }

        sftp.rmdir(path);
    }

    /**
     * 重命名文件或目录
     * @param serverId 服务器 ID
     * @param oldPath 原路径
     * @param newPath 新路径
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public void rename(Long serverId, String oldPath, String newPath) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            sftp.rename(oldPath, newPath);
            log.info("文件已重命名: serverId={}, {} -> {}", serverId, oldPath, newPath);
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 创建目录
     * @param serverId 服务器 ID
     * @param path 目录路径
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public void mkdir(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            sftp.mkdirs(path);
            log.info("目录已创建: serverId={}, path={}", serverId, path);
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 检查路径是否存在
     * @param serverId 服务器 ID
     * @param path 路径
     * @return 是否存在
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public boolean exists(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            try {
                sftp.stat(path);
                return true;
            } catch (net.schmizz.sshj.sftp.SFTPException e) {
                if (net.schmizz.sshj.sftp.Response.StatusCode.NO_SUCH_FILE.equals(e.getStatusCode())) {
                    return false;
                }
                throw e;
            }
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 获取文件信息
     * @param serverId 服务器 ID
     * @param path 文件路径
     * @return 文件信息
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public FileInfo getFileInfo(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            net.schmizz.sshj.sftp.FileAttributes attrs = sftp.stat(path);

            String name = path.substring(path.lastIndexOf('/') + 1);

            return FileInfo.builder()
                .name(name)
                .path(path)
                .isDirectory(attrs.getType().toString().contains("DIR"))
                .size(attrs.getSize())
                .modifiedTime(convertToLocalDateTime((int) attrs.getMtime()))
                .permissions(formatPermissions(attrs.getMode().getPermissions()))
                .build();
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 获取文件大小
     * @param serverId 服务器 ID
     * @param path 文件路径
     * @return 文件大小（字节）
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public long getFileSize(Long serverId, String path) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            return sftp.size(path);
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 读取文件内容
     * @param serverId 服务器 ID
     * @param path 文件路径
     * @param maxSize 最大读取大小（字节），超过则截断
     * @return 文件内容
     * @throws Exception 操作失败时抛出异常
     */
    @Override
    public String readFileContent(Long serverId, String path, long maxSize) throws Exception {
        SFTPClient sftp = null;
        try {
            sftp = sftpPool.borrowObject(serverId);
            
            // 获取文件大小
            long fileSize = sftp.size(path);
            
            // 如果文件太大，只读取前 maxSize 字节
            long readSize = Math.min(fileSize, maxSize);
            
            // 打开文件并读取内容
            RemoteFile file = sftp.open(path, EnumSet.of(OpenMode.READ));
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            long transferred = 0;
            int bytesRead;
            
            while (transferred < readSize) {
                int toRead = (int) Math.min(buffer.length, readSize - transferred);
                bytesRead = file.read(transferred, buffer, 0, toRead);
                
                if (bytesRead == -1) {
                    break;
                }
                
                outputStream.write(buffer, 0, bytesRead);
                transferred += bytesRead;
            }
            
            file.close();
            
            String content = outputStream.toString("UTF-8");
            
            // 如果文件被截断，添加提示
            if (fileSize > maxSize) {
                content += "\n\n... [文件过大，只显示前 " + maxSize + " 字节]";
            }
            
            log.info("文件内容读取完成: serverId={}, path={}, size={}", serverId, path, transferred);
            
            return content;
            
        } finally {
            if (sftp != null) {
                sftpPool.returnObject(serverId, sftp);
            }
        }
    }

    /**
     * 将 Unix 时间戳转换为 LocalDateTime
     * @param timestamp Unix 时间戳（秒）
     * @return LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(int timestamp) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp),
            ZoneId.systemDefault()
        );
    }

    /**
     * 格式化权限字符串
     * @param permissions 文件权限集合
     * @return 权限字符串（如 rw-r--r--）
     */
    private String formatPermissions(Set<FilePermission> permissions) {
        StringBuilder sb = new StringBuilder();

        sb.append(permissions.contains(FilePermission.USR_R) ? 'r' : '-');
        sb.append(permissions.contains(FilePermission.USR_W) ? 'w' : '-');
        sb.append(permissions.contains(FilePermission.USR_X) ? 'x' : '-');
        sb.append(permissions.contains(FilePermission.GRP_R) ? 'r' : '-');
        sb.append(permissions.contains(FilePermission.GRP_W) ? 'w' : '-');
        sb.append(permissions.contains(FilePermission.GRP_X) ? 'x' : '-');
        sb.append(permissions.contains(FilePermission.OTH_R) ? 'r' : '-');
        sb.append(permissions.contains(FilePermission.OTH_W) ? 'w' : '-');
        sb.append(permissions.contains(FilePermission.OTH_X) ? 'x' : '-');

        return sb.toString();
    }

    /**
     * 创建传输进度对象
     * @param path 文件路径
     * @param totalSize 总大小
     * @param transferred 已传输大小
     * @param startTime 开始时间
     * @param currentTime 当前时间
     * @return 传输进度对象
     */
    private TransferProgress createProgress(String path, long totalSize, long transferred,
                                           long startTime, long currentTime) {
        int progress = totalSize > 0 ? (int) ((transferred * 100) / totalSize) : 0;
        String speed = TransferProgress.calculateSpeed(transferred, currentTime - startTime);
        String fileName = path.substring(path.lastIndexOf('/') + 1);

        return TransferProgress.builder()
            .fileName(fileName)
            .filePath(path)
            .totalSize(totalSize)
            .transferredSize(transferred)
            .progress(progress)
            .speed(speed)
            .status(TransferProgress.TransferStatus.TRANSFERRING)
            .startTime(startTime)
            .build();
    }
}
