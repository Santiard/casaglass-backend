package com.casaglass.casaglass_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Locale;

/**
 * Totales para arqueo de caja de una entrega: suman los montos por medio de cada
 * {@link EntregaDetalleSimpleDTO}. Preferentemente valores persistidos por venta/abono
 * (<code>montoEfectivo</code>, …). Si los buckets vienen en 0, se usa heurística por texto
 * (<code>metodoPago</code> del abono, descripción orden contado) para no cargar todo a efectivo
 * cuando la UI muestra transferencia u otro medio.
 *
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

            // Sin buckets persistidos: repartir con heurística de texto para alinear cabeceras con tabla/impreso
            if (!egreso(d.getTipoMovimiento())
                    && d.getReembolsoId() == null
                    && me + mt + mc + md < EPS) {
                if (Boolean.TRUE.equals(d.getVentaCredito())) {
                    double netAbono = nz(d.getMontoOrden());
                    if (netAbono > EPS) {
                        double[] inf = distribuirPorTextoMedio(Math.abs(netAbono), d.getMetodoPago(), true);
                        me = inf[0];
                        mt = inf[1];
                        mc = inf[2];
                        md = inf[3];
                    }
                } else {
                    double totalLin = nz(d.getTotal());
                    double netCash = totalLin - nz(d.getRetencionFuente()) - nz(d.getRetencionIca());
                    double base = netCash > EPS ? netCash : (totalLin > EPS ? totalLin : 0);
                    if (base > EPS) {
                        String hintOrden =
                                nzString(d.getDescripcion()) + " " + nzString(d.getMetodoPago());
                        double[] inf = distribuirPorTextoMedio(base, hintOrden, false);
                        me = inf[0];
                        mt = inf[1];
                        mc = inf[2];
                        md = inf[3];
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

    private static String nzString(String s) {
        return s != null ? s : "";
    }

    /**
     * Cuando BD no tiene buckets pero sí hay texto (metodo_pago típico de abono, o descripción de orden MIXTO).
     * Sin pistas fuertes, en abono se asume transferencia ({@link com.casaglass.casaglass_backend.model.Abono} default método).
     */
    private static double[] distribuirPorTextoMedio(double absTotal, String textoHint, boolean esCreditoAbono) {
        double me = 0;
        double mt = 0;
        double mc = 0;
        double md = 0;
        if (absTotal <= EPS) {
            return new double[]{me, mt, mc, md};
        }
        String u = textoHint != null ? textoHint.toUpperCase(Locale.ROOT) : "";
        boolean cheq = u.contains("CHEQUE");
        boolean dep = u.contains("DEPÓSITO") || u.contains("DEPOSITO");
        boolean mixto = u.contains("MIXTO");
        boolean tr = u.contains("TRANSFER")
                || u.contains("NEQUI")
                || u.contains("DAVIPLATA")
                || u.contains("PSE")
                || u.contains("BANCOL"); // también “BANCOLOMBIA”
        boolean ef = u.contains("EFECTIV");

        if (cheq && !tr && !ef && !dep) {
            mc = absTotal;
        } else if (dep && !tr && !ef && !cheq) {
            md = absTotal;
        } else if (mixto || (tr && ef)) {
            me = round2(absTotal / 2.0);
            mt = round2(absTotal - me);
        } else if (tr && !ef) {
            mt = absTotal;
        } else if (ef && !tr) {
            me = absTotal;
        } else if (esCreditoAbono) {
            mt = absTotal;
        } else {
            me = absTotal;
        }
        return new double[]{me, mt, mc, md};
    }

    /** Suma efectivo + transferencia + cheque + depósito (solo uso en servidor; ver JSON con los cuatro montos). */
    @JsonIgnore
    public Double sumaTotalMedios() {
        return round2(nz(this.efectivo) + nz(this.transferencia) + nz(this.cheque) + nz(this.deposito));
    }
}
