package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.dto.AuthRequest;
import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.model.User;
import com.abhijeet.employee_management_system.repository.EmployeeRepository;
import com.abhijeet.employee_management_system.repository.UserRepository;
import com.abhijeet.employee_management_system.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Username is already taken"));
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if (request.getEmployeeCode() != null && !request.getEmployeeCode().isBlank()) {
            Optional<Employee> employee = employeeRepository.findByEmployeeCode(request.getEmployeeCode());
            if (employee.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No employee found with code: " + request.getEmployeeCode()));
            }
            user.setEmployeeId(employee.get().getId());
        }

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("token", token, "username", user.getUsername(), "role", user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        return userRepository.findByUsername(request.getUsername())
            .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            .map(user -> {
                String token = jwtUtil.generateToken(user.getUsername());
                return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername(), "role", user.getRole().name()));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password")));
    }
}
