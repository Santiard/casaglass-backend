package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.dto.OrdenTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaResponseDTO;
import com.casaglass.casaglass_backend.dto.OrdenDetalleDTO;
import com.casaglass.casaglass_backend.dto.OrdenResponseDTO;
import com.casaglass.casaglass_backend.dto.FacturaCreateDTO;
import com.casaglass.casaglass_backend.service.OrdenService;
import com.casaglass.casaglass_backend.service.FacturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordenes")
// CORS configurado globalmente en CorsConfig.java
public class OrdenController {

    private static final Logger log = LoggerFactory.getLogger(OrdenController.class);
    /**
     * 🗓️ VENTAS DEL DÍA POR SEDE
     * Devuelve todas las órdenes (contado y crédito) realizadas hoy en la sede indicada
     * GET /api/ordenes/ventas-dia/sede/{sedeId}
     */
    @GetMapping("/ventas-dia/sede/{sedeId}")
    public ResponseEntity<List<OrdenTablaDTO>> ventasDelDiaPorSede(
            @PathVariable Long sedeId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<OrdenTablaDTO> ordenes = service.ventasDelDiaPorSede(sedeId, fecha);
        return ResponseEntity.ok(ordenes);
    }

    /**
     * 🗓️ VENTAS DEL DÍA EN TODAS LAS SEDES
     * Devuelve todas las órdenes (contado y crédito) realizadas hoy en todas las sedes
     * GET /api/ordenes/ventas-dia/todas
     */
    @GetMapping("/ventas-dia/todas")
    public ResponseEntity<List<OrdenTablaDTO>> ventasDelDiaTodasLasSedes(
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<OrdenTablaDTO> ordenes = service.ventasDelDiaTodasLasSedes(fecha);
        return ResponseEntity.ok(ordenes);
    }

    private final OrdenService service;
    private final FacturaService facturaService;

    public OrdenController(OrdenService service, FacturaService facturaService) { 
        this.service = service;
        this.facturaService = facturaService;
    }

