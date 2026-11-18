package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.Orden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoSimpleDTO {
    
    private Long id;
    private List<Long> abonoIds = new ArrayList<>();
    private LocalDate fecha;
    private LocalDate fechaAbono;  // ← Alias para fecha (compatibilidad frontend)
    private Double total;
    private Double montoAbono;  // ← Alias para total (compatibilidad frontend)
    private String metodoPago;
    private String factura;
    private String numeroFactura;  // ← Alias para factura (compatibilidad frontend)
    private Double saldo;
    private Long creditoId;
    private Long ordenId;
    private Long numeroOrden;
    private String observaciones;  // ← Campo adicional para observaciones
    private ClienteAbonoDTO cliente;
    private List<DistribucionOrdenDTO> distribucion = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteAbonoDTO {
        private Long id;
        private String nombre;
        private String nit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribucionOrdenDTO {
        private Long abonoId;
        private Long creditoId;
        private Long ordenId;
        private Long numeroOrden;
        private String obra;
        private Double totalOrden;
        private Double montoAbono;
        private Double saldoRestante;
    }
    
    // Constructor desde entidad individual (SIN referencia al crédito completo para evitar ciclos)
    public AbonoSimpleDTO(Abono abono) {
        if (abono == null) {
            return;
        }
        Orden orden = resolverOrden(abono);
        Credito credito = abono.getCredito();
        Cliente cli = resolverCliente(abono);

        this.id = abono.getId();
        if (abono.getId() != null) {
            this.abonoIds = List.of(abono.getId());
        }
        this.fecha = abono.getFecha();
        this.fechaAbono = abono.getFecha();  // ← Mismo valor
        this.total = norm(abono.getTotal());
        this.montoAbono = this.total;  // ← Mismo valor
        this.metodoPago = abono.getMetodoPago();
        this.factura = abono.getFactura();
        this.numeroFactura = abono.getFactura();  // ← Mismo valor
        this.saldo = norm(abono.getSaldo());
        this.creditoId = credito != null ? credito.getId() : null;
        this.ordenId = orden != null ? orden.getId() : null;
        this.numeroOrden = resolverNumeroOrden(abono, orden);
        this.observaciones = null;  // ← Campo opcional, puede extraerse del metodoPago si es necesario
        if (cli != null) {
            this.cliente = new ClienteAbonoDTO(cli.getId(), cli.getNombre(), cli.getNit());
        }
        this.distribucion = List.of(crearDistribucionItem(abono));
    }

    /**
     * Construye un AbonoSimpleDTO consolidando un grupo de registros Abono
     * pertenecientes a un mismo pago múltiple (misma factura, cliente y fecha).
     */
    public static AbonoSimpleDTO desdeGrupo(List<Abono> grupo) {
        if (grupo == null || grupo.isEmpty()) {
            return new AbonoSimpleDTO();
        }

        List<Abono> ordenados = grupo.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Abono::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (ordenados.isEmpty()) {
            return new AbonoSimpleDTO();
        }

        Abono primero = ordenados.get(0);
        Orden primeraOrden = resolverOrden(primero);
        Credito primerCredito = primero.getCredito();
        Cliente cli = resolverCliente(primero);

        AbonoSimpleDTO dto = new AbonoSimpleDTO();
        dto.setId(primero.getId());
        dto.setAbonoIds(ordenados.stream()
                .map(Abono::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        dto.setFecha(primero.getFecha());
        dto.setFechaAbono(primero.getFecha());
        dto.setMetodoPago(primero.getMetodoPago());
        dto.setFactura(primero.getFactura());
        dto.setNumeroFactura(primero.getFactura());
        dto.setCreditoId(primerCredito != null ? primerCredito.getId() : null);
        dto.setOrdenId(primeraOrden != null ? primeraOrden.getId() : null);
        dto.setNumeroOrden(resolverNumeroOrden(primero, primeraOrden));
        dto.setObservaciones(null);

        if (cli != null) {
            dto.setCliente(new ClienteAbonoDTO(cli.getId(), cli.getNombre(), cli.getNit()));
        }

        double sumaTotal = 0.0;
        double sumaSaldo = 0.0;
        List<DistribucionOrdenDTO> dist = new ArrayList<>(ordenados.size());

        for (Abono a : ordenados) {
            sumaTotal += a.getTotal() != null ? a.getTotal() : 0.0;
            sumaSaldo += a.getSaldo() != null ? a.getSaldo() : 0.0;
            dist.add(crearDistribucionItem(a));
        }

        dto.setTotal(norm(sumaTotal));
        dto.setMontoAbono(dto.getTotal());
        dto.setSaldo(norm(sumaSaldo));
        dto.setDistribucion(dist);

        return dto;
    }

    private static DistribucionOrdenDTO crearDistribucionItem(Abono abono) {
        Credito credito = abono.getCredito();
        Orden orden = resolverOrden(abono);
        Long numOrden = resolverNumeroOrden(abono, orden);
        String obra = (orden != null && orden.getObra() != null) ? orden.getObra() : "";
        Double totalOrden = (credito != null && credito.getTotalCredito() != null)
                ? norm(credito.getTotalCredito())
                : (orden != null && orden.getTotal() != null ? norm(orden.getTotal()) : null);

        return new DistribucionOrdenDTO(
                abono.getId(),
                credito != null ? credito.getId() : null,
                orden != null ? orden.getId() : null,
                numOrden,
                obra,
                totalOrden,
                norm(abono.getTotal()),
                norm(abono.getSaldo())
        );
    }

    private static Orden resolverOrden(Abono abono) {
        if (abono == null) {
            return null;
        }
        if (abono.getOrden() != null) {
            return abono.getOrden();
        }
        return abono.getCredito() != null ? abono.getCredito().getOrden() : null;
    }

    private static Long resolverNumeroOrden(Abono abono, Orden orden) {
        if (abono != null && abono.getNumeroOrden() != null) {
            return abono.getNumeroOrden();
        }
        return orden != null ? orden.getNumero() : null;
    }

    private static Cliente resolverCliente(Abono abono) {
        if (abono == null) {
            return null;
        }
        if (abono.getCliente() != null) {
            return abono.getCliente();
        }
        return abono.getCredito() != null ? abono.getCredito().getCliente() : null;
    }

    private static Double norm(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }
}
