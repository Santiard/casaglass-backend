package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.SedeDashboardDTO;
import com.casaglass.casaglass_backend.dto.SedeDashboardDTO.*;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
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

    @Autowired
    private AbonoRepository abonoRepository;

    @Transactional(readOnly = true)
    public SedeDashboardDTO obtenerDashboard(Long sedeId) {
        // Verificar que la sede existe
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // Construir el dashboard
        SedeDashboardDTO dashboard = new SedeDashboardDTO();
        
        dashboard.setSede(obtenerInfoSede(sede));
        dashboard.setVentasHoy(obtenerVentasHoy(sedeId));
        dashboard.setVentasMes(obtenerVentasMes(sedeId));
        dashboard.setFaltanteEntrega(obtenerFaltanteEntrega(sedeId));
        dashboard.setCreditosPendientes(obtenerCreditosPendientes(sedeId));
        dashboard.setDeudasMes(obtenerDeudasMes(sedeId));
        dashboard.setDeudasActivas(obtenerDeudasActivas(sedeId));
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

    private VentasMesInfo obtenerVentasMes(Long sedeId) {
        YearMonth mesActual = YearMonth.now();
        LocalDate primerDia = mesActual.atDay(1);
        LocalDate ultimoDia = mesActual.atEndOfMonth();

        List<Orden> ventasMes = ordenRepository.findBySedeIdAndFechaBetweenAndVentaTrue(sedeId, primerDia, ultimoDia);

        Integer cantidad = ventasMes.size();
        Double total = ventasMes.stream().mapToDouble(Orden::getTotal).sum();

        List<Orden> ventasContado = ventasMes.stream()
                .filter(orden -> !orden.isCredito())
                .collect(Collectors.toList());

        List<Orden> ventasCredito = ventasMes.stream()
                .filter(Orden::isCredito)
                .collect(Collectors.toList());

        return new VentasMesInfo(
                cantidad,
                total,
                ventasContado.size(),
                ventasCredito.size(),
                ventasContado.stream().mapToDouble(Orden::getTotal).sum(),
                ventasCredito.stream().mapToDouble(Orden::getTotal).sum()
        );
    }

    private FaltanteEntregaInfo obtenerFaltanteEntrega(Long sedeId) {
        // Órdenes a contado que aún no se han incluido en ninguna entrega de dinero
        List<Orden> ordenesContadoPendientes = ordenRepository.findOrdenesContadoDisponiblesParaEntregaSinFecha(sedeId);
        Double montoOrdenesContado = ordenesContadoPendientes.stream()
                .mapToDouble(Orden::getTotal).sum();

        // Abonos de crédito que aún no se han incluido en ninguna entrega de dinero
        List<Abono> abonosPendientes = abonoRepository.findAbonosDisponiblesParaEntregaSinFecha(sedeId);
        Double montoAbonos = abonosPendientes.stream()
                .mapToDouble(Abono::getTotal).sum();

        Double montoFaltante = montoOrdenesContado + montoAbonos;

        // Datos de la última entrega registrada (contexto informativo)
        EntregaDinero ultimaEntrega = entregaDineroRepository.findFirstBySedeIdOrderByFechaEntregaDesc(sedeId);

        if (ultimaEntrega == null) {
            return new FaltanteEntregaInfo(
                    montoFaltante,
                    montoOrdenesContado,
                    montoAbonos,
                    ordenesContadoPendientes.size(),
                    abonosPendientes.size(),
                    null, 0.0, "SIN_ENTREGAS"
            );
        }

        return new FaltanteEntregaInfo(
                montoFaltante,
                montoOrdenesContado,
                montoAbonos,
                ordenesContadoPendientes.size(),
                abonosPendientes.size(),
                ultimaEntrega.getFechaEntrega().atStartOfDay(),
                ultimaEntrega.getMonto() != null ? ultimaEntrega.getMonto() : 0.0,
                ultimaEntrega.getEstado().name()
        );
    }

    private DeudasMesInfo obtenerDeudasMes(Long sedeId) {
        YearMonth mesActual = YearMonth.now();
        LocalDate primerDia = mesActual.atDay(1);
        LocalDate ultimoDia = mesActual.atEndOfMonth();

        List<Credito> deudasMes = creditoRepository.findByOrdenSedeIdAndFechaInicioBetween(sedeId, primerDia, ultimoDia);

        Integer totalDeudas = deudasMes.size();
        Double montoTotal = deudasMes.stream().mapToDouble(Credito::getTotalCredito).sum();
        Double montoPendiente = deudasMes.stream().mapToDouble(Credito::getSaldoPendiente).sum();

        long abiertas = deudasMes.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO)
                .count();
        long cerradas = deudasMes.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.CERRADO)
                .count();

        return new DeudasMesInfo(totalDeudas, montoTotal, montoPendiente, (int) abiertas, (int) cerradas);
    }

    private DeudasActivasInfo obtenerDeudasActivas(Long sedeId) {
        List<Credito> todas = creditoRepository.findByOrdenSedeId(sedeId);

        Integer totalDeudas = todas.size();
        Double montoTotalHistorico = todas.stream().mapToDouble(Credito::getTotalCredito).sum();
        Double montoPendienteActivo = todas.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO)
                .mapToDouble(Credito::getSaldoPendiente)
                .sum();

        int abiertas = (int) todas.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO).count();
        int cerradas = (int) todas.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.CERRADO).count();
        int anuladas = (int) todas.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.ANULADO).count();

        return new DeudasActivasInfo(totalDeudas, montoTotalHistorico, montoPendienteActivo, abiertas, cerradas, anuladas);
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