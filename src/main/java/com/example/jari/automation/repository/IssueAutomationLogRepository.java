package com.example.jari.automation.repository;

import com.example.jari.automation.entity.IssueAutomationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueAutomationLogRepository extends JpaRepository<IssueAutomationLog, UUID> {
    List<IssueAutomationLog> findByIssueIdOrderByExecutedAtDesc(UUID issueId);
}
