package com.abhijeet.employee_management_system.dto;

import jakarta.validation.constraints.NotBlank;

public class DesignationRequest {

    @NotBlank(message = "Title is required")
    private String title;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
