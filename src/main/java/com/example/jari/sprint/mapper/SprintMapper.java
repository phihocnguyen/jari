package com.example.jari.sprint.mapper;
import com.example.jari.sprint.dto.SprintResponse;
import com.example.jari.sprint.entity.Sprint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SprintMapper {
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "status",     target = "status")
    SprintResponse toResponse(Sprint sprint);
}
