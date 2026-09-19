package com.example.jari.issue.repository;

import com.example.jari.issue.entity.IssueWatcher;
import com.example.jari.issue.entity.IssueWatcherId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueWatcherRepository extends JpaRepository<IssueWatcher, IssueWatcherId> {

    boolean existsByIdIssueIdAndByIdUserId(UUID issueId, UUID userId);

    long countByIdIssueId(UUID issueId);

    List<IssueWatcher> findByIdIssueId(UUID issueId);

    List<IssueWatcher> findByIdUserId(UUID userId);

    void deleteByIdIssueIdAndByIdUserId(UUID issueId, UUID userId);
}
