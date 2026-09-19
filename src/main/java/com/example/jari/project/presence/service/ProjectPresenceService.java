package com.example.jari.project.presence.service;

import com.example.jari.project.presence.dto.OnlineUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectPresenceService {

    private static final Duration TTL = Duration.ofSeconds(45);
    private final SimpMessagingTemplate messagingTemplate;

    // projectId -> (userId -> OnlineUserResponse)
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, OnlineUserResponse>> presenceMap =
        new ConcurrentHashMap<>();

    public List<OnlineUserResponse> getOnlineUsers(UUID projectId) {
        ConcurrentHashMap<UUID, OnlineUserResponse> users = presenceMap.get(projectId);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        Instant threshold = Instant.now().minus(TTL);
        return users.values().stream()
            .filter(u -> u.getLastSeen().isAfter(threshold))
            .sorted(Comparator.comparing(OnlineUserResponse::getLastSeen).reversed())
            .toList();
    }

    public void heartbeat(UUID projectId, UUID userId, String displayName, String avatarUrl) {
        if (projectId == null || userId == null) return;

        presenceMap.compute(projectId, (k, currentUsers) -> {
            if (currentUsers == null) {
                currentUsers = new ConcurrentHashMap<>();
            }
            currentUsers.put(userId, OnlineUserResponse.builder()
                .userId(userId)
                .displayName(displayName != null && !displayName.isBlank() ? displayName : "Team Member")
                .avatarUrl(avatarUrl)
                .lastSeen(Instant.now())
                .build());
            return currentUsers;
        });

        broadcast(projectId);
    }

    public void leave(UUID projectId, UUID userId) {
        if (projectId == null || userId == null) return;

        ConcurrentHashMap<UUID, OnlineUserResponse> users = presenceMap.get(projectId);
        if (users != null) {
            OnlineUserResponse removed = users.remove(userId);
            if (removed != null) {
                broadcast(projectId);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void cleanupStalePresences() {
        Instant threshold = Instant.now().minus(TTL);

        for (Map.Entry<UUID, ConcurrentHashMap<UUID, OnlineUserResponse>> entry : presenceMap.entrySet()) {
            UUID projectId = entry.getKey();
            ConcurrentHashMap<UUID, OnlineUserResponse> users = entry.getValue();

            boolean changed = users.values().removeIf(u -> u.getLastSeen().isBefore(threshold));
            if (changed) {
                broadcast(projectId);
            }
        }
    }

    private void broadcast(UUID projectId) {
        try {
            List<OnlineUserResponse> onlineList = getOnlineUsers(projectId);
            messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/presence", onlineList);
        } catch (Exception e) {
            log.warn("Failed to broadcast presence update for project {}: {}", projectId, e.getMessage());
        }
    }
}
