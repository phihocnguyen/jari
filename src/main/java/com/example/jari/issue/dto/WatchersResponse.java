package com.example.jari.issue.dto;

import com.example.jari.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchersResponse {
    private long count;
    private boolean isWatching;
    private List<UserResponse> watchers;
}
