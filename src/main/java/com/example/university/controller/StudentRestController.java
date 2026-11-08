package com.example.university.controller;

import com.example.university.dto.OnCreate;
import com.example.university.dto.ProfileRequest;
import com.example.university.dto.StudentRequest;
import com.example.university.dto.StudentResponse;
import com.example.university.entity.Student;
import com.example.university.entity.User;
import com.example.university.security.UserPrincipal;
import com.example.university.service.AccountService;
import com.example.university.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentRestController {

    private final StudentService studentService;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    public StudentRestController(StudentService studentService, AccountService accountService, ModelMapper modelMapper) {
        this.studentService = studentService;
        this.accountService = accountService;
        this.modelMapper = modelMapper;
    }


    @GetMapping
    public List<StudentResponse> getAll() {
        return studentService.getAllStudents().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        return toResponse(studentService.getStudentById(id));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Validated(OnCreate.class) @RequestBody StudentRequest request) {
        Student saved = studentService.saveStudent(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return toResponse(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/me")
    public StudentResponse getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return toResponse(studentService.getStudentById(principal.getId()));
    }

    @PutMapping("/me")
    public StudentResponse updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody ProfileRequest request) {
        User updated = accountService.updateProfile(principal.getId(), request);
        return toResponse(updated);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal UserPrincipal principal) {
        accountService.deleteAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }

    private StudentResponse toResponse(User student) {
        return modelMapper.map(student, StudentResponse.class);
    }
}
