package com.example.jari.workspace.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.entity.ProjectMember;
import com.example.jari.project.entity.ProjectMemberId;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.shared.exception.*;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.dto.*;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.entity.WorkspaceMember;
import com.example.jari.workspace.entity.WorkspaceMemberId;
import com.example.jari.workspace.mapper.WorkspaceMapper;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private RbacService rbacService;
    @Mock private WorkspaceMapper mapper;
    @Mock private NotificationService notificationService;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;

    @InjectMocks private WorkspaceService workspaceService;

    private User owner;
    private User member;
    private UUID workspaceId;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = TestFixtures.user(UUID.randomUUID(), "Owner");
        member = TestFixtures.user(UUID.randomUUID(), "Member");
        workspaceId = UUID.randomUUID();
        workspace = TestFixtures.workspace(workspaceId, owner);
        when(rbacService.getRoleByName(anyString())).thenAnswer(inv ->
            TestFixtures.role(inv.getArgument(0)));
    }

    @Test
    void create_throwsWhenKeyExists() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setWorkspaceKey("WS");
        when(workspaceRepository.existsByWorkspaceKey("WS")).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.create(owner.getId(), req))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_addsOwnerAsAdmin() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Team");
        req.setWorkspaceKey("TEAM");
        when(workspaceRepository.existsByWorkspaceKey("TEAM")).thenReturn(false);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any())).thenAnswer(inv -> {
            Workspace ws = inv.getArgument(0);
            ws.setId(workspaceId);
            return ws;
        });
        when(mapper.toResponse(any())).thenReturn(WorkspaceResponse.builder().id(workspaceId).build());

        workspaceService.create(owner.getId(), req);

        verify(memberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void delete_onlyOwnerCanDelete() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        assertThatThrownBy(() -> workspaceService.delete(member.getId(), workspaceId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_ownerCanDelete() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        workspaceService.delete(owner.getId(), workspaceId);

        verify(workspaceRepository).delete(workspace);
    }

    @Test
    void addMember_requiresUserIdOrEmail() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        stubOwnerAdmin();

        assertThatThrownBy(() -> workspaceService.addMember(owner.getId(), workspaceId, new InviteMemberRequest()))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addMember_byEmail_invitesMember() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("Member@Example.com");
        WorkspaceMemberResponse mapped = WorkspaceMemberResponse.builder().userId(member.getId()).build();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        stubOwnerAdmin();
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(memberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, member.getId())).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(mapper.toMemberResponse(any())).thenReturn(mapped);

        WorkspaceMemberResponse result = workspaceService.addMember(owner.getId(), workspaceId, req);

        assertThat(result).isNotNull();
        verify(notificationService).notifyWorkspaceMemberAdded(eq(workspace), eq(owner), eq(member));
    }

    @Test
    void addMember_throwsWhenAlreadyMember() {
        InviteMemberRequest req = new InviteMemberRequest();
        req.setUserId(member.getId());
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        stubOwnerAdmin();
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(memberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, member.getId())).thenReturn(true);

        assertThatThrownBy(() -> workspaceService.addMember(owner.getId(), workspaceId, req))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateMemberRole_protectsOwner() {
        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRoleName("WORKSPACE_MEMBER");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        stubOwnerAdmin();

        assertThatThrownBy(() -> workspaceService.updateMemberRole(owner.getId(), workspaceId, owner.getId(), req))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void removeMember_protectsOwner() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        stubOwnerAdmin();

        assertThatThrownBy(() -> workspaceService.removeMember(owner.getId(), workspaceId, owner.getId()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireAdmin_rejectsNullRequester() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        org.assertj.core.api.Assertions.assertThatCode(
                () -> workspaceService.update(owner.getId(), workspaceId, new UpdateWorkspaceRequest()))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> workspaceService.update(null, workspaceId, new UpdateWorkspaceRequest()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listMembers_enrichesProjectAssignments() {
        WorkspaceMember wm = WorkspaceMember.builder()
            .id(new WorkspaceMemberId(workspaceId, member.getId()))
            .workspace(workspace)
            .user(member)
            .role(TestFixtures.role("WORKSPACE_MEMBER"))
            .build();
        Project project = TestFixtures.project(UUID.randomUUID(), workspace);
        ProjectMember pm = ProjectMember.builder()
            .id(new ProjectMemberId(project.getId(), member.getId()))
            .project(project)
            .user(member)
            .role(TestFixtures.role("PROJECT_MEMBER"))
            .build();
        WorkspaceMemberResponse resp = WorkspaceMemberResponse.builder().userId(member.getId()).build();
        when(memberRepository.findByIdWorkspaceId(workspaceId)).thenReturn(List.of(wm));
        when(projectMemberRepository.findAllByWorkspaceIdWithProject(workspaceId)).thenReturn(List.of(pm));
        when(mapper.toMemberResponse(wm)).thenReturn(resp);

        List<WorkspaceMemberResponse> result = workspaceService.listMembers(workspaceId);

        assertThat(result.get(0).getProjectIds()).containsExactly(project.getId());
        assertThat(result.get(0).getProjectNames()).containsExactly(project.getName());
    }

    private void stubOwnerAdmin() {
        when(memberRepository.findById(new WorkspaceMemberId(workspaceId, owner.getId())))
            .thenReturn(Optional.of(WorkspaceMember.builder()
                .id(new WorkspaceMemberId(workspaceId, owner.getId()))
                .workspace(workspace)
                .user(owner)
                .role(TestFixtures.role("WORKSPACE_ADMIN"))
                .build()));
    }
}
