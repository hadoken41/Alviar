package com.ordering.system.controller;

import com.ordering.system.entity.User;
import com.ordering.system.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role,
            Model model,
            RedirectAttributes ra) {

        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "register";
        }

        // Validate password length
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters!");
            return "register";
        }

        // Validate username length
        if (username.length() < 3) {
            model.addAttribute("error", "Username must be at least 3 characters!");
            return "register";
        }

        // Prevent registering with username "admin" if not registering as ADMIN
        if ("admin".equalsIgnoreCase(username) && !("ADMIN".equals(role))) {
            model.addAttribute("error", "Username 'admin' is reserved for admin accounts only.");
            return "register";
        }

        // Check if username already exists
        if (userService.usernameExists(username)) {
            model.addAttribute("error", "Username already exists! Please choose another.");
            return "register";
        }

        try {
            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            userService.saveUser(user);

            ra.addFlashAttribute("success", "Account created successfully! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating account. Please try again.");
            return "register";
        }
    }
}
