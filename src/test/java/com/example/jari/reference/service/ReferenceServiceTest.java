package com.example.jari.reference.service;

import com.example.jari.issue.entity.IssueType;
import com.example.jari.issue.entity.Priority;
import com.example.jari.issue.entity.Status;
import com.example.jari.issue.repository.IssueTypeRepository;
import com.example.jari.issue.repository.PriorityRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.reference.dto.ReferenceItemResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceServiceTest {

    @Mock
    private IssueTypeRepository issueTypeRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private PriorityRepository priorityRepository;

    @InjectMocks
    private ReferenceService referenceService;

    @Test
    void getIssueTypes_mapsEntitiesToResponses() {
        UUID id = UUID.randomUUID();
        when(issueTypeRepository.findAll()).thenReturn(List.of(
            IssueType.builder().id(id).name("Story").description("User story").build()
        ));

        List<ReferenceItemResponse> result = referenceService.getIssueTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
        assertThat(result.get(0).getName()).isEqualTo("Story");
        assertThat(result.get(0).getDescription()).isEqualTo("User story");
        verify(issueTypeRepository).findAll();
    }

    @Test
    void getStatuses_mapsCategoryToExtra() {
        when(statusRepository.findAll()).thenReturn(List.of(
            Status.builder().id(UUID.randomUUID()).name("In Progress").category("IN_PROGRESS").build()
        ));

        List<ReferenceItemResponse> result = referenceService.getStatuses();

        assertThat(result).singleElement().satisfies(item ->
            assertThat(item.getExtra()).isEqualTo("IN_PROGRESS"));
    }

    @Test
    void getPriorities_ordersFromRepository() {
        when(priorityRepository.findAllOrderByLevel()).thenReturn(List.of(
            Priority.builder().id(UUID.randomUUID()).name("High").level(1).build()
        ));

        List<ReferenceItemResponse> result = referenceService.getPriorities();

        assertThat(result).singleElement().satisfies(item ->
            assertThat(item.getExtra()).isEqualTo("1"));
        verify(priorityRepository).findAllOrderByLevel();
    }
}
