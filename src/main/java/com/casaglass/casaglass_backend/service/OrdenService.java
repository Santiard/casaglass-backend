package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrdenService {

    private final OrdenRepository repo;

    public OrdenService(OrdenRepository repo) { this.repo = repo; }

    @Transactional
    public Orden crear(Orden orden) {
        if (orden.getFecha() == null) orden.setFecha(LocalDateTime.now());

        BigDecimal subtotal = BigDecimal.ZERO;
        if (orden.getItems() != null) {
            for (OrdenItem it : orden.getItems()) {
                it.setOrden(orden); // amarra relación
                BigDecimal linea = it.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(it.getCantidad()));
                it.setTotalLinea(linea);
                subtotal = subtotal.add(linea);

                if ((it.getDescripcion() == null || it.getDescripcion().isBlank())
                        && it.getProducto() != null) {
                    it.setDescripcion(it.getProducto().getNombre());
                }
            }
        }
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal); // impuestos/desc. si aplica más adelante
        return repo.save(orden);
    }

    public Optional<Orden> obtenerPorId(Long id) { return repo.findById(id); }

    public Optional<Orden> obtenerPorNumero(Long numero) { return repo.findByNumero(numero); }

    public List<Orden> listar() { return repo.findAll(); }

    public List<Orden> listarPorCliente(Long clienteId) { return repo.findByClienteId(clienteId); }

    public List<Orden> listarPorVenta(boolean venta) { return repo.findByVenta(venta); }

    public List<Orden> listarPorCredito(boolean credito) { return repo.findByCredito(credito); }

    /** Órdenes de un día (00:00:00 a 23:59:59.999999999) */
    public List<Orden> listarPorFecha(LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(LocalTime.MAX);
        return repo.findByFechaBetween(desde, hasta);
    }

    /** Órdenes en rango [desde, hasta] (ambos inclusive por día) */
    public List<Orden> listarPorRangoFechas(LocalDate desdeDia, LocalDate hastaDia) {
        LocalDateTime desde = desdeDia.atStartOfDay();
        LocalDateTime hasta = hastaDia.atTime(LocalTime.MAX);
        return repo.findByFechaBetween(desde, hasta);
    }

     public List<Orden> buscar(Long clienteId,
                              Boolean venta,
                              Boolean credito,
                              LocalDate desdeDia,
                              LocalDate hastaDia,
                              String obra) {

        Specification<Orden> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (venta != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("venta"), venta));
        }
        if (credito != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("credito"), credito));
        }
        if (desdeDia != null || hastaDia != null) {
            LocalDateTime desde = (desdeDia != null) ? desdeDia.atStartOfDay() : LocalDate.MIN.atStartOfDay();
            LocalDateTime hasta = (hastaDia != null) ? hastaDia.atTime(LocalTime.MAX) : LocalDate.MAX.atTime(LocalTime.MAX);
            spec = spec.and((root, q, cb) -> cb.between(root.get("fecha"), desde, hasta));
        }
        if (obra != null && !obra.isBlank()) {
            String like = "%" + obra.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("obra")), like));
        }

        return repo.findAll(spec);
    }
}