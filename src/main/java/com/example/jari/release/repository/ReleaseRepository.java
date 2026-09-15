package com.example.jari.release.repository;

import com.example.jari.release.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    List<Release> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<Release> findByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
