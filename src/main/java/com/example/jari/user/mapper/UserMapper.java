package com.example.jari.user.mapper;

import com.example.jari.user.dto.UserResponse;
import com.example.jari.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "status", target = "status")
    UserResponse toResponse(User user);
}
