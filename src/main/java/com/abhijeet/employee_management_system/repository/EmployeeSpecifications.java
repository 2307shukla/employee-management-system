package com.abhijeet.employee_management_system.repository;

import com.abhijeet.employee_management_system.model.Employee;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {}

    public static Specification<Employee> nameContains(String name) {
        return (root, query, cb) -> {
            String pattern = "%" + name.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    public static Specification<Employee> hasDepartment(Long departmentId) {
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> hasDesignation(Long designationId) {
        return (root, query, cb) -> cb.equal(root.get("designation").get("id"), designationId);
    }

    public static Specification<Employee> salaryGreaterThanOrEqual(Double minSalary) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("salary"), minSalary);
    }

    public static Specification<Employee> salaryLessThanOrEqual(Double maxSalary) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("salary"), maxSalary);
    }
}
