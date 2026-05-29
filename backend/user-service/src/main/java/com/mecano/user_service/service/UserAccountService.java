package com.mecano.user_service.service;

import com.mecano.user_service.dto.UserAccountRequest;
import com.mecano.user_service.dto.UserAccountResponse;
import java.util.List;
import java.util.UUID;

public interface UserAccountService {
    UserAccountResponse saveProfile(UserAccountRequest request);
    UserAccountResponse getProfileById(UUID id);
    List<UserAccountResponse> getAllProfiles();
    void deleteProfile(UUID id);
}
