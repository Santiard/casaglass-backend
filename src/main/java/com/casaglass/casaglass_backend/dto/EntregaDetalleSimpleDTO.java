package com.casaglass.casaglass_backend.dto;

import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.model.EntregaDetalle;
import com.casaglass.casaglass_backend.model.ReembolsoVenta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDetalleSimpleDTO {
    
    private Long id;
    private Long ordenId;
    private Long abonoId; // ID del abono específico (si aplica)
    private Long reembolsoId; // ID del reembolso (si aplica)
    private Long numeroOrden;
    private LocalDate fechaOrden;
    private Double montoOrden;
    private Boolean ventaCredito;
    private String clienteNombre;
    private String observaciones;
    private Double abonosDelPeriodo; // Abonos del período para órdenes a crédito (o monto del abono específico)
    
    // Método de pago: descripcion para órdenes a contado, metodoPago para abonos
    private String descripcion; // Para órdenes a contado (ventaCredito = false): descripcion de la orden
    private String metodoPago; // Para abonos (ventaCredito = true y abonoId != null): metodoPago del abono
    
    // ✅ TIPO DE MOVIMIENTO: "INGRESO" o "EGRESO"
    private String tipoMovimiento; // INGRESO: órdenes y abonos normales | EGRESO: reembolsos/devoluciones
    
    /**
     * Método helper para obtener el Abono de forma segura
     * Maneja el caso cuando el Abono fue eliminado (referencia huérfana) o lazy loading fallido
     * Usa try-catch para capturar todas las excepciones relacionadas con Hibernate
     */
    private Abono obtenerAbonoSeguro(EntregaDetalle detalle) {
        if (detalle == null || detalle.getAbono() == null) {
            return null;
        }
        
        Abono abono = null;
        try {
            abono = detalle.getAbono();
            
            // Intentar acceder a múltiples propiedades para forzar la carga del proxy
            // Si el Abono no existe, Hibernate lanzará EntityNotFoundException
            // Si está fuera de sesión, lanzará LazyInitializationException
            
            // Primero verificar que el ID sea accesible (fuerza carga del proxy)
            Long abonoId = abono.getId();
            if (abonoId == null) {
                return null;
            }
            
            // Intentar acceder a otra propiedad para asegurar que el Abono está completamente cargado
            // Si el Abono fue eliminado, esto lanzará EntityNotFoundException
            abono.getTotal(); // Esto fuerza la carga completa del Abono
            
            // Si llegamos aquí, el Abono existe y está cargado correctamente
            return abono;
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // El Abono fue eliminado pero la referencia sigue en la BD (referencia huérfana)
            return null;
        } catch (org.hibernate.LazyInitializationException e) {
            // Error de inicialización perezosa (fuera de sesión de Hibernate)
            return null;
        } catch (org.hibernate.ObjectNotFoundException e) {
            // Objeto no encontrado en la base de datos
            return null;
        } catch (Exception e) {
            // Cualquier otro error al acceder al Abono (por seguridad, retornar null)
            // Esto incluye cualquier excepción de Hibernate relacionada con proxies
            return null;
        }
    }
    
    /**
     * Método helper para obtener el ReembolsoVenta de forma segura
     * Maneja el caso cuando el ReembolsoVenta fue eliminado (referencia huérfana) o lazy loading fallido
     */
    private ReembolsoVenta obtenerReembolsoSeguro(EntregaDetalle detalle) {
        if (detalle == null || detalle.getReembolsoVenta() == null) {
            return null;
        }
        
        ReembolsoVenta reembolso = null;
        try {
            reembolso = detalle.getReembolsoVenta();
            
            // Intentar acceder a múltiples propiedades para forzar la carga del proxy
            // Si el ReembolsoVenta no existe, Hibernate lanzará EntityNotFoundException
            
            // Primero verificar que el ID sea accesible (fuerza carga del proxy)
            Long reembolsoId = reembolso.getId();
            if (reembolsoId == null) {
                return null;
            }
            
            // Intentar acceder a otra propiedad para asegurar que el ReembolsoVenta está completamente cargado
            // Si el ReembolsoVenta fue eliminado, esto lanzará EntityNotFoundException
            reembolso.getTotalReembolso(); // Esto fuerza la carga completa del ReembolsoVenta
            
            // Si llegamos aquí, el ReembolsoVenta existe y está cargado correctamente
            return reembolso;
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // El ReembolsoVenta fue eliminado pero la referencia sigue en la BD (referencia huérfana)
            return null;
        } catch (org.hibernate.LazyInitializationException e) {
            // Error de inicialización perezosa (fuera de sesión de Hibernate)
            return null;
        } catch (org.hibernate.ObjectNotFoundException e) {
            // Objeto no encontrado en la base de datos
            return null;
        } catch (Exception e) {
            // Cualquier otro error al acceder al ReembolsoVenta (por seguridad, retornar null)
            return null;
        }
    }
    
    // Constructor desde entidad (SIN referencia a entrega para evitar ciclos)
    // Sin cálculo de abonos (para compatibilidad)
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle) {
        // ✅ Envolver todo el constructor en try-catch para manejar cualquier excepción de Hibernate
        // que pueda ocurrir durante la inicialización de proxies lazy
        try {
            this.id = detalle.getId();
            this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
            
            // ✅ Obtener Abono de forma segura (maneja referencias huérfanas)
            Abono abono = obtenerAbonoSeguro(detalle);
            this.abonoId = abono != null ? abono.getId() : null;
            
            // ✅ Obtener ReembolsoVenta de forma segura (maneja referencias huérfanas)
            ReembolsoVenta reembolso = obtenerReembolsoSeguro(detalle);
            this.reembolsoId = reembolso != null ? reembolso.getId() : null;
            
            this.numeroOrden = detalle.getNumeroOrden();
            this.fechaOrden = detalle.getFechaOrden();
            
            // ✅ MONTO: Usar fuente correcta según el tipo
            if (reembolso != null) {
                // Es reembolso: usar monto del reembolso (negativo)
                this.montoOrden = -Math.abs(reembolso.getTotalReembolso());
            } else {
                // Es orden/abono normal: usar montoOrden del detalle
                this.montoOrden = detalle.getMontoOrden();
            }
            
            this.ventaCredito = detalle.getVentaCredito();
            this.clienteNombre = detalle.getClienteNombre();
            this.observaciones = detalle.getObservaciones();
            
            // ✅ MAPEAR TIPO DE MOVIMIENTO
            // Si el campo tipoMovimiento está establecido, usarlo
            // Si no, inferir: si tiene reembolsoVenta = EGRESO, de lo contrario = INGRESO
            if (detalle.getTipoMovimiento() != null) {
                this.tipoMovimiento = detalle.getTipoMovimiento().name();
            } else if (reembolso != null) {
                this.tipoMovimiento = "EGRESO";
            } else {
                this.tipoMovimiento = "INGRESO";
            }
            
            // ✅ Si hay un abono específico y existe, usar su monto; si no, null
            if (abono != null && abono.getTotal() != null) {
                this.abonosDelPeriodo = abono.getTotal();
            } else {
                this.abonosDelPeriodo = null;
            }
            
            // Método de pago según el tipo:
            // - Para órdenes a contado (ventaCredito = false): descripcion de la orden
            // - Para abonos (ventaCredito = true y abonoId != null): metodoPago del abono
            if (detalle.getVentaCredito() != null && !detalle.getVentaCredito()) {
                // Orden a contado: usar descripcion de la orden
                if (detalle.getOrden() != null) {
                    this.descripcion = detalle.getOrden().getDescripcion();
                }
            } else if (detalle.getVentaCredito() != null && detalle.getVentaCredito() && abono != null) {
                // Abono: usar metodoPago del abono (solo si existe)
                this.metodoPago = abono.getMetodoPago();
            }
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Si ocurre una excepción durante la construcción, inicializar con valores por defecto
            // Esto puede ocurrir si Hibernate intenta cargar una relación lazy que no existe
            this.id = detalle != null ? detalle.getId() : null;
            this.ordenId = null;
            this.abonoId = null;
            this.reembolsoId = null;
            this.numeroOrden = detalle != null ? detalle.getNumeroOrden() : null;
            this.fechaOrden = detalle != null ? detalle.getFechaOrden() : null;
            this.montoOrden = detalle != null ? detalle.getMontoOrden() : null;
            this.ventaCredito = detalle != null ? detalle.getVentaCredito() : null;
            this.clienteNombre = detalle != null ? detalle.getClienteNombre() : null;
            this.observaciones = detalle != null ? detalle.getObservaciones() : null;
            this.abonosDelPeriodo = null;
            this.tipoMovimiento = "INGRESO";
            this.descripcion = null;
            this.metodoPago = null;
        } catch (Exception e) {
            // Cualquier otra excepción: inicializar con valores por defecto
            this.id = detalle != null ? detalle.getId() : null;
            this.ordenId = null;
            this.abonoId = null;
            this.reembolsoId = null;
            this.numeroOrden = detalle != null ? detalle.getNumeroOrden() : null;
            this.fechaOrden = detalle != null ? detalle.getFechaOrden() : null;
            this.montoOrden = detalle != null ? detalle.getMontoOrden() : null;
            this.ventaCredito = detalle != null ? detalle.getVentaCredito() : null;
            this.clienteNombre = detalle != null ? detalle.getClienteNombre() : null;
            this.observaciones = detalle != null ? detalle.getObservaciones() : null;
            this.abonosDelPeriodo = null;
            this.tipoMovimiento = "INGRESO";
            this.descripcion = null;
            this.metodoPago = null;
        }
    }
    
    // Constructor con cálculo de abonos del período
    public EntregaDetalleSimpleDTO(EntregaDetalle detalle, LocalDate fechaDesde, LocalDate fechaHasta, 
                                   com.casaglass.casaglass_backend.service.AbonoService abonoService) {
        // ✅ Envolver todo el constructor en try-catch para manejar cualquier excepción de Hibernate
        try {
            this.id = detalle.getId();
            this.ordenId = detalle.getOrden() != null ? detalle.getOrden().getId() : null;
            
            // ✅ Obtener Abono de forma segura (maneja referencias huérfanas)
            Abono abono = obtenerAbonoSeguro(detalle);
            this.abonoId = abono != null ? abono.getId() : null;
            
            // ✅ Obtener ReembolsoVenta de forma segura (maneja referencias huérfanas)
            ReembolsoVenta reembolso = obtenerReembolsoSeguro(detalle);
            this.reembolsoId = reembolso != null ? reembolso.getId() : null;
            
            this.numeroOrden = detalle.getNumeroOrden();
            this.fechaOrden = detalle.getFechaOrden();
            
            // ✅ MONTO: Usar fuente correcta según el tipo
            if (reembolso != null) {
                // Es reembolso: usar monto del reembolso (negativo)
                this.montoOrden = -Math.abs(reembolso.getTotalReembolso());
            } else {
                // Es orden/abono normal: usar montoOrden del detalle
                this.montoOrden = detalle.getMontoOrden();
            }
            
            this.ventaCredito = detalle.getVentaCredito();
            this.clienteNombre = detalle.getClienteNombre();
            this.observaciones = detalle.getObservaciones();
            
            // ✅ MAPEAR TIPO DE MOVIMIENTO
            // Si el campo tipoMovimiento está establecido, usarlo
            // Si no, inferir: si tiene reembolsoVenta = EGRESO, de lo contrario = INGRESO
            if (detalle.getTipoMovimiento() != null) {
                this.tipoMovimiento = detalle.getTipoMovimiento().name();
            } else if (reembolso != null) {
                this.tipoMovimiento = "EGRESO";
            } else {
                this.tipoMovimiento = "INGRESO";
            }
            
            // ✅ Si hay un abono específico y existe, usar su monto directamente
            if (abono != null && abono.getTotal() != null) {
                this.abonosDelPeriodo = abono.getTotal();
            } else if (detalle.getVentaCredito() != null && detalle.getVentaCredito() && 
                detalle.getOrden() != null && detalle.getOrden().getId() != null &&
                fechaDesde != null && fechaHasta != null && abonoService != null) {
                // Si no hay abono específico, calcular abonos del período (compatibilidad con lógica antigua)
                this.abonosDelPeriodo = abonoService.calcularAbonosOrdenEnPeriodo(
                    detalle.getOrden().getId(), fechaDesde, fechaHasta);
            } else {
                this.abonosDelPeriodo = 0.0; // Contado o sin datos
            }
            
            // Método de pago según el tipo:
            // - Para órdenes a contado (ventaCredito = false): descripcion de la orden
            // - Para abonos (ventaCredito = true y abonoId != null): metodoPago del abono
            if (detalle.getVentaCredito() != null && !detalle.getVentaCredito()) {
                // Orden a contado: usar descripcion de la orden
                if (detalle.getOrden() != null) {
                    this.descripcion = detalle.getOrden().getDescripcion();
                }
            } else if (detalle.getVentaCredito() != null && detalle.getVentaCredito() && abono != null) {
                // Abono: usar metodoPago del abono (solo si existe)
                this.metodoPago = abono.getMetodoPago();
            }
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Si ocurre una excepción durante la construcción, inicializar con valores por defecto
            this.id = detalle != null ? detalle.getId() : null;
            this.ordenId = null;
            this.abonoId = null;
            this.reembolsoId = null;
            this.numeroOrden = detalle != null ? detalle.getNumeroOrden() : null;
            this.fechaOrden = detalle != null ? detalle.getFechaOrden() : null;
            this.montoOrden = detalle != null ? detalle.getMontoOrden() : null;
            this.ventaCredito = detalle != null ? detalle.getVentaCredito() : null;
            this.clienteNombre = detalle != null ? detalle.getClienteNombre() : null;
            this.observaciones = detalle != null ? detalle.getObservaciones() : null;
            this.abonosDelPeriodo = 0.0;
            this.tipoMovimiento = "INGRESO";
            this.descripcion = null;
            this.metodoPago = null;
        } catch (Exception e) {
            // Cualquier otra excepción: inicializar con valores por defecto
            this.id = detalle != null ? detalle.getId() : null;
            this.ordenId = null;
            this.abonoId = null;
            this.reembolsoId = null;
            this.numeroOrden = detalle != null ? detalle.getNumeroOrden() : null;
            this.fechaOrden = detalle != null ? detalle.getFechaOrden() : null;
            this.montoOrden = detalle != null ? detalle.getMontoOrden() : null;
            this.ventaCredito = detalle != null ? detalle.getVentaCredito() : null;
            this.clienteNombre = detalle != null ? detalle.getClienteNombre() : null;
            this.observaciones = detalle != null ? detalle.getObservaciones() : null;
            this.abonosDelPeriodo = 0.0;
            this.tipoMovimiento = "INGRESO";
            this.descripcion = null;
            this.metodoPago = null;
        }
    }
}