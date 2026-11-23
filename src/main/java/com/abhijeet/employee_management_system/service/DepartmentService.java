package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.DepartmentRequest;
import com.abhijeet.employee_management_system.model.Department;
import java.util.List;

public interface DepartmentService {
    Department create(DepartmentRequest request);
    Department getById(Long id);
    List<Department> getAll();
    Department update(Long id, DepartmentRequest request);
    void delete(Long id);
}
