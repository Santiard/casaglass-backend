package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Proveedor;
import com.casaglass.casaglass_backend.service.ProveedorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
// CORS configurado globalmente en CorsConfig.java
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public List<Proveedor> listarProveedores() {
        return proveedorService.listarProveedores();
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