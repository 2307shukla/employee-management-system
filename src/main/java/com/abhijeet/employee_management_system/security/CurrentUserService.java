package com.abhijeet.employee_management_system.security;

import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Role;
import com.abhijeet.employee_management_system.model.User;
import com.abhijeet.employee_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + username));
    }

    /** The Employee id linked to the current user, or null if none is linked. */
    public Long getCurrentEmployeeId() {
        return getCurrentUser().getEmployeeId();
    }

    public boolean isManager() {
        Role role = getCurrentUser().getRole();
        return role == Role.ADMIN || role == Role.HR;
    }
}
