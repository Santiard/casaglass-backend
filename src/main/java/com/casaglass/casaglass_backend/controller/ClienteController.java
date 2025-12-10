package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.service.ClienteService;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/clientes")
// CORS configurado globalmente en CorsConfig.java
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * 📋 LISTADO DE CLIENTES CON FILTROS COMPLETOS
     * GET /api/clientes
     * 
     * Filtros disponibles (todos opcionales):
     * - nombre: Búsqueda parcial por nombre (case-insensitive)
     * - nit: Búsqueda parcial por NIT (case-insensitive)
     * - correo: Búsqueda parcial por correo (case-insensitive)
     * - ciudad: Búsqueda parcial por ciudad (case-insensitive)
     * - activo: Boolean (no implementado actualmente, el modelo no tiene campo activo)
     * - conCredito: Boolean (true para clientes con crédito habilitado)
     * - page: Número de página (default: sin paginación, retorna lista completa)
     * - size: Tamaño de página (default: 50, máximo: 200)
     * - sortBy: Campo para ordenar (nombre, nit, ciudad) - default: nombre
     * - sortOrder: ASC o DESC - default: ASC
     * 
     * Respuesta:
     * - Si se proporcionan page y size: PageResponse con paginación
     * - Si no se proporcionan: List<Cliente> (compatibilidad hacia atrás)
     */
    @GetMapping
    public ResponseEntity<Object> listarClientes(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nit,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean conCredito,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        
        try {
            // Si no hay filtros nuevos, usar método original (compatibilidad)
            if (nombre == null && nit == null && correo == null && ciudad == null && 
                activo == null && conCredito == null && page == null && size == null && 
                sortBy == null && sortOrder == null) {
                return ResponseEntity.ok(clienteService.listarClientes());
            }
            
            // Usar método con filtros
            Object resultado = clienteService.listarClientesConFiltros(
                nombre, nit, correo, ciudad, activo, conCredito, page, size, sortBy, sortOrder
            );
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Long id) {
        return clienteService.obtenerClientePorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nit/{nit}")
    public ResponseEntity<Cliente> obtenerClientePorNit(@PathVariable String nit) {
        return clienteService.obtenerClientePorNit(nit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Cliente guardarCliente(@RequestBody Cliente cliente) {
        return clienteService.guardarCliente(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, @RequestBody Cliente cliente) {
        try {
            return ResponseEntity.ok(clienteService.actualizarCliente(id, cliente));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
  try {
    clienteService.eliminarCliente(id);
    return ResponseEntity.noContent().build();
  } catch (EmptyResultDataAccessException e) {
    return ResponseEntity.notFound().build();
  } catch (DataIntegrityViolationException e) {
    // FK en uso: no se puede eliminar
    return ResponseEntity.status(409).body(
      Map.of("message", "No se puede eliminar: el cliente tiene movimientos/órdenes/créditos asociados.")
    );
  } catch (RuntimeException e) {
    // cualquier otra cosa
    return ResponseEntity.status(500).body(
      Map.of("message", "Error interno al eliminar el cliente.")
    );
  }
}
}