package com.aispring.repository;

import com.aispring.entity.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户文件仓库接口
 */
@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {
    
    /**
     * 根据用户ID查找所有文件
     */
    List<UserFile> findByUser_IdOrderByUploadTimeDesc(Long userId);
    
    /**
     * 根据用户ID和文件夹ID查找文件
     */
    List<UserFile> findByUser_IdAndFolderPathOrderByUploadTimeDesc(Long userId, String folderPath);
    
    /**
     * 根据用户ID和文件ID查找
     */
    Optional<UserFile> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 根据文件路径查找
     */
    Optional<UserFile> findByFilepath(String filepath);

    /**
     * 根据用户ID、文件夹路径和文件名查找文件
     */
    Optional<UserFile> findByUser_IdAndFolderPathAndFilename(Long userId, String folderPath, String filename);
}

