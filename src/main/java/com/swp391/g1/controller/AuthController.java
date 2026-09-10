package com.swp391.g1.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Serves the public login page and the protected landing page.
 *
 * <p>Authentication is handled entirely by Spring Security; this controller only
 * renders Thymeleaf views and never verifies credentials itself.
 */
@Controller
public class AuthController {

    /**
     * Renders the login page.
     *
     * <p>{@code error} is present after a failed login and {@code logout} after a
     * successful logout (both produced by Spring Security's redirects).
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            HttpServletRequest request,
            Model model) {
        model.addAttribute("loginError", error != null);
        model.addAttribute("logoutSuccess", logout != null);
        return "login";
    }

    /**
     * Renders the protected landing page (access enforced by the security chain).
     */
    @GetMapping("/home")
    public String home() {
        return "home";
    }
}