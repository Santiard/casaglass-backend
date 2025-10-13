package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.CorteInventarioCompletoDTO;
import com.casaglass.casaglass_backend.service.CorteInventarioCompletoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cortes-inventario-completo")
@CrossOrigin(origins = "*") // Permitir solicitudes desde cualquier origen (frontend)
public class CorteInventarioCompletoController {

    private final CorteInventarioCompletoService corteInventarioCompletoService;

    public CorteInventarioCompletoController(CorteInventarioCompletoService corteInventarioCompletoService) {
        this.corteInventarioCompletoService = corteInventarioCompletoService;
    }

    /**
     * GET /api/cortes-inventario-completo
     * Obtiene todos los cortes con su información completa de inventario
     */
    @GetMapping
    public ResponseEntity<List<CorteInventarioCompletoDTO>> obtenerInventarioCompleto() {
        List<CorteInventarioCompletoDTO> inventario = corteInventarioCompletoService.obtenerInventarioCompleto();
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/cortes-inventario-completo/categoria/{categoriaId}
     * Obtiene cortes de una categoría específica con información completa
     */
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<CorteInventarioCompletoDTO>> obtenerInventarioPorCategoria(
            @PathVariable Long categoriaId) {
        List<CorteInventarioCompletoDTO> inventario = 
            corteInventarioCompletoService.obtenerInventarioCompletoPorCategoria(categoriaId);
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/cortes-inventario-completo/buscar?q={query}
     * Busca cortes por nombre o código con información completa
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<CorteInventarioCompletoDTO>> buscarInventario(
            @RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<CorteInventarioCompletoDTO> inventario = 
            corteInventarioCompletoService.buscarInventarioCompleto(query.trim());
        return ResponseEntity.ok(inventario);
    }

    /**
     * GET /api/cortes-inventario-completo/largo?min={largoMin}&max={largoMax}
     * Busca cortes por rango de largo con información completa
     */
    @GetMapping("/largo")
    public ResponseEntity<List<CorteInventarioCompletoDTO>> obtenerInventarioPorRangoLargo(
            @RequestParam("min") Double largoMin,
            @RequestParam("max") Double largoMax) {
        
        if (largoMin == null || largoMax == null || largoMin < 0 || largoMax < largoMin) {
            return ResponseEntity.badRequest().build();
        }
        
        List<CorteInventarioCompletoDTO> inventario = 
            corteInventarioCompletoService.obtenerInventarioCompletoPorRangoLargo(largoMin, largoMax);
        return ResponseEntity.ok(inventario);
    }
}