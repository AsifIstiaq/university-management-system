package com.example.university.config;

import com.example.university.entity.Admin;
import com.example.university.entity.Student;
import com.example.university.entity.Teacher;
import com.example.university.repository.AdminRepository;
import com.example.university.repository.StudentRepository;
import com.example.university.repository.TeacherRepository;
import com.example.university.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.email:admin@university.local}")
    private String adminEmail;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    public DataInitializer(AdminRepository adminRepository,
                           TeacherRepository teacherRepository,
                           StudentRepository studentRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("System Administrator");
            admin.setEmail(adminEmail);
            adminRepository.save(admin);
            log.info("Created bootstrap admin account '{}'", adminUsername);
        }

        if (seedDemoData) {
            seedDemoTeacher();
            seedDemoStudent();
        }
    }

    private void seedDemoTeacher() {
        if (userRepository.existsByUsernameIgnoreCase("teacher1")) {
            return;
        }
        Teacher teacher = new Teacher();
        teacher.setUsername("teacher1");
        teacher.setPassword(passwordEncoder.encode("teacher123"));
        teacher.setFullName("Demo Teacher");
        teacher.setEmail("teacher1@university.local");
        teacher.setDepartment("Computer Science");
        teacher.setDesignation("Lecturer");
        teacherRepository.save(teacher);
        log.info("Created demo teacher 'teacher1'");
    }

    private void seedDemoStudent() {
        if (userRepository.existsByUsernameIgnoreCase("student1")) {
            return;
        }
        Student student = new Student();
        student.setUsername("student1");
        student.setPassword(passwordEncoder.encode("student123"));
        student.setFullName("Demo Student");
        student.setEmail("student1@university.local");
        student.setRoll("CSE-001");
        student.setDepartment("Computer Science");
        student.setSemester(1);
        studentRepository.save(student);
        log.info("Created demo student 'student1'");
    }
}
