package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Role;
import com.abhijeet.employee_management_system.model.User;
import com.abhijeet.employee_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
            .map(u -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", u.getId());
                dto.put("username", u.getUsername());
                dto.put("role", u.getRole().name());
                dto.put("employeeId", u.getEmployeeId());
                return dto;
            })
            .toList();
    }

    @PutMapping("/{id}/role")
    public Map<String, String> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        Role role = Role.valueOf(body.get("role").toUpperCase());
        user.setRole(role);
        userRepository.save(user);
        return Map.of("username", user.getUsername(), "role", user.getRole().name());
    }
}
