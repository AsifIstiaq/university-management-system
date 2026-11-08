package com.example.university.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the authorization matrix end to end (REST API with HTTP Basic, UI with form login).
 * Uses the demo accounts created by DataInitializer: admin, teacher1, student1.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAuthorizationIntegrationTest {

    private static final String NEW_TEACHER_JSON = """
            {"username":"newteacher","password":"secret123","fullName":"New Teacher",
             "email":"new.teacher@university.local","department":"Physics","designation":"Professor"}""";

    private static final String NEW_STUDENT_JSON = """
            {"username":"newstudent","password":"secret123","fullName":"New Student",
             "email":"new.student@university.local","roll":"PHY-100","department":"Physics","semester":2}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    private static RequestPostProcessor admin() {
        return httpBasic("admin", "admin123");
    }

    private static RequestPostProcessor teacher() {
        return httpBasic("teacher1", "teacher123");
    }

    private static RequestPostProcessor student() {
        return httpBasic("student1", "student123");
    }

    private RequestPostProcessor sessionOf(String username) {
        return user(userDetailsService.loadUserByUsername(username));
    }

    /** Looks up an account id through the admin API (admins may read every account type). */
    private Long idOf(String collectionPath, String username) throws Exception {
        String body = mockMvc.perform(get(collectionPath).with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\{[^{}]*\"id\":(\\d+)[^{}]*\"username\":\"" + username + "\"").matcher(body);
        if (!m.find()) {
            throw new AssertionError("No account '" + username + "' in " + collectionPath + ": " + body);
        }
        return Long.valueOf(m.group(1));
    }

    // ------------------------------------------------------------------ REST: authentication

    @Test
    void api_withoutCredentials_returns401Json() throws Exception {
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void api_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(get("/api/teachers").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ REST: admin

    @Test
    void api_adminCanListEverything() throws Exception {
        mockMvc.perform(get("/api/teachers").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='teacher1')]").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        mockMvc.perform(get("/api/students").with(admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/admins").with(admin())).andExpect(status().isOk());
    }

    @Test
    void api_adminCanCrudTeacher() throws Exception {
        String location = mockMvc.perform(post("/api/teachers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("newteacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getHeader("Location");

        String path = location.substring(location.indexOf("/api/"));

        mockMvc.perform(get(path).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Physics"));

        mockMvc.perform(put(path).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Renamed\",\"email\":\"new.teacher@university.local\","
                                + "\"department\":\"Chemistry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed"))
                .andExpect(jsonPath("$.department").value("Chemistry"));

        mockMvc.perform(delete(path).with(admin())).andExpect(status().isNoContent());
        mockMvc.perform(get(path).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void api_adminCanCreateStudent_andDuplicatesAreRejected() throws Exception {
        mockMvc.perform(post("/api/students").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_STUDENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roll").value("PHY-100"));

        mockMvc.perform(post("/api/students").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_STUDENT_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void api_validationErrorsReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/teachers").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.department").exists());
    }

    @Test
    void api_lastAdminCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/admins/me").with(admin()))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ REST: teacher

    @Test
    void api_teacherCannotTouchStudentsOrAdmins() throws Exception {
        mockMvc.perform(get("/api/students").with(teacher())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/students").with(teacher())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_STUDENT_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/students/1").with(teacher())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_STUDENT_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/students/1").with(teacher())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admins").with(teacher())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admins").with(teacher())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admins/1").with(teacher())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admins/me").with(teacher())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/students/me").with(teacher())).andExpect(status().isForbidden());
    }

    @Test
    void api_teacherCanCrudTeachers() throws Exception {
        mockMvc.perform(get("/api/teachers").with(teacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='teacher1')]").exists());

        String location = mockMvc.perform(post("/api/teachers").with(teacher())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getHeader("Location");
        String path = location.substring(location.indexOf("/api/"));

        mockMvc.perform(get(path).with(teacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newteacher"));

        mockMvc.perform(put(path).with(teacher()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Renamed By Teacher\",\"email\":\"new.teacher@university.local\","
                                + "\"department\":\"Chemistry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed By Teacher"))
                .andExpect(jsonPath("$.department").value("Chemistry"));

        mockMvc.perform(delete(path).with(teacher())).andExpect(status().isNoContent());
        mockMvc.perform(get(path).with(teacher())).andExpect(status().isNotFound());
    }

    @Test
    void api_teacherCannotReachAnAdminOrStudentThroughTheTeacherEndpoints() throws Exception {
        // Ids are shared by all account types, but /api/teachers only ever finds teachers.
        Long adminId = idOf("/api/admins", "admin");
        Long studentId = idOf("/api/students", "student1");

        mockMvc.perform(get("/api/teachers/" + adminId).with(teacher())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/teachers/" + adminId).with(teacher())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/teachers/" + studentId).with(teacher())).andExpect(status().isNotFound());
        // ...and the targets are still there.
        mockMvc.perform(get("/api/admins/" + adminId).with(admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/students/" + studentId).with(admin())).andExpect(status().isOk());
    }

    @Test
    void api_teacherCanReadAndUpdateOwnRecord_butNotEscalate() throws Exception {
        mockMvc.perform(get("/api/teachers/me").with(teacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("teacher1"));

        mockMvc.perform(put("/api/teachers/me").with(teacher()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Teacher\",\"email\":\"teacher1@university.local\","
                                + "\"phone\":\"555\",\"role\":\"ADMIN\",\"department\":\"Hacked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Teacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.department").value("Computer Science"));
    }

    // ------------------------------------------------------------------ REST: student

    @Test
    void api_studentCannotTouchTeachersOrAdmins() throws Exception {
        mockMvc.perform(get("/api/teachers").with(student())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/teachers").with(student())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/teachers/1").with(student())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_TEACHER_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/teachers/1").with(student())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admins").with(student())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admins/1").with(student())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teachers/me").with(student())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admins/me").with(student())).andExpect(status().isForbidden());
    }

    @Test
    void api_studentCanCrudStudents() throws Exception {
        mockMvc.perform(get("/api/students").with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='student1')]").exists());

        String location = mockMvc.perform(post("/api/students").with(student())
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_STUDENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getHeader("Location");
        String path = location.substring(location.indexOf("/api/"));

        mockMvc.perform(get(path).with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roll").value("PHY-100"));

        mockMvc.perform(put(path).with(student()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Renamed By Student\",\"email\":\"new.student@university.local\","
                                + "\"roll\":\"PHY-100\",\"department\":\"Chemistry\",\"semester\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed By Student"))
                .andExpect(jsonPath("$.semester").value(3));

        mockMvc.perform(delete(path).with(student())).andExpect(status().isNoContent());
        mockMvc.perform(get(path).with(student())).andExpect(status().isNotFound());
    }

    @Test
    void api_studentCannotReachAnAdminOrTeacherThroughTheStudentEndpoints() throws Exception {
        Long adminId = idOf("/api/admins", "admin");
        Long teacherId = idOf("/api/teachers", "teacher1");

        mockMvc.perform(get("/api/students/" + adminId).with(student())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/students/" + adminId).with(student())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/students/" + teacherId).with(student())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admins/" + adminId).with(admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/teachers/" + teacherId).with(admin())).andExpect(status().isOk());
    }

    @Test
    void api_studentCanReadUpdateAndDeleteOwnRecord() throws Exception {
        mockMvc.perform(get("/api/students/me").with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roll").value("CSE-001"));

        mockMvc.perform(put("/api/students/me").with(student()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Student\",\"email\":\"student1@university.local\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Student"));

        mockMvc.perform(delete("/api/students/me").with(student()))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------ UI

    @Test
    void ui_anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/teacher/profile")).andExpect(status().is3xxRedirection());
    }

    @Test
    void ui_loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void ui_formLoginSucceedsAndFails() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("admin123"))
                .andExpect(authenticated().withUsername("admin"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(formLogin().user("admin").password("nope"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void ui_panelsAreRestrictedByRole() throws Exception {
        mockMvc.perform(get("/admin").with(sessionOf("student1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/teachers").with(sessionOf("student1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/students").with(sessionOf("teacher1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/teachers").with(sessionOf("teacher1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/student/students").with(sessionOf("teacher1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/student/students/new").with(sessionOf("teacher1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/student/profile").with(sessionOf("teacher1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/teachers").with(sessionOf("student1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/teachers/new").with(sessionOf("student1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/teachers").with(sessionOf("admin"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/profile").with(sessionOf("student1"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/teacher/profile").with(sessionOf("admin"))).andExpect(status().isForbidden());
    }

    @Test
    void ui_adminPagesRender() throws Exception {
        mockMvc.perform(get("/admin").with(sessionOf("admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
        mockMvc.perform(get("/admin/teachers").with(sessionOf("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("teacher1")));
        mockMvc.perform(get("/admin/students").with(sessionOf("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CSE-001")));
        mockMvc.perform(get("/admin/admins").with(sessionOf("admin"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/teachers/new").with(sessionOf("admin"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/students/new").with(sessionOf("admin"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/profile").with(sessionOf("admin"))).andExpect(status().isOk());
    }

    @Test
    void ui_teacherAndStudentSeeTheirOwnProfile() throws Exception {
        mockMvc.perform(get("/teacher/profile").with(sessionOf("teacher1")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(content().string(containsString("Computer Science")));
        mockMvc.perform(get("/student/profile/edit").with(sessionOf("student1")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }

    @Test
    void ui_adminCreatesTeacherThroughForm_andCsrfIsEnforced() throws Exception {
        mockMvc.perform(post("/admin/teachers").with(sessionOf("admin"))
                        .param("username", "formteacher").param("password", "secret123")
                        .param("fullName", "Form Teacher").param("email", "form.teacher@university.local")
                        .param("department", "Math"))
                .andExpect(status().isForbidden()); // no CSRF token

        mockMvc.perform(post("/admin/teachers").with(sessionOf("admin")).with(csrf())
                        .param("username", "formteacher").param("password", "secret123")
                        .param("fullName", "Form Teacher").param("email", "form.teacher@university.local")
                        .param("department", "Math"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/teachers"));
    }

    @Test
    void ui_teacherManagesTeachersInTheTeacherPanel() throws Exception {
        mockMvc.perform(get("/teacher/teachers").with(sessionOf("teacher1")))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/teachers"))
                .andExpect(content().string(containsString("teacher1")))
                .andExpect(content().string(containsString("/teacher/teachers/new")));
        mockMvc.perform(get("/teacher/teachers/new").with(sessionOf("teacher1")))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/teacher-form"));

        mockMvc.perform(post("/teacher/teachers").with(sessionOf("teacher1")).with(csrf())
                        .param("username", "uiteacher").param("password", "secret123")
                        .param("fullName", "UI Teacher").param("email", "ui.teacher@university.local")
                        .param("department", "Math"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/teachers"));

        Long id = idOf("/api/teachers", "uiteacher");
        mockMvc.perform(get("/teacher/teachers/" + id + "/edit").with(sessionOf("teacher1")))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/teacher-form"));
        mockMvc.perform(post("/teacher/teachers/" + id).with(sessionOf("teacher1")).with(csrf())
                        .param("username", "uiteacher").param("fullName", "UI Teacher 2")
                        .param("email", "ui.teacher@university.local").param("department", "Math"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/teachers"));
        mockMvc.perform(post("/teacher/teachers/" + id + "/delete").with(sessionOf("teacher1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher/teachers"));
        mockMvc.perform(get("/api/teachers/" + id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void ui_teacherCannotDeleteOwnAccountFromTheList() throws Exception {
        Long ownId = idOf("/api/teachers", "teacher1");
        mockMvc.perform(post("/teacher/teachers/" + ownId + "/delete").with(sessionOf("teacher1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
        mockMvc.perform(get("/api/teachers/" + ownId).with(admin())).andExpect(status().isOk());
    }

    @Test
    void ui_studentManagesStudentsInTheStudentPanel() throws Exception {
        mockMvc.perform(get("/student/students").with(sessionOf("student1")))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/students"))
                .andExpect(content().string(containsString("CSE-001")))
                .andExpect(content().string(containsString("/student/students/new")));
        mockMvc.perform(get("/student/students/new").with(sessionOf("student1")))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/student-form"));

        mockMvc.perform(post("/student/students").with(sessionOf("student1")).with(csrf())
                        .param("username", "uistudent").param("password", "secret123")
                        .param("fullName", "UI Student").param("email", "ui.student@university.local")
                        .param("roll", "UI-1").param("department", "Math").param("semester", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/students"));

        Long id = idOf("/api/students", "uistudent");
        mockMvc.perform(post("/student/students/" + id).with(sessionOf("student1")).with(csrf())
                        .param("username", "uistudent").param("fullName", "UI Student 2")
                        .param("email", "ui.student@university.local")
                        .param("roll", "UI-1").param("department", "Math").param("semester", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/students"));
        mockMvc.perform(post("/student/students/" + id + "/delete").with(sessionOf("student1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/students"));
        mockMvc.perform(get("/api/students/" + id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void ui_studentCannotDeleteOwnAccountFromTheList() throws Exception {
        Long ownId = idOf("/api/students", "student1");
        mockMvc.perform(post("/student/students/" + ownId + "/delete").with(sessionOf("student1")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
        mockMvc.perform(get("/api/students/" + ownId).with(admin())).andExpect(status().isOk());
    }

    @Test
    void ui_adminStillUsesTheAdminPanelUrls() throws Exception {
        mockMvc.perform(get("/admin/teachers").with(sessionOf("admin")))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/teachers"))
                .andExpect(content().string(containsString("/admin/teachers/new")));
        mockMvc.perform(post("/admin/students").with(sessionOf("admin")).with(csrf())
                        .param("username", "adminmade").param("password", "secret123")
                        .param("fullName", "Admin Made").param("email", "admin.made@university.local")
                        .param("roll", "ADM-1").param("department", "Math").param("semester", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students"));
    }

    @Test
    void ui_invalidFormIsRedisplayedWithErrors() throws Exception {
        mockMvc.perform(post("/admin/teachers").with(sessionOf("admin")).with(csrf())
                        .param("username", "").param("password", "").param("fullName", "")
                        .param("email", "bad"))
                .andExpect(status().isOk())
                .andExpect(view().name("manage/teacher-form"))
                .andExpect(model().attributeHasFieldErrors("teacher", "username", "password", "fullName", "email", "department"));
    }

    @Test
    void ui_studentUpdatesOwnProfile() throws Exception {
        mockMvc.perform(post("/student/profile").with(sessionOf("student1")).with(csrf())
                        .param("fullName", "Renamed Student").param("email", "student1@university.local"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/profile"));
    }
}
