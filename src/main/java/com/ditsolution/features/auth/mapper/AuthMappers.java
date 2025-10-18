package com.ditsolution.features.auth.mapper;

import com.ditsolution.features.auth.dto.UserDto;
import com.ditsolution.features.auth.entity.UserEntity;

public class AuthMappers {
    public static UserDto toDto(UserEntity u) {
        if (u == null) return null;
        return new UserDto(
                u.id,
                u.role,
                u.status,
                u.email,
                u.phoneE164,
                u.phoneVerified,
                u.emailVerified,
                u.firstName,
                u.lastName,
                u.avatarUrl,
                u.createdAt,
                u.updatedAt
        );
    }
}


