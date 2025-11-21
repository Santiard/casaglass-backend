package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ReembolsoVentaCreateDTO;
import com.casaglass.casaglass_backend.dto.ReembolsoVentaResponseDTO;
import com.casaglass.casaglass_backend.service.ReembolsoVentaService;
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

    @GetMapping
    public ResponseEntity<List<ReembolsoVentaResponseDTO>> listarReembolsos() {
        try {
            return ResponseEntity.ok(service.listarReembolsos());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
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

