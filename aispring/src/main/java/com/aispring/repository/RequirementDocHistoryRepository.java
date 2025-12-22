package com.aispring.repository;

import com.aispring.entity.RequirementDocHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequirementDocHistoryRepository extends JpaRepository<RequirementDocHistory, Long> {
    List<RequirementDocHistory> findByDocIdOrderByVersionDesc(Long docId);
}
