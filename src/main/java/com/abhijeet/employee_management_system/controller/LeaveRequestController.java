package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.dto.LeaveRequestDto;
import com.abhijeet.employee_management_system.model.LeaveRequest;
import com.abhijeet.employee_management_system.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @PostMapping
    public LeaveRequest submit(@Valid @RequestBody LeaveRequestDto dto) {
        return leaveRequestService.submitLeaveRequest(dto);
    }

    @GetMapping("/me")
    public PagedModel<LeaveRequest> getMyLeaveRequests(@PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(leaveRequestService.getMyLeaveRequests(pageable));
    }

    @GetMapping
    public PagedModel<LeaveRequest> getAllLeaveRequests(@PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(leaveRequestService.getAllLeaveRequests(pageable));
    }

    @PutMapping("/{id}/approve")
    public LeaveRequest approve(@PathVariable Long id) {
        return leaveRequestService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public LeaveRequest reject(@PathVariable Long id) {
        return leaveRequestService.reject(id);
    }
}
