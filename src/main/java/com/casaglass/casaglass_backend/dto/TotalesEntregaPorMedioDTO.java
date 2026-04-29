package com.casaglass.casaglass_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Totales para arqueo de caja de una entrega: suman los montos por medio de cada
 * {@link EntregaDetalleSimpleDTO}. Preferentemente valores persistidos por venta/abono
 * (<code>montoEfectivo</code>, …). Si esos buckets vienen en 0 pero la venta cargó igual
 * (caso habitual si <code>crearOrdenVenta</code> no persiste los medios),
 * se aproxima línea a línea: contado como <code>total − retenciones</code>; abono como
 * <code>montoOrden</code> del detalle.</p>
 * <p>No confundir con el campo contable <code>subtotal</code> del detalle (base sin IVA).</p>
 * <p>EGRESO resta; INGRESO suma.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalesEntregaPorMedioDTO {

    private Double efectivo;
    private Double transferencia;
    private Double cheque;
    private Double deposito;

    private static final double EPS = 1e-6;

    public static TotalesEntregaPorMedioDTO desdeDetalles(List<EntregaDetalleSimpleDTO> detalles) {
        TotalesEntregaPorMedioDTO out = new TotalesEntregaPorMedioDTO(0.0, 0.0, 0.0, 0.0);
        if (detalles == null || detalles.isEmpty()) {
            return out;
        }
        double ef = 0.0;
        double tr = 0.0;
        double ch = 0.0;
        double dep = 0.0;
        for (EntregaDetalleSimpleDTO d : detalles) {
            if (d == null) {
                continue;
            }
            double signo = egreso(d.getTipoMovimiento()) ? -1.0 : 1.0;
            double me = nz(d.getMontoEfectivo());
            double mt = nz(d.getMontoTransferencia());
            double mc = nz(d.getMontoCheque());
            double md = nz(d.getMontoDeposito());

            // Si los cuatro buckets van en 0 en BD pero la línea es ingreso, estimar efectivo único:
            // no desglosa mixto efectivo/transf cuando faltaron persistir medios en la orden contado.
            if (!egreso(d.getTipoMovimiento())
                    && d.getReembolsoId() == null
                    && me + mt + mc + md < EPS) {
                if (Boolean.TRUE.equals(d.getVentaCredito())) {
                    double netAbono = nz(d.getMontoOrden());
                    if (netAbono > EPS) {
                        me = Math.abs(netAbono);
                    }
                } else {
                    double totalLin = nz(d.getTotal());
                    double netCash = totalLin - nz(d.getRetencionFuente()) - nz(d.getRetencionIca());
                    if (netCash > EPS) {
                        me = netCash;
                    } else if (totalLin > EPS) {
                        me = totalLin;
                    }
                }
            }

            ef += signo * me;
            tr += signo * mt;
            ch += signo * mc;
            dep += signo * md;
        }
        out.setEfectivo(round2(ef));
        out.setTransferencia(round2(tr));
        out.setCheque(round2(ch));
        out.setDeposito(round2(dep));
        return out;
    }

    public static TotalesEntregaPorMedioDTO ceros() {
        return new TotalesEntregaPorMedioDTO(0.0, 0.0, 0.0, 0.0);
    }

    private static boolean egreso(String tipoMovimiento) {
        return tipoMovimiento != null && tipoMovimiento.equalsIgnoreCase("EGRESO");
    }

    private static double nz(Double v) {
        return v != null ? v : 0.0;
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    /** Suma efectivo + transferencia + cheque + depósito (solo uso en servidor; ver JSON con los cuatro montos). */
    @JsonIgnore
    public Double sumaTotalMedios() {
        return round2(nz(this.efectivo) + nz(this.transferencia) + nz(this.cheque) + nz(this.deposito));
    }
}
