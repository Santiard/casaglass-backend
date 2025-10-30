package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.DashboardVentasPorSedeDTO;
import com.casaglass.casaglass_backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/ventas-por-sede")
    public ResponseEntity<List<DashboardVentasPorSedeDTO>> ventasPorSede(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {
        LocalDate desdeDate = desde != null && !desde.isBlank() ? LocalDate.parse(desde) : LocalDate.now();
        LocalDate hastaDate = hasta != null && !hasta.isBlank() ? LocalDate.parse(hasta) : desdeDate;
        List<DashboardVentasPorSedeDTO> result = dashboardService.obtenerVentasPorSede(desdeDate, hastaDate);
        return ResponseEntity.ok(result);
    }
}


