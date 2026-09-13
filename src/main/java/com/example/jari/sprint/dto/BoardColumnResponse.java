package com.example.jari.sprint.dto;
import com.example.jari.issue.dto.IssueResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class BoardColumnResponse {
    private UUID statusId;
    private String statusName;
    private String statusCategory;
    private List<IssueResponse> issues;
}
