package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.service.OrdenService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@CrossOrigin(origins = "*")
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) { this.service = service; }

    @PostMapping
    public Orden crear(@RequestBody Orden orden) {
        return service.crear(orden);
    }

    /** Listado simple con atajos */
    @GetMapping
    public List<Orden> listar(@RequestParam(required = false) Long clienteId,
                              @RequestParam(required = false) Boolean venta,
                              @RequestParam(required = false) Boolean credito) {
        if (clienteId != null) return service.listarPorCliente(clienteId);
        if (venta != null)     return service.listarPorVenta(venta);
        if (credito != null)   return service.listarPorCredito(credito);
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orden> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<Orden> obtenerPorNumero(@PathVariable Long numero) {
        return service.obtenerPorNumero(numero).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public List<Orden> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId);
    }

    @GetMapping("/fecha/{fecha}")
    public List<Orden> listarPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listarPorFecha(fecha);
    }

    @GetMapping("/fecha")
    public List<Orden> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarPorRangoFechas(desde, hasta);
    }

    @GetMapping("/venta/{venta}")
    public List<Orden> listarPorVenta(@PathVariable boolean venta) {
        return service.listarPorVenta(venta);
    }

    @GetMapping("/credito/{credito}")
    public List<Orden> listarPorCredito(@PathVariable boolean credito) {
        return service.listarPorCredito(credito);
    }

    /** 🔎 Búsqueda combinada: clienteId, venta, credito, desde, hasta, obra */
    @GetMapping("/buscar")
    public List<Orden> buscar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Boolean venta,
            @RequestParam(required = false) Boolean credito,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String obra) {
        return service.buscar(clienteId, venta, credito, desde, hasta, obra);
    }
}
