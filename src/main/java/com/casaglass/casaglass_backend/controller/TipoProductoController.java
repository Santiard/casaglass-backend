package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.TipoProducto;
import com.casaglass.casaglass_backend.model.ColorProducto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tipos")
@CrossOrigin(origins = "*")
public class TipoProductoController {

    /**
     * GET /api/tipos
     * Obtiene todos los tipos de producto disponibles
     */
    @GetMapping
    public ResponseEntity<List<String>> obtenerTiposDisponibles() {
        List<String> tipos = Arrays.stream(TipoProducto.values())
            .map(Enum::name)
            .collect(Collectors.toList());
        return ResponseEntity.ok(tipos);
    }

    /**
     * GET /api/tipos/descripcion
     * Obtiene los tipos con una descripción más amigable
     */
    @GetMapping("/descripcion")
    public ResponseEntity<List<TipoInfo>> obtenerTiposConDescripcion() {
        List<TipoInfo> tipos = Arrays.stream(TipoProducto.values())
            .map(tipo -> new TipoInfo(tipo.name(), obtenerDescripcion(tipo)))
            .collect(Collectors.toList());
        return ResponseEntity.ok(tipos);
    }

    /**
     * GET /api/tipos/colores
     * Obtiene todos los colores disponibles
     */
    @GetMapping("/colores")
    public ResponseEntity<List<String>> obtenerColoresDisponibles() {
        List<String> colores = Arrays.stream(ColorProducto.values())
            .map(Enum::name)
            .collect(Collectors.toList());
        return ResponseEntity.ok(colores);
    }

    /**
     * GET /api/tipos/colores/descripcion
     * Obtiene los colores con descripción amigable
     */
    @GetMapping("/colores/descripcion")
    public ResponseEntity<List<ColorInfo>> obtenerColoresConDescripcion() {
        List<ColorInfo> colores = Arrays.stream(ColorProducto.values())
            .map(color -> new ColorInfo(color.name(), obtenerDescripcionColor(color)))
            .collect(Collectors.toList());
        return ResponseEntity.ok(colores);
    }

    private String obtenerDescripcion(TipoProducto tipo) {
        return switch (tipo) {
            case MATE -> "Mate";
            case BLANCO -> "Blanco";
            case NEGRO -> "Negro";
            case BRONCE -> "Bronce";
            case NATURAL -> "Natural";
            case NA -> "N/A";
        };
    }

    private String obtenerDescripcionColor(ColorProducto color) {
        return switch (color) {
            case MATE -> "Mate";
            case BLANCO -> "Blanco";
            case NEGRO -> "Negro";
            case BRONCE -> "Bronce";
            case NATURAL -> "Natural";
            case NA -> "N/A";
        };
    }

    // Clase interna para el response con descripción
    public static class TipoInfo {
        public String valor;
        public String descripcion;

        public TipoInfo(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }
    }

    // Clase interna para colores con descripción
    public static class ColorInfo {
        public String valor;
        public String descripcion;

        public ColorInfo(String valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }
    }
}