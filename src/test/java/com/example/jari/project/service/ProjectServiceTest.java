package com.example.jari.project.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.dto.*;
import com.example.jari.project.entity.*;
import com.example.jari.project.mapper.ProjectMapper;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.shared.exception.*;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.entity.WorkspaceMember;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceTest {

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

    private User owner;
    private User member;
    private Workspace workspace;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        owner = TestFixtures.user(UUID.randomUUID(), "Owner");
        member = TestFixtures.user(UUID.randomUUID(), "Member");
        workspace = TestFixtures.workspace(UUID.randomUUID(), owner);
        projectId = UUID.randomUUID();
        project = TestFixtures.project(projectId, workspace);
        project.setLead(owner);
        when(rbacService.getRoleByName(anyString())).thenAnswer(inv -> TestFixtures.role(inv.getArgument(0)));
    }

    @Test
    void create_throwsWhenProjectKeyExists() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectKey("APP");
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(projectRepository.existsByWorkspaceIdAndProjectKey(workspace.getId(), "APP")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(workspace.getId(), owner.getId(), req))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void findOrThrow_byKeyWhenUuidInvalid() {
        when(projectRepository.findByProjectKeyIgnoreCase("APP")).thenReturn(Optional.of(project));

        assertThat(projectService.findOrThrow("APP")).isEqualTo(project);
    }

    @Test
    void findOrThrow_byUuidThenKey() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        when(projectRepository.findByProjectKeyIgnoreCase(projectId.toString())).thenReturn(Optional.of(project));

        assertThat(projectService.findOrThrow(projectId.toString())).isEqualTo(project);
    }

    @Test
    void update_publishesIndexEventWhenNameChanges() {
        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setName("Renamed");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);
        when(mapper.toResponse(project)).thenReturn(ProjectResponse.builder().id(projectId).build());

        projectService.update(projectId, owner.getId(), req);

        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void addMember_requiresIdentifier() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.addMember(projectId, owner.getId(), new AddProjectMemberRequest()))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addMember_byEmail_notifiesNewMember() {
        AddProjectMemberRequest req = new AddProjectMemberRequest();
        req.setEmail("member@example.com");
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(memberRepository.existsByIdProjectIdAndIdUserId(projectId, member.getId())).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(mapper.toMemberResponse(any())).thenReturn(ProjectMemberResponse.builder().userId(member.getId()).build());

        projectService.addMember(projectId, owner.getId(), req);

        verify(notificationService).notifyProjectMemberAdded(eq(project), eq(owner), eq(member));
    }

    @Test
    void removeMember_blocksLeadRemoval() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.removeMember(projectId.toString(), owner.getId(), owner.getId()))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireAdmin_allowsWorkspaceAdmin() {
        UUID adminId = UUID.randomUUID();
        project.setLead(null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(memberRepository.findById(new ProjectMemberId(projectId, adminId))).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findById(new WorkspaceMemberId(workspace.getId(), adminId)))
            .thenReturn(Optional.of(WorkspaceMember.builder()
                .id(new WorkspaceMemberId(workspace.getId(), adminId))
                .workspace(workspace)
                .user(TestFixtures.user(adminId, "Admin"))
                .role(TestFixtures.role("WORKSPACE_ADMIN"))
                .build()));
        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setDescription("Updated");
        when(projectRepository.save(project)).thenReturn(project);
        when(mapper.toResponse(project)).thenReturn(ProjectResponse.builder().id(projectId).build());

        projectService.update(projectId.toString(), adminId, req);

        verify(projectRepository).save(project);
    }

    @Test
    void requireAdmin_throwsForNonAdmin() {
        UUID outsiderId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        project.setLead(null);
        when(memberRepository.findById(new ProjectMemberId(projectId, outsiderId))).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findById(new WorkspaceMemberId(workspace.getId(), outsiderId)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update(projectId.toString(), outsiderId, new UpdateProjectRequest()))
            .isInstanceOf(ForbiddenException.class);
    }
}
