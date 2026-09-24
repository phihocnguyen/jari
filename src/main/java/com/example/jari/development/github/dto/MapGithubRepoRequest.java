package com.example.jari.development.github.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MapGithubRepoRequest {
    /** Pass null to unmap the repo from any project. */
    private UUID projectId;
}
