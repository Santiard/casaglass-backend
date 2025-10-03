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
@CrossOrigin(origins = "*") // Permite llamadas desde cualquier frontend
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<Cliente> listarClientes() {
        return clienteService.listarClientes();
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