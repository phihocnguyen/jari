package com.example.jari.workspace.mapper;

import com.example.jari.workspace.dto.WorkspaceMemberResponse;
import com.example.jari.workspace.dto.WorkspaceResponse;
import com.example.jari.workspace.entity.Workspace;
import com.example.jari.workspace.entity.WorkspaceMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    WorkspaceResponse toResponse(Workspace workspace);

    @Mapping(source = "user.id",          target = "userId")
    @Mapping(source = "user.displayName", target = "displayName")
    @Mapping(source = "user.email",       target = "email")
    @Mapping(source = "role.name",        target = "roleName")
    @Mapping(source = "joinedAt",         target = "joinedAt")
    WorkspaceMemberResponse toMemberResponse(WorkspaceMember member);
}
