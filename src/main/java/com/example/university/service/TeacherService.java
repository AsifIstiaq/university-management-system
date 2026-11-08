package com.example.university.service;

import com.example.university.dto.TeacherRequest;
import com.example.university.entity.Teacher;
import com.example.university.exception.ResourceNotFoundException;
import com.example.university.repository.TeacherRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    public TeacherService(TeacherRepository teacherRepository, AccountService accountService, ModelMapper modelMapper) {
        this.teacherRepository = teacherRepository;
        this.accountService = accountService;
        this.modelMapper = modelMapper;
    }

    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id " + id));
    }

    public long count() {
        return teacherRepository.count();
    }

    @Transactional
    public Teacher saveTeacher(TeacherRequest request) {
        Teacher teacher = modelMapper.map(request, Teacher.class);
        accountService.initializeNewAccount(teacher, request);
        teacher.setDepartment(request.getDepartment().trim());
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher updateTeacher(Long id, TeacherRequest request) {
        Teacher teacher = getTeacherById(id);
        accountService.applyProfile(teacher, request);
        teacher.setDepartment(request.getDepartment().trim());
        teacher.setDesignation(request.getDesignation());
        return teacherRepository.save(teacher);
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = getTeacherById(id);
        accountService.deleteAccount(teacher.getId());
    }
}
