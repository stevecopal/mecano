package com.mecano.user_service.service;

import com.mecano.user_service.dto.UserAccountRequest;
import com.mecano.user_service.dto.UserAccountResponse;
import com.mecano.user_service.exception.ResourceNotFoundException;
import com.mecano.user_service.mapper.UserMapper;
import com.mecano.user_service.model.UserAccount;
import com.mecano.user_service.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository repository;
    private final UserMapper mapper;

    public UserAccountServiceImpl(UserAccountRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserAccountResponse saveProfile(UserAccountRequest request) {
        UserAccount account = mapper.toEntity(request);
        UserAccount saved = repository.save(account);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountResponse getProfileById(UUID id) {
        UserAccount account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profil utilisateur introuvable pour l'ID : " + id));
        return mapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAccountResponse> getAllProfiles() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteProfile(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Impossible de supprimer : Profil introuvable pour l'ID : " + id);
        }
        repository.deleteById(id);
    }
}