    /**
     * 🔄 ACTUALIZAR ORDEN DE VENTA
     * Endpoint para editar órdenes de venta con manejo automático de inventario
     * 
     * Características:
     * - Maneja productos nuevos (descuenta inventario)
     * - Maneja productos modificados (ajusta cantidades)
     * - Maneja productos eliminados (devuelve cantidades)
     * - Procesa cortes si existen
     * - Mantiene la misma lógica que POST /api/ordenes/venta
     */
    @PutMapping("/venta/{id}")
    public ResponseEntity<?> actualizarOrdenVenta(@PathVariable Long id, @RequestBody OrdenVentaDTO ventaDTO) {
        try {
            // Actualizar orden (con o sin crédito según el flag)
            Orden ordenActualizada;
            if (ventaDTO.isCredito()) {
                ordenActualizada = service.actualizarOrdenVentaConCredito(id, ventaDTO);
            } else {
                ordenActualizada = service.actualizarOrdenVenta(id, ventaDTO);
            }
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Orden de venta actualizada exitosamente",
                "orden", ordenActualizada,
                "numero", ordenActualizada.getNumero()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                "message", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "La orden ya fue incluida en una entrega de dinero y no puede editarse.",
                "code", "ORDER_ALREADY_IN_DELIVERY"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION",
                "codigo", "VALIDACION_FALLIDA"
            ));
        } catch (jakarta.persistence.OptimisticLockException | 
                 org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // 🔒 CONFLICTO DE CONCURRENCIA - Lock optimista detectó modificación simultánea
            return ResponseEntity.status(409).body(Map.of(
                "error", "⚠️ Otro usuario modificó el inventario simultáneamente. Por favor, intente nuevamente.",
                "tipo", "CONCURRENCIA",
                "codigo", "CONFLICTO_STOCK",
                "mensaje", "Conflicto de concurrencia. Por favor, intente nuevamente."
            ));
        } catch (RuntimeException e) {
            // RuntimeException NO es concurrencia, puede ser: entidad no encontrada, etc.
            // Detectar si es un error de "no encontrado"
            String mensaje = e.getMessage();
            if (mensaje != null && (mensaje.contains("no encontrado") || mensaje.contains("no encontrada"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "tipo", "ENTIDAD_NO_ENCONTRADA",
                    "codigo", "NOT_FOUND"
                ));
            }
            
            // Otros errores de runtime
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error al procesar la orden: " + e.getMessage(),
                "tipo", "ERROR_PROCESAMIENTO"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor: " + e.getMessage(),
                "tipo", "SERVIDOR"
            ));
        }
    }
    @PostMapping("/venta")
    public ResponseEntity<?> crearOrdenVenta(@RequestBody OrdenVentaDTO ventaDTO) {
        try {
            // Crear orden (con o sin crédito según el flag)
            OrdenVentaResponseDTO respuesta;
            if (ventaDTO.isCredito()) {
                respuesta = service.crearOrdenVentaConCredito(ventaDTO);
            } else {
                respuesta = service.crearOrdenVenta(ventaDTO);
            }
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Orden de venta creada exitosamente",
                "orden", respuesta.getOrden(),
                "numero", respuesta.getOrden().getNumero(),
                "cortesCreados", respuesta.getCortesCreados()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION",
                "codigo", "VALIDACION_FALLIDA"
            ));
        } catch (jakarta.persistence.OptimisticLockException | 
                 org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // 🔒 CONFLICTO DE CONCURRENCIA - Lock optimista detectó modificación simultánea
            return ResponseEntity.status(409).body(Map.of(
                "error", "⚠️ Otro usuario modificó el inventario simultáneamente. Por favor, intente nuevamente.",
                "tipo", "CONCURRENCIA",
                "codigo", "CONFLICTO_STOCK",
                "mensaje", "Conflicto de concurrencia. Por favor, intente nuevamente."
            ));
        } catch (RuntimeException e) {
            // RuntimeException NO es concurrencia, puede ser: entidad no encontrada, etc.
            // Detectar si es un error de "no encontrado"
            String mensaje = e.getMessage();
            if (mensaje != null && (mensaje.contains("no encontrado") || mensaje.contains("no encontrada"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "tipo", "ENTIDAD_NO_ENCONTRADA",
                    "codigo", "NOT_FOUND"
                ));
            }
            
            // Otros errores de runtime
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error al procesar la orden: " + e.getMessage(),
                "tipo", "ERROR_PROCESAMIENTO"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor: " + e.getMessage(),
                "tipo", "SERVIDOR"
            ));
        }
    }

    /**
     * 📋 CREAR ORDEN BÁSICA (compatibilidad)
     * Mantiene el endpoint original para compatibilidad con código existente
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Orden orden) {
        try {
            Orden ordenCreada = service.crear(orden);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Orden creada exitosamente",
                "orden", ordenCreada
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor: " + e.getMessage()
            ));
        }
    }

    /**
     * 📋 LISTADO DE ÓRDENES CON FILTROS COMPLETOS
     * GET /api/ordenes
     * 
     * Filtros disponibles (todos opcionales):
     * - clienteId: Filtrar por cliente
     * - sedeId: Filtrar por sede
     * - trabajadorId: Filtrar por trabajador (compatibilidad hacia atrás)
     * - estado: ACTIVA, ANULADA
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - venta: true para ventas, false para cotizaciones
     * - credito: true para órdenes a crédito
     * - facturada: true para órdenes facturadas, false para no facturadas
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, numero, total) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<Orden> (compatibilidad hacia atrás)
     */
    @GetMapping
    public Object listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long trabajadorId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Boolean venta,
            @RequestParam(required = false) Boolean credito,
            @RequestParam(required = false) Boolean facturada,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Convertir estado String a enum
        Orden.EstadoOrden estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = Orden.EstadoOrden.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado inválido: " + estado + ". Valores válidos: ACTIVA, ANULADA");
            }
        }

        // Si solo hay trabajadorId y ningún otro filtro nuevo, usar método específico (compatibilidad)
        if (trabajadorId != null && estadoEnum == null && fechaDesde == null && fechaHasta == null && 
            facturada == null && page == null && size == null && sortBy == null && sortOrder == null) {
            // Filtros combinados con trabajador (compatibilidad hacia atrás)
            if (sedeId != null) return service.listarPorSedeYTrabajador(sedeId, trabajadorId);
            if (venta != null) return service.listarPorTrabajadorYVenta(trabajadorId, venta);
            return service.listarPorTrabajador(trabajadorId);
        }
        
        // Si hay filtros antiguos simples sin filtros nuevos, mantener compatibilidad
        if (estadoEnum == null && fechaDesde == null && fechaHasta == null && facturada == null && 
            page == null && size == null && sortBy == null && sortOrder == null) {
            // Filtros combinados con sede (compatibilidad hacia atrás)
            if (clienteId != null && sedeId != null) return service.listarPorClienteYSede(clienteId, sedeId);
            if (sedeId != null && venta != null) return service.listarPorSedeYVenta(sedeId, venta);
            if (sedeId != null && credito != null) return service.listarPorSedeYCredito(sedeId, credito);
            
            // Filtros individuales (compatibilidad hacia atrás)
            if (sedeId != null) return service.listarPorSede(sedeId);
            if (clienteId != null) return service.listarPorCliente(clienteId);
            if (venta != null) return service.listarPorVenta(venta);
            if (credito != null) return service.listarPorCredito(credito);
            
            return service.listar();
        }
        
        // Usar método con filtros completos
        return service.listarConFiltros(
            clienteId, sedeId, estadoEnum, fechaDesde, fechaHasta, 
            venta, credito, facturada, page, size, sortBy, sortOrder
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponseDTO> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
            .map(orden -> {
                OrdenResponseDTO dto = new OrdenResponseDTO(orden);
                OrdenService.OrdenEntregaEstadoInfo estadoEntrega = service.obtenerEstadoEntregaOrden(orden.getId());
                dto.setEstaEnEntregaDinero(estadoEntrega.estaEnEntregaDinero());
                dto.setEntregaDineroId(estadoEntrega.entregaDineroId());
                dto.setEstadoEntrega(estadoEntrega.estadoEntrega());

                boolean puedeEditar = "ACTIVA".equals(dto.getEstado())
                    && !dto.isEstaEnEntregaDinero();
                dto.setPuedeEditar(puedeEditar);
                return ResponseEntity.ok(dto);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 📋 OBTENER ORDEN CON DETALLE COMPLETO (incluye items y cliente completo)
     * GET /api/ordenes/{id}/detalle
     * 
     * Retorna la orden con la estructura completa:
     * - Información básica de la orden (id, numero, fecha, obra, total)
     * - Cliente con todos sus datos (id, nombre, nit, direccion, telefono)
     * - Items con información del producto (id, nombre)
     * 
     * ✅ Usa fetch joins para cargar todas las relaciones de una vez
     * ✅ Funciona correctamente incluso para órdenes facturadas
     * ✅ Evita problemas de lazy loading
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<OrdenDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        try {
            // ✅ Usar método que carga todas las relaciones con fetch joins
            // Esto evita problemas de lazy loading, especialmente para órdenes facturadas
            return service.obtenerPorIdConRelaciones(id)
                    .map(orden -> {
                        // ✅ Forzar inicialización de colecciones si es necesario
                        if (orden.getItems() != null) {
                            orden.getItems().size(); // Forzar carga de items
                        }
                        OrdenDetalleDTO dto = new OrdenDetalleDTO(orden);
                        OrdenService.OrdenEntregaEstadoInfo estadoEntrega = service.obtenerEstadoEntregaOrden(orden.getId());
                        dto.setEstaEnEntregaDinero(estadoEntrega.estaEnEntregaDinero());
                        dto.setEntregaDineroId(estadoEntrega.entregaDineroId());
                        dto.setEstadoEntrega(estadoEntrega.estadoEntrega());
                        dto.setPuedeEditar("ACTIVA".equals(dto.getEstado()) && !dto.isEstaEnEntregaDinero());
                        return ResponseEntity.ok(dto);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (org.hibernate.LazyInitializationException e) {
            // Si aún falla por lazy loading, intentar cargar de nuevo
            log.warn("Error de lazy loading al obtener detalle de orden {}: {}", id, e.getMessage());
            return service.obtenerPorIdConRelaciones(id)
                    .map(orden -> ResponseEntity.ok(new OrdenDetalleDTO(orden)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (jakarta.persistence.EntityNotFoundException e) {
            log.error("Orden no encontrada con ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al obtener detalle de orden {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<Orden> obtenerPorNumero(@PathVariable Long numero) {
        return service.obtenerPorNumero(numero).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 📋 LISTAR ÓRDENES POR CLIENTE CON FILTROS OPCIONALES
     * GET /api/ordenes/cliente/{clienteId}?fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD
     * 
     * Optimizado para mejorar rendimiento:
     * - Filtra en la base de datos en lugar del frontend
     * - Reduce el tamaño de la respuesta
     * - Mejora el tiempo de carga
     */
    @GetMapping("/cliente/{clienteId}")
    public List<Orden> listarPorCliente(
            @PathVariable Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        
        if (fechaDesde != null && fechaHasta != null) {
            // Validar que fechaDesde <= fechaHasta
            if (fechaDesde.isAfter(fechaHasta)) {
                throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
            }
            return service.listarPorClienteConFiltros(clienteId, fechaDesde, fechaHasta);
        }
        return service.listarPorCliente(clienteId);
    }

    @GetMapping("/fecha/{fecha}")
    public List<Orden> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listarPorFecha(fecha);
    }

    @GetMapping("/fecha")
    public List<Orden> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorRangoFechas(desde, hasta);
    }

    @GetMapping("/venta/{venta}")
    public List<Orden> listarPorVenta(@PathVariable boolean venta) {
        return service.listarPorVenta(venta);
    }

    @GetMapping("/credito/{credito}")
    public List<Orden> listarPorCredito(@PathVariable boolean credito) {
        return service.listarPorCredito(credito);
    }

    // Nuevos endpoints para sede
    @GetMapping("/sede/{sedeId}")
    public List<Orden> listarPorSede(@PathVariable Long sedeId) {
        return service.listarPorSede(sedeId);
    }

    @GetMapping("/sede/{sedeId}/cliente/{clienteId}")
    public List<Orden> listarPorSedeYCliente(@PathVariable Long sedeId, @PathVariable Long clienteId) {
        return service.listarPorClienteYSede(clienteId, sedeId);
    }

    @GetMapping("/sede/{sedeId}/venta/{venta}")
    public List<Orden> listarPorSedeYVenta(@PathVariable Long sedeId, @PathVariable boolean venta) {
        return service.listarPorSedeYVenta(sedeId, venta);
    }

    @GetMapping("/sede/{sedeId}/credito/{credito}")
    public List<Orden> listarPorSedeYCredito(@PathVariable Long sedeId, @PathVariable boolean credito) {
        return service.listarPorSedeYCredito(sedeId, credito);
    }

    @GetMapping("/sede/{sedeId}/fecha/{fecha}")
    public List<Orden> listarPorSedeYFecha(@PathVariable Long sedeId, 
                                           @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listarPorSedeYFecha(sedeId, fecha);
    }

    @GetMapping("/sede/{sedeId}/fecha")
    public List<Orden> listarPorSedeYRango(@PathVariable Long sedeId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorSedeYRangoFechas(sedeId, desde, hasta);
    }

    // 🆕 Endpoints para trabajador
    @GetMapping("/trabajador/{trabajadorId}")
    public List<Orden> listarPorTrabajador(@PathVariable Long trabajadorId) {
        return service.listarPorTrabajador(trabajadorId);
    }

    @GetMapping("/trabajador/{trabajadorId}/venta/{venta}")
    public List<Orden> listarPorTrabajadorYVenta(@PathVariable Long trabajadorId, @PathVariable boolean venta) {
        return service.listarPorTrabajadorYVenta(trabajadorId, venta);
    }

    @GetMapping("/trabajador/{trabajadorId}/fecha/{fecha}")
    public List<Orden> listarPorTrabajadorYFecha(@PathVariable Long trabajadorId, 
                                                 @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listarPorTrabajadorYFecha(trabajadorId, fecha);
    }

    @GetMapping("/trabajador/{trabajadorId}/fecha")
    public List<Orden> listarPorTrabajadorYRango(@PathVariable Long trabajadorId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorTrabajadorYRangoFechas(trabajadorId, desde, hasta);
    }

    @GetMapping("/sede/{sedeId}/trabajador/{trabajadorId}")
    public List<Orden> listarPorSedeYTrabajador(@PathVariable Long sedeId, @PathVariable Long trabajadorId) {
        return service.listarPorSedeYTrabajador(sedeId, trabajadorId);
    }

    /**
     * Obtiene el próximo número de orden que se asignará
     * Útil para mostrar en el frontend como referencia provisional
     */
    @GetMapping("/proximo-numero")
    public ResponseEntity<Long> obtenerProximoNumero() {
        try {
            Long proximoNumero = service.obtenerProximoNumero();
            return ResponseEntity.ok(proximoNumero);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 💰 ACTUALIZAR RETENCIÓN DE FUENTE DE UNA ORDEN
     * PUT /api/ordenes/{id}/retencion-fuente
     * 
     * Endpoint especializado para actualizar SOLO los campos de retención de fuente
     * sin necesidad de enviar todos los datos de la orden (items, cliente, sede, etc.)
     * 
     * Request Body:
     * {
     *   "tieneRetencionFuente": true,     // OBLIGATORIO: boolean
     *   "retencionFuente": 25000.50,      // OBLIGATORIO: número (0.0 si no tiene retención)
     *   "iva": 47500.00                   // OPCIONAL: número (se calcula automáticamente si no se envía)
     * }
     * 
     * Response 200 OK:
     * {
     *   "id": 124,
     *   "numero": 1001,
     *   "tieneRetencionFuente": true,
     *   "retencionFuente": 25000.50,
     *   "iva": 47500.00,
     *   "total": 297500.50,
     *   "creditoDetalle": {
     *     "id": 45,
     *     "saldoPendiente": 272500.00,
     *     ...
     *   },
     *   ...
     * }
     * 
     * Características:
     * - Actualiza tieneRetencionFuente, retencionFuente, e IVA
     * - Recalcula el total de la orden
     * - Si la orden tiene crédito, actualiza también el saldo del crédito
     * - Validaciones de seguridad (orden debe existir y estar ACTIVA)
     * 
     * Errores posibles:
     * - 400 Bad Request: Validaciones fallidas (retención inválida, orden anulada)
     * - 404 Not Found: Orden no existe
     * - 500 Internal Server Error: Error inesperado
     */
    @PutMapping("/{id}/retencion-fuente")
    public ResponseEntity<?> actualizarRetencionFuente(
            @PathVariable Long id,
            @RequestBody com.casaglass.casaglass_backend.dto.RetencionFuenteDTO retencionDTO) {
        try {
            Orden ordenActualizada = service.actualizarRetencionFuente(id, retencionDTO);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Retención de fuente actualizada exitosamente",
                "orden", ordenActualizada
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                "message", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "La orden ya fue incluida en una entrega de dinero y no puede editarse.",
                "code", "ORDER_ALREADY_IN_DELIVERY"
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

    /**
     * 💰 ACTUALIZAR RETENCIÓN ICA DE UNA ORDEN
     * PUT /api/ordenes/{id}/retencion-ica
     * 
     * Endpoint especializado para actualizar SOLO los campos de retención ICA
     * sin necesidad de enviar todos los datos de la orden (items, cliente, sede, etc.)
     * 
     * Request Body:
     * {
     *   "tieneRetencionIca": true,        // OBLIGATORIO: boolean
     *   "porcentajeIca": 1.0,             // OPCIONAL: número (si no se envía, se usa el de BusinessSettings)
     *   "retencionIca": 10000.50,         // OBLIGATORIO: número (0.0 si no tiene retención)
     *   "iva": 47500.00                   // OPCIONAL: número (se calcula automáticamente si no se envía)
     * }
     */
    @PutMapping("/{id}/retencion-ica")
    public ResponseEntity<?> actualizarRetencionIca(
            @PathVariable Long id,
            @RequestBody com.casaglass.casaglass_backend.dto.RetencionIcaDTO retencionDTO) {
        try {
            Orden ordenActualizada = service.actualizarRetencionIca(id, retencionDTO);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Retención ICA actualizada exitosamente",
                "orden", ordenActualizada
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of(
                "message", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "La orden ya fue incluida en una entrega de dinero y no puede editarse.",
                "code", "ORDER_ALREADY_IN_DELIVERY"
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

    // 🎯 ================================
    // 🎯 ENDPOINTS OPTIMIZADOS PARA TABLA
    // 🎯 ================================

    /**
     * 🚀 LISTADO OPTIMIZADO PARA TABLA DE ÓRDENES CON FILTROS COMPLETOS
     * GET /api/ordenes/tabla
     * 
     * Filtros disponibles:
     * - clienteId: Filtrar por cliente
     * - sedeId: Filtrar por sede de la orden (o.sede)
     * - estado: ACTIVA, ANULADA
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - venta: true para ventas, false para cotizaciones
     * - credito: true para órdenes a crédito
     * - facturada: true para órdenes facturadas, false para no facturadas
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, numero, total) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<OrdenTablaDTO> (compatibilidad hacia atrás)
     */
    @GetMapping("/tabla")
    public Object listarParaTabla(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long trabajadorId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Boolean venta,
            @RequestParam(required = false) Boolean credito,
            @RequestParam(required = false) Boolean facturada,
            @RequestParam(required = false) String estadoPago,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Convertir estado String a enum
        Orden.EstadoOrden estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = Orden.EstadoOrden.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado inválido: " + estado + ". Valores válidos: ACTIVA, ANULADA");
            }
        }

        String estadoPagoNormalizado = normalizarEstadoPago(estadoPago);
        
        // Si hay trabajadorId, filtrar por trabajador (compatibilidad hacia atrás)
        // Nota: trabajadorId no está en el query del repositorio, se maneja después
        if (trabajadorId != null && (clienteId == null && sedeId == null && estadoEnum == null && 
            fechaDesde == null && fechaHasta == null && venta == null && credito == null && facturada == null &&
            estadoPagoNormalizado == null)) {
            // Solo filtro por trabajador, usar método específico
            if (page != null && size != null) {
                // TODO: Implementar paginación para trabajador
                return service.listarPorTrabajadorParaTabla(trabajadorId);
            }
            return service.listarPorTrabajadorParaTabla(trabajadorId);
        }
        
        // Usar método con filtros completos
        return service.listarParaTablaConFiltros(
            clienteId, sedeId, estadoEnum, fechaDesde, fechaHasta, 
            venta, credito, facturada, estadoPagoNormalizado, page, size, sortBy, sortOrder
        );
    }

    private String normalizarEstadoPago(String estadoPago) {
        if (estadoPago == null) {
            return null;
        }

        String normalizado = estadoPago.trim().toUpperCase();
        if (normalizado.isEmpty()) {
            return null;
        }

        if ("NO_PAGADO".equals(normalizado)) {
            normalizado = "NO PAGADO";
        }

        if (!"PAGADO".equals(normalizado) &&
            !"ABONADO".equals(normalizado) &&
            !"NO PAGADO".equals(normalizado)) {
            throw new IllegalArgumentException(
                "estadoPago inválido: " + estadoPago + ". Valores válidos: PAGADO, ABONADO, NO PAGADO"
            );
        }

        return normalizado;
    }

    /**
     * 💳 LISTADO DE ÓRDENES A CRÉDITO POR CLIENTE CON FILTROS
     * GET /api/ordenes/credito?clienteId=X&fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD&estado=ABIERTO&page=1&size=50
     * 
     * Parámetros:
     * - clienteId: Integer (OBLIGATORIO) - ID del cliente
     * - fechaDesde: YYYY-MM-DD (opcional) - Fecha desde
     * - fechaHasta: YYYY-MM-DD (opcional) - Fecha hasta
     * - estado: String (opcional) - Estado del crédito (ABIERTO, CERRADO, ANULADO)
     * - page: Integer (opcional, default: sin paginación) - Número de página
     * - size: Integer (opcional, default: 50, máximo: 200) - Tamaño de página
     * 
     * Retorna solo órdenes a crédito del cliente especificado con información del crédito:
     * - id, numero, fecha, total, credito
     * - creditoDetalle: { creditoId, saldoPendiente }
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<OrdenCreditoDTO> (compatibilidad hacia atrás)
     */
    @GetMapping("/credito")
    public Object listarOrdenesCredito(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        if (clienteId == null) {
            throw new IllegalArgumentException("El parámetro clienteId es obligatorio");
        }
        
        // Convertir estado String a enum
        com.casaglass.casaglass_backend.model.Credito.EstadoCredito estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = com.casaglass.casaglass_backend.model.Credito.EstadoCredito.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado inválido: " + estado + ". Valores válidos: ABIERTO, CERRADO, ANULADO");
            }
        }
        
        // Si no hay filtros nuevos, usar método original (compatibilidad)
        if (fechaDesde == null && fechaHasta == null && estadoEnum == null && page == null && size == null) {
            return service.listarOrdenesCreditoPorCliente(clienteId);
        }
        
        // Usar método con filtros
        return service.listarOrdenesCreditoPorClienteConFiltros(
            clienteId, fechaDesde, fechaHasta, estadoEnum, page, size
        );
    }

    /**
     * 🚀 ÓRDENES DE UNA SEDE PARA TABLA (optimizado)
     */
    @GetMapping("/tabla/sede/{sedeId}")
    public List<OrdenTablaDTO> listarPorSedeParaTabla(@PathVariable Long sedeId) {
        return service.listarPorSedeParaTabla(sedeId);
    }

    /**
     * 🚀 ÓRDENES DE UN TRABAJADOR PARA TABLA (optimizado)
     */
    @GetMapping("/tabla/trabajador/{trabajadorId}")
    public List<OrdenTablaDTO> listarPorTrabajadorParaTabla(@PathVariable Long trabajadorId) {
        return service.listarPorTrabajadorParaTabla(trabajadorId);
    }

    /**
     * 🚀 ÓRDENES DE UN CLIENTE PARA TABLA (optimizado)
     */
    @GetMapping("/tabla/cliente/{clienteId}")
    public List<OrdenTablaDTO> listarPorClienteParaTabla(@PathVariable Long clienteId) {
        return service.listarPorClienteParaTabla(clienteId);
    }

    // 🔄 ================================
    // 🔄 ENDPOINT DE ACTUALIZACIÓN
    // 🔄 ================================

    /**
     * 🔄 ACTUALIZAR ORDEN COMPLETA (compatible con tabla)
     * Permite actualizar orden + items desde la estructura de tabla
     * 
     * Body esperado:
     * {
     *   "id": 1,
     *   "fecha": "2025-10-16",
     *   "obra": "Nueva obra",
     *   "venta": true,
     *   "credito": false,
     *   "clienteId": 1,
     *   "trabajadorId": 2,
     *   "sedeId": 1,
     *   "items": [
     *     {
     *       "id": 1,           // null = nuevo, valor = actualizar
     *       "productoId": 1,
     *       "descripcion": "Descripción actualizada",
     *       "cantidad": 15,
     *       "precioUnitario": 2.0,
     *       "totalLinea": 30.0,
     *       "eliminar": false  // true = eliminar este item
     *     }
     *   ]
     * }
     */
    @PutMapping("/tabla/{id}")
    public ResponseEntity<?> actualizarOrden(@PathVariable Long id, 
                                            @RequestBody OrdenActualizarDTO ordenDTO) {
        try {
            log.info("[PUT /api/ordenes/tabla/{}] Inicio actualización items={} venta={} credito={} sedeId={}",
                id,
                ordenDTO.getItems() != null ? ordenDTO.getItems().size() : 0,
                ordenDTO.isVenta(),
                ordenDTO.isCredito(),
                ordenDTO.getSedeId());
            OrdenTablaDTO ordenActualizada = service.actualizarOrden(id, ordenDTO);
            log.info("[PUT /api/ordenes/tabla/{}] OK actualización completada", id);
            return ResponseEntity.ok(ordenActualizada);
        } catch (IllegalStateException e) {
            log.warn("[PUT /api/ordenes/tabla/{}] Bloqueada por entrega de dinero: {}", id, e.getMessage());
            return ResponseEntity.status(409)
                    .body(Map.of(
                        "message", e.getMessage() != null && !e.getMessage().isBlank()
                                ? e.getMessage()
                                : "La orden ya fue incluida en una entrega de dinero y no puede editarse.",
                        "code", "ORDER_ALREADY_IN_DELIVERY"
                    ));
        } catch (IllegalArgumentException e) {
            log.error("[PUT /api/ordenes/tabla/{}] Error de validación: {}", id, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Orden no encontrada", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[PUT /api/ordenes/tabla/{}] Error inesperado: {}", id, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Error procesando solicitud", "message", e.getMessage()));
        }
    }

    // 🚫 ================================
    // 🚫 ENDPOINT DE ANULACIÓN
    // 🚫 ================================

    /**
     * 🚫 ANULAR ORDEN (no eliminar, cambiar estado a ANULADA)
     * Restaura automáticamente el inventario de productos
     * 
     * Solo se pueden anular órdenes ACTIVAS
     * Una vez anulada, no se puede revertir
     */
    @PutMapping("/{id}/anular")
    public ResponseEntity<?> anularOrden(@PathVariable Long id) {
        try {
            Orden ordenAnulada = service.anularOrden(id);
            return ResponseEntity.ok(Map.of(
                "message", "Orden anulada correctamente", 
                "ordenId", ordenAnulada.getId(),
                "numero", ordenAnulada.getNumero(),
                "estado", ordenAnulada.getEstado().toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Error al anular orden", "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Error procesando anulación", "message", e.getMessage()));
        }
    }

    /**
     * 🧹 ELIMINAR ORDEN ANULADA (BORRADO FÍSICO)
     *
     * Reglas de negocio:
     * - Solo elimina órdenes en estado ANULADA
     * - Si existe crédito asociado, debe estar ANULADO
     * - Si tiene factura asociada, no se elimina
     *
     * Uso recomendado: limpieza administrativa en módulo de órdenes anuladas.
     */
    @DeleteMapping("/{id}/eliminar-anulada")
    public ResponseEntity<?> eliminarOrdenAnulada(@PathVariable Long id) {
        try {
            service.eliminarOrdenAnulada(id);
            return ResponseEntity.ok(Map.of(
                "message", "Orden anulada eliminada correctamente",
                "ordenId", id
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se pudo eliminar la orden anulada",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error interno al eliminar orden anulada",
                "message", e.getMessage()
            ));
        }
    }

    // 🧾 ================================
    // 🧾 ENDPOINT DE FACTURACIÓN
    // 🧾 ================================

    /**
     * 🧾 MARCAR ORDEN COMO FACTURADA O DESFACTURADA
     * PUT /api/ordenes/{id}/facturar
     * 
     * Body esperado:
     * {
     *   "facturada": true  // true = crear factura, false = eliminar factura
     * }
     * 
     * Si facturada = true: Crea una factura para esta orden
     * Si facturada = false: Elimina la factura asociada a esta orden
     */
    @PutMapping("/{id}/facturar")
    public ResponseEntity<?> cambiarEstadoFacturacion(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean facturada = body.get("facturada");
            
            if (facturada == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "El campo 'facturada' es requerido",
                    "message", "Debe enviar 'facturada': true o false"
                ));
            }
            
            if (facturada) {
                // MARCAR COMO FACTURADA - Crear factura
                return service.obtenerPorId(id)
                    .map(orden -> {
                        try {
                            // Verificar que no tenga factura ya
                            if (orden.getFactura() != null) {
                                return ResponseEntity.badRequest().body(Map.of(
                                    "error", "La orden ya tiene una factura",
                                    "facturaId", orden.getFactura().getId()
                                ));
                            }
                            
                            // Crear factura automática
                            FacturaCreateDTO facturaDTO = new FacturaCreateDTO();
                            facturaDTO.setOrdenId(orden.getId());
                            facturaDTO.setFecha(LocalDate.now());
                            facturaDTO.setSubtotal(orden.getSubtotal());
                            // Calcular IVA correctamente desde el subtotal (que ya incluye IVA)
                            // Fórmula: IVA = Subtotal * (tasa_iva / (100 + tasa_iva))
                            Double ivaCalculado = service.calcularIvaDesdeSubtotal(orden.getSubtotal());
                            facturaDTO.setIva(ivaCalculado);
                            // Usar la retención de fuente de la orden (ya calculada)
                            facturaDTO.setRetencionFuente(orden.getRetencionFuente() != null ? orden.getRetencionFuente() : 0.0);
                            facturaDTO.setTotal(orden.getTotal());
                            facturaDTO.setFormaPago("PENDIENTE");
                            facturaDTO.setObservaciones("Factura generada automáticamente");
                            
                            var facturaCreada = facturaService.crearFactura(facturaDTO);
                            
                            return ResponseEntity.ok(Map.of(
                                "mensaje", "Orden marcada como facturada",
                                "ordenId", orden.getId(),
                                "facturaId", facturaCreada.getId(),
                                "numeroFactura", facturaCreada.getNumeroFactura(),
                                "facturada", true
                            ));
                        } catch (Exception e) {
                            e.printStackTrace();
                            return ResponseEntity.status(500).body(Map.of(
                                "error", "Error al crear factura",
                                "message", e.getMessage()
                            ));
                        }
                    })
                    .orElse(ResponseEntity.status(404).body(Map.of("error", "Orden no encontrada")));
            } else {
                // DESFACTURAR - Eliminar factura
                return service.obtenerPorId(id)
                    .map(orden -> {
                        try {
                            if (orden.getFactura() == null) {
                                return ResponseEntity.badRequest().body(Map.of(
                                    "error", "La orden no tiene una factura para eliminar"
                                ));
                            }
                            
                            Long facturaId = orden.getFactura().getId();
                            facturaService.eliminarFactura(facturaId);
                            
                            return ResponseEntity.ok(Map.of(
                                "mensaje", "Orden desfacturada correctamente",
                                "ordenId", orden.getId(),
                                "facturaId", facturaId,
                                "facturada", false
                            ));
                        } catch (Exception e) {
                            e.printStackTrace();
                            return ResponseEntity.status(500).body(Map.of(
                                "error", "Error al eliminar factura",
                                "message", e.getMessage()
                            ));
                        }
                    })
                    .orElse(ResponseEntity.status(404).body(Map.of("error", "Orden no encontrada")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error procesando solicitud",
                "message", e.getMessage()
            ));
        }
    }

}
