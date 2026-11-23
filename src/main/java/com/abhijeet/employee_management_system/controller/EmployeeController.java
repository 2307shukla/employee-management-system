package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.dto.EmployeeRequest;
import com.abhijeet.employee_management_system.dto.EmployeeSearchCriteria;
import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public PagedModel<Employee> getAllEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @PageableDefault(size = 20) Pageable pageable) {
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria();
        criteria.setName(name);
        criteria.setDepartmentId(departmentId);
        criteria.setDesignationId(designationId);
        criteria.setMinSalary(minSalary);
        criteria.setMaxSalary(maxSalary);
        // Spring Data warns that serializing a raw Page/PageImpl isn't a stable
        // JSON contract across versions; PagedModel is its own recommended fix.
        return new PagedModel<>(employeeService.searchEmployees(criteria, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }
}