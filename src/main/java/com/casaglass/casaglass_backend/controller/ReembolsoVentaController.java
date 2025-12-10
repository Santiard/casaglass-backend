package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ReembolsoVentaCreateDTO;
import com.casaglass.casaglass_backend.dto.ReembolsoVentaResponseDTO;
import com.casaglass.casaglass_backend.model.ReembolsoVenta;
import com.casaglass.casaglass_backend.service.ReembolsoVentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reembolsos-venta")
public class ReembolsoVentaController {

    private final ReembolsoVentaService service;

    public ReembolsoVentaController(ReembolsoVentaService service) {
        this.service = service;
    }

    /**
     * 📋 LISTADO DE REEMBOLSOS DE VENTA CON FILTROS COMPLETOS
     * GET /api/reembolsos-venta
     * 
     * Filtros disponibles (todos opcionales):
     * - ordenId: Filtrar por orden
     * - clienteId: Filtrar por cliente
     * - sedeId: Filtrar por sede
     * - estado: PENDIENTE, PROCESADO, ANULADO
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - procesado: Boolean (true para procesados, false para pendientes)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, monto) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<ReembolsoVentaResponseDTO> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> listarReembolsos(
            @RequestParam(required = false) Long ordenId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaHasta,
            @RequestParam(required = false) Boolean procesado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        try {
            // Convertir estado String a enum
            ReembolsoVenta.EstadoReembolso estadoEnum = null;
            if (estado != null && !estado.isEmpty()) {
                try {
                    estadoEnum = ReembolsoVenta.EstadoReembolso.valueOf(estado.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Estado inválido: " + estado + ". Valores válidos: PENDIENTE, PROCESADO, ANULADO"
                    ));
                }
            }
            
            // Si solo hay sedeId y ningún otro filtro nuevo, usar método específico (compatibilidad)
            if (sedeId != null && ordenId == null && clienteId == null && estadoEnum == null && 
                fechaDesde == null && fechaHasta == null && procesado == null && 
                page == null && size == null && sortBy == null && sortOrder == null) {
                return ResponseEntity.ok(service.listarReembolsosPorSede(sedeId));
            }
            
            // Usar método con filtros
            Object resultado = service.listarReembolsosConFiltros(
                ordenId, clienteId, sedeId, estadoEnum, fechaDesde, fechaHasta, procesado, 
                page, size, sortBy, sortOrder
            );
            
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            Optional<ReembolsoVentaResponseDTO> reembolso = service.obtenerPorId(id);
            if (reembolso.isPresent()) {
                return ResponseEntity.ok(reembolso.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Reembolso de venta no encontrado con ID: " + id));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/orden/{ordenId}")
    public ResponseEntity<List<ReembolsoVentaResponseDTO>> obtenerReembolsosPorOrden(@PathVariable Long ordenId) {
        try {
            return ResponseEntity.ok(service.obtenerReembolsosPorOrden(ordenId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> crearReembolso(@RequestBody ReembolsoVentaCreateDTO dto) {
        try {
            ReembolsoVentaResponseDTO resultado = service.crearReembolso(dto);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/procesar")
    public ResponseEntity<?> procesarReembolso(@PathVariable Long id) {
        try {
            Map<String, Object> resultado = service.procesarReembolso(id);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<?> anularReembolso(@PathVariable Long id) {
        try {
            service.anularReembolso(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Reembolso anulado exitosamente",
                    "reembolsoId", id,
                    "estado", "ANULADO"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReembolso(@PathVariable Long id) {
        try {
            service.eliminarReembolso(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Reembolso eliminado exitosamente",
                    "reembolsoId", id
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }
}

