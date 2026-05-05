package com.calendario.callapp.callapp_backend.controller;

import com.calendario.callapp.callapp_backend.dto.response.DashboardResumenResponse;
import com.calendario.callapp.callapp_backend.service.impl.DashboardServiceImpl;
import com.calendario.callapp.callapp_backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMUNICACIONES', 'ADMIN', 'OFICINA')")
    public ResponseEntity<ApiResponse<DashboardResumenResponse>> resumen(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.resumen(authentication)));
    }
}
