package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.FacturaCreateDTO;
import com.casaglass.casaglass_backend.dto.FacturaTablaDTO;
import com.casaglass.casaglass_backend.model.Factura;
import com.casaglass.casaglass_backend.service.FacturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facturas")
// CORS configurado globalmente en CorsConfig.java
public class FacturaController {

    private static final Logger log = LoggerFactory.getLogger(FacturaController.class);

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

    /**
     * 📋 LISTADO DE FACTURAS CON FILTROS COMPLETOS
     * GET /api/facturas
     * 
     * Filtros disponibles (todos opcionales):
     * - clienteId: Filtrar por cliente
     * - sedeId: Filtrar por sede (a través de la orden)
     * - estado: PENDIENTE, PAGADA, ANULADA, EN_PROCESO
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - numeroFactura: Búsqueda parcial (case-insensitive)
     * - ordenId: Filtrar por orden
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, numeroFactura, total) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<Factura> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> listarFacturas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String numeroFactura,
            @RequestParam(required = false) Long ordenId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Convertir estado String a enum
        Factura.EstadoFactura estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = Factura.EstadoFactura.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Estado inválido: " + estado + ". Valores válidos: PENDIENTE, PAGADA, ANULADA, EN_PROCESO"
                ));
            }
        }
        
        // Si no hay filtros nuevos, usar método original (compatibilidad)
        if (clienteId == null && sedeId == null && estadoEnum == null && 
            fechaDesde == null && fechaHasta == null && numeroFactura == null && ordenId == null &&
            page == null && size == null && sortBy == null && sortOrder == null) {
            return ResponseEntity.ok(facturaService.listar());
        }
        
        // Usar método con filtros
        Object resultado = facturaService.listarFacturasConFiltros(
            clienteId, sedeId, estadoEnum, fechaDesde, fechaHasta, numeroFactura, ordenId, 
            page, size, sortBy, sortOrder
        );
        
        return ResponseEntity.ok(resultado);
    }

    /**
     * 📋 LISTADO DE FACTURAS PARA TABLA CON FILTROS COMPLETOS
     * GET /api/facturas/tabla
     * 
     * Filtros disponibles (todos opcionales):
     * - clienteId: Filtrar por cliente
     * - sedeId: Filtrar por sede (a través de la orden)
     * - estado: PENDIENTE, PAGADA, ANULADA, EN_PROCESO
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<FacturaTablaDTO> (compatibilidad hacia atrás)
     */
    @GetMapping("/tabla")
    public ResponseEntity<Object> listarFacturasParaTabla(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        // Convertir estado String a enum
        Factura.EstadoFactura estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = Factura.EstadoFactura.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Estado inválido: " + estado + ". Valores válidos: PENDIENTE, PAGADA, ANULADA, EN_PROCESO"
                ));
            }
        }
        
        // Si solo hay sedeId y ningún otro filtro nuevo, usar método específico (compatibilidad)
        if (sedeId != null && clienteId == null && estadoEnum == null && 
            fechaDesde == null && fechaHasta == null && page == null && size == null) {
            return ResponseEntity.ok(facturaService.listarParaTablaPorSede(sedeId));
        }
        
        // Usar método con filtros
        Object resultado = facturaService.listarParaTablaConFiltros(
            clienteId, sedeId, estadoEnum, fechaDesde, fechaHasta, page, size
        );
        
        return ResponseEntity.ok(resultado);
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

