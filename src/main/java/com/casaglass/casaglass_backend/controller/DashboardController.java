package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.DashboardCompletoDTO;
import com.casaglass.casaglass_backend.dto.DashboardVentasPorSedeDTO;
import com.casaglass.casaglass_backend.dto.SedeDashboardDTO;
import com.casaglass.casaglass_backend.service.DashboardCompletoService;
import com.casaglass.casaglass_backend.service.DashboardService;
import com.casaglass.casaglass_backend.service.SedeDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
// CORS configurado globalmente en CorsConfig.java
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DashboardCompletoService dashboardCompletoService;

    @Autowired
    private SedeDashboardService sedeDashboardService;

    /**
     * 📊 DASHBOARD COMPLETO - Endpoint consolidado
     * Retorna todos los datos relevantes en una sola llamada
     * 
     * GET /api/dashboard/completo
     * GET /api/dashboard/completo?desde=2025-01-01&hasta=2025-01-31
     */
    @GetMapping("/completo")
    public ResponseEntity<DashboardCompletoDTO> obtenerDashboardCompleto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        try {
            DashboardCompletoDTO dashboard = dashboardCompletoService.obtenerDashboardCompleto(desde, hasta);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 🏠 TARJETAS DEL HOME CON FILTRO DE SEDE OPCIONAL
     * 
     * GET /api/dashboard/tarjetas              → agrega todas las sedes
     * GET /api/dashboard/tarjetas?sedeId=2    → datos solo de la sede indicada
     * 
     * Retorna la misma estructura que GET /api/sedes/{sedeId}/dashboard
     */
    @GetMapping("/tarjetas")
    public ResponseEntity<SedeDashboardDTO> obtenerTarjetas(
            @RequestParam(required = false) Long sedeId
    ) {
        try {
            SedeDashboardDTO dashboard = sedeDashboardService.obtenerDashboardTarjetas(sedeId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

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


