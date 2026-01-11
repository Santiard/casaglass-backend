package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Proveedor;
import com.casaglass.casaglass_backend.service.ProveedorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
// CORS configurado globalmente en CorsConfig.java
public class ProveedorController {

    private static final Logger log = LoggerFactory.getLogger(ProveedorController.class);

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    /**
     * 📋 LISTADO DE PROVEEDORES CON FILTROS COMPLETOS
     * GET /api/proveedores
     * 
     * Filtros disponibles (todos opcionales):
     * - nombre: Búsqueda parcial por nombre (case-insensitive)
     * - nit: Búsqueda parcial por NIT (case-insensitive)
     * - ciudad: Búsqueda parcial por ciudad (case-insensitive)
     * - correo: No implementado (el modelo no tiene campo correo)
     * - activo: No implementado (el modelo no tiene campo activo)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 50, máximo: 200)
     * - sortBy: Campo para ordenar (nombre, nit) - default: nombre
     * - sortOrder: ASC o DESC - default: ASC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<Proveedor> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> listarProveedores(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nit,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        try {
            // Si no hay filtros nuevos, usar método original (compatibilidad)
            if (nombre == null && nit == null && correo == null && ciudad == null && 
                activo == null && page == null && size == null && sortBy == null && sortOrder == null) {
                return ResponseEntity.ok(proveedorService.listarProveedores());
            }
            
            // Usar método con filtros (correo y activo se ignoran)
            Object resultado = proveedorService.listarProveedoresConFiltros(
                nombre, nit, correo, ciudad, activo, page, size, sortBy, sortOrder
            );
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proveedor> obtenerProveedor(@PathVariable Long id) {
        return proveedorService.obtenerProveedorPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nit/{nit}")
    public ResponseEntity<Proveedor> obtenerProveedorPorNit(@PathVariable String nit) {
        return proveedorService.obtenerProveedorPorNit(nit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Proveedor guardarProveedor(@RequestBody Proveedor proveedor) {
        return proveedorService.guardarProveedor(proveedor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proveedor> actualizarProveedor(@PathVariable Long id,
                                                         @RequestBody Proveedor proveedor) {
        try {
            return ResponseEntity.ok(proveedorService.actualizarProveedor(id, proveedor));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable Long id) {
        proveedorService.eliminarProveedor(id);
        return ResponseEntity.noContent().build();
    }
}