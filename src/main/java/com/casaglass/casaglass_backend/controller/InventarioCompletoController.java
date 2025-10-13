package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ProductoInventarioCompletoDTO;
import com.casaglass.casaglass_backend.service.InventarioCompletoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario-completo")
@CrossOrigin(origins = "*") // Permitir solicitudes desde cualquier origen (frontend)
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
}