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
import com.example.jari.workspace.dto.InviteMemberRequest;
import com.example.jari.workspace.dto.WorkspaceMemberResponse;
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
class WorkspaceServiceAddMemberTest {

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
    private User target;
    private Workspace workspace;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        owner = TestFixtures.user(UUID.randomUUID(), "Owner");
        target = TestFixtures.user(UUID.randomUUID(), "Target");
        workspaceId = UUID.randomUUID();
        workspace = TestFixtures.workspace(workspaceId, owner);
        when(rbacService.getRoleByName(any())).thenAnswer(inv -> TestFixtures.role(inv.getArgument(0)));
        when(memberRepository.findById(new WorkspaceMemberId(workspaceId, owner.getId())))
            .thenReturn(Optional.of(WorkspaceMember.builder()
                .id(new WorkspaceMemberId(workspaceId, owner.getId()))
                .workspace(workspace).user(owner).role(TestFixtures.role("WORKSPACE_ADMIN")).build()));
    }

    @Test
    void addMember_assignsProjectsForWorkspaceAdmin() {
        UUID projectId = UUID.randomUUID();
        Project project = TestFixtures.project(projectId, workspace);
        InviteMemberRequest req = new InviteMemberRequest();
        req.setUserId(target.getId());
        req.setRoleName("WORKSPACE_ADMIN");
        req.setProjectIds(List.of(projectId));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, target.getId())).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByIdProjectIdAndIdUserId(projectId, target.getId())).thenReturn(false);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(mapper.toMemberResponse(any())).thenReturn(WorkspaceMemberResponse.builder().userId(target.getId()).build());

        workspaceService.addMember(owner.getId(), workspaceId, req);

        verify(projectMemberRepository).save(argThat(pm -> "PROJECT_ADMIN".equals(pm.getRole().getName())));
    }

    @Test
    void addMember_skipsProjectsOutsideWorkspace() {
        UUID foreignProjectId = UUID.randomUUID();
        Workspace foreignWs = TestFixtures.workspace(UUID.randomUUID(), owner);
        Project foreignProject = TestFixtures.project(foreignProjectId, foreignWs);
        InviteMemberRequest req = new InviteMemberRequest();
        req.setUserId(target.getId());
        req.setProjectIds(List.of(foreignProjectId));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, target.getId())).thenReturn(false);
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.findById(foreignProjectId)).thenReturn(Optional.of(foreignProject));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(mapper.toMemberResponse(any())).thenReturn(WorkspaceMemberResponse.builder().userId(target.getId()).build());

        workspaceService.addMember(owner.getId(), workspaceId, req);

        verify(projectMemberRepository, never()).save(any());
    }
}
