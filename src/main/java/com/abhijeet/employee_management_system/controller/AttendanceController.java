package com.abhijeet.employee_management_system.controller;

import com.abhijeet.employee_management_system.model.Attendance;
import com.abhijeet.employee_management_system.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/check-in")
    public Attendance checkIn() {
        return attendanceService.checkIn();
    }

    @PostMapping("/check-out")
    public Attendance checkOut() {
        return attendanceService.checkOut();
    }

    @GetMapping("/me")
    public PagedModel<Attendance> getMyAttendance(@PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(attendanceService.getMyAttendance(pageable));
    }

    @GetMapping("/employee/{employeeId}")
    public PagedModel<Attendance> getAttendanceForEmployee(@PathVariable Long employeeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(attendanceService.getAttendanceForEmployee(employeeId, pageable));
    }

    @GetMapping
    public PagedModel<Attendance> getAllAttendance(@PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(attendanceService.getAllAttendance(pageable));
    }
}
