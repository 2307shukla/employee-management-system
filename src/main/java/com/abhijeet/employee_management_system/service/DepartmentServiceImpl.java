package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.DepartmentRequest;
import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Department;
import com.abhijeet.employee_management_system.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public Department create(DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.getName());
        return departmentRepository.save(department);
    }

    @Override
    public Department getById(Long id) {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    @Override
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    @Override
    public Department update(Long id, DepartmentRequest request) {
        Department department = getById(id);
        department.setName(request.getName());
        return departmentRepository.save(department);
    }

    @Override
    public void delete(Long id) {
        Department department = getById(id);
        departmentRepository.delete(department);
    }
}
