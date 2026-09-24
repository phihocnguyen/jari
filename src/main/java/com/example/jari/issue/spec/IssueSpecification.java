package com.example.jari.issue.spec;

import com.example.jari.issue.entity.Issue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class IssueSpecification {

    private IssueSpecification() {}

    public static Specification<Issue> filter(
            UUID projectId,
            UUID statusId,
            UUID assigneeId,
            UUID issueTypeId,
            UUID priorityId,
            UUID sprintId,
            String keyword) {
        return filter(projectId, null, statusId, assigneeId, issueTypeId, priorityId, sprintId, keyword);
    }

    public static Specification<Issue> filter(
            UUID projectId,
            Collection<UUID> projectIds,
            UUID statusId,
            UUID assigneeId,
            UUID issueTypeId,
            UUID priorityId,
            UUID sprintId,
            String keyword) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            } else if (projectIds != null && !projectIds.isEmpty()) {
                predicates.add(root.get("project").get("id").in(projectIds));
            }
            if (statusId != null) {
                predicates.add(cb.equal(root.get("status").get("id"), statusId));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (issueTypeId != null) {
                predicates.add(cb.equal(root.get("issueType").get("id"), issueTypeId));
            }
            if (priorityId != null) {
                predicates.add(cb.equal(root.get("priority").get("id"), priorityId));
            }
            if (sprintId != null) {
                var sprintIssuesJoin = root.join("sprintIssues", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.equal(sprintIssuesJoin.get("sprint").get("id"), sprintId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")),    like),
                    cb.like(cb.lower(root.get("issueKey")), like)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
