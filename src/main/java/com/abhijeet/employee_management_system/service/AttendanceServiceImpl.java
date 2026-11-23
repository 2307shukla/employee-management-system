package com.abhijeet.employee_management_system.service;

import com.abhijeet.employee_management_system.exception.ResourceNotFoundException;
import com.abhijeet.employee_management_system.model.Attendance;
import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.repository.AttendanceRepository;
import com.abhijeet.employee_management_system.repository.EmployeeRepository;
import com.abhijeet.employee_management_system.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public Attendance checkIn() {
        Long employeeId = requireLinkedEmployeeId();
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
            .orElseGet(() -> {
                Attendance a = new Attendance();
                a.setEmployee(getEmployee(employeeId));
                a.setDate(today);
                return a;
            });
        if (attendance.getCheckIn() != null) {
            throw new IllegalStateException("Already checked in today");
        }
        attendance.setCheckIn(LocalTime.now());
        return attendanceRepository.save(attendance);
    }

    @Override
    public Attendance checkOut() {
        Long employeeId = requireLinkedEmployeeId();
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
            .orElseThrow(() -> new IllegalStateException("You haven't checked in today"));
        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException("Already checked out today");
        }
        attendance.setCheckOut(LocalTime.now());
        return attendanceRepository.save(attendance);
    }

    @Override
    public Page<Attendance> getMyAttendance(Pageable pageable) {
        Long employeeId = requireLinkedEmployeeId();
        return attendanceRepository.findByEmployeeId(employeeId, pageable);
    }

    @Override
    public Page<Attendance> getAttendanceForEmployee(Long employeeId, Pageable pageable) {
        requireManager();
        return attendanceRepository.findByEmployeeId(employeeId, pageable);
    }

    @Override
    public Page<Attendance> getAllAttendance(Pageable pageable) {
        requireManager();
        return attendanceRepository.findAll(pageable);
    }

    private Long requireLinkedEmployeeId() {
        Long employeeId = currentUserService.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new ResourceNotFoundException(
                "Your account is not linked to an employee record. Register with an employeeCode, or ask an admin to link one.");
        }
        return employeeId;
    }

    private void requireManager() {
        if (!currentUserService.isManager()) {
            throw new AccessDeniedException("Only ADMIN/HR can view other employees' attendance");
        }
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }
}
