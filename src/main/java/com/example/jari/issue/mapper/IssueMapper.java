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
    @Mapping(source = "labels",           target = "labels")
    IssueResponse toResponse(Issue issue);

    default List<LabelResponse> mapLabels(Set<Label> labels) {
        if (labels == null) return List.of();
        return labels.stream()
            .map(l -> LabelResponse.builder().id(l.getId()).name(l.getName()).color(l.getColor()).build())
            .collect(Collectors.toList());
    }

    @Mapping(source = "author.id",          target = "authorId")
    @Mapping(source = "author.displayName", target = "authorName")
    CommentResponse toCommentResponse(Comment comment);

    @Mapping(source = "user.id",          target = "userId")
    @Mapping(source = "user.displayName", target = "userName")
    IssueHistoryResponse toHistoryResponse(IssueHistory history);
}
