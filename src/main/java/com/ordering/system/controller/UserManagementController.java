package com.ordering.system.controller;

import com.ordering.system.entity.User;
import com.ordering.system.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("userCount", users.size());
        return "users";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role,
            RedirectAttributes ra) {

        try {
            // Validate passwords match
            if (!password.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Passwords do not match!");
                return "redirect:/users";
            }

            // Validate password length
            if (password.length() < 6) {
                ra.addFlashAttribute("error", "Password must be at least 6 characters!");
                return "redirect:/users";
            }

            // Check if username already exists
            if (userService.usernameExists(username)) {
                ra.addFlashAttribute("error", "Username already exists!");
                return "redirect:/users";
            }

            // Prevent registering with username "admin"
            if ("admin".equalsIgnoreCase(username)) {
                ra.addFlashAttribute("error", "Username 'admin' is reserved!");
                return "redirect:/users";
            }

            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            userService.saveUser(user);

            ra.addFlashAttribute("success", "User '" + username + "' registered successfully!");
            return "redirect:/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/users";
        }
    }

    @PostMapping("/role/{id}")
    public String updateRole(
            @PathVariable Long id,
            @RequestParam String role,
            RedirectAttributes ra) {

        try {
            List<User> users = userService.getAllUsers();
            User user = users.stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setRole(role);
            userService.updateUser(user);
            ra.addFlashAttribute("success", "User role updated to " + role + "!");
            return "redirect:/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating role: " + e.getMessage());
            return "redirect:/users";
        }
    }

    @PostMapping("/password/{id}")
    public String updatePassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            RedirectAttributes ra) {

        try {
            if (newPassword.length() < 6) {
                ra.addFlashAttribute("error", "Password must be at least 6 characters!");
                return "redirect:/users";
            }

            List<User> users = userService.getAllUsers();
            User user = users.stream()
                    .filter(u -> u.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            ra.addFlashAttribute("success", "Password updated successfully!");
            return "redirect:/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating password: " + e.getMessage());
            return "redirect:/users";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes ra) {

        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted successfully!");
            return "redirect:/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
            return "redirect:/users";
        }
    }
}
