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
    private String cloudDiskDir = "cloud_disk";
    private String avatarsDir = "avatars";

    public String getRootDir() { return rootDir; }
    public void setRootDir(String rootDir) { this.rootDir = rootDir; }
    public String getCloudDiskDir() { return cloudDiskDir; }
    public void setCloudDiskDir(String cloudDiskDir) { this.cloudDiskDir = cloudDiskDir; }
    public String getAvatarsDir() { return avatarsDir; }
    public void setAvatarsDir(String avatarsDir) { this.avatarsDir = avatarsDir; }

    public Path getRootAbsolute() {
        String dir = System.getProperty("user.dir");
        Path rootPath;
        if (rootDir == null || rootDir.trim().isEmpty()) {
            rootPath = Paths.get(dir).normalize();
        } else {
            Path rp = Paths.get(rootDir);
            rootPath = rp.isAbsolute() ? rp.normalize() : Paths.get(dir).resolve(rp).normalize();
        }
        try { Files.createDirectories(rootPath); } catch (Exception ignore) {}
        return rootPath;
    }

    public String getCloudDiskAbsolute() {
        Path base = getRootAbsolute().resolve(cloudDiskDir).normalize();
        try { Files.createDirectories(base); } catch (Exception ignore) {}
        return base.toString();
    }

    public String getAiTerminalAbsolute() {
        Path base = getRootAbsolute().resolve(aiTerminalDir).normalize();
        try { Files.createDirectories(base); } catch (Exception ignore) {}
        return base.toString();
    }

    public String getAvatarsAbsolute() {
        Path base = getRootAbsolute().resolve(avatarsDir).normalize();
        try { Files.createDirectories(base); } catch (Exception ignore) {}
        return base.toString();
    }
}
