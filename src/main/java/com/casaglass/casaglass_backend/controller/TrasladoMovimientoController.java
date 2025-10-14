package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ConfirmarTrasladoRequest;
import com.casaglass.casaglass_backend.dto.ConfirmarTrasladoResponse;
import com.casaglass.casaglass_backend.dto.TrasladoMovimientoDTO;
import com.casaglass.casaglass_backend.service.TrasladoMovimientoService;
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
@CrossOrigin(origins = "*")
public class TrasladoMovimientoController {

    private final TrasladoMovimientoService service;

    public TrasladoMovimientoController(TrasladoMovimientoService service) {
        this.service = service;
    }

    /**
     * GET /api/traslados-movimientos
     * Obtiene todos los movimientos de traslado con información consolidada
     * Formato optimizado para el frontend según especificación
     */
    @GetMapping
    public ResponseEntity<List<TrasladoMovimientoDTO>> obtenerMovimientos() {
        List<TrasladoMovimientoDTO> movimientos = service.obtenerMovimientos();
        return ResponseEntity.ok(movimientos);
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