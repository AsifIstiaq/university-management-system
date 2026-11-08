package com.example.university.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test of the Bean Validation rules (no Spring context needed): username and password are
 * mandatory only when creating, while department is required for both create and update.
 */
class TeacherRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> invalidFields(Set<ConstraintViolation<TeacherRequest>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void createGroupRequiresCredentials_butUpdateDoesNot() {
        TeacherRequest request = new TeacherRequest();
        request.setFullName("Alice Example");
        request.setEmail("alice@uni.test");

        assertEquals(Set.of("username", "password", "department"),
                invalidFields(validator.validate(request, OnCreate.class)));
        assertEquals(Set.of("department"),
                invalidFields(validator.validate(request)));
    }
}
