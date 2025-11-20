package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.GastoSedeCreateDTO;
import com.casaglass.casaglass_backend.model.GastoSede;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Proveedor;
import com.casaglass.casaglass_backend.service.GastoSedeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos-sede")
public class GastoSedeController {

    @Autowired
    private GastoSedeService service;

    /**
     * 📋 LISTAR TODOS LOS GASTOS
     */
    @GetMapping
    public List<GastoSede> listar() {
        return service.obtenerTodos();
    }

    /**
     * 🔍 OBTENER GASTO POR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GastoSede> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🏢 LISTAR GASTOS POR SEDE
     */
    @GetMapping("/sede/{sedeId}")
    public List<GastoSede> listarPorSede(@PathVariable Long sedeId) {
        return service.obtenerPorSede(sedeId);
    }

    /**
     * ✅ LISTAR GASTOS DISPONIBLES PARA ENTREGA (aprobados y sin entrega asociada)
     */
    @GetMapping("/sede/{sedeId}/sin-entrega")
    public List<GastoSede> obtenerGastosSinEntrega(@PathVariable Long sedeId) {
        return service.obtenerGastosSinEntrega(sedeId);
    }

    /**
     * ✅ LISTAR GASTOS APROBADOS
     */
    @GetMapping("/aprobados")
    public List<GastoSede> obtenerGastosAprobados() {
        return service.obtenerGastosAprobados();
    }

    /**
     * ⏳ LISTAR GASTOS PENDIENTES DE APROBACIÓN
     */
    @GetMapping("/pendientes")
    public List<GastoSede> obtenerGastosPendientes() {
        return service.obtenerGastosPendientes();
    }

    /**
     * ⏳ LISTAR GASTOS PENDIENTES DE APROBACIÓN POR SEDE
     */
    @GetMapping("/sede/{sedeId}/pendientes")
    public List<GastoSede> obtenerGastosPendientesPorSede(@PathVariable Long sedeId) {
        return service.obtenerGastosPendientesAprobacion(sedeId);
    }

    /**
     * 💰 CREAR NUEVO GASTO
     */
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody GastoSedeCreateDTO gastoDTO) {
        try {
            GastoSede gasto = new GastoSede();
            
            // Configurar sede
            Sede sede = new Sede();
            sede.setId(gastoDTO.getSedeId());
            gasto.setSede(sede);
            
            gasto.setFechaGasto(gastoDTO.getFechaGasto());
            gasto.setMonto(gastoDTO.getMonto());
            gasto.setConcepto(gastoDTO.getConcepto());
            gasto.setDescripcion(gastoDTO.getDescripcion());
            gasto.setComprobante(gastoDTO.getComprobante());
            gasto.setTipo(GastoSede.TipoGasto.valueOf(gastoDTO.getTipo()));
            gasto.setAprobado(true); // Los gastos se crean aprobados automáticamente
            gasto.setObservaciones(gastoDTO.getObservaciones());
            
            // Configurar empleado si existe
            if (gastoDTO.getEmpleadoId() != null) {
                Trabajador empleado = new Trabajador();
                empleado.setId(gastoDTO.getEmpleadoId());
                gasto.setEmpleado(empleado);
            }
            
            // Configurar proveedor si existe
            if (gastoDTO.getProveedorId() != null) {
                Proveedor proveedor = new Proveedor();
                proveedor.setId(gastoDTO.getProveedorId());
                gasto.setProveedor(proveedor);
            }
            
            GastoSede gastoCreado = service.crearGasto(gasto);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Gasto creado exitosamente",
                "gasto", gastoCreado
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * ✅ APROBAR GASTO
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        try {
            GastoSede gasto = service.aprobarGasto(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Gasto aprobado exitosamente",
                "gasto", gasto
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ❌ RECHAZAR GASTO
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long id) {
        try {
            GastoSede gasto = service.rechazarGasto(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Gasto rechazado exitosamente",
                "gasto", gasto
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔄 ACTUALIZAR GASTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody GastoSedeCreateDTO gastoDTO) {
        try {
            GastoSede gastoActualizado = new GastoSede();
            gastoActualizado.setFechaGasto(gastoDTO.getFechaGasto());
            gastoActualizado.setMonto(gastoDTO.getMonto());
            gastoActualizado.setConcepto(gastoDTO.getConcepto());
            gastoActualizado.setDescripcion(gastoDTO.getDescripcion());
            gastoActualizado.setComprobante(gastoDTO.getComprobante());
            gastoActualizado.setTipo(GastoSede.TipoGasto.valueOf(gastoDTO.getTipo()));
            gastoActualizado.setAprobado(true); // Mantener aprobado al actualizar
            gastoActualizado.setObservaciones(gastoDTO.getObservaciones());
            
            GastoSede gasto = service.actualizarGasto(id, gastoActualizado);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Gasto actualizado exitosamente",
                "gasto", gasto
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🗑️ ELIMINAR GASTO
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            service.eliminarGasto(id);
            return ResponseEntity.ok(Map.of("mensaje", "Gasto eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📊 OBTENER TOTAL GASTOS POR SEDE EN PERÍODO
     */
    @GetMapping("/sede/{sedeId}/total")
    public ResponseEntity<Double> obtenerTotal(@PathVariable Long sedeId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Double total = service.obtenerTotalGastosSedeEnPeriodo(sedeId, desde, hasta);
        return ResponseEntity.ok(total);
    }
}

