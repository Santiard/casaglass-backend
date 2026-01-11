// src/main/java/com/casaglass/casaglass_backend/controller/IngresoController.java
package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Ingreso;
import com.casaglass.casaglass_backend.service.IngresoService;
import com.casaglass.casaglass_backend.dto.IngresoCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ingresos")
// CORS configurado globalmente en CorsConfig.java
public class IngresoController {

    private static final Logger log = LoggerFactory.getLogger(IngresoController.class);

    private final IngresoService ingresoService;

    public IngresoController(IngresoService ingresoService) {
        this.ingresoService = ingresoService;
    }

    /**
     * 📋 LISTADO DE INGRESOS CON FILTROS COMPLETOS
     * GET /api/ingresos
     * 
     * Filtros disponibles (todos opcionales):
     * - proveedorId: Filtrar por proveedor
     * - fechaDesde: YYYY-MM-DD (fecha desde, inclusive)
     * - fechaHasta: YYYY-MM-DD (fecha hasta, inclusive)
     * - procesado: true para procesados, false para no procesados
     * - numeroFactura: Búsqueda parcial por número de factura (case-insensitive)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 20, máximo: 100)
     * - sortBy: Campo para ordenar (fecha, numeroFactura, totalCosto) - default: fecha
     * - sortOrder: ASC o DESC - default: DESC
     * 
     * Nota: El parámetro sedeId se mantiene por compatibilidad pero actualmente
     * los ingresos no tienen campo sede (todos se procesan en sede principal)
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<Ingreso> (compatibilidad hacia atrás)
     */
    @GetMapping
    public Object listarIngresos(
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Boolean procesado,
            @RequestParam(required = false) String numeroFactura,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        // Si solo hay sedeId y ningún otro filtro nuevo, usar método específico (compatibilidad)
        if (sedeId != null && proveedorId == null && fechaDesde == null && fechaHasta == null && 
            procesado == null && numeroFactura == null && page == null && size == null && 
            sortBy == null && sortOrder == null) {
            return ingresoService.listarIngresosPorSede(sedeId);
        }
        
        // Usar método con filtros completos
        return ingresoService.listarIngresosConFiltros(
            proveedorId, fechaDesde, fechaHasta, procesado, numeroFactura, page, size, sortBy, sortOrder
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ingreso> obtenerIngreso(@PathVariable Long id) {
        // IMPORTANTE: no mezclar tipos en las ramas (no pongas body("...") aquí)
        return ingresoService.obtenerIngresoPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proveedor/{proveedorId}")
    public List<Ingreso> obtenerIngresosPorProveedor(@PathVariable Long proveedorId) {
        return ingresoService.obtenerIngresosPorProveedor(proveedorId);
    }

    @GetMapping("/no-procesados")
    public List<Ingreso> obtenerIngresosNoProcesados() {
        return ingresoService.obtenerIngresosNoProcesados();
    }

    @GetMapping("/por-fecha")
    public List<Ingreso> obtenerIngresosPorFecha(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin) {
        return ingresoService.obtenerIngresosPorFecha(fechaInicio, fechaFin);
    }

    @PostMapping
    public ResponseEntity<?> crearIngreso(@RequestBody IngresoCreateDTO ingresoDTO) {
        try {
            System.out.println("🔄 POST /api/ingresos - Creando ingreso desde DTO");
            System.out.println("📥 Datos recibidos - Proveedor ID: " + 
                (ingresoDTO.getProveedor() != null ? ingresoDTO.getProveedor().getId() : "null"));
            System.out.println("📥 Detalles: " + 
                (ingresoDTO.getDetalles() != null ? ingresoDTO.getDetalles().size() : 0) + " items");
            
            Ingreso resultado = ingresoService.crearIngresoDesdeDTO(ingresoDTO);
            
            System.out.println("✅ Ingreso creado exitosamente - ID: " + resultado.getId());
            
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ Error de ejecución: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inesperado: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarIngreso(@PathVariable Long id, @RequestBody Ingreso ingreso) {
        try {
            System.out.println("🔄 PUT /api/ingresos/" + id + " - Iniciando actualización");
            System.out.println("📥 Datos recibidos: " + ingreso.getNumeroFactura());
            
            Ingreso resultado = ingresoService.actualizarIngreso(id, ingreso);
            
            System.out.println("✅ Actualización exitosa - ID: " + resultado.getId());
            
            // 🔧 ARREGLO: Recargar la entidad para evitar problemas de serialización
            Ingreso ingresoLimpio = ingresoService.obtenerIngresoPorId(id)
                    .orElseThrow(() -> new RuntimeException("Error al recargar ingreso"));
            
            return ResponseEntity.ok(ingresoLimpio);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ Error de ejecución: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error inesperado: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarIngreso(@PathVariable Long id) {
        try {
            ingresoService.eliminarIngreso(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/procesar")
    public ResponseEntity<?> procesarInventario(@PathVariable Long id) {
        try {
            Ingreso ingreso = ingresoService.obtenerIngresoPorId(id)
                    .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));
            ingresoService.procesarInventario(ingreso);
            return ResponseEntity.ok("Inventario procesado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reprocesar")
    public ResponseEntity<?> reprocesarInventario(@PathVariable Long id) {
        try {
            ingresoService.reprocesarInventario(id);
            return ResponseEntity.ok("Inventario reprocesado correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/marcar-procesado")
    public ResponseEntity<?> marcarComoProcesado(@PathVariable Long id) {
        try {
            Ingreso ingreso = ingresoService.marcarComoProcesado(id);
            return ResponseEntity.ok(ingreso);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
