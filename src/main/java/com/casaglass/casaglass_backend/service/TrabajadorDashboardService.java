package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.TrabajadorDashboardDTO;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrabajadorDashboardService {

    private final OrdenRepository ordenRepository;
    private final TrabajadorRepository trabajadorRepository;

    public TrabajadorDashboardService(OrdenRepository ordenRepository, TrabajadorRepository trabajadorRepository) {
        this.ordenRepository = ordenRepository;
        this.trabajadorRepository = trabajadorRepository;
    }

    @Transactional(readOnly = true)
    public TrabajadorDashboardDTO obtenerDashboard(Long trabajadorId, LocalDate desde, LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusDays(30);
        if (hasta == null) hasta = LocalDate.now();

        Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado"));

        List<Orden> ordenes = ordenRepository.findByTrabajadorIdAndFechaBetween(trabajadorId, desde, hasta)
                .stream()
                .filter(Orden::isVenta)
                .collect(Collectors.toList());

        // Resumen contado vs crédito
        List<Orden> contado = ordenes.stream().filter(o -> !o.isCredito()).collect(Collectors.toList());
        List<Orden> credito = ordenes.stream().filter(Orden::isCredito).collect(Collectors.toList());

        double contadoMonto = contado.stream().mapToDouble(Orden::getTotal).sum();
        double creditoMonto = credito.stream().mapToDouble(Orden::getTotal).sum();
        double totalMonto = contadoMonto + creditoMonto;
        long totalOrdenes = ordenes.size();
        double ticketPromedio = totalOrdenes == 0 ? 0.0 : totalMonto / totalOrdenes;

        TrabajadorDashboardDTO.Resumen resumen = new TrabajadorDashboardDTO.Resumen(
                trabajadorId,
                trabajador.getNombre(),
                totalOrdenes,
                (long) contado.size(),
                contadoMonto,
                (long) credito.size(),
                creditoMonto,
                totalMonto,
                ticketPromedio
        );

        // Ventas por día
        Map<LocalDate, List<Orden>> porDia = ordenes.stream().collect(Collectors.groupingBy(Orden::getFecha));
        List<TrabajadorDashboardDTO.VentaPorDia> ventasPorDia = porDia.entrySet().stream()
                .map(e -> new TrabajadorDashboardDTO.VentaPorDia(
                        e.getKey(),
                        (long) e.getValue().size(),
                        e.getValue().stream().mapToDouble(Orden::getTotal).sum()
                ))
                .sorted(Comparator.comparing(TrabajadorDashboardDTO.VentaPorDia::getFecha))
                .collect(Collectors.toList());

        // Top productos (excluye cortes)
        Map<Long, List<OrdenItem>> itemsPorProducto = new HashMap<>();
        for (Orden o : ordenes) {
            if (o.getItems() == null) continue;
            for (OrdenItem it : o.getItems()) {
                if (it.getProducto() == null || (it.getProducto() instanceof Corte)) continue;
                itemsPorProducto.computeIfAbsent(it.getProducto().getId(), k -> new ArrayList<>()).add(it);
            }
        }
        List<TrabajadorDashboardDTO.TopProducto> topProductos = itemsPorProducto.entrySet().stream()
                .map(entry -> {
                    OrdenItem any = entry.getValue().get(0);
                    long cant = entry.getValue().stream().mapToLong(i -> i.getCantidad() != null ? i.getCantidad() : 0L).sum();
                    double monto = entry.getValue().stream().mapToDouble(i -> i.getTotalLinea() != null ? i.getTotalLinea() : 0.0).sum();
                    return new TrabajadorDashboardDTO.TopProducto(
                            any.getProducto().getId(), any.getProducto().getNombre(), any.getProducto().getCodigo(), cant, monto
                    );
                })
                .sorted(Comparator.comparing(TrabajadorDashboardDTO.TopProducto::getMonto).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Top clientes
        Map<Long, List<Orden>> porCliente = ordenes.stream()
                .filter(o -> o.getCliente() != null)
                .collect(Collectors.groupingBy(o -> o.getCliente().getId()));
        List<TrabajadorDashboardDTO.TopCliente> topClientes = porCliente.entrySet().stream()
                .map(entry -> {
                    List<Orden> lista = entry.getValue();
                    return new TrabajadorDashboardDTO.TopCliente(
                            entry.getKey(),
                            lista.get(0).getCliente().getNombre(),
                            (long) lista.size(),
                            lista.stream().mapToDouble(Orden::getTotal).sum()
                    );
                })
                .sorted(Comparator.comparing(TrabajadorDashboardDTO.TopCliente::getMonto).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new TrabajadorDashboardDTO(resumen, ventasPorDia, topProductos, topClientes);
    }
}


