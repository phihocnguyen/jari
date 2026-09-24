package com.example.jari.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReportsResponse {

    private BurndownReport burndown;
    private VelocityReport velocity;
    private CumulativeFlowReport cumulativeFlow;
    private CreatedVsResolvedReport createdVsResolved;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownReport {
        private UUID sprintId;
        private String sprintName;
        private String sprintStatus;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalStoryPoints;
        private List<BurndownPoint> points;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownPoint {
        private LocalDate date;
        private BigDecimal idealRemaining;
        private BigDecimal actualRemaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityReport {
        private BigDecimal averageCompleted;
        private List<VelocitySprint> sprints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocitySprint {
        private UUID sprintId;
        private String sprintName;
        private String sprintStatus;
        private BigDecimal committed;
        private BigDecimal completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CumulativeFlowReport {
        private List<CumulativeFlowPoint> points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CumulativeFlowPoint {
        private LocalDate date;
        private long todo;
        private long inProgress;
        private long done;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedVsResolvedReport {
        /** Resolved / created over the window, 0–1. */
        private double resolutionRate;
        private long totalCreated;
        private long totalResolved;
        private List<CreatedVsResolvedPoint> points;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedVsResolvedPoint {
        private LocalDate date;
        private long created;
        private long resolved;
    }
}
