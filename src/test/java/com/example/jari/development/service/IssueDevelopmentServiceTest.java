package com.example.jari.development.service;

import com.example.jari.development.dto.CreateDevelopmentRequest;
import com.example.jari.development.entity.IssueDevelopment;
import com.example.jari.development.repository.IssueDevelopmentRepository;
import com.example.jari.issue.repository.IssueRepository;
import com.example.jari.shared.exception.ResourceNotFoundException;
import com.example.jari.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueDevelopmentServiceTest {

    @Mock private IssueDevelopmentRepository developmentRepository;
    @Mock private IssueRepository issueRepository;
    @InjectMocks private IssueDevelopmentService issueDevelopmentService;

    @Test
    void list_requiresExistingIssue() {
        UUID issueId = UUID.randomUUID();
        when(issueRepository.existsById(issueId)).thenReturn(false);

        assertThatThrownBy(() -> issueDevelopmentService.list(issueId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_normalizesFields() {
        UUID issueId = UUID.randomUUID();
        var issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Dev"))));
        CreateDevelopmentRequest req = new CreateDevelopmentRequest();
        req.setType(" pull_request ");
        req.setTitle(" Fix bug ");
        req.setUrl(" https://github.com/a/b ");
        req.setRepoUrl("https://github.com/a/b");
        IssueDevelopment saved = IssueDevelopment.builder()
            .id(UUID.randomUUID())
            .issue(issue)
            .type("PULL_REQUEST")
            .title("Fix bug")
            .url("https://github.com/a/b")
            .status("OPEN")
            .build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(developmentRepository.save(any())).thenReturn(saved);

        var response = issueDevelopmentService.create(issueId, req);

        assertThat(response.getType()).isEqualTo("PULL_REQUEST");
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void create_usesProvidedStatus() {
        UUID issueId = UUID.randomUUID();
        var issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Dev"))));
        CreateDevelopmentRequest req = new CreateDevelopmentRequest();
        req.setType("commit");
        req.setTitle("Commit");
        req.setUrl("https://x");
        req.setRepoUrl("https://x");
        req.setStatus(" merged ");
        IssueDevelopment saved = IssueDevelopment.builder()
            .id(UUID.randomUUID()).issue(issue).type("COMMIT").title("Commit")
            .url("https://x").status("MERGED").build();

        when(issueRepository.findById(issueId)).thenReturn(Optional.of(issue));
        when(developmentRepository.save(any())).thenAnswer(inv -> {
            IssueDevelopment dev = inv.getArgument(0);
            dev.setId(saved.getId());
            return dev;
        });

        var response = issueDevelopmentService.create(issueId, req);

        assertThat(response.getStatus()).isEqualTo("MERGED");
    }

    @Test
    void delete_requiresExistingRecord() {
        UUID id = UUID.randomUUID();
        when(developmentRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> issueDevelopmentService.delete(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void list_mapsRecords() {
        UUID issueId = UUID.randomUUID();
        var issue = TestFixtures.issue(issueId, TestFixtures.project(UUID.randomUUID(),
            TestFixtures.workspace(UUID.randomUUID(), TestFixtures.user(UUID.randomUUID(), "Dev"))));
        IssueDevelopment dev = IssueDevelopment.builder().id(UUID.randomUUID()).issue(issue).type("COMMIT")
            .title("T").url("u").status("OPEN").build();
        when(issueRepository.existsById(issueId)).thenReturn(true);
        when(developmentRepository.findByIssueIdOrderByCreatedAtDesc(issueId)).thenReturn(List.of(dev));

        assertThat(issueDevelopmentService.list(issueId)).hasSize(1);
    }
}
