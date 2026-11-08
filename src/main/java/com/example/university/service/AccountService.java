package com.example.university.service;

import com.example.university.dto.ProfileRequest;
import com.example.university.dto.UserRequest;
import com.example.university.entity.Role;
import com.example.university.entity.User;
import com.example.university.exception.BusinessRuleException;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.exception.ResourceNotFoundException;
import com.example.university.repository.AdminRepository;
import com.example.university.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
                          AdminRepository adminRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getAccount(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id " + id));
    }


    public void initializeNewAccount(User user, UserRequest request) {
        assertUsernameAvailable(request.getUsername());
        assertEmailAvailable(request.getEmail(), null);

        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPhone(blankToNull(request.getPhone()));
    }


    public void applyProfile(User user, ProfileRequest request) {
        assertEmailAvailable(request.getEmail(), user.getId());

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPhone(blankToNull(request.getPhone()));
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    @Transactional
    public User updateProfile(Long id, ProfileRequest request) {
        User user = getAccount(id);
        applyProfile(user, request);
        return userRepository.save(user);
    }


    @Transactional
    public void deleteAccount(Long id) {
        User user = getAccount(id);
        if (user.getRole() == Role.ADMIN && adminRepository.count() <= 1) {
            throw new BusinessRuleException("The last remaining admin account cannot be deleted.");
        }
        userRepository.delete(user);
    }

    public void assertUsernameAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new DuplicateResourceException("username", "Username '" + username.trim() + "' is already taken");
        }
    }

    public void assertEmailAvailable(String email, Long excludeId) {
        String trimmed = email.trim();
        boolean exists = excludeId == null
                ? userRepository.existsByEmailIgnoreCase(trimmed)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(trimmed, excludeId);
        if (exists) {
            throw new DuplicateResourceException("email", "Email '" + trimmed + "' is already in use");
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
