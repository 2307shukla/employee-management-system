package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.dto.DashboardStats;
import com.abhijeet.employee_management_system.model.EmployeeStatus;
import com.abhijeet.employee_management_system.model.LeaveStatus;
import com.abhijeet.employee_management_system.repository.DepartmentRepository;
import com.abhijeet.employee_management_system.repository.EmployeeRepository;
import com.abhijeet.employee_management_system.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @GetMapping
    public DashboardStats getStats() {
        LocalDate today = LocalDate.now();
        return new DashboardStats(
            employeeRepository.count(),
            employeeRepository.countByStatus(EmployeeStatus.ACTIVE),
            departmentRepository.count(),
            leaveRequestRepository.countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                LeaveStatus.APPROVED, today, today)
        );
    }
}
