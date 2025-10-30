package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.DashboardVentasPorSedeDTO;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private OrdenRepository ordenRepository;

    public List<DashboardVentasPorSedeDTO> obtenerVentasPorSede(LocalDate desde, LocalDate hasta) {
        List<Orden> ordenes = ordenRepository.findByFechaBetween(desde, hasta);

        // Filtrar solo ventas efectivas (venta=true) y estado ACTIVA si aplica
        List<Orden> ventas = ordenes.stream()
                .filter(Orden::isVenta)
                .collect(Collectors.toList());

        Map<Long, List<Orden>> agrupadoPorSede = ventas.stream()
                .filter(o -> o.getSede() != null)
                .collect(Collectors.groupingBy(o -> o.getSede().getId()));

        List<DashboardVentasPorSedeDTO> resultado = new ArrayList<>();
        for (Map.Entry<Long, List<Orden>> entry : agrupadoPorSede.entrySet()) {
            Long sedeId = entry.getKey();
            List<Orden> lista = entry.getValue();
            String nombreSede = lista.get(0).getSede().getNombre();
            int cantidad = lista.size();
            double monto = lista.stream().mapToDouble(Orden::getTotal).sum();
            resultado.add(new DashboardVentasPorSedeDTO(sedeId, nombreSede, cantidad, monto));
        }

        // Ordenar por monto desc para conveniencia del front
        resultado.sort(Comparator.comparing(DashboardVentasPorSedeDTO::getMonto).reversed());
        return resultado;
    }
}


