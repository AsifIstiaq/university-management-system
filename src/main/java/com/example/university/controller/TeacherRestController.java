package com.example.university.controller;

import com.example.university.dto.OnCreate;
import com.example.university.dto.ProfileRequest;
import com.example.university.dto.TeacherRequest;
import com.example.university.dto.TeacherResponse;
import com.example.university.entity.Teacher;
import com.example.university.entity.User;
import com.example.university.security.UserPrincipal;
import com.example.university.service.AccountService;
import com.example.university.service.TeacherService;
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
@RequestMapping("/api/teachers")
public class TeacherRestController {

    private final TeacherService teacherService;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    public TeacherRestController(TeacherService teacherService, AccountService accountService, ModelMapper modelMapper) {
        this.teacherService = teacherService;
        this.accountService = accountService;
        this.modelMapper = modelMapper;
    }


    @GetMapping
    public List<TeacherResponse> getAll() {
        return teacherService.getAllTeachers().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TeacherResponse getById(@PathVariable Long id) {
        return toResponse(teacherService.getTeacherById(id));
    }

    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Validated(OnCreate.class) @RequestBody TeacherRequest request) {
        Teacher saved = teacherService.saveTeacher(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return toResponse(teacherService.updateTeacher(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me")
    public TeacherResponse getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return toResponse(teacherService.getTeacherById(principal.getId()));
    }

    @PutMapping("/me")
    public TeacherResponse updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody ProfileRequest request) {
        User updated = accountService.updateProfile(principal.getId(), request);
        return toResponse(updated);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        accountService.deleteAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }

    private TeacherResponse toResponse(User teacher) {
        return modelMapper.map(teacher, TeacherResponse.class);
    }
}
