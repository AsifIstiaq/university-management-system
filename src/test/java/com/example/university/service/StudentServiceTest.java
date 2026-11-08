package com.example.university.service;

import com.example.university.dto.StudentRequest;
import com.example.university.entity.Student;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit tests for StudentService: the repository, AccountService and ModelMapper are mocked. */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private StudentService studentService;

    @Test
    void saveStudent_duplicateRoll_isRejectedBeforeAnythingIsSaved() {
        StudentRequest request = new StudentRequest();
        request.setRoll(" CSE-1 ");
        when(studentRepository.existsByRollIgnoreCase("CSE-1")).thenReturn(true);

        DuplicateResourceException ex =
                assertThrows(DuplicateResourceException.class, () -> studentService.saveStudent(request));

        assertEquals("roll", ex.getField());
        verify(studentRepository, never()).save(any());
        verifyNoInteractions(accountService, modelMapper);
    }

    @Test
    void updateStudent_trimsAndAppliesAcademicFields() {
        Student existing = new Student();
        existing.setId(7L);
        when(studentRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByRollIgnoreCaseAndIdNot("EEE-9", 7L)).thenReturn(false);
        when(studentRepository.save(existing)).thenReturn(existing);

        StudentRequest request = new StudentRequest();
        request.setRoll(" EEE-9 ");
        request.setDepartment(" EEE ");
        request.setSemester(4);

        Student updated = studentService.updateStudent(7L, request);

        assertEquals("EEE-9", updated.getRoll());
        assertEquals("EEE", updated.getDepartment());
        assertEquals(4, updated.getSemester());
        verify(accountService).applyProfile(existing, request);
    }
}
