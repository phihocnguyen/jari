package com.example.jari.issue.repository;
import com.example.jari.issue.entity.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueTypeRepository extends JpaRepository<IssueType, UUID> {
    Optional<IssueType> findByName(String name);
}
