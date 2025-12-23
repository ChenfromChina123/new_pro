package com.aispring.repository;

import com.aispring.entity.approval.UserApprovalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户批准设置 Repository
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Repository
public interface UserApprovalSettingsRepository extends JpaRepository<UserApprovalSettings, Long> {
    
    /**
     * 根据用户ID查找设置
     */
    Optional<UserApprovalSettings> findByUserId(Long userId);
    
    /**
     * 检查用户设置是否存在
     */
    boolean existsByUserId(Long userId);
}

