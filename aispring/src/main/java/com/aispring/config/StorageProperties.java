package com.aispring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String rootDir = "./";
    private String cloudDiskDir = "cloud_disk_storage";
    private String avatarsDir = "avatars";
    private String publicFilesDir = "public_files";

    public String getRootDir() { return rootDir; }
    public void setRootDir(String rootDir) { this.rootDir = rootDir; }
    public String getCloudDiskDir() { return cloudDiskDir; }
    public void setCloudDiskDir(String cloudDiskDir) { this.cloudDiskDir = cloudDiskDir; }
    public String getAvatarsDir() { return avatarsDir; }
    public void setAvatarsDir(String avatarsDir) { this.avatarsDir = avatarsDir; }
    public String getPublicFilesDir() { return publicFilesDir; }
    public void setPublicFilesDir(String publicFilesDir) { this.publicFilesDir = publicFilesDir; }

    private boolean isProduction() {
        String profile = System.getProperty("spring.profiles.active", "");
        String appMode = System.getProperty("app.mode", "");
        return "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(appMode);
    }

    public Path getRootAbsolute() {
        String dir;
        
        // 检查环境变量
        String envRootDir = System.getenv("APP_DATA_ROOT");
        if (envRootDir != null && !envRootDir.trim().isEmpty()) {
            dir = envRootDir;
        } else if (isProduction()) {
            // 生产环境使用专门的数据目录
            dir = "/home/aispring/data";
        } else {
            dir = System.getProperty("user.dir", ".");
        }
        
        Path rootPath;
        if (dir == null || dir.trim().isEmpty() || ".".equals(dir) || "./".equals(dir)) {
            rootPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        } else {
            Path rp = Paths.get(dir);
            rootPath = rp.isAbsolute() ? rp.normalize() : Paths.get(System.getProperty("user.dir")).resolve(rp).toAbsolutePath().normalize();
        }
        try { 
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath); 
            }
        } catch (Exception ignore) {}
        return rootPath;
    }

    public String getCloudDiskAbsolute() {
        Path base = getRootAbsolute().resolve(cloudDiskDir).normalize();
        try { Files.createDirectories(base); } catch (Exception ignore) {}
        return base.toString();
    }

    public String getAvatarsAbsolute() {
        Path base = getRootAbsolute().resolve(avatarsDir).normalize();
        try { Files.createDirectories(base); } catch (Exception ignore) {}
        return base.toString();
    }

    public String getPublicFilesAbsolute() {
        Path base = getRootAbsolute().resolve(publicFilesDir).normalize();
        try { Files.createDirectories(base); } catch (Exception ignore) {}
        return base.toString();
    }
}
