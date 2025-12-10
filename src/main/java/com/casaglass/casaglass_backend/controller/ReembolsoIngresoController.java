package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ReembolsoIngresoCreateDTO;
import com.casaglass.casaglass_backend.dto.ReembolsoIngresoResponseDTO;
import com.casaglass.casaglass_backend.model.ReembolsoIngreso;
import com.casaglass.casaglass_backend.service.ReembolsoIngresoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reembolsos-ingreso")
public class ReembolsoIngresoController {

    private final ReembolsoIngresoService service;

    public ReembolsoIngresoController(ReembolsoIngresoService service) {
        this.service = service;
    }

    /**
     * 📋 LISTADO DE REEMBOLSOS DE INGRESO CON FILTROS COMPLETOS
     * GET /api/reembolsos-ingreso
     * 
     * Filtros disponibles (todos opcionales):
     * - ingresoId: Filtrar por ingreso
     * - proveedorId: Filtrar por proveedor
     * - sedeId: No implementado actualmente (los ingresos no tienen campo sede)
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
     * - Si no se proporcionan: List<ReembolsoIngresoResponseDTO> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> listarReembolsos(
            @RequestParam(required = false) Long ingresoId,
            @RequestParam(required = false) Long proveedorId,
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
            ReembolsoIngreso.EstadoReembolso estadoEnum = null;
            if (estado != null && !estado.isEmpty()) {
                try {
                    estadoEnum = ReembolsoIngreso.EstadoReembolso.valueOf(estado.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Estado inválido: " + estado + ". Valores válidos: PENDIENTE, PROCESADO, ANULADO"
                    ));
                }
            }
            
            // Si solo hay sedeId y ningún otro filtro nuevo, usar método específico (compatibilidad)
            if (sedeId != null && ingresoId == null && proveedorId == null && estadoEnum == null && 
                fechaDesde == null && fechaHasta == null && procesado == null && 
                page == null && size == null && sortBy == null && sortOrder == null) {
                return ResponseEntity.ok(service.listarReembolsosPorSede(sedeId));
            }
            
            // Usar método con filtros (sedeId se ignora por ahora)
            Object resultado = service.listarReembolsosConFiltros(
                ingresoId, proveedorId, sedeId, estadoEnum, fechaDesde, fechaHasta, procesado, 
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
            Optional<ReembolsoIngresoResponseDTO> reembolso = service.obtenerPorId(id);
            if (reembolso.isPresent()) {
                return ResponseEntity.ok(reembolso.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Reembolso de ingreso no encontrado con ID: " + id));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/ingreso/{ingresoId}")
    public ResponseEntity<List<ReembolsoIngresoResponseDTO>> obtenerReembolsosPorIngreso(@PathVariable Long ingresoId) {
        try {
            return ResponseEntity.ok(service.obtenerReembolsosPorIngreso(ingresoId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> crearReembolso(@RequestBody ReembolsoIngresoCreateDTO dto) {
        try {
            ReembolsoIngresoResponseDTO resultado = service.crearReembolso(dto);
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

