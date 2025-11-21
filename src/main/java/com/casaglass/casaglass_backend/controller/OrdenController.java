package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.dto.OrdenTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaDTO;
import com.casaglass.casaglass_backend.dto.OrdenDetalleDTO;
import com.casaglass.casaglass_backend.dto.FacturaCreateDTO;
import com.casaglass.casaglass_backend.service.OrdenService;
import com.casaglass.casaglass_backend.service.FacturaService;
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
            System.out.println("🔄 DEBUG: Actualizando orden de venta ID: " + id);
            System.out.println("🔄 DEBUG: Datos recibidos: " + ventaDTO);
            
            // 🔪 LOGGING DETALLADO PARA CORTES
            System.out.println("🔪 ===== ANÁLISIS DE CORTES EN ACTUALIZACIÓN =====");
            System.out.println("🔪 ventaDTO.getCortes() es null? " + (ventaDTO.getCortes() == null));
            if (ventaDTO.getCortes() != null) {
                System.out.println("🔪 Cantidad de cortes: " + ventaDTO.getCortes().size());
                System.out.println("🔪 Lista vacía? " + ventaDTO.getCortes().isEmpty());
            }
            
            if (ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
                System.out.println("🔪 ✅ CORTES ENCONTRADOS EN ACTUALIZACIÓN - Procesando...");
                for (int i = 0; i < ventaDTO.getCortes().size(); i++) {
                    OrdenVentaDTO.CorteSolicitadoDTO corte = ventaDTO.getCortes().get(i);
                    System.out.println("🔪 Corte " + i + ": " + corte.toString());
                    System.out.println("🔪   - ProductoId: " + corte.getProductoId());
                    System.out.println("🔪   - Medida solicitada: " + corte.getMedidaSolicitada());
                    System.out.println("🔪   - Cantidad: " + corte.getCantidad());
                    System.out.println("🔪   - Precio solicitado: " + corte.getPrecioUnitarioSolicitado());
                    System.out.println("🔪   - Precio sobrante: " + corte.getPrecioUnitarioSobrante());
                }
            } else {
                System.out.println("⚠️ ❌ NO SE RECIBIERON CORTES EN LA ACTUALIZACIÓN");
            }
            System.out.println("🔪 ================================================");
            
            // Actualizar orden (con o sin crédito según el flag)
            Orden ordenActualizada;
            if (ventaDTO.isCredito()) {
                ordenActualizada = service.actualizarOrdenVentaConCredito(id, ventaDTO);
            } else {
                ordenActualizada = service.actualizarOrdenVenta(id, ventaDTO);
            }
            
            System.out.println("🔄 DEBUG: Orden actualizada exitosamente: " + ordenActualizada.getId());
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Orden de venta actualizada exitosamente",
                "orden", ordenActualizada,
                "numero", ordenActualizada.getNumero()
            ));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ ERROR VALIDACION: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION",
                "codigo", "STOCK_INSUFICIENTE"
            ));
        } catch (RuntimeException e) {
            System.err.println("❌ ERROR CONCURRENCIA: " + e.getMessage());
            return ResponseEntity.status(409).body(Map.of(
                "error", e.getMessage(),
                "tipo", "CONCURRENCIA",
                "codigo", "CONFLICTO_STOCK",
                "mensaje", "Conflicto de concurrencia. Por favor, intente nuevamente."
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
    @PostMapping("/venta")
    public ResponseEntity<?> crearOrdenVenta(@RequestBody OrdenVentaDTO ventaDTO) {
        try {
            System.out.println("🔍 DEBUG: Iniciando creación de orden venta");
            System.out.println("🔍 DEBUG: Datos recibidos: " + ventaDTO);
            
            // 🔪 LOGGING DETALLADO PARA CORTES
            System.out.println("🔪 ===== ANÁLISIS DE CORTES =====");
            System.out.println("🔪 ventaDTO.getCortes() es null? " + (ventaDTO.getCortes() == null));
            if (ventaDTO.getCortes() != null) {
                System.out.println("🔪 Cantidad de cortes: " + ventaDTO.getCortes().size());
                System.out.println("🔪 Lista vacía? " + ventaDTO.getCortes().isEmpty());
            }
            
            if (ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
                System.out.println("🔪 ✅ CORTES ENCONTRADOS - Procesando...");
                for (int i = 0; i < ventaDTO.getCortes().size(); i++) {
                    OrdenVentaDTO.CorteSolicitadoDTO corte = ventaDTO.getCortes().get(i);
                    System.out.println("🔪 Corte " + i + ": " + corte.toString());
                    System.out.println("🔪   - ProductoId: " + corte.getProductoId());
                    System.out.println("🔪   - Medida solicitada: " + corte.getMedidaSolicitada());
                    System.out.println("🔪   - Cantidad: " + corte.getCantidad());
                    System.out.println("🔪   - Precio solicitado: " + corte.getPrecioUnitarioSolicitado());
                    System.out.println("🔪   - Precio sobrante: " + corte.getPrecioUnitarioSobrante());
                }
            } else {
                System.out.println("⚠️ ❌ NO SE RECIBIERON CORTES EN EL PAYLOAD");
                System.out.println("⚠️ Esto puede indicar que el frontend no está enviando los cortes correctamente");
            }
            System.out.println("🔪 ================================");
            
            // Crear orden (con o sin crédito según el flag)
            Orden ordenCreada;
            if (ventaDTO.isCredito()) {
                ordenCreada = service.crearOrdenVentaConCredito(ventaDTO);
            } else {
                ordenCreada = service.crearOrdenVenta(ventaDTO);
            }
            
            System.out.println("🔍 DEBUG: Orden creada exitosamente: " + ordenCreada.getId());
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Orden de venta creada exitosamente",
                "orden", ordenCreada,
                "numero", ordenCreada.getNumero()
            ));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ ERROR VALIDACION: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "tipo", "VALIDACION",
                "codigo", "STOCK_INSUFICIENTE"
            ));
        } catch (RuntimeException e) {
            System.err.println("❌ ERROR CONCURRENCIA: " + e.getMessage());
            return ResponseEntity.status(409).body(Map.of(
                "error", e.getMessage(),
                "tipo", "CONCURRENCIA",
                "codigo", "CONFLICTO_STOCK",
                "mensaje", "Conflicto de concurrencia. Por favor, intente nuevamente."
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

    /** Listado simple con atajos */
    @GetMapping
    public List<Orden> listar(@RequestParam(required = false) Long clienteId,
                              @RequestParam(required = false) Long sedeId,
                              @RequestParam(required = false) Long trabajadorId,
                              @RequestParam(required = false) Boolean venta,
                              @RequestParam(required = false) Boolean credito) {
        // Filtros combinados con sede y trabajador
        if (clienteId != null && sedeId != null) return service.listarPorClienteYSede(clienteId, sedeId);
        if (sedeId != null && trabajadorId != null) return service.listarPorSedeYTrabajador(sedeId, trabajadorId);
        if (sedeId != null && venta != null)     return service.listarPorSedeYVenta(sedeId, venta);
        if (sedeId != null && credito != null)   return service.listarPorSedeYCredito(sedeId, credito);
        if (trabajadorId != null && venta != null) return service.listarPorTrabajadorYVenta(trabajadorId, venta);
        
        // Filtros individuales
        if (sedeId != null)       return service.listarPorSede(sedeId);
        if (clienteId != null)    return service.listarPorCliente(clienteId);
        if (trabajadorId != null) return service.listarPorTrabajador(trabajadorId);
        if (venta != null)        return service.listarPorVenta(venta);
        if (credito != null)      return service.listarPorCredito(credito);
        
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orden> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(orden -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(orden))
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
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<OrdenDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(orden -> ResponseEntity.ok(new OrdenDetalleDTO(orden)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<Orden> obtenerPorNumero(@PathVariable Long numero) {
        return service.obtenerPorNumero(numero).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public List<Orden> listarPorCliente(@PathVariable Long clienteId) {
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

    // 🎯 ================================
    // 🎯 ENDPOINTS OPTIMIZADOS PARA TABLA
    // 🎯 ================================

    /**
     * 🚀 LISTADO OPTIMIZADO PARA TABLA DE ÓRDENES
     * Retorna solo campos esenciales para mejorar rendimiento
     * - Cliente: solo nombre
     * - Trabajador: solo nombre  
     * - Sede: solo nombre
     * - Items: todos los campos + producto simplificado (código, nombre)
     */
    @GetMapping("/tabla")
    public List<OrdenTablaDTO> listarParaTabla(@RequestParam(required = false) Long clienteId,
                                               @RequestParam(required = false) Long sedeId,
                                               @RequestParam(required = false) Long trabajadorId) {
        // Filtros específicos para tabla optimizada
        if (sedeId != null)       return service.listarPorSedeParaTabla(sedeId);
        if (clienteId != null)    return service.listarPorClienteParaTabla(clienteId);
        if (trabajadorId != null) return service.listarPorTrabajadorParaTabla(trabajadorId);
        
        return service.listarParaTabla();
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
            OrdenTablaDTO ordenActualizada = service.actualizarOrden(id, ordenDTO);
            return ResponseEntity.ok(ordenActualizada);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Orden no encontrada", "message", e.getMessage()));
        } catch (Exception e) {
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
                            facturaDTO.setDescuentos(orden.getDescuentos() != null ? orden.getDescuentos() : 0.0);
                            facturaDTO.setIva(0.0);
                            facturaDTO.setRetencionFuente(0.0);
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
