package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.CatalogoProductosTrasladoResponseDTO;
import com.casaglass.casaglass_backend.dto.TrasladoResponseDTO;
import com.casaglass.casaglass_backend.exception.InventarioInsuficienteException;
import com.casaglass.casaglass_backend.model.Traslado;
import com.casaglass.casaglass_backend.service.TrasladoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p><strong>Confirmar traslado:</strong> use {@code POST /api/traslados/{id}/confirmar?trabajadorId=…}
 * (este controlador) o, con el mismo efecto, {@code PUT /api/traslados-movimientos/{id}/confirmar}
 * con cuerpo {@code {"trabajadorId": …}}. Ambos delegan en {@code TrasladoService#confirmarLlegada}.</p>
 * <p>Catálogo: {@code GET /api/traslados/catalogo-productos} lista inventario de <em>producto</em> en sede origen;
 * las líneas con <em>cortes</em> (Insula↔2/3) suelen alimentarse también desde el front con los endpoints
 * de inventario de cortes (p. ej. inventario-cortes / cortes) según el diseño de pantalla.</p>
 */
@RestController
@RequestMapping("/api/traslados")
@Tag(name = "Traslados (cabecera)", description = "Alta, listado, catálogo y confirmación vía query param")
// CORS configurado globalmente en CorsConfig.java
public class TrasladoController {

    private final TrasladoService service;

    public TrasladoController(TrasladoService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Crear traslado (cabecera + detalles anidados)", description = "Cada detalle: ver esquema TrasladoDetalle. Errores de stock: 409 con error INVENTARIO_INSUFICIENTE (ApiExceptionHandler).")
    public ResponseEntity<?> crear(@RequestBody Traslado traslado) {
        try {
            return ResponseEntity.ok(service.crear(traslado));
        } catch (InventarioInsuficienteException e) {
            throw e; // 409 + JSON: ApiExceptionHandler
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<Traslado> listar(@RequestParam(required = false) Long sedeOrigenId,
                                 @RequestParam(required = false) Long sedeDestinoId) {
        if (sedeOrigenId != null && sedeDestinoId != null) {
            // combinación simple
            return service.listarPorSedeOrigen(sedeOrigenId).stream()
                    .filter(t -> t.getSedeDestino().getId().equals(sedeDestinoId))
                    .toList();
        }
        if (sedeOrigenId != null) return service.listarPorSedeOrigen(sedeOrigenId);
        if (sedeDestinoId != null) return service.listarPorSedeDestino(sedeDestinoId);
        return service.listar();
    }

    @GetMapping("/fecha/{fecha}")
    public List<Traslado> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listarPorFecha(fecha);
    }

    @GetMapping("/fecha")
    public List<Traslado> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorRango(desde, hasta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener traslado por id", description = "Detalles incluyen productoInventarioADescontarSede1Id y productoInventarioADescontarSede1 (objeto) cuando aplica 1→2/3 con corte.")
    public ResponseEntity<TrasladoResponseDTO> obtener(@PathVariable Long id) {
        return service.obtener(id)
                .map(traslado -> ResponseEntity.ok(new TrasladoResponseDTO(traslado)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/catalogo-productos")
    @Operation(summary = "Catálogo de producto (inventario normal) en sede origen", description = "Productos y cantidades en inventario estándar; no incluye listado de cortes — combinar con APIs de corte/vidrio en el front si aplica el flujo con pedazos.")
    public ResponseEntity<?> obtenerCatalogoProductos(
            @RequestParam(required = false) Long sedeOrigenId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long trabajadorId) {
        try {
            CatalogoProductosTrasladoResponseDTO response = service.obtenerCatalogoParaTraslado(
                    sedeOrigenId,
                    q,
                    categoriaId,
                    color,
                    page,
                    size,
                    trabajadorId
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCabecera(@PathVariable Long id, @RequestBody Traslado traslado) {
        try {
            return ResponseEntity.ok(service.actualizarCabecera(id, traslado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar recepción del traslado (query param)", description = "Equivale a PUT /api/traslados-movimientos/{id}/confirmar con body { \"trabajadorId\" }. Misma lógica de negocio.")
    public ResponseEntity<?> confirmar(
            @PathVariable Long id,
            @Parameter(description = "Trabajador que confirma en destino", required = true) @RequestParam Long trabajadorId) {
        try {
            return ResponseEntity.ok(service.confirmarLlegada(id, trabajadorId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
