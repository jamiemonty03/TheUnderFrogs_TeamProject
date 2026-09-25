package com.neueda.accountservice.services;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.neueda.accountservice.models.User;
import com.neueda.accountservice.repositories.UserRepository;
import com.neueda.accountservice.services.JwtService;
import com.neueda.accountservice.dtos.LoginRequest;
import com.neueda.accountservice.dtos.RegisterRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Model constructor will validate input format
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        
        user.setUpdatedBy("SYSTEM");
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());
        user.setToken(token);

        return user;
    }

    public User login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.username());
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("User account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername());
        user.setToken(token);

        return user;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, User updatedUser) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        
        if (updatedUser.getFullName() != null) {
            user.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(updatedUser.getEmail());
        }

        user.setLastUpdated(LocalDateTime.now());
        user.setVersion(user.getVersion() + 1);
        
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(id);
    }
}
