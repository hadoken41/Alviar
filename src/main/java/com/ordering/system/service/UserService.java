package com.ordering.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordering.system.entity.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File dataDir;
    private final File usersFile;

    public UserService() {
        // Create a 'data' folder in the project root to persist user data
        String projectRoot = System.getProperty("user.dir");
        this.dataDir = new File(projectRoot, "data");
        
        // Ensure the data directory exists
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        this.usersFile = new File(dataDir, "users.json");
        
        // Initialize with empty array if file doesn't exist
        if (!usersFile.exists()) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(usersFile, new User[0]);
            } catch (IOException e) {
                System.err.println("Failed to create users.json: " + e.getMessage());
            }
        }
    }

    /**
     * Get all users from JSON file
     */
    public List<User> getAllUsers() {
        try {
            if (!usersFile.exists()) {
                System.out.println("⚠️  users.json not found at: " + usersFile.getAbsolutePath());
                return new ArrayList<>();
            }
            String content = new String(Files.readAllBytes(usersFile.toPath()));
            if (content.trim().isEmpty()) {
                System.out.println("⚠️  users.json is empty");
                return new ArrayList<>();
            }
            User[] users = objectMapper.readValue(content, User[].class);
            System.out.println("✅ Loaded " + users.length + " users from " + usersFile.getAbsolutePath());
            return List.of(users);
        } catch (IOException e) {
            System.err.println("❌ Error reading users: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return getAllUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    /**
     * Save a new user to JSON file
     */
    public void saveUser(User user) throws IOException {
        List<User> users = new ArrayList<>(getAllUsers());

        // Generate ID (max ID + 1)
        long maxId = users.stream()
                .mapToLong(User::getId)
                .max()
                .orElse(0L);
        user.setId(maxId + 1);

        users.add(user);

        // Write back to file
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(usersFile, users);
        
        System.out.println("✅ User saved: " + user.getUsername() + " (" + user.getRole() + ") to " + usersFile.getAbsolutePath());
    }

    /**
     * Check if username already exists
     */
    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * Update an existing user
     */
    public void updateUser(User user) throws IOException {
        List<User> users = new ArrayList<>(getAllUsers());
        users = users.stream()
                .map(u -> u.getId().equals(user.getId()) ? user : u)
                .toList();

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(usersFile, users);
    }

    /**
     * Delete a user by ID
     */
    public void deleteUser(Long id) throws IOException {
        List<User> users = new ArrayList<>(getAllUsers());
        users = users.stream()
                .filter(u -> !u.getId().equals(id))
                .toList();

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(usersFile, users);
    }
}
