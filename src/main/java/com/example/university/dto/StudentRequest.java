package com.example.university.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentRequest extends UserRequest {

    @NotBlank(message = "Roll is required")
    @Size(max = 30, message = "Roll must be at most 30 characters")
    private String roll;

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must be at most 100 characters")
    private String department;

    @Min(value = 1, message = "Semester must be between 1 and 12")
    @Max(value = 12, message = "Semester must be between 1 and 12")
    private Integer semester;

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }
}
