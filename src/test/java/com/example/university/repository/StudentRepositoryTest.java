package com.example.university.repository;

import com.example.university.entity.Role;
import com.example.university.entity.Student;
import com.example.university.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private Student newStudent(String username, String roll, String email) {
        Student student = new Student();
        student.setUsername(username);
        student.setPassword("hash");
        student.setFullName("Student " + username);
        student.setEmail(email);
        student.setRoll(roll);
        student.setDepartment("CSE");
        student.setSemester(1);
        return student;
    }

    @Test
    void testFindById() {
        Student student = studentRepository.save(newStudent("abc", "1", "abc@uni.test"));

        Optional<Student> found = studentRepository.findById(student.getId());

        assertTrue(found.isPresent());
        assertEquals(Role.STUDENT, found.get().getRole());
    }

    @Test
    void testFindAll() {
        studentRepository.save(newStudent("abc", "1", "abc@uni.test"));
        studentRepository.save(newStudent("def", "2", "def@uni.test"));

        assertEquals(2, studentRepository.findAll().size());
    }

    @Test
    void testExistsByRollIgnoreCase() {
        Student saved = studentRepository.save(newStudent("abc", "CSE-7", "abc@uni.test"));

        assertTrue(studentRepository.existsByRollIgnoreCase("cse-7"));
        assertFalse(studentRepository.existsByRollIgnoreCaseAndIdNot("cse-7", saved.getId()));
        assertFalse(studentRepository.existsByRollIgnoreCase("other"));
    }

    @Test
    void testUserRepositoryFindsStudentPolymorphically() {
        studentRepository.save(newStudent("Abc", "1", "abc@uni.test"));

        Optional<User> found = userRepository.findByUsernameIgnoreCase("aBC");

        assertTrue(found.isPresent());
        assertInstanceOf(Student.class, found.get());
        assertTrue(userRepository.existsByEmailIgnoreCase("ABC@uni.test"));
    }
}
