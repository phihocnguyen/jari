package com.example.jari.issue.repository;
import com.example.jari.issue.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, UUID> {
    Optional<Priority> findByName(String name);
    @Query("SELECT p FROM Priority p ORDER BY p.level ASC")
    List<Priority> findAllOrderByLevel();
}
