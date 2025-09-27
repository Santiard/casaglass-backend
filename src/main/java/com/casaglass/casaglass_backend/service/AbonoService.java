package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.AbonoRepository;
import com.casaglass.casaglass_backend.repository.CreditoRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AbonoService {

    private final AbonoRepository abonoRepo;
    private final CreditoRepository creditoRepo;
    private final OrdenRepository ordenRepo;

    public AbonoService(AbonoRepository abonoRepo,
                        CreditoRepository creditoRepo,
                        OrdenRepository ordenRepo) {
        this.abonoRepo = abonoRepo;
        this.creditoRepo = creditoRepo;
        this.ordenRepo = ordenRepo;
    }

    /* -------- Helpers de dinero (redondeado a 2 decimales) -------- */

    private Double norm(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }

    private Double sum(List<Double> vals) {
        double acc = 0.0;
        for (Double v : vals) acc += v == null ? 0.0 : v;
        return Math.round(acc * 100.0) / 100.0;
    }

    /** Suma total de órdenes del crédito (con 2 decimales). */
    private Double totalOrdenes(Credito c) {
        return sum(c.getOrdenes().stream().map(Orden::getTotal).toList());
    }

    /** Suma total de abonos aplicados al crédito (con 2 decimales). */
    private Double totalAbonos(Long creditoId) {
        return sum(abonoRepo.findByCreditoId(creditoId).stream().map(Abono::getTotal).toList());
    }

    /** Recalcula y guarda el totalDeuda del crédito = órdenes - abonos (clamp >= 0). */
    @Transactional
    protected Credito recalcularCredito(Long creditoId) {
        Credito c = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado: " + creditoId));
        Double deuda = norm(totalOrdenes(c) - totalAbonos(creditoId));
        if (deuda < 0) deuda = 0.0;
        c.setTotalDeuda(deuda);
        return creditoRepo.save(c);
    }

    /* ------------------- Consultas ------------------- */

    public Optional<Abono> obtener(Long abonoId) {
        return abonoRepo.findById(abonoId);
    }

    public List<Abono> listarPorCredito(Long creditoId) {
        return abonoRepo.findByCreditoId(creditoId);
    }

    public List<Abono> listarPorCliente(Long clienteId) {
        return abonoRepo.findByClienteId(clienteId);
    }

    public List<Abono> listarPorOrden(Long ordenId) {
        return abonoRepo.findByOrdenId(ordenId);
    }

    /* ----------------- Crear / Actualizar / Eliminar ----------------- */

    @Transactional
    public Abono crear(Long creditoId, Abono payload) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado: " + creditoId));

        // Normaliza monto
        if (payload.getTotal() == null) throw new IllegalArgumentException("El monto (total) es obligatorio");
        Double monto = norm(payload.getTotal());
        if (monto <= 0) throw new IllegalArgumentException("El monto debe ser > 0");

        // Cliente: usar el del crédito; si vino distinto, error.
        Cliente clienteCredito = credito.getCliente();
        if (payload.getCliente() != null && payload.getCliente().getId() != null &&
            !Objects.equals(payload.getCliente().getId(), clienteCredito.getId())) {
            throw new IllegalArgumentException("El cliente del abono no coincide con el del crédito");
        }

        // Orden (opcional): verificar pertenencia de cliente (y opcionalmente que esté asociada al crédito)
        Orden orden = null;
        if (payload.getOrden() != null && payload.getOrden().getId() != null) {
            orden = ordenRepo.findById(payload.getOrden().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Orden no existe: " + payload.getOrden().getId()));
            if (!Objects.equals(orden.getCliente().getId(), clienteCredito.getId())) {
                throw new IllegalArgumentException("La orden pertenece a otro cliente");
            }
            // Si quieres forzar que la orden esté en el crédito, descomenta:
            // boolean pertenece = credito.getOrdenes().stream().anyMatch(o -> o.getId().equals(orden.getId()));
            // if (!pertenece) throw new IllegalArgumentException("La orden no está asociada a este crédito");
        }

        // Saldo actual (antes del nuevo abono)
        Double saldoActual = norm(totalOrdenes(credito) - totalAbonos(creditoId));
        if (saldoActual < 0) saldoActual = 0.0;
        if (monto > saldoActual) throw new IllegalArgumentException("El abono excede el saldo actual");

        Abono abono = new Abono();
        abono.setCredito(credito);
        abono.setCliente(clienteCredito);
        abono.setOrden(orden);
        abono.setNumeroOrden(orden != null ? orden.getNumero() : payload.getNumeroOrden());
        abono.setFecha(payload.getFecha() != null ? payload.getFecha() : LocalDate.now());
        abono.setMetodoPago(payload.getMetodoPago() != null ? payload.getMetodoPago() : Abono.MetodoPago.TRANSFERENCIA);
        abono.setFactura(payload.getFactura());
        abono.setTotal(monto);

        // Saldo posterior a este abono (snapshot)
        Double saldoPosterior = norm(saldoActual - monto);
        abono.setSaldo(saldoPosterior);

        Abono guardado = abonoRepo.save(abono);

        // Actualiza totalDeuda del crédito
        recalcularCredito(creditoId);

        return guardado;
    }

    @Transactional
    public Abono actualizar(Long creditoId, Long abonoId, Abono payload) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));

        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }

        // Permitir edición de fecha, método de pago, factura y monto
        if (payload.getFecha() != null) abono.setFecha(payload.getFecha());
        if (payload.getMetodoPago() != null) abono.setMetodoPago(payload.getMetodoPago());
        if (payload.getFactura() != null) abono.setFactura(payload.getFactura());

        if (payload.getTotal() != null) {
            Double nuevoMonto = norm(payload.getTotal());
            if (nuevoMonto <= 0) {
                throw new IllegalArgumentException("El monto debe ser > 0");
            }
            abono.setTotal(nuevoMonto);
        }

        // (Opcional) cambiar orden: validar cliente y asociación si lo requieres
        if (payload.getOrden() != null && payload.getOrden().getId() != null) {
            Orden orden = ordenRepo.findById(payload.getOrden().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Orden no existe: " + payload.getOrden().getId()));
            if (!Objects.equals(orden.getCliente().getId(), abono.getCliente().getId())) {
                throw new IllegalArgumentException("La orden pertenece a otro cliente");
            }
            abono.setOrden(orden);
            abono.setNumeroOrden(orden.getNumero());
        }

        // Recalcular saldo del crédito y saldo snapshot del abono
        Abono actualizado = abonoRepo.save(abono);
        Credito credito = recalcularCredito(creditoId);
        // El 'saldo' de este abono se registra como snapshot con el saldo actual del crédito.
        actualizado.setSaldo(credito.getTotalDeuda());
        return abonoRepo.save(actualizado);
    }

    @Transactional
    public void eliminar(Long creditoId, Long abonoId) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));
        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }
        abonoRepo.delete(abono);
        recalcularCredito(creditoId);
    }
}
