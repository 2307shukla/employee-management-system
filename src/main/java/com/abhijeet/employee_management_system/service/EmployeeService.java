package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.EmployeeRequest;
import com.abhijeet.employee_management_system.dto.EmployeeSearchCriteria;
import com.abhijeet.employee_management_system.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    Employee createEmployee(EmployeeRequest request);

    Employee getEmployeeById(Long id);

    Page<Employee> searchEmployees(EmployeeSearchCriteria criteria, Pageable pageable);

    Employee updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
}
