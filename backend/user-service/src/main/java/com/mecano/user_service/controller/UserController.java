package com.mecano.user_service.controller;

import com.mecano.user_service.dto.UserAccountRequest;
import com.mecano.user_service.dto.UserAccountResponse;
import com.mecano.user_service.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAccountService service;

    public UserController(UserAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserAccountResponse> saveProfile(@Valid @RequestBody UserAccountRequest request) {
        return new ResponseEntity<>(service.saveProfile(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getProfileById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserAccountResponse>> getAllProfiles() {
        return ResponseEntity.ok(service.getAllProfiles());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
        service.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
