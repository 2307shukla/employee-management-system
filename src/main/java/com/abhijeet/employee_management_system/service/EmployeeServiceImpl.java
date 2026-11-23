package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    @Override
    public Page<Employee> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Override
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee existing = getEmployeeById(id);
        existing.setFirstName(employeeDetails.getFirstName());
        existing.setLastName(employeeDetails.getLastName());
        existing.setEmail(employeeDetails.getEmail());
        existing.setDepartment(employeeDetails.getDepartment());
        existing.setPosition(employeeDetails.getPosition());
        existing.setSalary(employeeDetails.getSalary());
        return employeeRepository.save(existing);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        employeeRepository.delete(existing);
    }
}
