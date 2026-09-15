package com.example.jari.reference.service;

import com.example.jari.issue.repository.IssueTypeRepository;
import com.example.jari.issue.repository.PriorityRepository;
import com.example.jari.issue.repository.StatusRepository;
import com.example.jari.reference.dto.ReferenceItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferenceService {

    private final IssueTypeRepository issueTypeRepository;
    private final StatusRepository statusRepository;
    private final PriorityRepository priorityRepository;

    @Cacheable("ref:issue-types")
    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> getIssueTypes() {
        return issueTypeRepository.findAll().stream()
            .map(t -> ReferenceItemResponse.builder()
                .id(t.getId()).name(t.getName()).description(t.getDescription()).build())
            .toList();
    }

    @Cacheable("ref:statuses")
    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> getStatuses() {
        return statusRepository.findAll().stream()
            .map(s -> ReferenceItemResponse.builder()
                .id(s.getId()).name(s.getName()).extra(s.getCategory()).build())
            .toList();
    }

    @Cacheable("ref:priorities")
    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> getPriorities() {
        return priorityRepository.findAllOrderByLevel().stream()
            .map(p -> ReferenceItemResponse.builder()
                .id(p.getId()).name(p.getName()).extra(String.valueOf(p.getLevel())).build())
            .toList();
    }
}
