package com.example.university.controller;

import com.example.university.dto.OnCreate;
import com.example.university.dto.UserRequest;
import com.example.university.entity.Admin;
import com.example.university.exception.BusinessRuleException;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.security.UserPrincipal;
import com.example.university.service.AdminService;
import com.example.university.service.StudentService;
import com.example.university.service.TeacherService;
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
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final ModelMapper modelMapper;

    public AdminController(AdminService adminService,
                           TeacherService teacherService,
                           StudentService studentService,
                           ModelMapper modelMapper) {
        this.adminService = adminService;
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("adminCount", adminService.count());
        model.addAttribute("teacherCount", teacherService.count());
        model.addAttribute("studentCount", studentService.count());
        return "admin/dashboard";
    }

    @GetMapping("/admins")
    public String list(Model model) {
        model.addAttribute("admins", adminService.getAllAdmins());
        return "admin/admins";
    }

    @GetMapping("/admins/new")
    public String createForm(Model model) {
        model.addAttribute("admin", new UserRequest());
        model.addAttribute("editMode", false);
        return "admin/admin-form";
    }

    @PostMapping("/admins")
    public String create(@Validated(OnCreate.class) @ModelAttribute("admin") UserRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                adminService.saveAdmin(request);
                redirectAttributes.addFlashAttribute("success", "Admin created successfully.");
                return "redirect:/admin/admins";
            } catch (DuplicateResourceException ex) {
                result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("editMode", false);
        return "admin/admin-form";
    }

    @GetMapping("/admins/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Admin admin = adminService.getAdminById(id);
        UserRequest form = modelMapper.map(admin, UserRequest.class);
        form.setPassword(null);
        model.addAttribute("admin", form);
        model.addAttribute("adminId", id);
        model.addAttribute("editMode", true);
        return "admin/admin-form";
    }

    @PostMapping("/admins/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("admin") UserRequest request,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                adminService.updateAdmin(id, request);
                redirectAttributes.addFlashAttribute("success", "Admin updated successfully.");
                return "redirect:/admin/admins";
            } catch (DuplicateResourceException ex) {
                result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("adminId", id);
        model.addAttribute("editMode", true);
        return "admin/admin-form";
    }

    @PostMapping("/admins/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        if (id.equals(principal.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "To delete your own account use the \"My Profile\" page.");
            return "redirect:/admin/admins";
        }
        try {
            adminService.deleteAdmin(id);
            redirectAttributes.addFlashAttribute("success", "Admin deleted successfully.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/admins";
    }
}
