package com.example.jari.shared.config;

import com.example.jari.project.entity.Project;
import com.example.jari.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StringToUuidConverter implements Converter<String, UUID> {

    private final ProjectRepository projectRepository;

    @Override
    public UUID convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        // Tự động map các alias demo từ frontend thành UUID của project đầu tiên trong database
        if ("proj-demo-1".equalsIgnoreCase(source)
                || "proj-demo-2".equalsIgnoreCase(source)
                || "demo".equalsIgnoreCase(source)
                || "default".equalsIgnoreCase(source)) {
            return projectRepository.findAll().stream()
                .findFirst()
                .map(Project::getId)
                .orElseGet(() -> UUID.fromString("00000000-0000-0000-0000-000000000003"));
        }

        return UUID.fromString(source);
    }
}
