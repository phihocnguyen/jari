package com.example.jari.shared.config;

import com.example.jari.project.dto.CreateProjectRequest;
import com.example.jari.project.entity.ProjectType;
import com.example.jari.project.repository.ProjectRepository;
import com.example.jari.project.service.ProjectService;
import com.example.jari.user.entity.User;
import com.example.jari.user.entity.UserStatus;
import com.example.jari.user.repository.UserRepository;
import com.example.jari.workspace.dto.CreateWorkspaceRequest;
import com.example.jari.workspace.dto.WorkspaceResponse;
import com.example.jari.workspace.repository.WorkspaceRepository;
import com.example.jari.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Optional demo seed data when running with Spring profile {@code dev}.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceService workspaceService;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            User devUser = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(User.builder()
                    .username("dev_user")
                    .email("dev@jari.local")
                    .displayName("Học Nguyễn")
                    .status(UserStatus.ACTIVE)
                    .build())
            );

            if (devUser != null && ("Developer".equals(devUser.getDisplayName()) || "dev_user".equals(devUser.getDisplayName()))) {
                devUser.setDisplayName("Học Nguyễn");
                userRepository.save(devUser);
            }

            if (workspaceRepository.count() == 0) {
                CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest();
                wsReq.setName("Acme Engineering");
                wsReq.setWorkspaceKey("ACME");
                wsReq.setDescription("Default development workspace");
                WorkspaceResponse ws = workspaceService.create(devUser.getId(), wsReq);
                log.info("Initialized default workspace: {} (ID: {})", ws.getName(), ws.getId());

                CreateProjectRequest projReq = new CreateProjectRequest();
                projReq.setName("Teams in Space");
                projReq.setProjectKey("TIS");
                projReq.setDescription("Main Development Project");
                projReq.setProjectType(ProjectType.SOFTWARE);
                projReq.setLeadId(devUser.getId());
                var proj = projectService.create(ws.getId(), devUser.getId(), projReq);
                log.info("Initialized default project: {} (ID: {})", proj.getName(), proj.getId());
            } else if (projectRepository.count() == 0) {
                var ws = workspaceRepository.findAll().get(0);
                CreateProjectRequest projReq = new CreateProjectRequest();
                projReq.setName("Teams in Space");
                projReq.setProjectKey("TIS");
                projReq.setDescription("Main Development Project");
                projReq.setProjectType(ProjectType.SOFTWARE);
                projReq.setLeadId(devUser.getId());
                var proj = projectService.create(ws.getId(), devUser.getId(), projReq);
                log.info("Initialized default project: {} (ID: {})", proj.getName(), proj.getId());
            }
        } catch (Exception e) {
            log.warn("DevDataInitializer skipped or already initialized: {}", e.getMessage());
        }
    }
}
