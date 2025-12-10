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

    /**
     * 📋 LISTADO DE CRÉDITOS CON FILTROS COMPLETOS
     * GET /api/creditos
     * 
     * Filtros disponibles (todos opcionales):
     * - clienteId: Filtrar por cliente (recomendado para mejorar rendimiento)
     * - sedeId: Filtrar por sede (a través de la orden)
     * - estado: ABIERTO, CERRADO, VENCIDO, ANULADO
     * - fechaDesde: YYYY-MM-DD (fecha inicio del crédito, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha inicio del crédito, inclusive)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 50, máximo: 200)
     * - sortBy: Campo para ordenar (fecha, montoTotal, saldoPendiente) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<CreditoResponseDTO> (compatibilidad hacia atrás)
     * 
     * NOTA: Si no se proporciona clienteId, se retornan TODOS los créditos (puede ser lento)
     */
    @GetMapping
    public Object listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaDesde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaHasta,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Convertir estado String a enum
        Credito.EstadoCredito estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = Credito.EstadoCredito.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado inválido: " + estado + ". Valores válidos: ABIERTO, CERRADO, VENCIDO, ANULADO");
            }
        }
        
        // Si no hay filtros nuevos, usar método original (compatibilidad)
        if (clienteId == null && sedeId == null && estadoEnum == null && 
            fechaDesde == null && fechaHasta == null && page == null && size == null && 
            sortBy == null && sortOrder == null) {
            return service.listar().stream()
                    .map(CreditoResponseDTO::new)
                    .collect(Collectors.toList());
        }
        
        // Usar método con filtros
        Object resultado = service.listarConFiltros(
            clienteId, sedeId, estadoEnum, fechaDesde, fechaHasta, page, size, sortBy, sortOrder
        );
        
        // Si es lista paginada, convertir los créditos a DTOs
        if (resultado instanceof com.casaglass.casaglass_backend.dto.PageResponse) {
            @SuppressWarnings("unchecked")
            com.casaglass.casaglass_backend.dto.PageResponse<Credito> pageResponse = 
                (com.casaglass.casaglass_backend.dto.PageResponse<Credito>) resultado;
            
            List<CreditoResponseDTO> contenidoDTO = pageResponse.getContent().stream()
                    .map(CreditoResponseDTO::new)
                    .collect(Collectors.toList());
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(
                contenidoDTO, pageResponse.getTotalElements(), pageResponse.getPage(), pageResponse.getSize()
            );
        }
        
        // Si es lista simple, convertir a DTOs
        @SuppressWarnings("unchecked")
        List<Credito> creditos = (List<Credito>) resultado;
        return creditos.stream()
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