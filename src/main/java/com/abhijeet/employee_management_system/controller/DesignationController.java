package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.dto.DesignationRequest;
import com.abhijeet.employee_management_system.model.Designation;
import com.abhijeet.employee_management_system.service.DesignationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/designations")
public class DesignationController {

    @Autowired
    private DesignationService designationService;

    @GetMapping
    public List<Designation> getAll() {
        return designationService.getAll();
    }

    @GetMapping("/{id}")
    public Designation getById(@PathVariable Long id) {
        return designationService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Designation create(@Valid @RequestBody DesignationRequest request) {
        return designationService.create(request);
    }

    @PutMapping("/{id}")
    public Designation update(@PathVariable Long id, @Valid @RequestBody DesignationRequest request) {
        return designationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        designationService.delete(id);
    }
}
