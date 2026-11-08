package com.example.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TeacherRequest extends UserRequest {

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must be at most 100 characters")
    private String department;

    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
