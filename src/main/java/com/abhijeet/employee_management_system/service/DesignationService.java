package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.DesignationRequest;
import com.abhijeet.employee_management_system.model.Designation;
import java.util.List;

public interface DesignationService {
    Designation create(DesignationRequest request);
    Designation getById(Long id);
    List<Designation> getAll();
    Designation update(Long id, DesignationRequest request);
    void delete(Long id);
}
