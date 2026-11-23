package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.model.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttendanceService {

    Attendance checkIn();

    Attendance checkOut();

    Page<Attendance> getMyAttendance(Pageable pageable);

    /** ADMIN/HR only - throws AccessDeniedException otherwise. */
    Page<Attendance> getAttendanceForEmployee(Long employeeId, Pageable pageable);

    /** ADMIN/HR only - throws AccessDeniedException otherwise. */
    Page<Attendance> getAllAttendance(Pageable pageable);
}
