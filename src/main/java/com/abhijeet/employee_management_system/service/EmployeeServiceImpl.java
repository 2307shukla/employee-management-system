package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.EmployeeRequest;
import com.abhijeet.employee_management_system.dto.EmployeeSearchCriteria;
import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Department;
import com.abhijeet.employee_management_system.model.Designation;
import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.model.EmployeeStatus;
import com.abhijeet.employee_management_system.repository.DepartmentRepository;
import com.abhijeet.employee_management_system.repository.DesignationRepository;
import com.abhijeet.employee_management_system.repository.EmployeeRepository;
import com.abhijeet.employee_management_system.repository.EmployeeSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @Override
    public Employee createEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        applyRequest(employee, request);
        return employeeRepository.save(employee);
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    @Override
    public Page<Employee> searchEmployees(EmployeeSearchCriteria criteria, Pageable pageable) {
        // Spring Data 4.x's Specification.where(null) now throws
        // IllegalArgumentException instead of returning a match-all spec (verified
        // by running the tests - this isn't the classic null-safe idiom anymore).
        // cb.conjunction() is an explicit, always-true base to .and() onto instead.
        Specification<Employee> spec = (root, query, cb) -> cb.conjunction();

        if (criteria.getName() != null && !criteria.getName().isBlank()) {
            spec = spec.and(EmployeeSpecifications.nameContains(criteria.getName()));
        }
        if (criteria.getDepartmentId() != null) {
            spec = spec.and(EmployeeSpecifications.hasDepartment(criteria.getDepartmentId()));
        }
        if (criteria.getDesignationId() != null) {
            spec = spec.and(EmployeeSpecifications.hasDesignation(criteria.getDesignationId()));
        }
        if (criteria.getMinSalary() != null) {
            spec = spec.and(EmployeeSpecifications.salaryGreaterThanOrEqual(criteria.getMinSalary()));
        }
        if (criteria.getMaxSalary() != null) {
            spec = spec.and(EmployeeSpecifications.salaryLessThanOrEqual(criteria.getMaxSalary()));
        }

        return employeeRepository.findAll(spec, pageable);
    }

    @Override
    public Employee updateEmployee(Long id, EmployeeRequest request) {
        Employee existing = getEmployeeById(id);
        applyRequest(existing, request);
        return employeeRepository.save(existing);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        employeeRepository.delete(existing);
    }

    private void applyRequest(Employee employee, EmployeeRequest request) {
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setMobileNo(request.getMobileNo());
        employee.setDob(request.getDob());
        employee.setGender(request.getGender());
        employee.setSalary(request.getSalary());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setStatus(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE);

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        if (request.getDesignationId() != null) {
            Designation designation = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + request.getDesignationId()));
            employee.setDesignation(designation);
        } else {
            employee.setDesignation(null);
        }
    }
}
