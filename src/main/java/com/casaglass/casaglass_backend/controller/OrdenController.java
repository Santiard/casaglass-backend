package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.dto.OrdenTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaDTO;
import com.casaglass.casaglass_backend.service.OrdenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordenes")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) { this.service = service; }

    /**
     * 🛒 CREAR ORDEN DE VENTA
     * Endpoint optimizado para realizar ventas reales desde el frontend
     */
    @PostMapping("/venta")
    public ResponseEntity<?> crearOrdenVenta(@RequestBody OrdenVentaDTO ventaDTO) {
        try {
            Orden ordenCreada = service.crearOrdenVenta(ventaDTO);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Orden de venta creada exitosamente",
                "orden", ordenCreada,
                "numero", ordenCreada.getNumero()
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
        return service.obtenerPorId(id).map(ResponseEntity::ok)
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

}
