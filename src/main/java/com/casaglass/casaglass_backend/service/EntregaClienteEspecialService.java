package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.EntregaClienteEspecial;
import com.casaglass.casaglass_backend.model.EntregaClienteEspecialDetalle;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.repository.EntregaClienteEspecialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EntregaClienteEspecialService {

    private final EntregaClienteEspecialRepository repository;

    public EntregaClienteEspecialService(EntregaClienteEspecialRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EntregaClienteEspecial registrarEntrega(List<DetalleRegistro> registros,
                                                   String ejecutadoPor,
                                                   String observaciones) {
        if (registros == null || registros.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un crédito para registrar la entrega especial");
        }

        EntregaClienteEspecial entrega = new EntregaClienteEspecial();
        entrega.setEjecutadoPor(ejecutadoPor != null && !ejecutadoPor.isBlank() ? ejecutadoPor.trim() : "SISTEMA");
        entrega.setObservaciones(observaciones);
        entrega.setTotalCreditos(registros.size());

        double totalCredito = 0.0;
        List<EntregaClienteEspecialDetalle> detalles = new ArrayList<>();
        for (DetalleRegistro registro : registros) {
            Credito credito = registro.getCredito();
            Orden orden = credito.getOrden();

            double creditoTotal = credito.getTotalCredito() != null ? credito.getTotalCredito() : 0.0;
            totalCredito += creditoTotal;

            EntregaClienteEspecialDetalle detalle = new EntregaClienteEspecialDetalle();
            detalle.setEntrega(entrega);
            detalle.setCredito(credito);
            detalle.setOrden(orden);
            detalle.setNumeroOrden(orden != null ? orden.getNumero() : null);
            detalle.setFechaCredito(credito.getFechaInicio());
            detalle.setTotalCredito(creditoTotal);
            detalle.setSaldoAnterior(registro.getSaldoAnterior());

            detalles.add(detalle);
        }

        entrega.setTotalMontoCredito(totalCredito);
        entrega.setDetalles(detalles);

        return repository.save(entrega);
    }

    @Transactional(readOnly = true)
    public List<EntregaClienteEspecial> listar(LocalDate desde, LocalDate hasta) {
        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(23, 59, 59) : null;
        return repository.buscarPorRango(desdeDateTime, hastaDateTime);
    }

    @Transactional(readOnly = true)
    public EntregaClienteEspecial obtener(Long id) {
        return repository.findWithDetallesById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entrega especial no encontrada con ID: " + id));
    }

    public static class DetalleRegistro {
        private final Credito credito;
        private final Double saldoAnterior;

        public DetalleRegistro(Credito credito, Double saldoAnterior) {
            this.credito = credito;
            this.saldoAnterior = saldoAnterior != null ? saldoAnterior : 0.0;
        }

        public Credito getCredito() {
            return credito;
        }

        public Double getSaldoAnterior() {
            return saldoAnterior;
        }
    }
}
