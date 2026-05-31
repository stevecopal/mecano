package com.mecano.user_service.mapper;

import com.mecano.user_service.dto.UserAccountRequest;
import com.mecano.user_service.dto.UserAccountResponse;
import com.mecano.user_service.model.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserAccount toEntity(UserAccountRequest request) {
        if (request == null) return null;
        return UserAccount.builder()
                .id(request.getId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .avatarUrl(request.getAvatarUrl())
                .build();
    }

    public UserAccountResponse toResponse(UserAccount entity) {
        if (entity == null) return null;
        return UserAccountResponse.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .avatarUrl(entity.getAvatarUrl())
                .displayName(entity.getFirstName() + " " + entity.getLastName().toUpperCase())
                .build();
    }
}
