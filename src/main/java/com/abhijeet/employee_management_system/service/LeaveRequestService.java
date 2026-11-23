package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.dto.LeaveRequestDto;
import com.abhijeet.employee_management_system.model.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaveRequestService {
    LeaveRequest submitLeaveRequest(LeaveRequestDto dto);
    Page<LeaveRequest> getMyLeaveRequests(Pageable pageable);
    Page<LeaveRequest> getAllLeaveRequests(Pageable pageable);
    LeaveRequest approve(Long id);
    LeaveRequest reject(Long id);
}
