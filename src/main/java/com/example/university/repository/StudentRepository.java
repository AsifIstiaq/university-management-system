package com.example.university.repository;

import com.example.university.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRollIgnoreCase(String roll);

    boolean existsByRollIgnoreCaseAndIdNot(String roll, Long id);
}
