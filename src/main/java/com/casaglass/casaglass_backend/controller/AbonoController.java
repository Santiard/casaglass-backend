package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.AbonoDTO;
import com.casaglass.casaglass_backend.dto.AbonoSimpleDTO;
import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.service.AbonoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class AbonoController {

    private final AbonoService service;

    public AbonoController(AbonoService service) {
        this.service = service;
    }

    /* --------- 💰 CREAR ABONO (ENDPOINT PRINCIPAL PARA FRONTEND) --------- */

    /**
     * 💰 CREAR ABONO A UN CRÉDITO
     * Endpoint simplificado para el frontend
     */
    @PostMapping("/creditos/{creditoId}/abonos")
    public ResponseEntity<?> crearAbono(@PathVariable Long creditoId, 
                                       @Valid @RequestBody AbonoDTO abonoDTO) {
        try {
            System.out.println("🔍 DEBUG: Creando abono para crédito " + creditoId);
            System.out.println("🔍 DEBUG: Datos recibidos: " + abonoDTO);
            
            Abono abono = service.crearDesdeDTO(creditoId, abonoDTO);
            
            System.out.println("✅ DEBUG: Abono creado con ID: " + abono.getId());
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Abono registrado exitosamente",
                "abono", new AbonoSimpleDTO(abono),
                "saldoRestante", abono.getSaldo()
            ));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ ERROR VALIDACION: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION"
            ));
        } catch (Exception e) {
            System.err.println("❌ ERROR SERVIDOR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor: " + e.getMessage(),
                "tipo", "SERVIDOR"
            ));
        }
    }

    /* --------- CONSULTAS --------- */

    @GetMapping("/creditos/{creditoId}/abonos")
    public List<AbonoSimpleDTO> listarPorCredito(@PathVariable Long creditoId) {
        return service.listarPorCredito(creditoId).stream()
                .map(AbonoSimpleDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/abonos/{abonoId}")
    public ResponseEntity<AbonoSimpleDTO> obtener(@PathVariable Long abonoId) {
        return service.obtener(abonoId)
                .map(abono -> ResponseEntity.ok(new AbonoSimpleDTO(abono)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/abonos/cliente/{clienteId}")
    public List<AbonoSimpleDTO> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId).stream()
                .map(AbonoSimpleDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/abonos/orden/{ordenId}")
    public List<AbonoSimpleDTO> listarPorOrden(@PathVariable Long ordenId) {
        return service.listarPorOrden(ordenId).stream()
                .map(AbonoSimpleDTO::new)
                .collect(Collectors.toList());
    }

    /* --------- CRUD COMPLETO --------- */

    @PutMapping("/creditos/{creditoId}/abonos/{abonoId}")
    public ResponseEntity<?> actualizar(@PathVariable Long creditoId,
                                        @PathVariable Long abonoId,
                                        @Valid @RequestBody Abono abono) {
        try {
            return ResponseEntity.ok(service.actualizar(creditoId, abonoId, abono));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/creditos/{creditoId}/abonos/{abonoId}")
    public ResponseEntity<?> eliminar(@PathVariable Long creditoId, @PathVariable Long abonoId) {
        try {
            service.eliminar(creditoId, abonoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
