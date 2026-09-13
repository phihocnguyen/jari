package com.example.jari.project.mapper;
import com.example.jari.project.dto.ProjectMemberResponse;
import com.example.jari.project.dto.ProjectResponse;
import com.example.jari.project.entity.Project;
import com.example.jari.project.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "lead.id",      target = "leadId")
    @Mapping(source = "projectType",  target = "projectType")
    @Mapping(source = "status",       target = "status")
    ProjectResponse toResponse(Project project);

    @Mapping(source = "user.id",          target = "userId")
    @Mapping(source = "user.displayName", target = "displayName")
    @Mapping(source = "user.email",       target = "email")
    @Mapping(source = "role.name",        target = "roleName")
    @Mapping(source = "joinedAt",         target = "joinedAt")
    ProjectMemberResponse toMemberResponse(ProjectMember member);
}
