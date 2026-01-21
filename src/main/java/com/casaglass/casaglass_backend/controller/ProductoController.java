package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.ProductoActualizarDTO;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.TipoProducto;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.service.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
// CORS configurado globalmente en CorsConfig.java
public class ProductoController {

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    /**
     * 📋 LISTADO DE PRODUCTOS CON FILTROS COMPLETOS
     * GET /api/productos
     * 
     * Filtros disponibles (todos opcionales):
     * - categoriaId: Filtrar por ID de categoría
     * - categoria: Filtrar por nombre de categoría (búsqueda parcial)
     * - tipo: Filtrar por tipo (enum TipoProducto)
     * - color: Filtrar por color (enum ColorProducto)
     * - codigo: Búsqueda parcial por código (case-insensitive)
     * - nombre: Búsqueda parcial por nombre (case-insensitive)
     * - conStock: Boolean (true para productos con stock > 0, requiere sedeId)
     * - sedeId: Filtrar por sede para verificar stock (requerido si conStock=true)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 50, máximo: 200)
     * - sortBy: Campo para ordenar (codigo, nombre, categoria) - default: codigo
     * - sortOrder: ASC o DESC - default: ASC
     * 
     * Nota: El parámetro 'q' (query) sigue funcionando para compatibilidad hacia atrás
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<Producto> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean conStock,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false, name = "q") String query) {
        try {
            // Convertir tipo y color String a enum
            TipoProducto tipoEnum = null;
            if (tipo != null && !tipo.isEmpty()) {
                try {
                    tipoEnum = TipoProducto.valueOf(tipo.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Tipo inválido: " + tipo
                    ));
                }
            }
            
            ColorProducto colorEnum = null;
            if (color != null && !color.isEmpty()) {
                try {
                    colorEnum = ColorProducto.valueOf(color.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Color inválido: " + color
                    ));
                }
            }
            
            // Si solo hay query o categoriaId y ningún otro filtro nuevo, usar método original (compatibilidad)
            if (query != null && !query.isBlank() && categoria == null && tipoEnum == null && 
                colorEnum == null && codigo == null && nombre == null && conStock == null && 
                sedeId == null && page == null && size == null && sortBy == null && sortOrder == null) {
                return ResponseEntity.ok(service.buscar(query));
            }
            
            if (categoriaId != null && categoria == null && tipoEnum == null && 
                colorEnum == null && codigo == null && nombre == null && conStock == null && 
                sedeId == null && query == null && page == null && size == null && 
                sortBy == null && sortOrder == null) {
                return ResponseEntity.ok(service.listarPorCategoriaId(categoriaId));
            }
            
            // Validar conStock requiere sedeId
            if (conStock != null && conStock && sedeId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "El parámetro sedeId es obligatorio cuando conStock=true"
                ));
            }
            
            // Usar método con filtros
            Object resultado = service.listarProductosConFiltros(
                categoriaId, categoria, tipoEnum, colorEnum, codigo, nombre, conStock, sedeId,
                page, size, sortBy, sortOrder
            );
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Producto> obtenerPorCodigo(@PathVariable String codigo) {
        return service.obtenerPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Producto producto) {
        try {
            // ✅ Si es ProductoVidrio, NO debe crearse aquí, debe usar el endpoint /api/productos-vidrio
            if (producto instanceof com.casaglass.casaglass_backend.model.ProductoVidrio) {
                return ResponseEntity.badRequest().body("Los productos vidrio deben crearse en /api/productos-vidrio");
            }
            
            return ResponseEntity.ok(service.guardar(producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ProductoActualizarDTO productoDTO) {
        try {
            return ResponseEntity.ok(service.actualizar(id, productoDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 💰 ACTUALIZAR SOLO EL COSTO DE UN PRODUCTO
     * PUT /api/productos/{id}/costo
     * 
     * Body esperado:
     * {
     *   "costo": 15000.0
     * }
     */
    @PutMapping("/{id}/costo")
    public ResponseEntity<Producto> actualizarCostoProducto(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        try {
            Double nuevoCosto = request.get("costo");
            if (nuevoCosto == null || nuevoCosto < 0) {
                return ResponseEntity.badRequest().build();
            }
            
            Producto producto = service.actualizarCosto(id, nuevoCosto);
            return ResponseEntity.ok(producto);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias-texto")
    public List<String> categoriasTexto() {
        return service.listarCategoriasTexto();
    }
}
