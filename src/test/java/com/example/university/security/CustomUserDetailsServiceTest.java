package com.example.university.security;

import com.example.university.entity.Admin;
import com.example.university.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Unit tests for the class Spring Security calls at login time. */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_existingUser_returnsPrincipalWithStoredHash() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword("stored-bcrypt-hash");
        when(userRepository.findByUsernameIgnoreCase("ADMIN")).thenReturn(Optional.of(admin));

        UserDetails details = userDetailsService.loadUserByUsername("ADMIN");

        assertInstanceOf(UserPrincipal.class, details);
        assertEquals("admin", details.getUsername());
        assertEquals("stored-bcrypt-hash", details.getPassword());
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("ghost"));
    }
}
