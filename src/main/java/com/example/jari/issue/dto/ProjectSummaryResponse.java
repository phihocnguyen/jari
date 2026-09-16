package com.example.jari.issue.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProjectSummaryResponse {

    private Metrics metrics;
    private List<StatusCount> statusBreakdown;
    private List<PriorityCount> priorityBreakdown;
    private List<TypeCount> typeBreakdown;
    private List<MemberWorkload> teamWorkload;
    private List<ActivityItem> recentActivity;
    private List<EpicProgress> epicProgress;

    @Data
    @Builder
    public static class Metrics {
        private long completedLast7Days;
        private long updatedLast7Days;
        private long createdLast7Days;
        private long dueNext7Days;
    }

    @Data
    @Builder
    public static class StatusCount {
        private String status;
        private long count;
    }

    @Data
    @Builder
    public static class PriorityCount {
        private String priority;
        private long count;
    }

    @Data
    @Builder
    public static class TypeCount {
        private String type;
        private long count;
    }

    @Data
    @Builder
    public static class MemberWorkload {
        private UUID userId;
        private String fullName;
        private long assignedCount;
        private long inProgressCount;
        private int percent;
    }

    @Data
    @Builder
    public static class ActivityItem {
        private String actorName;
        private String action;
        private String issueKey;
        private String issueTitle;
        private OffsetDateTime occurredAt;
    }

    @Data
    @Builder
    public static class EpicProgress {
        private UUID epicId;
        private String epicKey;
        private String epicTitle;
        private int total;
        private int doneCount;
        private int inProgressCount;
        private int todoCount;
        private int donePercent;
        private int inProgressPercent;
        private int todoPercent;
    }
}
