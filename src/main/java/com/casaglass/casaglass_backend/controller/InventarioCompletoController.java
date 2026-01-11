package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ProductoInventarioCompletoDTO;
import com.casaglass.casaglass_backend.service.InventarioCompletoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/inventario-completo")
// CORS configurado globalmente en CorsConfig.java
public class InventarioCompletoController {

    private static final Logger log = LoggerFactory.getLogger(InventarioCompletoController.class);

    private final InventarioCompletoService inventarioCompletoService;

    public InventarioCompletoController(InventarioCompletoService inventarioCompletoService) {
        this.inventarioCompletoService = inventarioCompletoService;
    }

    /**
     * 📋 LISTADO DE INVENTARIO COMPLETO CON FILTROS COMPLETOS
     * GET /api/inventario-completo
     * 
     * Filtros disponibles (todos opcionales):
     * - categoriaId: Filtrar por ID de categoría
     * - categoria: Filtrar por nombre de categoría (búsqueda parcial)
     * - tipo: Filtrar por tipo (enum TipoProducto)
     * - color: Filtrar por color (enum ColorProducto)
     * - codigo: Búsqueda parcial por código (case-insensitive)
     * - nombre: Búsqueda parcial por nombre (case-insensitive)
     * - sedeId: Filtrar por sede para verificar stock
     * - conStock: Boolean (true para productos con stock > 0, requiere sedeId)
     * - sinStock: Boolean (true para productos sin stock, requiere sedeId)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 100, máximo: 500)
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<ProductoInventarioCompletoDTO> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> obtenerInventarioCompleto(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Boolean conStock,
            @RequestParam(required = false) Boolean sinStock,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        try {
            // Validar conStock/sinStock requiere sedeId
            if ((conStock != null && conStock || sinStock != null && sinStock) && sedeId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "El parámetro sedeId es obligatorio cuando conStock=true o sinStock=true"
                ));
            }
            
            // Si no hay filtros nuevos, usar método original (compatibilidad)
            if (categoriaId == null && categoria == null && tipo == null && color == null && 
                codigo == null && nombre == null && sedeId == null && conStock == null && 
                sinStock == null && page == null && size == null) {
                return ResponseEntity.ok(inventarioCompletoService.obtenerInventarioCompleto());
            }
            
            // Usar método con filtros
            Object resultado = inventarioCompletoService.obtenerInventarioCompletoConFiltros(
                categoriaId, categoria, tipo, color, codigo, nombre, sedeId, conStock, sinStock, page, size
            );
            
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * GET /api/inventario-completo/sede/{sedeId}
     * Obtiene productos de una sede específica con información completa
     */
    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> obtenerInventarioPorSede(
            @PathVariable Long sedeId) {
        List<ProductoInventarioCompletoDTO> inventario = 
            inventarioCompletoService.obtenerInventarioCompletoPorSede(sedeId);
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/inventario-completo/categoria/{categoriaId}
     * Obtiene productos de una categoría específica con información completa
     */
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> obtenerInventarioPorCategoria(
            @PathVariable Long categoriaId) {
        List<ProductoInventarioCompletoDTO> inventario = 
            inventarioCompletoService.obtenerInventarioCompletoPorCategoria(categoriaId);
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/inventario-completo/buscar?q={query}
     * Busca productos por nombre o código con información completa
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> buscarInventario(
            @RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<ProductoInventarioCompletoDTO> inventario = 
            inventarioCompletoService.buscarInventarioCompleto(query.trim());
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/inventario-completo/tipo/{tipo}
     * Obtiene productos de un tipo específico con información completa
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> obtenerInventarioPorTipo(
            @PathVariable String tipo) {
        try {
            List<ProductoInventarioCompletoDTO> inventario = 
                inventarioCompletoService.obtenerInventarioCompletoPorTipo(tipo.toUpperCase());
            return ResponseEntity.ok(inventario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/inventario-completo/color/{color}
     * Obtiene productos de un color específico con información completa
     */
    @GetMapping("/color/{color}")
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> obtenerInventarioPorColor(
            @PathVariable String color) {
        try {
            List<ProductoInventarioCompletoDTO> inventario = 
                inventarioCompletoService.obtenerInventarioCompletoPorColor(color.toUpperCase());
            return ResponseEntity.ok(inventario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 🪟 GET /api/inventario-completo/vidrios
     * Obtiene todos los productos vidrio con su información completa de inventario
     * Endpoint exclusivo para productos vidrio
     */
    @GetMapping("/vidrios")
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> obtenerInventarioVidrios() {
        List<ProductoInventarioCompletoDTO> inventario = 
            inventarioCompletoService.obtenerInventarioCompletoVidrios();
        return ResponseEntity.ok(inventario);
    }
}