package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.AbonoDTO;
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
    private final CreditoService creditoService;

    public AbonoService(AbonoRepository abonoRepo,
                        CreditoRepository creditoRepo,
                        OrdenRepository ordenRepo,
                        CreditoService creditoService) {
        this.abonoRepo = abonoRepo;
        this.creditoRepo = creditoRepo;
        this.ordenRepo = ordenRepo;
        this.creditoService = creditoService;
    }

    /* -------- Helpers de dinero (redondeado a 2 decimales) -------- */

    private Double norm(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
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

    /**
     * 💰 CREAR ABONO DESDE DTO (MÉTODO SIMPLIFICADO PARA FRONTEND)
     * Crea un abono usando solo los datos mínimos del frontend
     */
    @Transactional
    public Abono crearDesdeDTO(Long creditoId, AbonoDTO abonoDTO) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado: " + creditoId));

        // Validar que el crédito esté abierto
        if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
        }
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito anulado");
        }

        // Normalizar y validar monto
        Double monto = norm(abonoDTO.getTotal());
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        // Validar que no exceda el saldo pendiente
        if (monto > credito.getSaldoPendiente()) {
            throw new IllegalArgumentException(
                String.format("El abono ($%.2f) excede el saldo pendiente ($%.2f)", 
                            monto, credito.getSaldoPendiente())
            );
        }

        // Crear el abono con los datos del DTO
        Abono abono = new Abono();
        abono.setCredito(credito);
        abono.setCliente(credito.getCliente());
        abono.setOrden(credito.getOrden());
        abono.setNumeroOrden(credito.getOrden().getNumero());
        abono.setFecha(abonoDTO.getFecha());
        abono.setMetodoPago(abonoDTO.getMetodoPago());
        abono.setFactura(abonoDTO.getFactura());
        abono.setTotal(monto);

        // Calcular saldo posterior al abono
        Double saldoPosterior = norm(credito.getSaldoPendiente() - monto);
        abono.setSaldo(saldoPosterior);

        // Guardar abono
        Abono guardado = abonoRepo.save(abono);

        // Actualizar totales del crédito
        credito.setTotalAbonado(norm(credito.getTotalAbonado() + monto));
        credito.setSaldoPendiente(saldoPosterior);
        
        // Actualizar estado si se pagó completamente
        if (saldoPosterior <= 0.0) {
            credito.setEstado(Credito.EstadoCredito.CERRADO);
            credito.setFechaCierre(LocalDate.now());
        }
        
        creditoRepo.save(credito);

        return guardado;
    }

    /**
     * 💰 CREAR ABONO PARA UN CRÉDITO
     * Crea un abono y actualiza automáticamente los totales del crédito
     */
    @Transactional
    public Abono crear(Long creditoId, Abono payload) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado: " + creditoId));

        // Validar que el crédito esté abierto
        if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
        }
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito anulado");
        }

        // Normalizar y validar monto
        if (payload.getTotal() == null) {
            throw new IllegalArgumentException("El monto (total) es obligatorio");
        }
        Double monto = norm(payload.getTotal());
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        // Validar que no exceda el saldo pendiente
        if (monto > credito.getSaldoPendiente()) {
            throw new IllegalArgumentException(
                String.format("El abono ($%.2f) excede el saldo pendiente ($%.2f)", 
                            monto, credito.getSaldoPendiente())
            );
        }

        // Cliente: usar el del crédito
        Cliente clienteCredito = credito.getCliente();
        if (payload.getCliente() != null && payload.getCliente().getId() != null &&
            !Objects.equals(payload.getCliente().getId(), clienteCredito.getId())) {
            throw new IllegalArgumentException("El cliente del abono no coincide con el del crédito");
        }

        // Orden: debe ser la orden del crédito (si se especifica)
        Orden orden = credito.getOrden(); // En el nuevo modelo, cada crédito tiene una orden específica
        if (payload.getOrden() != null && payload.getOrden().getId() != null) {
            if (!Objects.equals(payload.getOrden().getId(), orden.getId())) {
                throw new IllegalArgumentException("El abono debe aplicarse a la orden del crédito");
            }
        }

        // Crear el abono
        Abono abono = new Abono();
        abono.setCredito(credito);
        abono.setCliente(clienteCredito);
        abono.setOrden(orden);
        abono.setNumeroOrden(orden.getNumero());
        abono.setFecha(payload.getFecha() != null ? payload.getFecha() : LocalDate.now());
        abono.setMetodoPago(payload.getMetodoPago() != null ? payload.getMetodoPago() : Abono.MetodoPago.TRANSFERENCIA);
        abono.setFactura(payload.getFactura());
        abono.setTotal(monto);

        // Calcular saldo posterior al abono
        Double saldoPosterior = norm(credito.getSaldoPendiente() - monto);
        abono.setSaldo(saldoPosterior);

        // Guardar abono
        Abono guardado = abonoRepo.save(abono);

        // Actualizar crédito usando el método del crédito
        credito.agregarAbono(guardado);
        creditoRepo.save(credito);

        return guardado;
    }

    /**
     * ✏️ ACTUALIZAR ABONO EXISTENTE
     */
    @Transactional
    public Abono actualizar(Long creditoId, Long abonoId, Abono payload) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));

        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }

        Credito credito = abono.getCredito();
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden modificar abonos de un crédito anulado");
        }

        // Permitir edición de fecha, método de pago, factura
        if (payload.getFecha() != null) abono.setFecha(payload.getFecha());
        if (payload.getMetodoPago() != null) abono.setMetodoPago(payload.getMetodoPago());
        if (payload.getFactura() != null) abono.setFactura(payload.getFactura());

        // Si se cambia el monto, validar y recalcular
        if (payload.getTotal() != null) {
            Double nuevoMonto = norm(payload.getTotal());
            if (nuevoMonto <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor a 0");
            }

            Double montoAnterior = abono.getTotal();
            Double diferencia = nuevoMonto - montoAnterior;
            Double nuevoSaldoPendiente = credito.getSaldoPendiente() + montoAnterior - nuevoMonto;

            if (nuevoSaldoPendiente < 0) {
                throw new IllegalArgumentException("El nuevo monto haría que se exceda el total del crédito");
            }

            abono.setTotal(nuevoMonto);
            abono.setSaldo(nuevoSaldoPendiente);
        }

        Abono actualizado = abonoRepo.save(abono);
        
        // Recalcular totales del crédito
        creditoService.recalcularTotales(creditoId);
        
        return actualizado;
    }

    /**
     * 🗑️ ELIMINAR ABONO
     */
    @Transactional
    public void eliminar(Long creditoId, Long abonoId) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));
        
        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }

        Credito credito = abono.getCredito();
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden eliminar abonos de un crédito anulado");
        }

        abonoRepo.delete(abono);
        
        // Recalcular totales del crédito
        creditoService.recalcularTotales(creditoId);
    }
}
