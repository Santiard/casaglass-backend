package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.EntregaDineroCreateDTO;
import com.casaglass.casaglass_backend.dto.EntregaDineroResponseDTO;
import com.casaglass.casaglass_backend.dto.OrdenParaEntregaDTO;
import com.casaglass.casaglass_backend.dto.EntregaDetalleSimpleDTO;
import com.casaglass.casaglass_backend.dto.AbonoParaEntregaDTO;
import com.casaglass.casaglass_backend.model.EntregaDinero;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.service.EntregaDineroService;
import com.casaglass.casaglass_backend.service.EntregaDetalleService;
import com.casaglass.casaglass_backend.service.AbonoService;
import com.casaglass.casaglass_backend.model.Abono;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/entregas-dinero")
// CORS configurado globalmente en CorsConfig.java
public class EntregaDineroController {

    private final EntregaDineroService service;

    @Autowired
    private EntregaDetalleService entregaDetalleService;
    
    @Autowired
    private AbonoService abonoService;

    public EntregaDineroController(EntregaDineroService service) {
        this.service = service;
    }

    /* ========== CONSULTAS ========== */

    /**
     * 📋 LISTADO DE ENTREGAS DE DINERO CON FILTROS COMPLETOS
     * GET /api/entregas-dinero
     * 
     * Filtros disponibles (todos opcionales):
     * - sedeId: Filtrar por sede
     * - empleadoId: Filtrar por empleado
     * - estado: PENDIENTE, ENTREGADA, VERIFICADA, RECHAZADA
     * - desde: YYYY-MM-DD (fecha desde, inclusive)
     * - hasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - conDiferencias: Boolean (no implementado actualmente)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, id) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<EntregaDineroResponseDTO> (compatibilidad hacia atrás)
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Object> listar(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long empleadoId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Boolean conDiferencias,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        try {
            // Convertir estado String a enum
            EntregaDinero.EstadoEntrega estadoEnum = null;
            if (estado != null && !estado.isEmpty()) {
                try {
                    estadoEnum = EntregaDinero.EstadoEntrega.valueOf(estado.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Estado inválido: " + estado + ". Valores válidos: PENDIENTE, ENTREGADA, VERIFICADA, RECHAZADA"
                    ));
                }
            }
            
            // Si no hay filtros nuevos (paginación, ordenamiento), usar método original (compatibilidad)
            if (page == null && size == null && sortBy == null && sortOrder == null && conDiferencias == null) {
                // Usar lógica original para compatibilidad
                List<EntregaDinero> entregas;
                if (sedeId != null && estadoEnum != null) {
                    entregas = service.obtenerPorSedeYEstado(sedeId, estadoEnum);
                } else if (sedeId != null && desde != null && hasta != null) {
                    entregas = service.obtenerPorSedeYPeriodo(sedeId, desde, hasta);
                } else if (desde != null && hasta != null) {
                    entregas = service.obtenerPorPeriodo(desde, hasta);
                } else if (sedeId != null) {
                    entregas = service.obtenerPorSede(sedeId);
                } else if (empleadoId != null) {
                    entregas = service.obtenerPorEmpleado(empleadoId);
                } else if (estadoEnum != null) {
                    entregas = service.obtenerPorEstado(estadoEnum);
                } else {
                    entregas = service.obtenerTodas();
                }
                
                return ResponseEntity.ok(entregas.stream()
                        .map(EntregaDineroResponseDTO::new)
                        .collect(Collectors.toList()));
            }
            
            // Usar método con filtros completos
            Object resultado = service.obtenerEntregasConFiltros(
                sedeId, empleadoId, estadoEnum, desde, hasta, conDiferencias, page, size, sortBy, sortOrder
            );
            
            // Si es lista paginada, convertir las entregas a DTOs
            if (resultado instanceof com.casaglass.casaglass_backend.dto.PageResponse) {
                @SuppressWarnings("unchecked")
                com.casaglass.casaglass_backend.dto.PageResponse<EntregaDinero> pageResponse = 
                    (com.casaglass.casaglass_backend.dto.PageResponse<EntregaDinero>) resultado;
                
                List<EntregaDineroResponseDTO> contenidoDTO = pageResponse.getContent().stream()
                        .map(EntregaDineroResponseDTO::new)
                        .collect(Collectors.toList());
                
                return ResponseEntity.ok(com.casaglass.casaglass_backend.dto.PageResponse.of(
                    contenidoDTO, pageResponse.getTotalElements(), pageResponse.getPage(), pageResponse.getSize()
                ));
            }
            
            // Si es lista simple, convertir a DTOs
            @SuppressWarnings("unchecked")
            List<EntregaDinero> entregas = (List<EntregaDinero>) resultado;
            return ResponseEntity.ok(entregas.stream()
                    .map(EntregaDineroResponseDTO::new)
                    .collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * 🔍 OBTENER ENTREGA POR ID
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<EntregaDineroResponseDTO> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(entrega -> {
                    EntregaDineroResponseDTO dto = new EntregaDineroResponseDTO(entrega);
                    // Los detalles ya incluyen el monto del abono específico si existe
                    if (dto.getDetalles() != null) {
                        dto.setDetalles(entrega.getDetalles().stream()
                                .map(EntregaDetalleSimpleDTO::new)
                                .collect(Collectors.toList()));
                    }
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🏢 LISTAR ENTREGAS POR SEDE
     */
    @GetMapping("/sede/{sedeId}")
    @Transactional(readOnly = true)
    public List<EntregaDineroResponseDTO> listarPorSede(@PathVariable Long sedeId) {
        return service.obtenerPorSede(sedeId).stream()
                .map(EntregaDineroResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 👤 LISTAR ENTREGAS POR EMPLEADO
     */
    @GetMapping("/empleado/{empleadoId}")
    @Transactional(readOnly = true)
    public List<EntregaDineroResponseDTO> listarPorEmpleado(@PathVariable Long empleadoId) {
        return service.obtenerPorEmpleado(empleadoId).stream()
                .map(EntregaDineroResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 📈 RESUMEN POR EMPLEADO
     */
    @GetMapping("/resumen/empleado")
    public List<Object[]> obtenerResumenPorEmpleado(@RequestParam Long sedeId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.obtenerResumenPorEmpleado(sedeId, desde, hasta);
    }

    /* ========== CREAR ENTREGA ========== */

    /**
     * 💰 CREAR NUEVA ENTREGA
     */
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody EntregaDineroCreateDTO entregaDTO) {
        try {
            System.out.println("🔍 DEBUG: Creando entrega de dinero");
            System.out.println("🔍 DEBUG: Datos recibidos: " + entregaDTO);
            
            // Convertir DTO a entidad
            EntregaDinero entrega = new EntregaDinero();
            
            // Configurar sede
            Sede sede = new Sede();
            sede.setId(entregaDTO.getSedeId());
            entrega.setSede(sede);
            
            // Configurar empleado
            Trabajador empleado = new Trabajador();
            empleado.setId(entregaDTO.getEmpleadoId());
            entrega.setEmpleado(empleado);
            
            // Configurar fechas
            entrega.setFechaEntrega(entregaDTO.getFechaEntrega());
            
            // Configurar modalidad y otros campos
            entrega.setModalidadEntrega(EntregaDinero.ModalidadEntrega.valueOf(entregaDTO.getModalidadEntrega()));
            
            // Configurar montos (el servicio puede recalcular si es necesario)
            entrega.setMonto(entregaDTO.getMonto() != null ? entregaDTO.getMonto() : 0.0);
            entrega.setMontoEfectivo(entregaDTO.getMontoEfectivo() != null ? entregaDTO.getMontoEfectivo() : 0.0);
            entrega.setMontoTransferencia(entregaDTO.getMontoTransferencia() != null ? entregaDTO.getMontoTransferencia() : 0.0);
            entrega.setMontoCheque(entregaDTO.getMontoCheque() != null ? entregaDTO.getMontoCheque() : 0.0);
            entrega.setMontoDeposito(entregaDTO.getMontoDeposito() != null ? entregaDTO.getMontoDeposito() : 0.0);
            
            System.out.println("🔍 DEBUG: Entrega configurada: " + entrega);
            System.out.println("🔍 DEBUG: Órdenes a incluir: " + entregaDTO.getOrdenesIds());
            
            // Obtener IDs de abonos del DTO (para órdenes a crédito)
            List<Long> abonosIds = entregaDTO.getAbonosIds() != null && !entregaDTO.getAbonosIds().isEmpty() 
                ? entregaDTO.getAbonosIds() 
                : null;
            
            // Obtener IDs de reembolsos del DTO (para egresos)
            List<Long> reembolsosIds = entregaDTO.getReembolsosIds() != null && !entregaDTO.getReembolsosIds().isEmpty() 
                ? entregaDTO.getReembolsosIds() 
                : null;
            
            // Llamar al servicio para crear la entrega (con soporte para reembolsos)
            EntregaDinero entregaCreada = service.crearEntregaConReembolsos(
                entrega, 
                entregaDTO.getOrdenesIds(), 
                abonosIds,
                reembolsosIds
            );
            
            System.out.println("✅ DEBUG: Entrega creada con ID: " + entregaCreada.getId());
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Entrega creada exitosamente",
                "entrega", new EntregaDineroResponseDTO(entregaCreada)
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

    /* ========== ACCIONES DE ENTREGA ========== */

    /**
     * ✅ CONFIRMAR ENTREGA (cambiar estado a ENTREGADA)
     */
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long id) {
        try {
            EntregaDinero entregaConfirmada = service.confirmarEntrega(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Entrega confirmada exitosamente",
                "entrega", new EntregaDineroResponseDTO(entregaConfirmada)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * ❌ CANCELAR ENTREGA (cambiar estado a RECHAZADA)
     */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            EntregaDinero entregaCancelada = service.cancelarEntrega(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Entrega cancelada exitosamente",
                "entrega", new EntregaDineroResponseDTO(entregaCancelada)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * 🔄 ACTUALIZAR ENTREGA
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                      @Valid @RequestBody EntregaDineroCreateDTO entregaDTO) {
        try {
            // Convertir DTO a entidad
            EntregaDinero entrega = new EntregaDinero();
            
            // Configurar sede
            Sede sede = new Sede();
            sede.setId(entregaDTO.getSedeId());
            entrega.setSede(sede);
            
            // Configurar empleado
            Trabajador empleado = new Trabajador();
            empleado.setId(entregaDTO.getEmpleadoId());
            entrega.setEmpleado(empleado);
            
            // Configurar fechas y otros campos
            entrega.setFechaEntrega(entregaDTO.getFechaEntrega());
            entrega.setModalidadEntrega(EntregaDinero.ModalidadEntrega.valueOf(entregaDTO.getModalidadEntrega()));
            entrega.setMonto(entregaDTO.getMonto() != null ? entregaDTO.getMonto() : 0.0);
            entrega.setMontoEfectivo(entregaDTO.getMontoEfectivo() != null ? entregaDTO.getMontoEfectivo() : 0.0);
            entrega.setMontoTransferencia(entregaDTO.getMontoTransferencia() != null ? entregaDTO.getMontoTransferencia() : 0.0);
            entrega.setMontoCheque(entregaDTO.getMontoCheque() != null ? entregaDTO.getMontoCheque() : 0.0);
            entrega.setMontoDeposito(entregaDTO.getMontoDeposito() != null ? entregaDTO.getMontoDeposito() : 0.0);
            
            EntregaDinero entregaActualizada = service.actualizarEntrega(id, entrega);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Entrega actualizada exitosamente",
                "entrega", new EntregaDineroResponseDTO(entregaActualizada)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ ELIMINAR ENTREGA
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminarEntrega(id);
            return ResponseEntity.ok(Map.of("mensaje", "Entrega eliminada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /* ========== MÉTODOS AUXILIARES ========== */

    /**
     * 📋 OBTENER ÓRDENES Y ABONOS DISPONIBLES PARA ENTREGA
     * - Órdenes A CONTADO: Se muestran las órdenes completas
     * - Órdenes A CRÉDITO: Se muestran los ABONOS individuales (no las órdenes)
     */
    @GetMapping("/ordenes-disponibles")
    public ResponseEntity<?> obtenerOrdenesDisponibles(@RequestParam Long sedeId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            System.out.println("🔍 DEBUG: Buscando órdenes y abonos disponibles para entrega");
            System.out.println("🔍 DEBUG: Sede " + sedeId + ", período " + desde + " a " + hasta);
            
            // Obtener órdenes A CONTADO disponibles
            List<Orden> ordenesContado = entregaDetalleService.obtenerOrdenesContadoDisponibles(sedeId, desde, hasta);
            
            // Obtener ABONOS disponibles (no órdenes) de créditos en el período
            List<Abono> abonosDisponibles = abonoService.obtenerAbonosDisponiblesParaEntrega(sedeId, desde, hasta);
            
            System.out.println("🔍 DEBUG: Encontradas " + ordenesContado.size() + " órdenes a contado");
            System.out.println("🔍 DEBUG: Encontrados " + abonosDisponibles.size() + " abonos disponibles");
            
            return ResponseEntity.ok(Map.of(
                "ordenesContado", ordenesContado.stream()
                    .map(this::convertirAOrdenParaEntregaDTO)
                    .collect(Collectors.toList()),
                "abonosDisponibles", abonosDisponibles.stream()
                    .map(this::convertirAAbonoParaEntregaDTO)
                    .collect(Collectors.toList()),
                "totales", Map.of(
                    "contado", ordenesContado.size(),
                    "credito", abonosDisponibles.size(),
                    "total", ordenesContado.size() + abonosDisponibles.size()
                )
            ));
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }
    
    /**
     * Convierte una Orden a OrdenParaEntregaDTO
     */
    private OrdenParaEntregaDTO convertirAOrdenParaEntregaDTO(Orden orden) {
        OrdenParaEntregaDTO dto = new OrdenParaEntregaDTO();
        dto.setId(orden.getId());
        dto.setNumero(orden.getNumero());
        dto.setFecha(orden.getFecha());
        dto.setClienteNombre(orden.getCliente() != null ? orden.getCliente().getNombre() : null);
        dto.setClienteNit(orden.getCliente() != null ? orden.getCliente().getNit() : null);
        dto.setTotal(orden.getTotal());
        dto.setObra(orden.getObra());
        dto.setDescripcion(orden.getDescripcion());
        dto.setSedeNombre(orden.getSede() != null ? orden.getSede().getNombre() : null);
        dto.setTrabajadorNombre(orden.getTrabajador() != null ? orden.getTrabajador().getNombre() : null);
        
        // 💰 MONTOS POR MÉTODO DE PAGO (solo para órdenes de contado)
        dto.setMontoEfectivo(orden.getMontoEfectivo());
        dto.setMontoTransferencia(orden.getMontoTransferencia());
        dto.setMontoCheque(orden.getMontoCheque());
        
        dto.setYaEntregada(orden.isIncluidaEntrega());
        dto.setEsContado(!orden.isCredito());
        dto.setEstado(orden.getEstado().name());
        dto.setVenta(orden.isVenta()); // ✅ Campo venta agregado
        
        return dto;
    }
    
    /**
     * Convierte un Abono a AbonoParaEntregaDTO
     */
    private AbonoParaEntregaDTO convertirAAbonoParaEntregaDTO(Abono abono) {
        AbonoParaEntregaDTO dto = new AbonoParaEntregaDTO();
        dto.setId(abono.getId());
        dto.setFechaAbono(abono.getFecha());
        dto.setMontoAbono(abono.getTotal());
        dto.setMetodoPago(abono.getMetodoPago());
        dto.setFactura(abono.getFactura());
        
        // 💰 MONTOS POR MÉTODO DE PAGO
        dto.setMontoEfectivo(abono.getMontoEfectivo());
        dto.setMontoTransferencia(abono.getMontoTransferencia());
        dto.setMontoCheque(abono.getMontoCheque());
        dto.setMontoRetencion(abono.getMontoRetencion());
        
        // Información de la orden
        if (abono.getOrden() != null) {
            dto.setOrdenId(abono.getOrden().getId());
            dto.setNumeroOrden(abono.getNumeroOrden() != null ? abono.getNumeroOrden() : abono.getOrden().getNumero());
            dto.setFechaOrden(abono.getOrden().getFecha());
            dto.setMontoOrden(abono.getOrden().getTotal());
            dto.setObra(abono.getOrden().getObra());
            dto.setSedeNombre(abono.getOrden().getSede() != null ? abono.getOrden().getSede().getNombre() : null);
            dto.setTrabajadorNombre(abono.getOrden().getTrabajador() != null ? abono.getOrden().getTrabajador().getNombre() : null);
            dto.setYaEntregado(abono.getOrden().isIncluidaEntrega());
            dto.setEstadoOrden(abono.getOrden().getEstado() != null ? abono.getOrden().getEstado().name() : null);
            dto.setVentaOrden(abono.getOrden().isVenta()); // ✅ Campo ventaOrden agregado
        }
        
        // Información del cliente
        if (abono.getCliente() != null) {
            dto.setClienteNombre(abono.getCliente().getNombre());
            dto.setClienteNit(abono.getCliente().getNit());
        }
        
        return dto;
    }

    /**

    /**
     * ✔️ VALIDAR SI ENTREGA ESTÁ COMPLETA
     */
    @GetMapping("/{id}/validar")
    public ResponseEntity<Boolean> validarCompleta(@PathVariable Long id) {
        try {
            Boolean esCompleta = service.validarEntregaCompleta(id);
            return ResponseEntity.ok(esCompleta);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 💵 OBTENER TOTAL ENTREGADO POR SEDE EN PERÍODO
     */
    @GetMapping("/sede/{sedeId}/total-entregado")
    public ResponseEntity<Double> obtenerTotalEntregado(@PathVariable Long sedeId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Double total = service.obtenerTotalEntregadoPorSedeEnPeriodo(sedeId, desde, hasta);
        return ResponseEntity.ok(total);
    }

}