package com.abhijeet.employee_management_system.repository;

import com.abhijeet.employee_management_system.model.LeaveRequest;
import com.abhijeet.employee_management_system.model.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        LeaveStatus status, LocalDate onOrAfterStart, LocalDate onOrBeforeEnd);
}
