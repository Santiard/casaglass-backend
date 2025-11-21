package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ReembolsoIngresoCreateDTO;
import com.casaglass.casaglass_backend.dto.ReembolsoIngresoResponseDTO;
import com.casaglass.casaglass_backend.service.ReembolsoIngresoService;
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

    @GetMapping
    public ResponseEntity<List<ReembolsoIngresoResponseDTO>> listarReembolsos() {
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

