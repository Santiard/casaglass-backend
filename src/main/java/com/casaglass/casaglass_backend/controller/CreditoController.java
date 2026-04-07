package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.CreditoResponseDTO;
import com.casaglass.casaglass_backend.dto.EntregaClienteEspecialResponseDTO;
import com.casaglass.casaglass_backend.dto.EntregaClienteEspecialResumenDTO;
import com.casaglass.casaglass_backend.dto.MarcarCreditosClienteEspecialRequest;
import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.EntregaClienteEspecial;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.service.CreditoService;
import com.casaglass.casaglass_backend.service.EntregaClienteEspecialService;
import com.casaglass.casaglass_backend.service.OrdenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/creditos")
// CORS configurado globalmente en CorsConfig.java
public class CreditoController {

    private static final Logger log = LoggerFactory.getLogger(CreditoController.class);

    private final CreditoService service;
    private final EntregaClienteEspecialService entregaClienteEspecialService;
    private final OrdenService ordenService;
    private final OrdenRepository ordenRepository;

    public CreditoController(CreditoService service,
                             EntregaClienteEspecialService entregaClienteEspecialService,
                             OrdenService ordenService,
                             OrdenRepository ordenRepository) {
        this.service = service;
        this.entregaClienteEspecialService = entregaClienteEspecialService;
        this.ordenService = ordenService;
        this.ordenRepository = ordenRepository;
    }

