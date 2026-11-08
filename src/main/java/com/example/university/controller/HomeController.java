package com.example.university.controller;

import com.example.university.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserPrincipal principal) {
        return switch (principal.getRole()) {
            case ADMIN -> "redirect:/admin";
            case TEACHER -> "redirect:/teacher/profile";
            case STUDENT -> "redirect:/student/profile";
        };
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UserPrincipal principal) {
        return principal != null ? "redirect:/" : "login";
    }
}
