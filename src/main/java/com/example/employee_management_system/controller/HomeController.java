package com.example.employee_management_system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    @RequestMapping("/welcome")
    public String welcome() {
        return "Welcome to the Employee Management System!";
    }
}