    /** 💳 Crear crédito para una orden específica */
    @PostMapping("/orden/{ordenId}")
    public ResponseEntity<?> crearParaOrden(@PathVariable Long ordenId, 
                                           @RequestParam Long clienteId, 
                                           @RequestParam Double totalOrden,
                                           @RequestParam(required = false) Double retencionFuente,
                                           @RequestParam(required = false) Double retencionIca) {
        try {
            Double retencionFuenteValor = (retencionFuente != null && retencionFuente > 0) ? retencionFuente : 0.0;
            Double retencionIcaValor = (retencionIca != null && retencionIca > 0) ? retencionIca : 0.0;
            Credito credito = service.crearCreditoParaOrden(ordenId, clienteId, totalOrden, retencionFuenteValor, retencionIcaValor);
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
     * ⭐ LISTADO DE CRÉDITOS DEL CLIENTE ESPECIAL SOLAMENTE
     * GET /api/creditos/cliente-especial
     * 
     * Cliente especial: ID 499 - JAIRO JAVIER VELANDIA (NIT: 88249472)
     * 
     * Filtros disponibles (todos opcionales):
     * - sedeId: Filtrar por sede
     * - estado: ABIERTO, CERRADO, VENCIDO, ANULADO
     * - fechaDesde: YYYY-MM-DD (fecha inicio del crédito, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha inicio del crédito, inclusive)
     * - page: Número de página (default: sin paginación)
     * - size: Tamaño de página (default: 50, máximo: 200)
     * - sortBy: Campo para ordenar (fecha, montoTotal, saldoPendiente) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     */
    @GetMapping("/cliente-especial")
    public Object listarClienteEspecial(
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
                throw new IllegalArgumentException("Estado inválido: " + estado);
            }
        }
        
        Object resultado = service.listarClienteEspecial(
            sedeId, estadoEnum, fechaDesde, fechaHasta, page, size, sortBy, sortOrder
        );
        
        // Convertir a DTOs
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
        
        @SuppressWarnings("unchecked")
        List<Credito> creditos = (List<Credito>) resultado;
        return creditos.stream()
                .map(CreditoResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 📋 LISTADO DE CRÉDITOS CON FILTROS COMPLETOS
     * GET /api/creditos
     * 
     * ⚠️ IMPORTANTE: Este endpoint EXCLUYE al cliente especial (ID 499)
     * Para créditos del cliente especial, usar: GET /api/creditos/cliente-especial
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

    /** 👤 Listar créditos por cliente (con filtros opcionales de fecha) */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<CreditoResponseDTO>> listarPorCliente(
            @PathVariable Long clienteId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaDesde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaHasta) {
        List<CreditoResponseDTO> creditos = service.listarCreditosClienteConFiltros(clienteId, fechaDesde, fechaHasta);
        return ResponseEntity.ok(creditos);
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
            Credito credito = service.obtener(creditoId)
                    .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

            Long ordenId = null;
            if (credito.getOrden() != null && credito.getOrden().getId() != null) {
                ordenId = credito.getOrden().getId();
            } else {
                ordenId = ordenRepository.findByCreditoDetalleId(creditoId)
                        .map(Orden::getId)
                        .orElse(null);
            }

            if (ordenId != null) {
                Orden ordenAnulada = ordenService.anularOrden(ordenId);
                Credito creditoActualizado = ordenAnulada.getCreditoDetalle() != null
                        ? ordenAnulada.getCreditoDetalle()
                        : service.obtener(creditoId).orElse(credito);

                log.info("[CreditoController.anularCredito] Anulación vía orden completada creditoId={} ordenId={} estadoOrden={}",
                        creditoId,
                        ordenId,
                        ordenAnulada.getEstado());

                return ResponseEntity.ok(Map.of(
                        "mensaje", "Crédito y orden anulados exitosamente",
                        "credito", new CreditoResponseDTO(creditoActualizado),
                        "ordenId", ordenAnulada.getId(),
                        "estadoOrden", ordenAnulada.getEstado().toString()
                ));
            }

            Credito creditoAnulado = service.anularCredito(creditoId);
            log.warn("[CreditoController.anularCredito] Crédito sin orden asociada (incluyendo fallback por creditoDetalleId). Solo se anula crédito creditoId={}", creditoId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Crédito anulado exitosamente",
                "credito", new CreditoResponseDTO(creditoAnulado)
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

    /**
     * ⭐ ESTADO DE CUENTA DEL CLIENTE ESPECIAL
     * GET /api/creditos/cliente-especial/estado-cuenta
     * 
     * Retorna SOLO los créditos ACTIVOS con saldo pendiente > 0 del cliente especial.
     * Cliente especial: ID 499 - JAIRO JAVIER VELANDIA (NIT: 88249472)
     * 
     * Filtros opcionales:
     * - sedeId: Filtrar por sede específica
     * 
     * Los créditos se ordenan por fecha de inicio (más antiguos primero)
     */
    @GetMapping("/cliente-especial/estado-cuenta")
    public ResponseEntity<List<CreditoResponseDTO>> obtenerEstadoCuentaClienteEspecial(
            @RequestParam(required = false) Long sedeId) {
        
        List<CreditoResponseDTO> estadoCuenta = service.obtenerEstadoCuentaClienteEspecial(sedeId);
        return ResponseEntity.ok(estadoCuenta);
    }
    
    /**
     * 📊 ESTADO DE CUENTA DE UN CLIENTE
     * GET /api/creditos/cliente/{clienteId}/estado-cuenta
     * 
     * ⚠️ IMPORTANTE: Este endpoint EXCLUYE al cliente especial (ID 499)
     * Para estado de cuenta del cliente especial, usar: GET /api/creditos/cliente-especial/estado-cuenta
     * 
     * Retorna SOLO los créditos ACTIVOS con saldo pendiente > 0 del cliente.
     * Ideal para generar estados de cuenta, reportes de cartera, y seguimiento de deudas.
     * 
     * Incluye:
     * - Información completa del crédito
     * - Detalle de todos los abonos realizados
     * - Saldo pendiente actual
     * - Fecha de inicio y días transcurridos
     * - Información de la orden asociada
     * 
     * Filtros opcionales:
     * - sedeId: Filtrar por sede específica
     * 
     * Los créditos se ordenan por fecha de inicio (más antiguos primero)
     */
    @GetMapping("/cliente/{clienteId}/estado-cuenta")
    public ResponseEntity<List<CreditoResponseDTO>> obtenerEstadoCuenta(
            @PathVariable Long clienteId,
            @RequestParam(required = false) Long sedeId) {
        
        List<CreditoResponseDTO> estadoCuenta = service.obtenerEstadoCuenta(clienteId, sedeId);
        return ResponseEntity.ok(estadoCuenta);
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

    /**
     * 💰 LISTAR CRÉDITOS PENDIENTES DE UN CLIENTE
     * GET /api/creditos/cliente/{clienteId}/pendientes
     * 
     * Endpoint especializado para la página de abonos.
     * Retorna SOLO los créditos con saldo pendiente > 0 y estado ABIERTO.
     * 
     * Incluye:
     * - Datos del crédito (id, totalCredito, totalAbonado, saldoPendiente, estado)
     * - Datos de la orden (id, numero, fecha, obra)
     * - Montos (total, subtotal, iva)
     * - Retención de fuente (tieneRetencionFuente, retencionFuente)
     * - Sede y cliente
     * - Información de abonos (fechaUltimoAbono, cantidadAbonos)
     * 
     * Response 200 OK:
     * [
     *   {
     *     "creditoId": 31,
     *     "totalCredito": 500000.00,
     *     "saldoPendiente": 300000.00,
     *     "ordenNumero": 1001,
     *     "subtotal": 420168.07,
     *     "tieneRetencionFuente": true,
     *     "retencionFuente": 45693.28,
     *     ...
     *   }
     * ]
     */
    @GetMapping("/cliente/{clienteId}/pendientes")
    public ResponseEntity<?> listarCreditosPendientes(@PathVariable Long clienteId) {
        try {
            List<com.casaglass.casaglass_backend.dto.CreditoPendienteDTO> creditos = 
                service.listarCreditosPendientes(clienteId);
            
            return ResponseEntity.ok(creditos);
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

    /**
     * 💰 MARCAR CRÉDITOS DEL CLIENTE ESPECIAL COMO PAGADOS
     * 
     * Endpoint específico para el cliente especial (ID 499 - JAIRO JAVIER VELANDIA).
     * Permite marcar créditos como pagados sin crear registros de abonos detallados,
     * ya que estos pagos se realizan en persona manualmente.
     * 
     * Los créditos marcados como pagados:
     * - Se establecen con estado CERRADO
     * - Su saldo pendiente pasa a 0.0
     * - Ya NO aparecen en el estado de cuenta
     * 
     * @param creditoIds Lista de IDs de créditos a marcar como pagados
     * @return Respuesta con el número de créditos marcados como pagados
     * 
     * POST /api/creditos/cliente-especial/marcar-pagados
     * Body: { "creditoIds": [1, 2, 3] }
     */
    @PostMapping("/cliente-especial/marcar-pagados")
    public ResponseEntity<?> marcarCreditosClienteEspecialComoPagados(
            @Valid @RequestBody MarcarCreditosClienteEspecialRequest request) {
        try {
            EntregaClienteEspecial entregaEspecial = service.marcarCreditosClienteEspecialComoPagados(
                request.getCreditoIds(),
                request.getEjecutadoPor(),
                request.getObservaciones()
            );

            return ResponseEntity.ok(Map.of(
                "mensaje", "Créditos marcados como pagados exitosamente",
                "creditosPagados", entregaEspecial.getTotalCreditos(),
                "entregaEspecialId", entregaEspecial.getId(),
                "registro", new EntregaClienteEspecialResponseDTO(entregaEspecial)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of( // 409 Conflict
                "error", e.getMessage(),
                "tipo", "CONFLICTO_ESTADO"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor: " + e.getMessage(),
                "tipo", "SERVIDOR"
            ));
        }
    }

    /**
     * 📜 HISTÓRICO DE ENTREGAS DEL CLIENTE ESPECIAL
     * GET /api/creditos/cliente-especial/entregas
     */
    @GetMapping("/cliente-especial/entregas")
    public ResponseEntity<?> listarEntregasClienteEspecial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<EntregaClienteEspecial> entregas = entregaClienteEspecialService.listar(desde, hasta);
        List<EntregaClienteEspecialResumenDTO> respuesta = entregas.stream()
            .map(EntregaClienteEspecialResumenDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(respuesta);
    }

    /**
     * 🔍 DETALLE DE UNA ENTREGA DEL CLIENTE ESPECIAL
     * GET /api/creditos/cliente-especial/entregas/{id}
     */
    @GetMapping("/cliente-especial/entregas/{id}")
    public ResponseEntity<?> obtenerEntregaClienteEspecial(@PathVariable Long id) {
        try {
            EntregaClienteEspecial entrega = entregaClienteEspecialService.obtener(id);
            return ResponseEntity.ok(new EntregaClienteEspecialResponseDTO(entrega));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                "error", e.getMessage(),
                "tipo", "NO_ENCONTRADO"
            ));
        }
    }
}