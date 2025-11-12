package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.CreditoResponseDTO;
import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.service.CreditoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/creditos")
// CORS configurado globalmente en CorsConfig.java
public class CreditoController {

    private final CreditoService service;

    public CreditoController(CreditoService service) {
        this.service = service;
    }

    /** 💳 Crear crédito para una orden específica */
    @PostMapping("/orden/{ordenId}")
    public ResponseEntity<?> crearParaOrden(@PathVariable Long ordenId, 
                                           @RequestParam Long clienteId, 
                                           @RequestParam Double totalOrden) {
        try {
            Credito credito = service.crearCreditoParaOrden(ordenId, clienteId, totalOrden);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Crédito creado exitosamente",
                "credito", new CreditoResponseDTO(credito)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /** 📋 Obtener todos los créditos */
    @GetMapping
    public List<CreditoResponseDTO> listar() {
        return service.listar().stream()
                .map(CreditoResponseDTO::new)
                .collect(Collectors.toList());
    }

    /** 🔍 Obtener crédito por ID */
    @GetMapping("/{id}")
    public ResponseEntity<CreditoResponseDTO> obtener(@PathVariable Long id) {
        return service.obtener(id)
                .map(credito -> ResponseEntity.ok(new CreditoResponseDTO(credito)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 🔍 Obtener crédito por orden */
    @GetMapping("/orden/{ordenId}")
    public ResponseEntity<CreditoResponseDTO> obtenerPorOrden(@PathVariable Long ordenId) {
        return service.obtenerPorOrden(ordenId)
                .map(credito -> ResponseEntity.ok(new CreditoResponseDTO(credito)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** 👤 Listar créditos por cliente */
    @GetMapping("/cliente/{clienteId}")
    public List<CreditoResponseDTO> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId).stream()
                .map(CreditoResponseDTO::new)
                .collect(Collectors.toList());
    }

    /** 📊 Listar créditos por estado */
    @GetMapping("/estado/{estado}")
    public List<CreditoResponseDTO> listarPorEstado(@PathVariable Credito.EstadoCredito estado) {
        return service.listarPorEstado(estado).stream()
                .map(CreditoResponseDTO::new)
                .collect(Collectors.toList());
    }

    /** 💰 Registrar abono a un crédito */
    @PostMapping("/{creditoId}/abono")
    public ResponseEntity<?> registrarAbono(@PathVariable Long creditoId, 
                                          @RequestParam Double monto) {
        try {
            Credito credito = service.registrarAbono(creditoId, monto);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Abono registrado exitosamente",
                "credito", new CreditoResponseDTO(credito),
                "nuevoSaldo", credito.getSaldoPendiente()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /** 🔄 Recalcular totales de un crédito */
    @PostMapping("/{creditoId}/recalcular")
    public ResponseEntity<?> recalcularTotales(@PathVariable Long creditoId) {
        try {
            Credito credito = service.recalcularTotales(creditoId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Totales recalculados exitosamente",
                "credito", new CreditoResponseDTO(credito)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /** ❌ Anular crédito */
    @PutMapping("/{creditoId}/anular")
    public ResponseEntity<?> anularCredito(@PathVariable Long creditoId) {
        try {
            Credito credito = service.anularCredito(creditoId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Crédito anulado exitosamente",
                "credito", new CreditoResponseDTO(credito)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /** 🏁 Cerrar crédito manualmente */
    @PutMapping("/{creditoId}/cerrar")
    public ResponseEntity<?> cerrarCredito(@PathVariable Long creditoId) {
        try {
            Credito credito = service.cerrarCredito(creditoId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Crédito cerrado exitosamente",
                "credito", new CreditoResponseDTO(credito)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /** 🗑️ Eliminar crédito */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}