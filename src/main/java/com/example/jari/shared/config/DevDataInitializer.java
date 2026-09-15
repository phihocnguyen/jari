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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceService workspaceService;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!appProperties.getSecurity().isBypass()) {
            return;
        }

        try {
            // 1. Đảm bảo có User dev
            User devUser = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(User.builder()
                    .username("dev_user")
                    .email("dev@jari.local")
                    .displayName("Developer")
                    .status(UserStatus.ACTIVE)
                    .build())
            );

            // 2. Đảm bảo có Workspace mặc định
            if (workspaceRepository.count() == 0) {
                CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest();
                wsReq.setName("Acme Engineering");
                wsReq.setWorkspaceKey("ACME");
                wsReq.setDescription("Default development workspace");
                WorkspaceResponse ws = workspaceService.create(devUser.getId(), wsReq);
                log.info("Initialized default workspace: {} (ID: {})", ws.getName(), ws.getId());

                // 3. Đảm bảo có Project mặc định
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
