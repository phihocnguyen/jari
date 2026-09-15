package com.example.jari.issue.repository;

import com.example.jari.issue.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<Label> findByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
