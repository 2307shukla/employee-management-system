package com.abhijeet.employee_management_system.repository;

import com.abhijeet.employee_management_system.model.Employee;
import com.abhijeet.employee_management_system.model.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    long countByStatus(EmployeeStatus status);
}