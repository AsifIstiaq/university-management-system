package com.example.university.service;

import com.example.university.dto.ProfileRequest;
import com.example.university.entity.Admin;
import com.example.university.entity.Teacher;
import com.example.university.exception.BusinessRuleException;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.repository.AdminRepository;
import com.example.university.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void deleteAccount_lastAdmin_isRejected() {
        Admin admin = new Admin();
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.count()).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> accountService.deleteAccount(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_adminWhenOthersExist_isDeleted() {
        Admin admin = new Admin();
        admin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.count()).thenReturn(2L);

        accountService.deleteAccount(1L);

        verify(userRepository).delete(admin);
    }

    @Test
    void deleteAccount_teacher_doesNotCareAboutAdminCount() {
        Teacher teacher = new Teacher();
        teacher.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacher));

        accountService.deleteAccount(2L);

        verify(userRepository).delete(teacher);
        verifyNoInteractions(adminRepository);
    }

    @Test
    void applyProfile_blankPassword_keepsExistingHash() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);
        teacher.setPassword("existing-hash");

        ProfileRequest request = new ProfileRequest();
        request.setFullName("  New Name ");
        request.setEmail("new@uni.test");
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@uni.test", 5L)).thenReturn(false);

        accountService.applyProfile(teacher, request);

        assertEquals("New Name", teacher.getFullName());
        assertEquals("existing-hash", teacher.getPassword());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void applyProfile_newPassword_isHashed() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);
        teacher.setPassword("existing-hash");

        ProfileRequest request = new ProfileRequest();
        request.setFullName("Name");
        request.setEmail("new@uni.test");
        request.setPassword("brand-new");
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@uni.test", 5L)).thenReturn(false);
        when(passwordEncoder.encode("brand-new")).thenReturn("hashed-new");

        accountService.applyProfile(teacher, request);

        assertEquals("hashed-new", teacher.getPassword());
    }

    @Test
    void applyProfile_emailUsedByAnotherAccount_isRejected() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);

        ProfileRequest request = new ProfileRequest();
        request.setFullName("Name");
        request.setEmail("taken@uni.test");
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@uni.test", 5L)).thenReturn(true);

        DuplicateResourceException ex =
                assertThrows(DuplicateResourceException.class, () -> accountService.applyProfile(teacher, request));
        assertEquals("email", ex.getField());
    }
}
