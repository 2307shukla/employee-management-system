package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    Employee createEmployee(Employee employee);

    Employee getEmployeeById(Long id);

    Page<Employee> getAllEmployees(Pageable pageable);

    Employee updateEmployee(Long id, Employee employeeDetails);

    void deleteEmployee(Long id);
}
