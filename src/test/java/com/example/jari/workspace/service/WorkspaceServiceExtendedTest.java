package com.example.jari.workspace.service;

import com.example.jari.notification.service.NotificationService;
import com.example.jari.project.entity.Project;
import com.example.jari.project.entity.ProjectMember;
import com.example.jari.project.entity.ProjectMemberId;
import com.example.jari.project.repository.ProjectMemberRepository;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.rbac.service.RbacService;
import com.example.jari.support.TestFixtures;
import com.example.jari.user.entity.User;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.dto.UpdateMemberProjectsRequest;
import com.example.jari.workspace.dto.UpdateMemberRoleRequest;
import com.example.jari.workspace.dto.UpdateWorkspaceRequest;
import com.example.jari.workspace.dto.WorkspaceMemberResponse;
import com.example.jari.workspace.dto.WorkspaceResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceServiceExtendedTest {

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
    private Workspace workspace;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        owner = TestFixtures.user(UUID.randomUUID(), "Owner");
        member = TestFixtures.user(UUID.randomUUID(), "Member");
        workspaceId = UUID.randomUUID();
        workspace = TestFixtures.workspace(workspaceId, owner);
        when(rbacService.getRoleByName(any())).thenAnswer(inv -> TestFixtures.role(inv.getArgument(0)));
        when(memberRepository.findById(new WorkspaceMemberId(workspaceId, owner.getId())))
            .thenReturn(Optional.of(WorkspaceMember.builder()
                .id(new WorkspaceMemberId(workspaceId, owner.getId()))
                .workspace(workspace).user(owner).role(TestFixtures.role("WORKSPACE_ADMIN")).build()));
    }

    @Test
    void update_patchesWorkspaceFields() {
        UpdateWorkspaceRequest req = new UpdateWorkspaceRequest();
        req.setName("Renamed");
        req.setDescription("New desc");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(workspace)).thenReturn(workspace);
        when(mapper.toResponse(workspace)).thenReturn(WorkspaceResponse.builder().id(workspaceId).name("Renamed").build());

        workspaceService.update(owner.getId(), workspaceId, req);

        verify(workspaceRepository).save(workspace);
    }

    @Test
    void updateMemberRole_updatesRoleForMember() {
        UUID memberId = member.getId();
        WorkspaceMember wm = WorkspaceMember.builder()
            .id(new WorkspaceMemberId(workspaceId, memberId))
            .workspace(workspace).user(member).role(TestFixtures.role("WORKSPACE_MEMBER")).build();
        UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
        req.setRoleName("WORKSPACE_ADMIN");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findById(new WorkspaceMemberId(workspaceId, memberId))).thenReturn(Optional.of(wm));
        when(projectMemberRepository.findAllByUserIdAndWorkspaceId(memberId, workspaceId)).thenReturn(List.of());
        when(mapper.toMemberResponse(wm)).thenReturn(WorkspaceMemberResponse.builder().userId(memberId).build());

        workspaceService.updateMemberRole(owner.getId(), workspaceId, memberId, req);

        verify(memberRepository).save(wm);
    }

    @Test
    void updateMemberProjects_syncsProjectMemberships() {
        UUID memberId = member.getId();
        UUID keepProjectId = UUID.randomUUID();
        UUID addProjectId = UUID.randomUUID();
        Project keepProject = TestFixtures.project(keepProjectId, workspace);
        Project addProject = TestFixtures.project(addProjectId, workspace);
        Project removeProject = TestFixtures.project(UUID.randomUUID(), workspace);
        WorkspaceMember wm = WorkspaceMember.builder()
            .id(new WorkspaceMemberId(workspaceId, memberId))
            .workspace(workspace).user(member).role(TestFixtures.role("WORKSPACE_MEMBER")).build();
        ProjectMember existingKeep = ProjectMember.builder()
            .id(new ProjectMemberId(keepProjectId, memberId))
            .project(keepProject).user(member).role(TestFixtures.role("PROJECT_MEMBER")).build();
        ProjectMember existingRemove = ProjectMember.builder()
            .id(new ProjectMemberId(removeProject.getId(), memberId))
            .project(removeProject).user(member).role(TestFixtures.role("PROJECT_MEMBER")).build();
        UpdateMemberProjectsRequest req = new UpdateMemberProjectsRequest();
        req.setProjectIds(List.of(keepProjectId, addProjectId));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findById(new WorkspaceMemberId(workspaceId, memberId))).thenReturn(Optional.of(wm));
        when(projectMemberRepository.findAllByUserIdAndWorkspaceId(memberId, workspaceId))
            .thenReturn(List.of(existingKeep, existingRemove));
        when(projectRepository.findById(keepProjectId)).thenReturn(Optional.of(keepProject));
        when(projectRepository.findById(addProjectId)).thenReturn(Optional.of(addProject));
        when(projectMemberRepository.existsByIdProjectIdAndIdUserId(addProjectId, memberId)).thenReturn(false);
        when(mapper.toMemberResponse(wm)).thenReturn(WorkspaceMemberResponse.builder().userId(memberId).build());

        workspaceService.updateMemberProjects(owner.getId(), workspaceId, memberId, req);

        verify(projectMemberRepository).delete(existingRemove);
        verify(projectMemberRepository, times(2)).save(any(ProjectMember.class));
    }

    @Test
    void removeMember_deletesWorkspaceAndProjectMemberships() {
        UUID memberId = member.getId();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        workspaceService.removeMember(owner.getId(), workspaceId, memberId);

        verify(memberRepository).deleteById(new WorkspaceMemberId(workspaceId, memberId));
        verify(projectMemberRepository).deleteAllByUserIdAndWorkspaceId(memberId, workspaceId);
    }
}
