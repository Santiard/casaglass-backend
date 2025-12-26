package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.AbonoDTO;
import com.casaglass.casaglass_backend.dto.AbonoSimpleDTO;
import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.service.AbonoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
// CORS configurado globalmente en CorsConfig.java
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
            Abono abono = service.crearDesdeDTO(creditoId, abonoDTO);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Abono registrado exitosamente",
                "abono", new AbonoSimpleDTO(abono),
                "saldoRestante", abono.getSaldo()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor: " + e.getMessage(),
                "tipo", "SERVIDOR"
            ));
        }
    }

    /* --------- CONSULTAS --------- */

    /**
     * 📋 LISTADO DE ABONOS CON FILTROS COMPLETOS
     * GET /api/abonos
     * 
     * Filtros disponibles (todos opcionales):
     * - clienteId: Filtrar por cliente
     * - creditoId: Filtrar por crédito
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - metodoPago: Búsqueda parcial por método de pago (case-insensitive)
     * - sedeId: Filtrar por sede (a través de la orden)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 50, máximo: 200)
     * - sortBy: Campo para ordenar (fecha, total) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<AbonoSimpleDTO> (compatibilidad hacia atrás)
     */
    @GetMapping("/abonos")
    public Object listarAbonos(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long creditoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Usar método con filtros
        Object resultado = service.listarAbonosConFiltros(
            clienteId, creditoId, fechaDesde, fechaHasta, metodoPago, sedeId, page, size, sortBy, sortOrder
        );
        
        // Si es lista paginada, convertir los abonos a DTOs
        if (resultado instanceof com.casaglass.casaglass_backend.dto.PageResponse) {
            @SuppressWarnings("unchecked")
            com.casaglass.casaglass_backend.dto.PageResponse<Abono> pageResponse = 
                (com.casaglass.casaglass_backend.dto.PageResponse<Abono>) resultado;
            
            List<AbonoSimpleDTO> contenidoDTO = pageResponse.getContent().stream()
                    .map(AbonoSimpleDTO::new)
                    .collect(Collectors.toList());
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(
                contenidoDTO, pageResponse.getTotalElements(), pageResponse.getPage(), pageResponse.getSize()
            );
        }
        
        // Si es lista simple, convertir a DTOs
        @SuppressWarnings("unchecked")
        List<Abono> abonos = (List<Abono>) resultado;
        return abonos.stream()
                .map(AbonoSimpleDTO::new)
                .collect(Collectors.toList());
    }

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

    /**
     * 📋 LISTAR ABONOS POR CLIENTE CON FILTROS OPCIONALES
     * GET /api/abonos/cliente/{clienteId}?fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD
     * 
     * Optimizado para mejorar rendimiento:
     * - Filtra en la base de datos en lugar del frontend
     * - Reduce el tamaño de la respuesta
     * - Mejora el tiempo de carga
     */
    @GetMapping("/abonos/cliente/{clienteId}")
    public List<AbonoSimpleDTO> listarPorCliente(
            @PathVariable Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        
        List<Abono> abonos;
        if (fechaDesde != null && fechaHasta != null) {
            // Validar que fechaDesde <= fechaHasta
            if (fechaDesde.isAfter(fechaHasta)) {
                throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
            }
            abonos = service.listarPorClienteConFiltros(clienteId, fechaDesde, fechaHasta);
        } else {
            abonos = service.listarPorCliente(clienteId);
        }
        
        return abonos.stream()
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
                                        @Valid @RequestBody AbonoDTO abonoDTO) {
        try {
            Abono abono = service.actualizarDesdeDTO(creditoId, abonoId, abonoDTO);
            return ResponseEntity.ok(new AbonoSimpleDTO(abono));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION"
            ));
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
