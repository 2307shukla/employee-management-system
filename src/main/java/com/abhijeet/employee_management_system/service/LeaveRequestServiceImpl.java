package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.LeaveRequestDto;
import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.model.LeaveRequest;
import com.abhijeet.employee_management_system.model.LeaveStatus;
import com.abhijeet.employee_management_system.repository.EmployeeRepository;
import com.abhijeet.employee_management_system.repository.LeaveRequestRepository;
import com.abhijeet.employee_management_system.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CurrentUserService currentUserService;

    private Employee requireLinkedEmployee() {
        Long employeeId = currentUserService.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new ResourceNotFoundException(
                "Your account is not linked to an employee record. Register with an employeeCode, or ask an admin to link one.");
        }
        return employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
    }

    private void requireManager() {
        if (!currentUserService.isManager()) {
            throw new AccessDeniedException("Only ADMIN/HR can view or action other employees' leave requests");
        }
    }

    @Override
    public LeaveRequest submitLeaveRequest(LeaveRequestDto dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalStateException("End date cannot be before start date");
        }
        Employee employee = requireLinkedEmployee();
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(dto.getLeaveType());
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public Page<LeaveRequest> getMyLeaveRequests(Pageable pageable) {
        Employee employee = requireLinkedEmployee();
        return leaveRequestRepository.findByEmployeeId(employee.getId(), pageable);
    }

    @Override
    public Page<LeaveRequest> getAllLeaveRequests(Pageable pageable) {
        requireManager();
        return leaveRequestRepository.findAll(pageable);
    }

    @Override
    public LeaveRequest approve(Long id) {
        requireManager();
        LeaveRequest leaveRequest = findById(id);
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be approved");
        }
        leaveRequest.setStatus(LeaveStatus.APPROVED);
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest reject(Long id) {
        requireManager();
        LeaveRequest leaveRequest = findById(id);
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be rejected");
        }
        leaveRequest.setStatus(LeaveStatus.REJECTED);
        return leaveRequestRepository.save(leaveRequest);
    }

    private LeaveRequest findById(Long id) {
        return leaveRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }
}
