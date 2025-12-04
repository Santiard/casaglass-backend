package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ReembolsoIngresoCreateDTO;
import com.casaglass.casaglass_backend.dto.ReembolsoIngresoResponseDTO;
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
public class ReembolsoIngresoService {

    private final ReembolsoIngresoRepository reembolsoIngresoRepository;
    private final ReembolsoIngresoDetalleRepository reembolsoIngresoDetalleRepository;
    private final IngresoRepository ingresoRepository;
    private final IngresoDetalleRepository ingresoDetalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final SedeRepository sedeRepository;

    @Autowired
    public ReembolsoIngresoService(
            ReembolsoIngresoRepository reembolsoIngresoRepository,
            ReembolsoIngresoDetalleRepository reembolsoIngresoDetalleRepository,
            IngresoRepository ingresoRepository,
            IngresoDetalleRepository ingresoDetalleRepository,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository,
            InventarioService inventarioService,
            SedeRepository sedeRepository) {
        this.reembolsoIngresoRepository = reembolsoIngresoRepository;
        this.reembolsoIngresoDetalleRepository = reembolsoIngresoDetalleRepository;
        this.ingresoRepository = ingresoRepository;
        this.ingresoDetalleRepository = ingresoDetalleRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.sedeRepository = sedeRepository;
    }

