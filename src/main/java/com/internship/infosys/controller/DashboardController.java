package com.internship.infosys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.internship.infosys.dto.DashboardResponse;
import com.internship.infosys.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "https://agent-6a7decacc5a0a1ca77--bespoke-begonia-5037cf.netlify.app")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // ============================
    // Dashboard
    // ============================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ITSM','USER')")
    public ResponseEntity<DashboardResponse> dashboard() {

        return ResponseEntity.ok(
                dashboardService.getDashboard());

    }

}
