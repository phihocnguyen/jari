package com.example.jari.support;

import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.entity.IssueType;
import com.example.jari.issue.entity.Priority;
import com.example.jari.issue.entity.Status;
import com.example.jari.project.entity.Project;
import com.example.jari.rbac.entity.Role;
import com.example.jari.sprint.entity.Sprint;
import com.example.jari.sprint.entity.SprintIssue;
import com.example.jari.sprint.entity.SprintIssueId;
import com.example.jari.sprint.entity.SprintStatus;
import com.example.jari.user.entity.User;
import com.example.jari.user.entity.UserStatus;
import com.example.jari.workspace.entity.Workspace;

import java.math.BigDecimal;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {}

    public static User user(UUID id, String name) {
        return User.builder()
            .id(id)
            .username(name.toLowerCase())
            .email(name.toLowerCase() + "@example.com")
            .displayName(name)
            .status(UserStatus.ACTIVE)
            .build();
    }

    public static Workspace workspace(UUID id, User owner) {
        return Workspace.builder()
            .id(id)
            .name("Workspace")
            .workspaceKey("WS" + id.toString().substring(0, 4).toUpperCase())
            .owner(owner)
            .build();
    }

    public static Project project(UUID id, Workspace workspace) {
        return Project.builder()
            .id(id)
            .workspace(workspace)
            .name("Project")
            .projectKey("PROJ")
            .build();
    }

    public static Issue issue(UUID id, Project project) {
        return Issue.builder()
            .id(id)
            .project(project)
            .issueKey("PROJ-1")
            .title("Issue")
            .build();
    }

    public static Status status(UUID id, String name, String category) {
        return Status.builder().id(id).name(name).category(category).build();
    }

    public static Priority priority(UUID id, String name) {
        return Priority.builder().id(id).name(name).level(1).build();
    }

    public static IssueType issueType(UUID id, String name) {
        return IssueType.builder().id(id).name(name).build();
    }

    public static Sprint sprint(UUID id, Project project, SprintStatus status) {
        return Sprint.builder()
            .id(id)
            .project(project)
            .name("Sprint 1")
            .status(status)
            .build();
    }

    public static SprintIssue sprintIssue(Sprint sprint, Issue issue, BigDecimal position) {
        return SprintIssue.builder()
            .id(new SprintIssueId(sprint.getId(), issue.getId()))
            .sprint(sprint)
            .issue(issue)
            .position(position)
            .build();
    }

    public static Role role(String name) {
        return Role.builder().id(UUID.randomUUID()).name(name).build();
    }
}
