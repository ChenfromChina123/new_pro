package com.aispring.repository;

import com.aispring.entity.RequirementDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequirementDocRepository extends JpaRepository<RequirementDoc, Long> {
    List<RequirementDoc> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
