package com.example.jari.sprint.repository;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Sprint> findByProjectIdAndStatus(UUID projectId, SprintStatus status);
    boolean existsByProjectIdAndStatus(UUID projectId, SprintStatus status);
}
