package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMesDTO {
    
    // ========== MÉTRICAS DEL MES ACTUAL ==========
    
    /**
     * 💰 DINERO INGRESADO EN EL MES
     * Total de ventas de todas las órdenes del mes (con venta=true y estado=ACTIVA)
     * Ejemplo: Órdenes normales creadas en abril
     */
    private Double totalVentasDelMes;
    
    /**
     * 📊 DEUDAS NUEVAS EN EL MES
     * Total de créditos ABIERTOS que INICIARON en el mes
     * (No incluye deudas de meses anteriores, solo las creadas en este mes)
     */
    private Double totalDeudasDelMes;
    
    /**
     * 💵 DINERO ABONADO EN EL MES
     * Total de todos los abonos (pagos a créditos) registrados en el mes
     * Dinero que ingresó por pagos de clientes en este mes
     */
    private Double totalAbonasDelMes;
    
    /**
     * 🏧 DINERO FÍSICAMENTE ENTREGADO EN EL MES
     * Suma de TODAS las entregas de dinero creadas en el mes para esta sede
     * (Dinero que salió de la caja/oficina en este mes)
     */
    private Double totalEntregadoDelMes;

    // ========== HISTÓRICO ACUMULADO ==========
    
    /**
     * 📈 DEUDAS TOTALES HISTÓRICAS (ABIERTO)
     * Total de créditos que están ABIERTOS en este momento, sin importar cuándo se crearon
     * Incluye: Deudas de abril + marzo + febrero + más atrás (TODO lo que sigue abierto)
     * Útil para: Saber cuál es la deuda total pendiente del cliente/empresa
     */
    private Double totalDeudasHistorico;
    
    /**
     * 💸 TOTAL ABONOS HISTÓRICO
     * Suma de TODOS los abonos registrados desde el inicio del sistema
     * (Dinero total que ha ingresado por pagos de clientes en toda la historia)
     */
    private Double totalAbonosHistorico;

    /**
     * Total de esta entrega puntual
     */
    private Double totalEstaEntrega;
    
    /**
     * Mes en formato "2026-04"
     */
    private String mes;
    
    /**
     * Nombre de la sede
     */
    private String sede;
    
    /**
     * Nombre del trabajador
     */
    private String trabajador;
    
    /**
     * Nombre del mes en formato "febrero 2026" (campo adicional para referencias)
     */
    private String mesNombre;
}