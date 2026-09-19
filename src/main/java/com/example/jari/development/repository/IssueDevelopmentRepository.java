package com.example.jari.development.repository;

import com.example.jari.development.entity.IssueDevelopment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueDevelopmentRepository extends JpaRepository<IssueDevelopment, UUID> {
    List<IssueDevelopment> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
}
