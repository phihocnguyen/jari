package com.example.jari.issue.repository;
import com.example.jari.issue.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusRepository extends JpaRepository<Status, UUID> {
    Optional<Status> findByName(String name);
    Optional<Status> findByNameIgnoreCase(String name);

    @Query("SELECT s FROM Status s WHERE UPPER(s.category) = UPPER(:category) ORDER BY s.name")
    List<Status> findAllByCategoryIgnoreCase(@Param("category") String category);
}
