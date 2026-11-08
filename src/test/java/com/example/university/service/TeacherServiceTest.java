package com.example.university.service;

import com.example.university.dto.TeacherRequest;
import com.example.university.entity.Teacher;
import com.example.university.exception.ResourceNotFoundException;
import com.example.university.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private TeacherService teacherService;

    @Test
    void getAllTeachers_shouldReturnTeachers() {
        Teacher t = new Teacher();
        t.setFullName("Rahim");
        when(teacherRepository.findAll()).thenReturn(List.of(t));

        List<Teacher> teachers = teacherService.getAllTeachers();

        assertEquals(1, teachers.size());
        verify(teacherRepository).findAll();
    }

    @Test
    void getTeacherById_shouldReturnTeacher() {
        Teacher t = new Teacher();
        t.setId(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));

        assertEquals(1L, teacherService.getTeacherById(1L).getId());
    }

    @Test
    void getTeacherById_missing_shouldThrowNotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.getTeacherById(99L));
    }

    @Test
    void saveTeacher_shouldMapInitializeAndSave() {
        TeacherRequest request = new TeacherRequest();
        request.setUsername("t1");
        request.setPassword("secret1");
        request.setFullName("Karim");
        request.setEmail("karim@uni.test");
        request.setDepartment(" CSE ");

        Teacher mapped = new Teacher();
        when(modelMapper.map(request, Teacher.class)).thenReturn(mapped);
        when(teacherRepository.save(mapped)).thenReturn(mapped);

        Teacher saved = teacherService.saveTeacher(request);

        assertEquals("CSE", saved.getDepartment());
        verify(accountService).initializeNewAccount(mapped, request);
        verify(teacherRepository).save(mapped);
    }

    @Test
    void deleteTeacher_shouldDelegateToAccountService() {
        Teacher t = new Teacher();
        t.setId(3L);
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(t));

        teacherService.deleteTeacher(3L);

        verify(accountService).deleteAccount(3L);
    }
}
