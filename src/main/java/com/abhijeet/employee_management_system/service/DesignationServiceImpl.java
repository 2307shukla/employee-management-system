package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.DesignationRequest;
import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Designation;
import com.abhijeet.employee_management_system.repository.DesignationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DesignationServiceImpl implements DesignationService {

    @Autowired
    private DesignationRepository designationRepository;

    @Override
    public Designation create(DesignationRequest request) {
        Designation designation = new Designation();
        designation.setTitle(request.getTitle());
        return designationRepository.save(designation);
    }

    @Override
    public Designation getById(Long id) {
        return designationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
    }

    @Override
    public List<Designation> getAll() {
        return designationRepository.findAll();
    }

    @Override
    public Designation update(Long id, DesignationRequest request) {
        Designation designation = getById(id);
        designation.setTitle(request.getTitle());
        return designationRepository.save(designation);
    }

    @Override
    public void delete(Long id) {
        Designation designation = getById(id);
        designationRepository.delete(designation);
    }
}
