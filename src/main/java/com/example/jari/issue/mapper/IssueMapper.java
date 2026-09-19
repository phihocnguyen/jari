package com.example.jari.issue.mapper;
import com.example.jari.issue.dto.*;
import com.example.jari.issue.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface IssueMapper {

    @Mapping(source = "project.id",       target = "projectId")
    @Mapping(source = "issueType.name",   target = "issueType")
    @Mapping(source = "status.name",      target = "status")
    @Mapping(source = "status.category",  target = "statusCategory")
    @Mapping(source = "priority.name",    target = "priority")
    @Mapping(source = "reporter.id",      target = "reporterId")
    @Mapping(source = "reporter.displayName", target = "reporterName")
    @Mapping(source = "assignee.id",      target = "assigneeId")
    @Mapping(source = "assignee.displayName", target = "assigneeName")
    @Mapping(source = "parent.id",        target = "parentId")
    @Mapping(source = "release.id",       target = "releaseId")
    @Mapping(source = "release.name",     target = "releaseName")
    @Mapping(source = "labels",           target = "labels")
    @Mapping(source = "components",       target = "components")
    @Mapping(source = "sprintIssues",     target = "sprintId")
    @Mapping(source = "sprintIssues",     target = "sprintName")
    IssueResponse toResponse(Issue issue);

    default java.util.UUID mapSprintId(Set<com.example.jari.sprint.entity.SprintIssue> sprintIssues) {
        if (sprintIssues == null || sprintIssues.isEmpty()) return null;
        var first = sprintIssues.iterator().next();
        return first.getSprint() != null ? first.getSprint().getId() : null;
    }

    default String mapSprintName(Set<com.example.jari.sprint.entity.SprintIssue> sprintIssues) {
        if (sprintIssues == null || sprintIssues.isEmpty()) return null;
        var first = sprintIssues.iterator().next();
        return first.getSprint() != null ? first.getSprint().getName() : null;
    }

    default List<LabelResponse> mapLabels(Set<Label> labels) {
        if (labels == null) return List.of();
        return labels.stream()
            .map(l -> LabelResponse.builder().id(l.getId()).name(l.getName()).color(l.getColor()).build())
            .collect(Collectors.toList());
    }

    default List<com.example.jari.component.dto.ComponentResponse> mapComponents(Set<com.example.jari.component.entity.Component> components) {
        if (components == null) return List.of();
        return components.stream()
            .map(c -> com.example.jari.component.dto.ComponentResponse.builder()
                .id(c.getId())
                .projectId(c.getProject() != null ? c.getProject().getId() : null)
                .name(c.getName())
                .description(c.getDescription())
                .build())
            .collect(Collectors.toList());
    }

    @Mapping(source = "author.id",          target = "authorId")
    @Mapping(source = "author.displayName", target = "authorName")
    CommentResponse toCommentResponse(Comment comment);

    @Mapping(source = "user.id",          target = "userId")
    @Mapping(source = "user.displayName", target = "userName")
    IssueHistoryResponse toHistoryResponse(IssueHistory history);
}
