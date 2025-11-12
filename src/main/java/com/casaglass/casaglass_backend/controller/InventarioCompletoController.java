package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ProductoInventarioCompletoDTO;
import com.casaglass.casaglass_backend.service.InventarioCompletoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario-completo")
// CORS configurado globalmente en CorsConfig.java
public class InventarioCompletoController {

    private final InventarioCompletoService inventarioCompletoService;

    public InventarioCompletoController(InventarioCompletoService inventarioCompletoService) {
        this.inventarioCompletoService = inventarioCompletoService;
    }

    /**
     * GET /api/inventario-completo
     * Obtiene todos los productos con su información completa de inventario
     */
    @GetMapping
    public ResponseEntity<List<ProductoInventarioCompletoDTO>> obtenerInventarioCompleto() {
        List<ProductoInventarioCompletoDTO> inventario = inventarioCompletoService.obtenerInventarioCompleto();
        return ResponseEntity.ok(inventario);
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
}