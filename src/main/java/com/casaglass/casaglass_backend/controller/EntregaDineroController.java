package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.EntregaDineroCreateDTO;
import com.casaglass.casaglass_backend.dto.EntregaDineroResponseDTO;
import com.casaglass.casaglass_backend.dto.OrdenParaEntregaDTO;
import com.casaglass.casaglass_backend.model.EntregaDinero;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.service.EntregaDineroService;
import com.casaglass.casaglass_backend.service.EntregaDetalleService;
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
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class EntregaDineroController {

    private final EntregaDineroService service;

    @Autowired
    private EntregaDetalleService entregaDetalleService;

    public EntregaDineroController(EntregaDineroService service) {
        this.service = service;
    }

    /* ========== CONSULTAS ========== */

    /**
     * 📋 LISTAR TODAS LAS ENTREGAS
     */
    @GetMapping
    @Transactional(readOnly = true)
    public List<EntregaDineroResponseDTO> listar(@RequestParam(required = false) Long sedeId,
                                                @RequestParam(required = false) Long empleadoId,
                                                @RequestParam(required = false) String estado,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        
        List<EntregaDinero> entregas;
        
        // Aplicar filtros según parámetros
        if (sedeId != null && estado != null) {
            entregas = service.obtenerPorSedeYEstado(sedeId, EntregaDinero.EstadoEntrega.valueOf(estado));
        } else if (sedeId != null && desde != null && hasta != null) {
            entregas = service.obtenerPorSedeYPeriodo(sedeId, desde, hasta);
        } else if (desde != null && hasta != null) {
            entregas = service.obtenerPorPeriodo(desde, hasta);
        } else if (sedeId != null) {
            entregas = service.obtenerPorSede(sedeId);
        } else if (empleadoId != null) {
            entregas = service.obtenerPorEmpleado(empleadoId);
        } else if (estado != null) {
            entregas = service.obtenerPorEstado(EntregaDinero.EstadoEntrega.valueOf(estado));
        } else {
            entregas = service.obtenerTodas();
        }
        
        return entregas.stream()
                .map(EntregaDineroResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 OBTENER ENTREGA POR ID
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<EntregaDineroResponseDTO> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(entrega -> ResponseEntity.ok(new EntregaDineroResponseDTO(entrega)))
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
     * 📊 ENTREGAS CON DIFERENCIAS (para auditoría)
     */
    @GetMapping("/con-diferencias")
    public List<EntregaDineroResponseDTO> listarConDiferencias() {
        return service.obtenerEntregasConDiferencias().stream()
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
            entrega.setFechaDesde(entregaDTO.getFechaDesde());
            entrega.setFechaHasta(entregaDTO.getFechaHasta());
            
            // Configurar modalidad y otros campos
            entrega.setModalidadEntrega(EntregaDinero.ModalidadEntrega.valueOf(entregaDTO.getModalidadEntrega()));
            entrega.setObservaciones(entregaDTO.getObservaciones());
            entrega.setNumeroComprobante(entregaDTO.getNumeroComprobante());
            
            // Configurar montos (el servicio puede recalcular si es necesario)
            entrega.setMontoEsperado(entregaDTO.getMontoEsperado() != null ? entregaDTO.getMontoEsperado() : 0.0);
            entrega.setMontoGastos(entregaDTO.getMontoGastos() != null ? entregaDTO.getMontoGastos() : 0.0);
            entrega.setMontoEntregado(entregaDTO.getMontoEntregado() != null ? entregaDTO.getMontoEntregado() : 0.0);
            
            System.out.println("🔍 DEBUG: Entrega configurada: " + entrega);
            System.out.println("🔍 DEBUG: Órdenes a incluir: " + entregaDTO.getOrdenesIds());
            System.out.println("🔍 DEBUG: Gastos a incluir: " + (entregaDTO.getGastos() != null ? entregaDTO.getGastos().size() : 0));
            
            // Preparar IDs de gastos (por ahora vacío ya que gastos está vacío en tu ejemplo)
            List<Long> gastosIds = null; // TODO: Implementar conversión de gastos si es necesario
            
            // Llamar al servicio para crear la entrega
            EntregaDinero entregaCreada = service.crearEntrega(
                entrega, 
                entregaDTO.getOrdenesIds(), 
                gastosIds
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
    public ResponseEntity<?> confirmar(@PathVariable Long id,
                                     @RequestParam Double montoEntregado,
                                     @RequestParam(required = false) String observaciones) {
        try {
            EntregaDinero entregaConfirmada = service.confirmarEntrega(id, montoEntregado, observaciones);
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
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                    @RequestParam String motivo) {
        try {
            EntregaDinero entregaCancelada = service.cancelarEntrega(id, motivo);
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
            entrega.setObservaciones(entregaDTO.getObservaciones());
            entrega.setMontoEntregado(entregaDTO.getMontoEntregado() != null ? entregaDTO.getMontoEntregado() : 0.0);
            
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
     * 📋 OBTENER ÓRDENES DISPONIBLES PARA ENTREGA
     * Solo muestra órdenes A CONTADO de un período que aún no han sido entregadas
     */
    @GetMapping("/ordenes-disponibles")
    public ResponseEntity<?> obtenerOrdenesDisponibles(@RequestParam Long sedeId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            System.out.println("🔍 DEBUG: Buscando órdenes disponibles para entrega");
            System.out.println("🔍 DEBUG: Sede " + sedeId + ", período " + desde + " a " + hasta);
            
            // Obtener órdenes A CONTADO disponibles
            List<Orden> ordenesContado = entregaDetalleService.obtenerOrdenesContadoDisponibles(sedeId, desde, hasta);
            
            // Obtener órdenes A CRÉDITO con abonos en el período
            List<Orden> ordenesConAbonos = entregaDetalleService.obtenerOrdenesConAbonosEnPeriodo(sedeId, desde, hasta);
            
            System.out.println("🔍 DEBUG: Encontradas " + ordenesContado.size() + " órdenes a contado");
            System.out.println("🔍 DEBUG: Encontradas " + ordenesConAbonos.size() + " órdenes con abonos");
            
            return ResponseEntity.ok(Map.of(
                "ordenesContado", ordenesContado.stream()
                    .map(this::convertirAOrdenParaEntregaDTO)
                    .collect(Collectors.toList()),
                "ordenesConAbonos", ordenesConAbonos.stream()
                    .map(this::convertirAOrdenParaEntregaDTO)
                    .collect(Collectors.toList()),
                "totales", Map.of(
                    "contado", ordenesContado.size(),
                    "credito", ordenesConAbonos.size(),
                    "total", ordenesContado.size() + ordenesConAbonos.size()
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
        dto.setSedeNombre(orden.getSede() != null ? orden.getSede().getNombre() : null);
        dto.setTrabajadorNombre(orden.getTrabajador() != null ? orden.getTrabajador().getNombre() : null);
        dto.setYaEntregada(orden.isIncluidaEntrega());
        dto.setEsContado(!orden.isCredito());
        dto.setEstado(orden.getEstado().name());
        
        return dto;
    }

    /**
     * 🧮 CALCULAR DIFERENCIA DE ENTREGA
     */
    @GetMapping("/{id}/diferencia")
    public ResponseEntity<Double> calcularDiferencia(@PathVariable Long id) {
        try {
            Double diferencia = service.calcularDiferenciaEntrega(id);
            return ResponseEntity.ok(diferencia);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

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

    /**
     * 💸 OBTENER TOTAL GASTOS POR SEDE EN PERÍODO
     */
    @GetMapping("/sede/{sedeId}/total-gastos")
    public ResponseEntity<Double> obtenerTotalGastos(@PathVariable Long sedeId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Double total = service.obtenerTotalGastosPorSedeEnPeriodo(sedeId, desde, hasta);
        return ResponseEntity.ok(total);
    }
}