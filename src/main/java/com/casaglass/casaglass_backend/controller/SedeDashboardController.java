package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.SedeDashboardDTO;
import com.casaglass.casaglass_backend.service.SedeDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sedes")
// CORS configurado globalmente en CorsConfig.java
public class SedeDashboardController {

    @Autowired
    private SedeDashboardService dashboardService;

    /**
     * 📊 DASHBOARD OPERACIONAL DE SEDE
     * 
     * Endpoint consolidado que retorna toda la información necesaria 
     * para el panel principal de una sede:
     * - Información básica de la sede
     * - Ventas del día actual
     * - Estado de entregas de dinero
     * - Créditos pendientes y alertas
     * - Alertas de stock bajo
     * 
     * @param sedeId ID de la sede
     * @return Dashboard completo con todas las métricas
     */
    @GetMapping("/{sedeId}/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<SedeDashboardDTO> obtenerDashboard(@PathVariable Long sedeId) {
        try {
            SedeDashboardDTO dashboard = dashboardService.obtenerDashboard(sedeId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}