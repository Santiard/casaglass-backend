package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.FacturaCreateDTO;
import com.casaglass.casaglass_backend.dto.FacturaTablaDTO;
import com.casaglass.casaglass_backend.model.Factura;
import com.casaglass.casaglass_backend.service.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody FacturaCreateDTO facturaDTO) {
        try {
            System.out.println("🧾 Creando factura para orden: " + facturaDTO.getOrdenId());
            Factura factura = facturaService.crearFactura(facturaDTO);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Factura creada exitosamente",
                    "factura", factura,
                    "numeroFactura", factura.getNumeroFactura()
            ));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "tipo", "VALIDACION"
            ));
        } catch (Exception e) {
            System.err.println("❌ Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Error interno del servidor: " + e.getMessage(),
                    "tipo", "SERVIDOR"
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerFactura(@PathVariable Long id) {
        return facturaService.obtenerPorId(id)
                .map(factura -> ResponseEntity.ok(factura))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numeroFactura}")
    public ResponseEntity<?> obtenerFacturaPorNumeroFactura(@PathVariable String numeroFactura) {
        return facturaService.obtenerPorNumeroFactura(numeroFactura)
                .map(factura -> ResponseEntity.ok(factura))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orden/{ordenId}")
    public ResponseEntity<?> obtenerFacturaPorOrden(@PathVariable Long ordenId) {
        return facturaService.obtenerPorOrden(ordenId)
                .map(factura -> ResponseEntity.ok(factura))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Factura>> listarFacturas() {
        return ResponseEntity.ok(facturaService.listar());
    }

    @GetMapping("/tabla")
    public ResponseEntity<List<FacturaTablaDTO>> listarFacturasParaTabla() {
        return ResponseEntity.ok(facturaService.listarParaTabla());
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Factura>> listarFacturasPorEstado(@PathVariable String estado) {
        try {
            Factura.EstadoFactura estadoEnum = Factura.EstadoFactura.valueOf(estado.toUpperCase());
            return ResponseEntity.ok(facturaService.listarPorEstado(estadoEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/fecha/{fecha}")
    public ResponseEntity<List<Factura>> listarFacturasPorFecha(@PathVariable String fecha) {
        LocalDate fechaLocal = LocalDate.parse(fecha);
        return ResponseEntity.ok(facturaService.listarPorFecha(fechaLocal));
    }

    @GetMapping("/fecha")
    public ResponseEntity<List<Factura>> listarFacturasPorRangoFechas(
            @RequestParam String desde,
            @RequestParam String hasta) {
        LocalDate desdeLocal = LocalDate.parse(desde);
        LocalDate hastaLocal = LocalDate.parse(hasta);
        return ResponseEntity.ok(facturaService.listarPorRangoFechas(desdeLocal, hastaLocal));
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<?> marcarFacturaComoPagada(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String fechaPagoStr = body.get("fechaPago");
            LocalDate fechaPago = fechaPagoStr != null ? LocalDate.parse(fechaPagoStr) : null;
            Factura factura = facturaService.marcarComoPagada(id, fechaPago);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Factura marcada como pagada",
                    "factura", factura
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<?> anularFactura(@PathVariable Long id) {
        try {
            Factura factura = facturaService.anularFactura(id);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Factura anulada exitosamente",
                    "factura", factura
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarFactura(@PathVariable Long id, @RequestBody FacturaCreateDTO facturaDTO) {
        try {
            Factura factura = facturaService.actualizarFactura(id, facturaDTO);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Factura actualizada exitosamente",
                    "factura", factura
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarFactura(@PathVariable Long id) {
        try {
            facturaService.eliminarFactura(id);
            return ResponseEntity.ok(Map.of("mensaje", "Factura eliminada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}

