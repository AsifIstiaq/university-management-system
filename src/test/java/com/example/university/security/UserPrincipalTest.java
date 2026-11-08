package com.example.university.security;

import com.example.university.entity.Role;
import com.example.university.entity.Teacher;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit test: the logged-in user object must carry the id and the ROLE_ authority. */
class UserPrincipalTest {

    @Test
    void principal_copiesAccountDataAndExposesRoleAuthority() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);
        teacher.setUsername("t1");
        teacher.setPassword("hash");

        UserPrincipal principal = new UserPrincipal(teacher);

        assertEquals(5L, principal.getId().longValue());
        assertEquals("t1", principal.getUsername());
        assertEquals("hash", principal.getPassword());
        assertEquals(Role.TEACHER, principal.getRole());
        assertEquals(List.of("ROLE_TEACHER"),
                principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertTrue(principal.isEnabled());
        assertTrue(principal.isAccountNonLocked());
    }
}
