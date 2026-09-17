package com.example.jari.notification.service;

import com.example.jari.issue.entity.Issue;
import com.example.jari.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily reminder for issues whose deadline is close (due date <= today + DUE_SOON_WINDOW_DAYS),
 * including overdue ones. NotificationService de-duplicates per issue/recipient/day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.due-soon-enabled", havingValue = "true", matchIfMissing = true)
public class IssueDueSoonScheduler {

    private final IssueRepository issueRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.notifications.due-soon-cron:0 0 8 * * *}", zone = "${app.notifications.due-soon-zone:Asia/Ho_Chi_Minh}")
    public void remindDueSoonIssues() {
        LocalDate maxDueDate = LocalDate.now().plusDays(NotificationService.DUE_SOON_WINDOW_DAYS);
        List<Issue> dueSoon = issueRepository.findIssuesDueSoon(maxDueDate);
        if (dueSoon.isEmpty()) return;

        log.info("Due-soon reminder: checking {} issue(s) with dueDate <= {}", dueSoon.size(), maxDueDate);
        int processed = 0;
        for (Issue issue : dueSoon) {
            try {
                notificationService.notifyIssueDueSoon(issue, issue.getAssignee());
                processed++;
            } catch (Exception e) {
                log.error("Failed to send due-soon reminder for issue {}: {}", issue.getId(), e.getMessage());
            }
        }
        log.info("Due-soon reminder finished ({} issue(s) processed, duplicates skipped inside service)", processed);
    }
}
