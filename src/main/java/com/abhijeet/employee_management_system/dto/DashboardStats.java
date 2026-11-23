package com.abhijeet.employee_management_system.dto;

public class DashboardStats {
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;
    private long employeesOnLeave;

    public DashboardStats(long totalEmployees, long activeEmployees, long totalDepartments, long employeesOnLeave) {
        this.totalEmployees = totalEmployees;
        this.activeEmployees = activeEmployees;
        this.totalDepartments = totalDepartments;
        this.employeesOnLeave = employeesOnLeave;
    }

    public long getTotalEmployees() { return totalEmployees; }
    public long getActiveEmployees() { return activeEmployees; }
    public long getTotalDepartments() { return totalDepartments; }
    public long getEmployeesOnLeave() { return employeesOnLeave; }
}
