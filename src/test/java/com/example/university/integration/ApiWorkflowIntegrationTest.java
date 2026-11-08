package com.example.university.integration;

import com.example.university.entity.Role;
import com.example.university.entity.User;
import com.example.university.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the real application context (controllers, security, services, JPA) runs on an
 * in-memory H2 database, and the tests check what actually ends up in the database.
 * Each test is rolled back afterwards, so tests do not affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiWorkflowIntegrationTest {

    private static final String NEW_TEACHER_JSON = """
            {"username":"workflowteacher","password":"secret123","fullName":"Workflow Teacher",
             "email":"workflow.teacher@university.local","department":"Physics","designation":"Professor"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private static RequestPostProcessor admin() {
        return httpBasic("admin", "admin123");
    }

    private int rowsWithId(String table, long id) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }

    @Test
    void adminCreatesTeacher_passwordIsHashedInDatabase_andNewTeacherCanLogIn() throws Exception {
        mockMvc.perform(post("/api/teachers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON))
                .andExpect(status().isCreated());

        // What was really stored: a BCrypt hash, never the plain text.
        User stored = userRepository.findByUsernameIgnoreCase("workflowteacher").orElseThrow();
        assertEquals(Role.TEACHER, stored.getRole());
        assertNotEquals("secret123", stored.getPassword());
        assertTrue(passwordEncoder.matches("secret123", stored.getPassword()));

        // ...and the new teacher can authenticate with the original password.
        mockMvc.perform(get("/api/teachers/me").with(httpBasic("workflowteacher", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("workflowteacher"))
                .andExpect(jsonPath("$.department").value("Physics"));
    }

    @Test
    void teacherChangesPassword_oldPasswordStopsWorking_newOneWorks() throws Exception {
        RequestPostProcessor oldLogin = httpBasic("teacher1", "teacher123");
        RequestPostProcessor newLogin = httpBasic("teacher1", "newSecret456");

        mockMvc.perform(put("/api/teachers/me").with(oldLogin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Demo Teacher\",\"email\":\"teacher1@university.local\","
                                + "\"password\":\"newSecret456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teachers/me").with(oldLogin)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/teachers/me").with(newLogin)).andExpect(status().isOk());
    }

    @Test
    void deletingATeacher_removesTheRowFromBothTables() throws Exception {
        String location = mockMvc.perform(post("/api/teachers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        entityManager.flush();
        assertEquals(1, rowsWithId("users", id));
        assertEquals(1, rowsWithId("teachers", id));

        mockMvc.perform(delete("/api/teachers/" + id).with(admin())).andExpect(status().isNoContent());

        entityManager.flush();
        assertEquals(0, rowsWithId("users", id));
        assertEquals(0, rowsWithId("teachers", id));
    }
}
