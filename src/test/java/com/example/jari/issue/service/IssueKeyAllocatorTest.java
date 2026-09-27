package com.example.jari.issue.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueKeyAllocatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private IssueKeyAllocator allocator;

    @Test
    void allocateNext_returnsExistingCounterIncrement() {
        UUID projectId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(projectId)))
            .thenReturn(List.of(7));

        assertThat(allocator.allocateNext(projectId)).isEqualTo(7);
        verify(jdbcTemplate, never()).update(anyString(), eq(projectId));
    }

    @Test
    void allocateNext_insertsCounterRowWhenMissingThenReturns() {
        UUID projectId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(projectId)))
            .thenReturn(List.of())
            .thenReturn(List.of(1));
        when(jdbcTemplate.update(anyString(), eq(projectId))).thenReturn(1);

        assertThat(allocator.allocateNext(projectId)).isEqualTo(1);
        verify(jdbcTemplate).update(contains("INSERT INTO project_issue_counter"), eq(projectId));
    }

    @Test
    void allocateNext_throwsWhenStillMissingAfterInsert() {
        UUID projectId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(projectId)))
            .thenReturn(List.of());
        when(jdbcTemplate.update(anyString(), eq(projectId))).thenReturn(0);

        assertThatThrownBy(() -> allocator.allocateNext(projectId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(projectId.toString());
    }

    @Test
    void ensureCounterRow_insertsIdempotently() {
        UUID projectId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(projectId))).thenReturn(1);

        allocator.ensureCounterRow(projectId);

        verify(jdbcTemplate).update(contains("INSERT INTO project_issue_counter"), eq(projectId));
    }
}
