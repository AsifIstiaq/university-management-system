package com.example.university.service;

import com.example.university.dto.StudentRequest;
import com.example.university.entity.Student;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.exception.ResourceNotFoundException;
import com.example.university.repository.StudentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    public StudentService(StudentRepository studentRepository, AccountService accountService, ModelMapper modelMapper) {
        this.studentRepository = studentRepository;
        this.accountService = accountService;
        this.modelMapper = modelMapper;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + id));
    }

    public long count() {
        return studentRepository.count();
    }

    @Transactional
    public Student saveStudent(StudentRequest request) {
        assertRollAvailable(request.getRoll(), null);

        Student student = modelMapper.map(request, Student.class);
        accountService.initializeNewAccount(student, request);
        student.setRoll(request.getRoll().trim());
        student.setDepartment(request.getDepartment().trim());
        return studentRepository.save(student);
    }


    @Transactional
    public Student updateStudent(Long id, StudentRequest request) {
        Student student = getStudentById(id);
        assertRollAvailable(request.getRoll(), id);

        accountService.applyProfile(student, request);
        student.setRoll(request.getRoll().trim());
        student.setDepartment(request.getDepartment().trim());
        student.setSemester(request.getSemester());
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        accountService.deleteAccount(student.getId());
    }

    private void assertRollAvailable(String roll, Long excludeId) {
        String trimmed = roll.trim();
        boolean exists = excludeId == null
                ? studentRepository.existsByRollIgnoreCase(trimmed)
                : studentRepository.existsByRollIgnoreCaseAndIdNot(trimmed, excludeId);
        if (exists) {
            throw new DuplicateResourceException("roll", "Roll '" + trimmed + "' is already assigned");
        }
    }
}
