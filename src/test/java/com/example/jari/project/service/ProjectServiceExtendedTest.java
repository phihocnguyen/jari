package com.example.jari.project.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.dto.CreateProjectRequest;
import com.example.jari.project.dto.ProjectResponse;
import com.example.jari.project.entity.Project;
import com.example.jari.project.entity.ProjectMember;
import com.example.jari.project.entity.ProjectMemberId;
import com.example.jari.project.mapper.ProjectMapper;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.entity.WorkspaceMemberId;
import com.example.jari.workspace.repository.WorkspaceMemberRepository;
import com.example.jari.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceExtendedTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository memberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private RbacService rbacService;
    @Mock private ProjectMapper mapper;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ProjectService projectService;

    private UUID workspaceId;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        when(rbacService.getRoleByName(any())).thenAnswer(inv -> TestFixtures.role(inv.getArgument(0)));
    }

    @Test
    void create_addsRequesterAsProjectAdmin() {
        var owner = TestFixtures.user(requesterId, "Owner");
        var workspace = TestFixtures.workspace(workspaceId, owner);
        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectKey("APP");
        req.setName("App");
        Project saved = TestFixtures.project(UUID.randomUUID(), workspace);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(projectRepository.existsByWorkspaceIdAndProjectKey(workspaceId, "APP")).thenReturn(false);
        when(projectRepository.save(any())).thenReturn(saved);
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(owner));
        when(mapper.toResponse(saved)).thenReturn(ProjectResponse.builder().id(saved.getId()).build());

        projectService.create(workspaceId, requesterId, req);

        verify(memberRepository).save(any());
    }

    @Test
    void create_withOptionalLead() {
        UUID leadId = UUID.randomUUID();
        var owner = TestFixtures.user(requesterId, "Owner");
        var lead = TestFixtures.user(leadId, "Lead");
        var workspace = TestFixtures.workspace(workspaceId, owner);
        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectKey("APP");
        req.setName("App");
        req.setLeadId(leadId);
        Project saved = TestFixtures.project(UUID.randomUUID(), workspace);
        saved.setLead(lead);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(projectRepository.existsByWorkspaceIdAndProjectKey(workspaceId, "APP")).thenReturn(false);
        when(userRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(projectRepository.save(any())).thenReturn(saved);
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(owner));
        when(mapper.toResponse(saved)).thenReturn(ProjectResponse.builder().id(saved.getId()).build());

        projectService.create(workspaceId, requesterId, req);

        verify(userRepository).findById(leadId);
    }

    @Test
    void listByWorkspace_returnsOnlyAssignedProjectsForRegularMember() {
        var workspace = TestFixtures.workspace(workspaceId, TestFixtures.user(UUID.randomUUID(), "Owner"));
        Project assigned = TestFixtures.project(UUID.randomUUID(), workspace);
        ProjectMember membership = ProjectMember.builder()
            .id(new ProjectMemberId(assigned.getId(), requesterId))
            .project(assigned)
            .user(TestFixtures.user(requesterId, "Member"))
            .role(TestFixtures.role("PROJECT_MEMBER"))
            .build();

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findById(new WorkspaceMemberId(workspaceId, requesterId)))
            .thenReturn(Optional.empty());
        when(memberRepository.findAllByUserIdAndWorkspaceId(requesterId, workspaceId))
            .thenReturn(java.util.List.of(membership));
        when(mapper.toResponse(assigned)).thenReturn(ProjectResponse.builder().id(assigned.getId()).build());

        var result = projectService.listByWorkspace(workspaceId, requesterId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(assigned.getId());
        verify(projectRepository, never()).findByWorkspaceId(any());
    }

    @Test
    void listByWorkspace_returnsAllProjectsForWorkspaceAdmin() {
        var owner = TestFixtures.user(requesterId, "Owner");
        var workspace = TestFixtures.workspace(workspaceId, owner);
        Project project = TestFixtures.project(UUID.randomUUID(), workspace);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(projectRepository.findByWorkspaceId(workspaceId)).thenReturn(java.util.List.of(project));
        when(mapper.toResponse(project)).thenReturn(ProjectResponse.builder().id(project.getId()).build());

        var result = projectService.listByWorkspace(workspaceId, requesterId);

        assertThat(result).hasSize(1);
        verify(memberRepository, never()).findAllByUserIdAndWorkspaceId(any(), any());
    }

    @Test
    void listByWorkspace_returnsEmptyWhenUserMissing() {
        assertThat(projectService.listByWorkspace(workspaceId, null)).isEmpty();
    }
}
