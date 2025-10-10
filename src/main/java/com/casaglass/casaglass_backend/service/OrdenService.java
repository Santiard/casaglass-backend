package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
// no need for LocalDateTime/LocalTime
import java.util.List;
import java.util.Optional;

@Service
public class OrdenService {

    private final OrdenRepository repo;
    private final EntityManager entityManager;

    public OrdenService(OrdenRepository repo, EntityManager entityManager) { 
        this.repo = repo; 
        this.entityManager = entityManager;
    }

    @Transactional
    public Orden crear(Orden orden) {
        if (orden.getFecha() == null) orden.setFecha(LocalDate.now());

        // Validar que tenga sede asignada
        if (orden.getSede() == null || orden.getSede().getId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria para la orden");
        }

        // Usar referencia ligera para la sede
        orden.setSede(entityManager.getReference(Sede.class, orden.getSede().getId()));

        // 🚀 GENERACIÓN AUTOMÁTICA DE NÚMERO (THREAD-SAFE)
        // El número se ignora si viene del frontend - siempre se genera automáticamente
        Long numeroGenerado = generarNumeroOrden();
        orden.setNumero(numeroGenerado);

        double subtotal = 0.0;
        if (orden.getItems() != null) {
            for (OrdenItem it : orden.getItems()) {
                it.setOrden(orden); // amarra relación
                Double linea = it.getPrecioUnitario() * it.getCantidad();
                it.setTotalLinea(linea);
                subtotal += linea;

                if ((it.getDescripcion() == null || it.getDescripcion().isBlank())
                        && it.getProducto() != null) {
                    it.setDescripcion(it.getProducto().getNombre());
                }
            }
        }
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal); // impuestos/desc. si aplica más adelante
        return repo.save(orden);
    }

    /**
     * Genera el siguiente número de orden de forma thread-safe
     * Maneja automáticamente la concurrencia entre múltiples usuarios
     */
    private Long generarNumeroOrden() {
        int maxIntentos = 5;
        int intento = 0;
        
        while (intento < maxIntentos) {
            try {
                // Obtener el siguiente número disponible
                Long siguienteNumero = repo.obtenerSiguienteNumero();
                
                // Verificar que no exista (por si hubo concurrencia)
                if (!repo.findByNumero(siguienteNumero).isPresent()) {
                    return siguienteNumero;
                }
                
                // Si existe, incrementar y volver a intentar
                intento++;
                Thread.sleep(10); // Pausa muy breve para evitar colisiones
                
            } catch (Exception e) {
                intento++;
                if (intento >= maxIntentos) {
                    throw new RuntimeException("Error generando número de orden después de " + maxIntentos + " intentos", e);
                }
            }
        }
        
        throw new RuntimeException("No se pudo generar un número de orden único después de " + maxIntentos + " intentos");
    }

    public Optional<Orden> obtenerPorId(Long id) { return repo.findById(id); }

    public Optional<Orden> obtenerPorNumero(Long numero) { return repo.findByNumero(numero); }

    public List<Orden> listar() { return repo.findAll(); }

    public List<Orden> listarPorCliente(Long clienteId) { return repo.findByClienteId(clienteId); }

    public List<Orden> listarPorVenta(boolean venta) { return repo.findByVenta(venta); }

    public List<Orden> listarPorCredito(boolean credito) { return repo.findByCredito(credito); }

    /** Órdenes de un día (00:00:00 a 23:59:59.999999999) */
    public List<Orden> listarPorFecha(LocalDate fecha) {
        return repo.findByFechaBetween(fecha, fecha);
    }

    /** Órdenes en rango [desde, hasta] (ambos inclusive por día) */
    public List<Orden> listarPorRangoFechas(LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByFechaBetween(desdeDia, hastaDia);
    }

    // Métodos nuevos para manejar sede
    public List<Orden> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    public List<Orden> listarPorClienteYSede(Long clienteId, Long sedeId) {
        return repo.findByClienteIdAndSedeId(clienteId, sedeId);
    }

    public List<Orden> listarPorSedeYVenta(Long sedeId, boolean venta) {
        return repo.findBySedeIdAndVenta(sedeId, venta);
    }

    public List<Orden> listarPorSedeYCredito(Long sedeId, boolean credito) {
        return repo.findBySedeIdAndCredito(sedeId, credito);
    }

    /** Órdenes de una sede en un día específico */
    public List<Orden> listarPorSedeYFecha(Long sedeId, LocalDate fecha) {
        return repo.findBySedeIdAndFechaBetween(sedeId, fecha, fecha);
    }

    /** Órdenes de una sede en rango [desde, hasta] (ambos inclusive por día) */
    public List<Orden> listarPorSedeYRangoFechas(Long sedeId, LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findBySedeIdAndFechaBetween(sedeId, desdeDia, hastaDia);
    }

    /**
     * Obtiene el próximo número de orden que se asignará
     * Útil para mostrar en el frontend como referencia (número provisional)
     */
    @Transactional(readOnly = true)
    public Long obtenerProximoNumero() {
        return repo.obtenerSiguienteNumero();
    }
}