    @Transactional(readOnly = true)
    public List<ReembolsoIngresoResponseDTO> listarReembolsos() {
        return reembolsoIngresoRepository.findAllWithDetalles().stream()
                .map(ReembolsoIngresoResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Listar reembolsos de ingreso por sede
     * Filtra por la sede del ingreso relacionado
     */
    @Transactional(readOnly = true)
    public List<ReembolsoIngresoResponseDTO> listarReembolsosPorSede(Long sedeId) {
        // Los ingresos actualmente no tienen campo sede, pero se procesan en la sede principal
        // Por ahora, retornamos todos los reembolsos
        // TODO: Si se agrega campo sede a Ingreso, filtrar por reembolso.ingresoOriginal.sede.id = sedeId
        return reembolsoIngresoRepository.findAllWithDetalles().stream()
                .filter(reembolso -> {
                    // Por ahora, todos los ingresos se procesan en la sede principal (ID 1)
                    // Si se agrega campo sede, cambiar esta lógica
                    return true; // Retornar todos por ahora
                })
                .map(ReembolsoIngresoResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ReembolsoIngresoResponseDTO> obtenerPorId(Long id) {
        return reembolsoIngresoRepository.findByIdWithDetalles(id)
                .map(ReembolsoIngresoResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public List<ReembolsoIngresoResponseDTO> obtenerReembolsosPorIngreso(Long ingresoId) {
        return reembolsoIngresoRepository.findByIngresoOriginalId(ingresoId).stream()
                .map(ReembolsoIngresoResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReembolsoIngresoResponseDTO crearReembolso(ReembolsoIngresoCreateDTO dto) {
        // Validar ingreso original
        Ingreso ingresoOriginal = ingresoRepository.findById(dto.getIngresoId())
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado con ID: " + dto.getIngresoId()));

        // Crear reembolso
        ReembolsoIngreso reembolso = new ReembolsoIngreso();
        reembolso.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        reembolso.setIngresoOriginal(ingresoOriginal);
        reembolso.setProveedor(ingresoOriginal.getProveedor());
        reembolso.setNumeroFacturaDevolucion(dto.getNumeroFacturaDevolucion());
        reembolso.setMotivo(dto.getMotivo());
        reembolso.setEstado(ReembolsoIngreso.EstadoReembolso.PENDIENTE);
        reembolso.setProcesado(false);

        // Procesar detalles
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new RuntimeException("El reembolso debe tener al menos un detalle");
        }

        for (ReembolsoIngresoCreateDTO.ReembolsoIngresoDetalleDTO detalleDTO : dto.getDetalles()) {
            // Validar detalle original
            IngresoDetalle ingresoDetalleOriginal = ingresoDetalleRepository.findById(detalleDTO.getIngresoDetalleId())
                    .orElseThrow(() -> new RuntimeException("Ingreso detalle no encontrado con ID: " + detalleDTO.getIngresoDetalleId()));

            // Validar que pertenece al ingreso correcto
            if (!ingresoDetalleOriginal.getIngreso().getId().equals(ingresoOriginal.getId())) {
                throw new RuntimeException("El detalle #" + detalleDTO.getIngresoDetalleId() + " no pertenece al ingreso #" + dto.getIngresoId());
            }

            // Validar cantidad
            if (detalleDTO.getCantidad() == null || detalleDTO.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a 0");
            }

            // Validar que no exceda la cantidad recibida
            Integer cantidadYaReembolsada = calcularCantidadYaReembolsada(ingresoDetalleOriginal.getId());
            Integer cantidadDisponible = ingresoDetalleOriginal.getCantidad() - cantidadYaReembolsada;
            
            if (detalleDTO.getCantidad() > cantidadDisponible) {
                throw new RuntimeException("La cantidad a devolver (" + detalleDTO.getCantidad() + 
                    ") excede la cantidad disponible (" + cantidadDisponible + 
                    ") en el ingreso detalle #" + detalleDTO.getIngresoDetalleId());
            }

            // Crear detalle
            ReembolsoIngresoDetalle detalle = new ReembolsoIngresoDetalle();
            detalle.setReembolsoIngreso(reembolso);
            detalle.setIngresoDetalleOriginal(ingresoDetalleOriginal);
            detalle.setProducto(ingresoDetalleOriginal.getProducto());
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setCostoUnitario(detalleDTO.getCostoUnitario() != null ? 
                detalleDTO.getCostoUnitario() : ingresoDetalleOriginal.getCostoUnitario());
            detalle.calcularTotalLinea();

            reembolso.agregarDetalle(detalle);
        }

        // Calcular total
        reembolso.calcularTotal();

        // Guardar
        ReembolsoIngreso guardado = reembolsoIngresoRepository.save(reembolso);
        return new ReembolsoIngresoResponseDTO(guardado);
    }

    @Transactional
    public Map<String, Object> procesarReembolso(Long id) {
        ReembolsoIngreso reembolso = reembolsoIngresoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Reembolso de ingreso no encontrado con ID: " + id));

        if (reembolso.getProcesado()) {
            throw new RuntimeException("El reembolso ya está procesado");
        }

        if (reembolso.getEstado() == ReembolsoIngreso.EstadoReembolso.ANULADO) {
            throw new RuntimeException("No se puede procesar un reembolso anulado");
        }

        // Obtener sede principal (donde están los productos)
        Sede sedePrincipal = sedeRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Sede principal no encontrada"));

        int productosActualizados = 0;

        // Procesar cada detalle
        for (ReembolsoIngresoDetalle detalle : reembolso.getDetalles()) {
            Producto producto = detalle.getProducto();
            Integer cantidad = detalle.getCantidad();

            // Restar del inventario
            Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(
                    producto.getId(), sedePrincipal.getId());

            if (inventarioOpt.isPresent()) {
                Inventario inventario = inventarioOpt.get();
                inventario.setCantidad(inventario.getCantidad() - cantidad);
                inventarioService.actualizar(inventario.getId(), inventario);
            } else {
                // Si no existe inventario, crear uno con cantidad negativa
                Inventario nuevoInventario = new Inventario();
                nuevoInventario.setProducto(producto);
                nuevoInventario.setSede(sedePrincipal);
                nuevoInventario.setCantidad(-cantidad);
                inventarioService.guardar(nuevoInventario);
            }
            productosActualizados++;
        }

        // Marcar como procesado
        reembolso.setProcesado(true);
        reembolso.setEstado(ReembolsoIngreso.EstadoReembolso.PROCESADO);
        reembolsoIngresoRepository.save(reembolso);

        // Retornar información completa
        return Map.of(
                "mensaje", "Reembolso procesado exitosamente",
                "reembolsoId", id,
                "totalReembolso", reembolso.getTotalReembolso(),
                "productosActualizados", productosActualizados,
                "inventarioActualizado", true
        );
    }

    @Transactional
    public void anularReembolso(Long id) {
        ReembolsoIngreso reembolso = reembolsoIngresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reembolso de ingreso no encontrado con ID: " + id));

        if (reembolso.getProcesado()) {
            throw new RuntimeException("No se puede anular un reembolso ya procesado. Debe crear un reembolso inverso.");
        }

        reembolso.setEstado(ReembolsoIngreso.EstadoReembolso.ANULADO);
        reembolsoIngresoRepository.save(reembolso);
    }

    @Transactional
    public void eliminarReembolso(Long id) {
        ReembolsoIngreso reembolso = reembolsoIngresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reembolso de ingreso no encontrado con ID: " + id));

        if (reembolso.getProcesado()) {
            throw new RuntimeException("No se puede eliminar un reembolso procesado");
        }

        reembolsoIngresoRepository.delete(reembolso);
    }

    private Integer calcularCantidadYaReembolsada(Long ingresoDetalleId) {
        List<ReembolsoIngresoDetalle> reembolsos = reembolsoIngresoDetalleRepository
                .findByIngresoDetalleOriginalId(ingresoDetalleId);

        return reembolsos.stream()
                .filter(d -> d.getReembolsoIngreso().getProcesado() && 
                            d.getReembolsoIngreso().getEstado() != ReembolsoIngreso.EstadoReembolso.ANULADO)
                .mapToInt(ReembolsoIngresoDetalle::getCantidad)
                .sum();
    }
}

