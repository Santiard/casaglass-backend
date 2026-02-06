package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.SedeDashboardDTO;
import com.casaglass.casaglass_backend.dto.SedeDashboardDTO.*;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SedeDashboardService {

    @Autowired
    private SedeRepository sedeRepository;
    
    @Autowired
    private OrdenRepository ordenRepository;
    
    @Autowired
    private EntregaDineroRepository entregaDineroRepository;
    
    @Autowired
    private CreditoRepository creditoRepository;
    
    @Autowired
    private InventarioRepository inventarioRepository;
    
    @Autowired
    private TrasladoRepository trasladoRepository;

    @Transactional(readOnly = true)
    public SedeDashboardDTO obtenerDashboard(Long sedeId) {
        // Verificar que la sede existe
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // Construir el dashboard
        SedeDashboardDTO dashboard = new SedeDashboardDTO();
        
        dashboard.setSede(obtenerInfoSede(sede));
        dashboard.setVentasHoy(obtenerVentasHoy(sedeId));
        dashboard.setFaltanteEntrega(obtenerFaltanteEntrega(sedeId));
        dashboard.setCreditosPendientes(obtenerCreditosPendientes(sedeId));
        dashboard.setTrasladosPendientes(obtenerTrasladosPendientes(sedeId));
        dashboard.setAlertasStock(obtenerAlertasStock(sedeId));
        
        return dashboard;
    }

    private SedeInfo obtenerInfoSede(Sede sede) {
        return new SedeInfo(
                sede.getId(),
                sede.getNombre()
        );
    }

    private VentasHoyInfo obtenerVentasHoy(Long sedeId) {
        LocalDate hoy = LocalDate.now();
        
        // Obtener todas las órdenes de hoy que sean ventas
        List<Orden> ventasHoy = ordenRepository.findBySedeIdAndFechaAndVentaTrue(sedeId, hoy);
        
        Integer cantidad = ventasHoy.size();
        Double total = ventasHoy.stream().mapToDouble(Orden::getTotal).sum();
        
        // Separar por tipo de venta
        List<Orden> ventasContado = ventasHoy.stream()
                .filter(orden -> !orden.isCredito())
                .collect(Collectors.toList());
        
        List<Orden> ventasCredito = ventasHoy.stream()
                .filter(Orden::isCredito)
                .collect(Collectors.toList());
        
        Integer cantidadContado = ventasContado.size();
        Integer cantidadCredito = ventasCredito.size();
        Double totalContado = ventasContado.stream().mapToDouble(Orden::getTotal).sum();
        Double totalCredito = ventasCredito.stream().mapToDouble(Orden::getTotal).sum();
        
        return new VentasHoyInfo(cantidad, total, cantidadContado, cantidadCredito, totalContado, totalCredito);
    }

    private FaltanteEntregaInfo obtenerFaltanteEntrega(Long sedeId) {
        // Obtener la última entrega de dinero de la sede
        EntregaDinero ultimaEntrega = entregaDineroRepository.findFirstBySedeIdOrderByFechaEntregaDesc(sedeId);
        
        if (ultimaEntrega == null) {
            return new FaltanteEntregaInfo(0.0, null, 0.0, "SIN_ENTREGAS");
        }
        
        // Ya no hay diferencia porque siempre se entrega todo el dinero
        Double montoFaltante = 0.0;
        
        return new FaltanteEntregaInfo(
                montoFaltante,
                ultimaEntrega.getFechaEntrega().atStartOfDay(), // Convertir LocalDate a LocalDateTime
                ultimaEntrega.getMonto() != null ? ultimaEntrega.getMonto() : 0.0,
                ultimaEntrega.getEstado().name()
        );
    }

    private CreditosPendientesInfo obtenerCreditosPendientes(Long sedeId) {
        // Obtener todos los créditos activos de la sede
        List<Credito> creditosActivos = creditoRepository.findByOrdenSedeIdAndEstado(sedeId, Credito.EstadoCredito.ABIERTO);
        
        Integer totalCreditos = creditosActivos.size();
        Double montoTotal = creditosActivos.stream().mapToDouble(Credito::getTotalCredito).sum();
        Double montoPendiente = creditosActivos.stream().mapToDouble(Credito::getSaldoPendiente).sum();
        
        // Créditos vencidos (más de 30 días desde fecha_inicio sin estar cerrados)
        LocalDate fechaLimiteVencido = LocalDate.now().minusDays(30);
        List<CreditoResumenDTO> creditosVencidos = creditosActivos.stream()
                .filter(credito -> credito.getFechaInicio().isBefore(fechaLimiteVencido))
                .map(this::convertirACreditoResumen)
                .collect(Collectors.toList());
        
        // Créditos próximos a vencer (entre 20-30 días)
        LocalDate fechaLimiteProximo = LocalDate.now().minusDays(20);
        List<CreditoResumenDTO> creditosProximoVencimiento = creditosActivos.stream()
                .filter(credito -> credito.getFechaInicio().isAfter(fechaLimiteVencido) && 
                                   credito.getFechaInicio().isBefore(fechaLimiteProximo))
                .map(this::convertirACreditoResumen)
                .collect(Collectors.toList());
        
        return new CreditosPendientesInfo(totalCreditos, montoTotal, montoPendiente, creditosVencidos, creditosProximoVencimiento);
    }

    private CreditoResumenDTO convertirACreditoResumen(Credito credito) {
        return new CreditoResumenDTO(
                credito.getId(),
                credito.getCliente().getNombre(),
                credito.getOrden().getNumero(),
                credito.getSaldoPendiente(),
                credito.getFechaInicio(),
                credito.getFechaInicio().plusDays(30), // Asumiendo 30 días de plazo
                credito.getEstado().name()
        );
    }

    private AlertasStockInfo obtenerAlertasStock(Long sedeId) {
        // Obtener inventario de la sede donde el stock está bajo
        List<Inventario> inventarioBajo = inventarioRepository.findBySedeIdAndCantidadLessThanEqual(sedeId, 20);
        
        List<ProductoBajoStockDTO> productosBajos = inventarioBajo.stream()
                .map(this::convertirAProductoBajoStock)
                .collect(Collectors.toList());
        
        return new AlertasStockInfo(productosBajos.size(), productosBajos);
    }

    private ProductoBajoStockDTO convertirAProductoBajoStock(Inventario inventario) {
        String estado;
        if (inventario.getCantidad() == 0) {
            estado = "AGOTADO";
        } else if (inventario.getCantidad() <= 5) {
            estado = "CRÍTICO";
        } else {
            estado = "BAJO";
        }
        
        String categoria = inventario.getProducto().getCategoria() != null ? 
                inventario.getProducto().getCategoria().getNombre() : "Sin categoría";
        
        // Obtener color del producto (como String)
        String color = inventario.getProducto().getColor() != null ? 
                inventario.getProducto().getColor().name() : null;
        
        return new ProductoBajoStockDTO(
                inventario.getProducto().getCodigo(),
                inventario.getProducto().getNombre(),
                color, // ✅ Color del producto
                categoria,
                inventario.getCantidad(),
                20, // Nivel de reorden fijo por ahora, puedes hacerlo configurable
                estado
        );
    }

    private TrasladosPendientesInfo obtenerTrasladosPendientes(Long sedeId) {
        // Traslados que llegan a esta sede (pendientes de confirmación)
        List<Traslado> trasladosRecibir = trasladoRepository.findBySedeDestinoIdAndFechaConfirmacionIsNull(sedeId);
        
        // Traslados que salen de esta sede (pendientes de confirmación)
        List<Traslado> trasladosEnviar = trasladoRepository.findBySedeOrigenIdAndFechaConfirmacionIsNull(sedeId);
        
        List<TrasladoPendienteDTO> trasladosRecibirDTO = trasladosRecibir.stream()
                .map(this::convertirATrasladoPendiente)
                .collect(Collectors.toList());
        
        List<TrasladoPendienteDTO> trasladosEnviarDTO = trasladosEnviar.stream()
                .map(this::convertirATrasladoPendiente)
                .collect(Collectors.toList());
        
        Integer totalPendientes = trasladosRecibirDTO.size() + trasladosEnviarDTO.size();
        
        return new TrasladosPendientesInfo(totalPendientes, trasladosRecibirDTO, trasladosEnviarDTO);
    }

    private TrasladoPendienteDTO convertirATrasladoPendiente(Traslado traslado) {
        String estado = traslado.getFechaConfirmacion() == null ? "PENDIENTE_CONFIRMACION" : "CONFIRMADO";
        
        Integer totalProductos = traslado.getDetalles() != null ? 
                (int) traslado.getDetalles().stream()
                        .mapToDouble(detalle -> detalle.getCantidad() != null ? detalle.getCantidad() : 0.0)
                        .sum() : 0;
        
        return new TrasladoPendienteDTO(
                traslado.getId(),
                traslado.getSedeOrigen().getNombre(),
                traslado.getSedeDestino().getNombre(),
                traslado.getFecha(),
                totalProductos,
                estado
        );
    }
}