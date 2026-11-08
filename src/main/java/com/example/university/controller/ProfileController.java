package com.example.university.controller;

import com.example.university.dto.ProfileRequest;
import com.example.university.entity.User;
import com.example.university.exception.BusinessRuleException;
import com.example.university.exception.DuplicateResourceException;
import com.example.university.security.UserPrincipal;
import com.example.university.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/admin/profile", "/teacher/profile", "/student/profile"})
public class ProfileController {

    private final AccountService accountService;

    public ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("account", accountService.getAccount(principal.getId()));
        model.addAttribute("panel", panel(principal));
        return "profile/view";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        User user = accountService.getAccount(principal.getId());
        ProfileRequest form = new ProfileRequest();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        model.addAttribute("profile", form);
        model.addAttribute("panel", panel(principal));
        return "profile/edit";
    }

    @PostMapping
    public String update(@AuthenticationPrincipal UserPrincipal principal,
                         @Valid @ModelAttribute("profile") ProfileRequest profile,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        model.addAttribute("panel", panel(principal));
        if (result.hasErrors()) {
            return "profile/edit";
        }
        try {
            accountService.updateProfile(principal.getId(), profile);
        } catch (DuplicateResourceException ex) {
            result.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "profile/edit";
        }
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/" + panel(principal) + "/profile";
    }

    @PostMapping("/delete")
    public String delete(@AuthenticationPrincipal UserPrincipal principal,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteAccount(principal.getId());
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/" + panel(principal) + "/profile";
        }

        new SecurityContextLogoutHandler()
                .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?deleted";
    }

    private static String panel(UserPrincipal principal) {
        return principal.getRole().name().toLowerCase();
    }
}
