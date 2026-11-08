package com.example.university.controller;

import com.example.university.dto.OnCreate;
import com.example.university.dto.StudentRequest;
import com.example.university.entity.Role;
import com.example.university.entity.Student;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.security.UserPrincipal;
import com.example.university.service.StudentService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/admin/students", "/student/students"})
public class StudentManagementController {

    private final StudentService studentService;
    private final ModelMapper modelMapper;

    public StudentManagementController(StudentService studentService, ModelMapper modelMapper) {
        this.studentService = studentService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("base", base(principal));
        model.addAttribute("students", studentService.getAllStudents());
        return "manage/students";
    }

    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("base", base(principal));
        model.addAttribute("student", new StudentRequest());
        model.addAttribute("editMode", false);
        return "manage/student-form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserPrincipal principal,
                         @Validated(OnCreate.class) @ModelAttribute("student") StudentRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                studentService.saveStudent(request);
                redirectAttributes.addFlashAttribute("success", "Student created successfully.");
                return "redirect:" + base(principal);
            } catch (DuplicateResourceException ex) {
                result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("base", base(principal));
        model.addAttribute("editMode", false);
        return "manage/student-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable Long id, Model model) {
        Student student = studentService.getStudentById(id);
        StudentRequest form = modelMapper.map(student, StudentRequest.class);
        form.setPassword(null); // never put the stored hash into a form
        model.addAttribute("base", base(principal));
        model.addAttribute("student", form);
        model.addAttribute("studentId", id);
        model.addAttribute("editMode", true);
        return "manage/student-form";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal UserPrincipal principal,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("student") StudentRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                studentService.updateStudent(id, request);
                redirectAttributes.addFlashAttribute("success", "Student updated successfully.");
                return "redirect:" + base(principal);
            } catch (DuplicateResourceException ex) {
                result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("base", base(principal));
        model.addAttribute("studentId", id);
        model.addAttribute("editMode", true);
        return "manage/student-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserPrincipal principal,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        if (id.equals(principal.getId())) {

            redirectAttributes.addFlashAttribute("error",
                    "To delete your own account use the \"My Profile\" page.");
            return "redirect:" + base(principal);
        }
        studentService.deleteStudent(id);
        redirectAttributes.addFlashAttribute("success", "Student deleted successfully.");
        return "redirect:" + base(principal);
    }

    private static String base(UserPrincipal principal) {
        return principal.getRole() == Role.ADMIN ? "/admin/students" : "/student/students";
    }
}
