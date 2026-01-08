package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.DashboardCompletoDTO;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardCompletoService {

    private final OrdenRepository ordenRepository;
    private final FacturaRepository facturaRepository;
    private final CreditoRepository creditoRepository;
    private final ClienteRepository clienteRepository;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;

    public DashboardCompletoService(
            OrdenRepository ordenRepository,
            FacturaRepository facturaRepository,
            CreditoRepository creditoRepository,
            ClienteRepository clienteRepository,
            SedeRepository sedeRepository,
            ProductoRepository productoRepository) {
        this.ordenRepository = ordenRepository;
        this.facturaRepository = facturaRepository;
        this.creditoRepository = creditoRepository;
        this.clienteRepository = clienteRepository;
        this.sedeRepository = sedeRepository;
        this.productoRepository = productoRepository;
    }

    /**
     * 📊 OBTENER DASHBOARD COMPLETO
     * Retorna todos los datos relevantes en una sola llamada
     * 
     * @param desde Fecha inicio (opcional, default: 7 días atrás)
     * @param hasta Fecha fin (opcional, default: hoy)
     * @return DashboardCompletoDTO con todos los datos
     */
    @Transactional(readOnly = true)
    public DashboardCompletoDTO obtenerDashboardCompleto(LocalDate desde, LocalDate hasta) {
        // Si no se especifican fechas, usar últimos 7 días
        if (desde == null) {
            desde = LocalDate.now().minusDays(7);
        }
        if (hasta == null) {
            hasta = LocalDate.now();
        }

        DashboardCompletoDTO dashboard = new DashboardCompletoDTO();

        // 1. RESUMEN GENERAL
        dashboard.setResumenGeneral(obtenerResumenGeneral(desde, hasta));

        // 2. VENTAS POR DÍA
        dashboard.setVentasPorDia(obtenerVentasPorDia(desde, hasta));

        // 3. VENTAS POR SEDE
        dashboard.setVentasPorSede(obtenerVentasPorSede(desde, hasta));

        // 4. TOP PRODUCTOS
        dashboard.setTopProductos(obtenerTopProductos(desde, hasta, 10));

        // 5. TOP CLIENTES
        dashboard.setTopClientes(obtenerTopClientes(desde, hasta, 10));

        // 6. RESUMEN CRÉDITOS
        dashboard.setResumenCreditos(obtenerResumenCreditos());

        // 7. FACTURACIÓN POR ESTADO
        dashboard.setFacturacionPorEstado(obtenerFacturacionPorEstado(desde, hasta));

        // 8. TICKET PROMEDIO POR SEDE
        dashboard.setTicketPromedioPorSede(obtenerTicketPromedioPorSede(desde, hasta));

        return dashboard;
    }

    private DashboardCompletoDTO.ResumenGeneral obtenerResumenGeneral(LocalDate desde, LocalDate hasta) {
        List<Orden> todasOrdenes = ordenRepository.findByFechaBetween(desde, hasta);
        List<Orden> ventas = todasOrdenes.stream().filter(Orden::isVenta).collect(Collectors.toList());
        List<Orden> cotizaciones = todasOrdenes.stream().filter(o -> !o.isVenta()).collect(Collectors.toList());

        Double montoVentas = ventas.stream().mapToDouble(Orden::getTotal).sum();
        Double montoCotizaciones = cotizaciones.stream().mapToDouble(Orden::getTotal).sum();

        List<Factura> facturas = facturaRepository.findByFechaBetween(desde, hasta);
        Double montoFacturado = facturas.stream().mapToDouble(Factura::getTotal).sum();

        List<Credito> creditosAbiertos = creditoRepository.findByEstado(Credito.EstadoCredito.ABIERTO);
        Double montoCreditosPendiente = creditosAbiertos.stream()
                .mapToDouble(c -> c.getSaldoPendiente() != null ? c.getSaldoPendiente() : 0.0)
                .sum();

        return new DashboardCompletoDTO.ResumenGeneral(
                (long) todasOrdenes.size(),
                (long) ventas.size(),
                (long) cotizaciones.size(),
                montoVentas,
                montoCotizaciones,
                (long) facturas.size(),
                montoFacturado,
                (long) creditosAbiertos.size(),
                montoCreditosPendiente
        );
    }

    private List<DashboardCompletoDTO.VentaPorDia> obtenerVentasPorDia(LocalDate desde, LocalDate hasta) {
        List<Orden> ventas = ordenRepository.findByFechaBetween(desde, hasta).stream()
                .filter(Orden::isVenta)
                .collect(Collectors.toList());

        Map<LocalDate, List<Orden>> ventasPorFecha = ventas.stream()
                .collect(Collectors.groupingBy(Orden::getFecha));

        return ventasPorFecha.entrySet().stream()
                .map(entry -> {
                    LocalDate fecha = entry.getKey();
                    List<Orden> ordenes = entry.getValue();
                    Double monto = ordenes.stream().mapToDouble(Orden::getTotal).sum();
                    return new DashboardCompletoDTO.VentaPorDia(fecha, (long) ordenes.size(), monto);
                })
                .sorted(Comparator.comparing(DashboardCompletoDTO.VentaPorDia::getFecha))
                .collect(Collectors.toList());
    }

    private List<DashboardCompletoDTO.VentaPorSede> obtenerVentasPorSede(LocalDate desde, LocalDate hasta) {
        List<Orden> ventas = ordenRepository.findByFechaBetween(desde, hasta).stream()
                .filter(Orden::isVenta)
                .filter(o -> o.getSede() != null)
                .collect(Collectors.toList());

        Map<Long, List<Orden>> ventasPorSede = ventas.stream()
                .collect(Collectors.groupingBy(o -> o.getSede().getId()));

        return ventasPorSede.entrySet().stream()
                .map(entry -> {
                    Long sedeId = entry.getKey();
                    List<Orden> ordenes = entry.getValue();
                    String nombreSede = ordenes.get(0).getSede().getNombre();
                    Double monto = ordenes.stream().mapToDouble(Orden::getTotal).sum();
                    return new DashboardCompletoDTO.VentaPorSede(sedeId, nombreSede, (long) ordenes.size(), monto);
                })
                .sorted(Comparator.comparing(DashboardCompletoDTO.VentaPorSede::getMontoTotal).reversed())
                .collect(Collectors.toList());
    }

    private List<DashboardCompletoDTO.TopProducto> obtenerTopProductos(LocalDate desde, LocalDate hasta, int limite) {
        List<Orden> ventas = ordenRepository.findByFechaBetween(desde, hasta).stream()
                .filter(Orden::isVenta)
                .collect(Collectors.toList());

        Map<Long, List<OrdenItem>> itemsPorProducto = new HashMap<>();
        for (Orden venta : ventas) {
            if (venta.getItems() != null) {
                for (OrdenItem item : venta.getItems()) {
                    if (item.getProducto() != null && !(item.getProducto() instanceof Corte)) {
                        Long productoId = item.getProducto().getId();
                        itemsPorProducto.computeIfAbsent(productoId, k -> new ArrayList<>()).add(item);
                    }
                }
            }
        }

        return itemsPorProducto.entrySet().stream()
                .map(entry -> {
                    Long productoId = entry.getKey();
                    List<OrdenItem> items = entry.getValue();
                    OrdenItem primerItem = items.get(0);
                    Producto producto = primerItem.getProducto();
                    Double cantidad = items.stream().mapToDouble(i -> i.getCantidad() != null ? i.getCantidad() : 0.0).sum();
                    Double monto = items.stream().mapToDouble(i -> i.getTotalLinea() != null ? i.getTotalLinea() : 0.0).sum();
                    return new DashboardCompletoDTO.TopProducto(
                            productoId,
                            producto.getNombre(),
                            producto.getCodigo(),
                            cantidad,
                            monto
                    );
                })
                .sorted(Comparator.comparing(DashboardCompletoDTO.TopProducto::getMontoTotal).reversed())
                .limit(limite)
                .collect(Collectors.toList());
    }

    private List<DashboardCompletoDTO.TopCliente> obtenerTopClientes(LocalDate desde, LocalDate hasta, int limite) {
        List<Orden> ventas = ordenRepository.findByFechaBetween(desde, hasta).stream()
                .filter(Orden::isVenta)
                .filter(o -> o.getCliente() != null)
                .collect(Collectors.toList());

        Map<Long, List<Orden>> ventasPorCliente = ventas.stream()
                .collect(Collectors.groupingBy(o -> o.getCliente().getId()));

        return ventasPorCliente.entrySet().stream()
                .map(entry -> {
                    Long clienteId = entry.getKey();
                    List<Orden> ordenes = entry.getValue();
                    Cliente cliente = ordenes.get(0).getCliente();
                    Double monto = ordenes.stream().mapToDouble(Orden::getTotal).sum();
                    return new DashboardCompletoDTO.TopCliente(
                            clienteId,
                            cliente.getNombre(),
                            cliente.getNit(),
                            (long) ordenes.size(),
                            monto
                    );
                })
                .sorted(Comparator.comparing(DashboardCompletoDTO.TopCliente::getMontoTotal).reversed())
                .limit(limite)
                .collect(Collectors.toList());
    }

    private DashboardCompletoDTO.ResumenCreditos obtenerResumenCreditos() {
        List<Credito> todosCreditos = creditoRepository.findAll();

        long abiertos = todosCreditos.stream().filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO).count();
        long cerrados = todosCreditos.stream().filter(c -> c.getEstado() == Credito.EstadoCredito.CERRADO).count();
        long vencidos = todosCreditos.stream().filter(c -> c.getEstado() == Credito.EstadoCredito.VENCIDO).count();
        long anulados = todosCreditos.stream().filter(c -> c.getEstado() == Credito.EstadoCredito.ANULADO).count();

        Double montoPendiente = todosCreditos.stream()
                .filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO)
                .mapToDouble(c -> c.getSaldoPendiente() != null ? c.getSaldoPendiente() : 0.0)
                .sum();

        Double montoAbonado = todosCreditos.stream()
                .mapToDouble(c -> c.getTotalAbonado() != null ? c.getTotalAbonado() : 0.0)
                .sum();

        return new DashboardCompletoDTO.ResumenCreditos(abiertos, cerrados, vencidos, anulados, montoPendiente, montoAbonado);
    }

    private DashboardCompletoDTO.FacturacionPorEstado obtenerFacturacionPorEstado(LocalDate desde, LocalDate hasta) {
        List<Factura> facturas = facturaRepository.findByFechaBetween(desde, hasta);

        long pendientes = facturas.stream().filter(f -> f.getEstado() == Factura.EstadoFactura.PENDIENTE).count();
        long pagadas = facturas.stream().filter(f -> f.getEstado() == Factura.EstadoFactura.PAGADA).count();
        long anuladas = facturas.stream().filter(f -> f.getEstado() == Factura.EstadoFactura.ANULADA).count();

        Double montoPendiente = facturas.stream()
                .filter(f -> f.getEstado() == Factura.EstadoFactura.PENDIENTE)
                .mapToDouble(Factura::getTotal)
                .sum();

        Double montoPagado = facturas.stream()
                .filter(f -> f.getEstado() == Factura.EstadoFactura.PAGADA)
                .mapToDouble(Factura::getTotal)
                .sum();

        return new DashboardCompletoDTO.FacturacionPorEstado(pendientes, pagadas, anuladas, montoPendiente, montoPagado);
    }

    private List<DashboardCompletoDTO.TicketPromedioSede> obtenerTicketPromedioPorSede(LocalDate desde, LocalDate hasta) {
        List<Orden> ventas = ordenRepository.findByFechaBetween(desde, hasta).stream()
                .filter(Orden::isVenta)
                .filter(o -> o.getSede() != null)
                .collect(Collectors.toList());

        Map<Long, List<Orden>> ventasPorSede = ventas.stream()
                .collect(Collectors.groupingBy(o -> o.getSede().getId()));

        return ventasPorSede.entrySet().stream()
                .map(entry -> {
                    Long sedeId = entry.getKey();
                    List<Orden> ordenes = entry.getValue();
                    String nombreSede = ordenes.get(0).getSede().getNombre();
                    Double montoTotal = ordenes.stream().mapToDouble(Orden::getTotal).sum();
                    Double promedio = ordenes.isEmpty() ? 0.0 : montoTotal / ordenes.size();
                    return new DashboardCompletoDTO.TicketPromedioSede(sedeId, nombreSede, promedio, (long) ordenes.size());
                })
                .sorted(Comparator.comparing(DashboardCompletoDTO.TicketPromedioSede::getTicketPromedio).reversed())
                .collect(Collectors.toList());
    }
}

