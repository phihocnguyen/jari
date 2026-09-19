package com.example.jari.component.repository;

import com.example.jari.component.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<Component, UUID> {

    List<Component> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<Component> findByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    @Query(value = "SELECT COUNT(*) FROM issue_components WHERE component_id = :componentId", nativeQuery = true)
    long countIssuesByComponentId(@Param("componentId") UUID componentId);
}
