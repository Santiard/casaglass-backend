package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ConfirmarTrasladoRequest;
import com.casaglass.casaglass_backend.dto.ConfirmarTrasladoResponse;
import com.casaglass.casaglass_backend.dto.TrasladoMovimientoDTO;
import com.casaglass.casaglass_backend.service.TrasladoMovimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador especializado para movimientos de traslado
 * Proporciona endpoints optimizados para el frontend
 */
@RestController
@RequestMapping("/api/traslados-movimientos")
@Tag(name = "Traslados — movimientos (listas)", description = "Listados y confirmación vía body JSON. Misma confirmación lógica que POST /api/traslados/{id}/confirmar")
// CORS configurado globalmente en CorsConfig.java
public class TrasladoMovimientoController {

    private final TrasladoMovimientoService service;

    public TrasladoMovimientoController(TrasladoMovimientoService service) {
        this.service = service;
    }

    /**
     * 📋 LISTADO DE TRASLADOS CON FILTROS COMPLETOS
     * GET /api/traslados-movimientos
     * 
     * Filtros disponibles (todos opcionales):
     * - sedeOrigenId: Filtrar por sede origen
     * - sedeDestinoId: Filtrar por sede destino
     * - sedeId: Filtrar por sede origen O destino
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - estado: PENDIENTE, CONFIRMADO (se convierte a confirmado boolean)
     * - confirmado: true para confirmados, false para pendientes
     * - trabajadorId: Filtrar por trabajador que confirmó
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, id) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<TrasladoMovimientoDTO> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> obtenerMovimientos(
            @RequestParam(required = false) Long sedeOrigenId,
            @RequestParam(required = false) Long sedeDestinoId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean confirmado,
            @RequestParam(required = false) Long trabajadorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Si solo hay sedeId y ningún otro filtro nuevo, usar método específico (compatibilidad)
        if (sedeId != null && sedeOrigenId == null && sedeDestinoId == null && fechaDesde == null && 
            fechaHasta == null && estado == null && confirmado == null && trabajadorId == null && 
            page == null && size == null && sortBy == null && sortOrder == null) {
            return ResponseEntity.ok(service.obtenerMovimientosPorSede(sedeId));
        }
        
        // Usar método con filtros completos
        Object resultado = service.obtenerMovimientosConFiltros(
            sedeOrigenId, sedeDestinoId, sedeId, fechaDesde, fechaHasta, 
            estado, confirmado, trabajadorId, page, size, sortBy, sortOrder
        );
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/traslados-movimientos/rango?desde={fecha}&hasta={fecha}
     * Obtiene movimientos filtrados por rango de fechas
     */
    @GetMapping("/rango")
    public ResponseEntity<List<TrasladoMovimientoDTO>> obtenerMovimientosPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<TrasladoMovimientoDTO> movimientos = service.obtenerMovimientosPorRango(desde, hasta);
        return ResponseEntity.ok(movimientos);
    }

    /**
     * GET /api/traslados-movimientos/sede/{sedeId}
     * Obtiene movimientos donde la sede es origen o destino
     */
    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<TrasladoMovimientoDTO>> obtenerMovimientosPorSede(
            @PathVariable Long sedeId) {
        List<TrasladoMovimientoDTO> movimientos = service.obtenerMovimientosPorSede(sedeId);
        return ResponseEntity.ok(movimientos);
    }

    /**
     * GET /api/traslados-movimientos/pendientes
     * Obtiene movimientos pendientes de confirmación
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<TrasladoMovimientoDTO>> obtenerMovimientosPendientes() {
        List<TrasladoMovimientoDTO> movimientos = service.obtenerMovimientosPendientes();
        return ResponseEntity.ok(movimientos);
    }

    /**
     * GET /api/traslados-movimientos/confirmados
     * Obtiene movimientos ya confirmados
     */
    @GetMapping("/confirmados")
    public ResponseEntity<List<TrasladoMovimientoDTO>> obtenerMovimientosConfirmados() {
        List<TrasladoMovimientoDTO> movimientos = service.obtenerMovimientosConfirmados();
        return ResponseEntity.ok(movimientos);
    }

    /**
     * GET /api/traslados-movimientos/hoy
     * Obtiene movimientos del día actual
     */
    @GetMapping("/hoy")
    public ResponseEntity<List<TrasladoMovimientoDTO>> obtenerMovimientosHoy() {
        LocalDate hoy = LocalDate.now();
        List<TrasladoMovimientoDTO> movimientos = service.obtenerMovimientosPorRango(hoy, hoy);
        return ResponseEntity.ok(movimientos);
    }

    /**
     * PUT /api/traslados-movimientos/{id}/confirmar
     * Confirma un traslado estableciendo trabajador y fecha de confirmación
     * 
     * Body: { "trabajadorId": 25 }
     * Response: { "message": "...", "traslado": { ... } }
     */
    @PutMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar traslado (body JSON)", description = "Misma lógica que POST /api/traslados/{id}/confirmar?trabajadorId=... — delega en TrasladoService.confirmarLlegada.")
    public ResponseEntity<ConfirmarTrasladoResponse> confirmarTraslado(
            @PathVariable Long id, 
            @RequestBody ConfirmarTrasladoRequest request) {
        try {
            if (request.getTrabajadorId() == null) {
                return ResponseEntity.badRequest().build();
            }

            TrasladoMovimientoDTO trasladoConfirmado = service.confirmarTraslado(id, request.getTrabajadorId());
            
            ConfirmarTrasladoResponse response = new ConfirmarTrasladoResponse(
                "Traslado confirmado exitosamente",
                trasladoConfirmado
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}