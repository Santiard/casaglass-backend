package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ReembolsoVentaCreateDTO;
import com.casaglass.casaglass_backend.dto.ReembolsoVentaResponseDTO;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReembolsoVentaService {

    private final ReembolsoVentaRepository reembolsoVentaRepository;
    private final ReembolsoVentaDetalleRepository reembolsoVentaDetalleRepository;
    private final OrdenRepository ordenRepository;
    private final OrdenItemRepository ordenItemRepository;
    private final ClienteRepository clienteRepository;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final CreditoService creditoService;
    private final CreditoRepository creditoRepository;

    @Autowired
    public ReembolsoVentaService(
            ReembolsoVentaRepository reembolsoVentaRepository,
            ReembolsoVentaDetalleRepository reembolsoVentaDetalleRepository,
            OrdenRepository ordenRepository,
            OrdenItemRepository ordenItemRepository,
            ClienteRepository clienteRepository,
            SedeRepository sedeRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService,
            CreditoService creditoService,
            CreditoRepository creditoRepository) {
        this.reembolsoVentaRepository = reembolsoVentaRepository;
        this.reembolsoVentaDetalleRepository = reembolsoVentaDetalleRepository;
        this.ordenRepository = ordenRepository;
        this.ordenItemRepository = ordenItemRepository;
        this.clienteRepository = clienteRepository;
        this.sedeRepository = sedeRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.creditoService = creditoService;
        this.creditoRepository = creditoRepository;
    }

    @Transactional(readOnly = true)
    public List<ReembolsoVentaResponseDTO> listarReembolsos() {
        return reembolsoVentaRepository.findAllWithDetalles().stream()
                .map(ReembolsoVentaResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar reembolsos de venta por sede
     * Filtra por la sede de la orden relacionada
     */
    @Transactional(readOnly = true)
    public List<ReembolsoVentaResponseDTO> listarReembolsosPorSede(Long sedeId) {
        return reembolsoVentaRepository.findAllWithDetalles().stream()
                .filter(reembolso -> {
                    if (reembolso.getOrdenOriginal() != null && 
                        reembolso.getOrdenOriginal().getSede() != null) {
                        return reembolso.getOrdenOriginal().getSede().getId().equals(sedeId);
                    }
                    return false;
                })
                .map(ReembolsoVentaResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ReembolsoVentaResponseDTO> obtenerPorId(Long id) {
        return reembolsoVentaRepository.findByIdWithDetalles(id)
                .map(ReembolsoVentaResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public List<ReembolsoVentaResponseDTO> obtenerReembolsosPorOrden(Long ordenId) {
        return reembolsoVentaRepository.findByOrdenOriginalId(ordenId).stream()
                .map(ReembolsoVentaResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReembolsoVentaResponseDTO crearReembolso(ReembolsoVentaCreateDTO dto) {
        // Validar orden original
        Orden ordenOriginal = ordenRepository.findById(dto.getOrdenId())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + dto.getOrdenId()));

        // Validar que la orden no esté anulada
        if (ordenOriginal.getEstado() == Orden.EstadoOrden.ANULADA) {
            throw new RuntimeException("La orden está anulada, no se puede crear reembolso");
        }

        // Crear reembolso
        ReembolsoVenta reembolso = new ReembolsoVenta();
        reembolso.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        reembolso.setOrdenOriginal(ordenOriginal);
        reembolso.setCliente(ordenOriginal.getCliente());
        reembolso.setSede(ordenOriginal.getSede());
        reembolso.setMotivo(dto.getMotivo());
        reembolso.setFormaReembolso(dto.getFormaReembolso());
        reembolso.setDescuentos(dto.getDescuentos() != null ? dto.getDescuentos() : 0.0);
        reembolso.setEstado(ReembolsoVenta.EstadoReembolso.PENDIENTE);
        reembolso.setProcesado(false);

        // Procesar detalles
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new RuntimeException("El reembolso debe tener al menos un detalle");
        }

        for (ReembolsoVentaCreateDTO.ReembolsoVentaDetalleDTO detalleDTO : dto.getDetalles()) {
            // Validar item original
            OrdenItem ordenItemOriginal = ordenItemRepository.findById(detalleDTO.getOrdenItemId())
                    .orElseThrow(() -> new RuntimeException("Orden item no encontrado con ID: " + detalleDTO.getOrdenItemId()));

            // Validar que pertenece a la orden correcta
            if (!ordenItemOriginal.getOrden().getId().equals(ordenOriginal.getId())) {
                throw new RuntimeException("El item #" + detalleDTO.getOrdenItemId() + " no pertenece a la orden #" + dto.getOrdenId());
            }

            // Validar cantidad
            if (detalleDTO.getCantidad() == null || detalleDTO.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a 0");
            }

            // Validar que no exceda la cantidad vendida
            Integer cantidadYaReembolsada = calcularCantidadYaReembolsada(ordenItemOriginal.getId());
            Integer cantidadDisponible = ordenItemOriginal.getCantidad() - cantidadYaReembolsada;
            
            if (detalleDTO.getCantidad() > cantidadDisponible) {
                throw new RuntimeException("La cantidad a devolver (" + detalleDTO.getCantidad() + 
                    ") excede la cantidad disponible (" + cantidadDisponible + 
                    ") en el orden item #" + detalleDTO.getOrdenItemId());
            }

            // Crear detalle
            ReembolsoVentaDetalle detalle = new ReembolsoVentaDetalle();
            detalle.setReembolsoVenta(reembolso);
            detalle.setOrdenItemOriginal(ordenItemOriginal);
            detalle.setProducto(ordenItemOriginal.getProducto());
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario() != null ? 
                detalleDTO.getPrecioUnitario() : ordenItemOriginal.getPrecioUnitario());
            detalle.calcularTotalLinea();

            reembolso.agregarDetalle(detalle);
        }

        // Calcular total
        reembolso.calcularTotal();

        // Guardar
        ReembolsoVenta guardado = reembolsoVentaRepository.save(reembolso);
        return new ReembolsoVentaResponseDTO(guardado);
    }

    @Transactional
    public Map<String, Object> procesarReembolso(Long id) {
        ReembolsoVenta reembolso = reembolsoVentaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Reembolso de venta no encontrado con ID: " + id));

        if (reembolso.getProcesado()) {
            throw new RuntimeException("El reembolso ya está procesado");
        }

        if (reembolso.getEstado() == ReembolsoVenta.EstadoReembolso.ANULADO) {
            throw new RuntimeException("No se puede procesar un reembolso anulado");
        }

        // Obtener sede de la orden
        Sede sede = reembolso.getSede();

        int productosActualizados = 0;

        // Procesar cada detalle (sumar al inventario)
        for (ReembolsoVentaDetalle detalle : reembolso.getDetalles()) {
            Producto producto = detalle.getProducto();
            Integer cantidad = detalle.getCantidad();

            // Sumar al inventario
            Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(
                    producto.getId(), sede.getId());

            if (inventarioOpt.isPresent()) {
                Inventario inventario = inventarioOpt.get();
                inventario.setCantidad(inventario.getCantidad() + cantidad);
                inventarioService.actualizar(inventario.getId(), inventario);
            } else {
                // Si no existe inventario, crear uno
                Inventario nuevoInventario = new Inventario();
                nuevoInventario.setProducto(producto);
                nuevoInventario.setSede(sede);
                nuevoInventario.setCantidad(cantidad);
                inventarioService.guardar(nuevoInventario);
            }
            productosActualizados++;
        }

        // Ajustar crédito si la venta fue a crédito
        boolean creditoAjustado = false;
        Double saldoCreditoAnterior = null;
        Double saldoCreditoNuevo = null;
        boolean creditoCerrado = false;

        if (reembolso.getOrdenOriginal().isCredito() && 
            reembolso.getOrdenOriginal().getCreditoDetalle() != null) {
            
            Credito credito = reembolso.getOrdenOriginal().getCreditoDetalle();
            saldoCreditoAnterior = credito.getSaldoPendiente();
            creditoAjustado = true;
            
            // Reducir el total del crédito (equivalente a reducir la deuda)
            Double nuevoTotalCredito = credito.getTotalCredito() - reembolso.getTotalReembolso();
            credito.setTotalCredito(Math.max(0.0, nuevoTotalCredito)); // No puede ser negativo
            credito.actualizarSaldo();
            
            saldoCreditoNuevo = credito.getSaldoPendiente();
            
            // Si el saldo llega a 0, cerrar el crédito
            if (credito.getSaldoPendiente() <= 0 && credito.getEstado() == Credito.EstadoCredito.ABIERTO) {
                credito.setEstado(Credito.EstadoCredito.CERRADO);
                credito.setFechaCierre(LocalDate.now());
                credito.setSaldoPendiente(0.0);
                creditoCerrado = true;
            }
            
            // Guardar cambios del crédito
            creditoRepository.save(credito);
        }

        // Marcar como procesado
        reembolso.setProcesado(true);
        reembolso.setEstado(ReembolsoVenta.EstadoReembolso.PROCESADO);
        reembolsoVentaRepository.save(reembolso);

        // Construir respuesta
        Map<String, Object> respuesta = new java.util.HashMap<>();
        respuesta.put("mensaje", "Reembolso procesado exitosamente");
        respuesta.put("reembolsoId", id);
        respuesta.put("totalReembolso", reembolso.getTotalReembolso());
        respuesta.put("productosActualizados", productosActualizados);
        respuesta.put("inventarioActualizado", true);
        respuesta.put("creditoAjustado", creditoAjustado);
        respuesta.put("saldoCreditoAnterior", saldoCreditoAnterior);
        respuesta.put("saldoCreditoNuevo", saldoCreditoNuevo);
        if (creditoAjustado) {
            respuesta.put("creditoCerrado", creditoCerrado);
        }

        return respuesta;
    }

    @Transactional
    public void anularReembolso(Long id) {
        ReembolsoVenta reembolso = reembolsoVentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reembolso de venta no encontrado con ID: " + id));

        if (reembolso.getProcesado()) {
            throw new RuntimeException("No se puede anular un reembolso ya procesado. Debe crear un reembolso inverso.");
        }

        reembolso.setEstado(ReembolsoVenta.EstadoReembolso.ANULADO);
        reembolsoVentaRepository.save(reembolso);
    }

    @Transactional
    public void eliminarReembolso(Long id) {
        ReembolsoVenta reembolso = reembolsoVentaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reembolso de venta no encontrado con ID: " + id));

        if (reembolso.getProcesado()) {
            throw new RuntimeException("No se puede eliminar un reembolso procesado");
        }

        reembolsoVentaRepository.delete(reembolso);
    }

    private Integer calcularCantidadYaReembolsada(Long ordenItemId) {
        List<ReembolsoVentaDetalle> reembolsos = reembolsoVentaDetalleRepository
                .findByOrdenItemOriginalId(ordenItemId);

        return reembolsos.stream()
                .filter(d -> d.getReembolsoVenta().getProcesado() && 
                            d.getReembolsoVenta().getEstado() != ReembolsoVenta.EstadoReembolso.ANULADO)
                .mapToInt(ReembolsoVentaDetalle::getCantidad)
                .sum();
    }
}

