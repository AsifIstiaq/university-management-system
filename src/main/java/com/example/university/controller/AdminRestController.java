package com.example.university.controller;

import com.example.university.dto.OnCreate;
import com.example.university.dto.ProfileRequest;
import com.example.university.dto.UserRequest;
import com.example.university.dto.UserResponse;
import com.example.university.entity.User;
import com.example.university.security.UserPrincipal;
import com.example.university.service.AccountService;
import com.example.university.service.AdminService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminRestController {

    private final AdminService adminService;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    public AdminRestController(AdminService adminService, AccountService accountService, ModelMapper modelMapper) {
        this.adminService = adminService;
        this.accountService = accountService;
        this.modelMapper = modelMapper;
    }


    @GetMapping
    public List<UserResponse> getAll() {
        return adminService.getAllAdmins().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return toResponse(adminService.getAdminById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Validated(OnCreate.class) @RequestBody UserRequest request) {
        User saved = adminService.saveAdmin(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return toResponse(adminService.updateAdmin(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me")
    public UserResponse getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return toResponse(adminService.getAdminById(principal.getId()));
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                 @Valid @RequestBody ProfileRequest request) {
        return toResponse(accountService.updateProfile(principal.getId(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        accountService.deleteAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User admin) {
        return modelMapper.map(admin, UserResponse.class);
    }
}
