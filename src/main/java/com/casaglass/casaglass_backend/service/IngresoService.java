package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class IngresoService {

    private final IngresoRepository ingresoRepository;
    private final IngresoDetalleRepository ingresoDetalleRepository;
    private final InventarioService inventarioService;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    // ID de la sede principal donde llegan todos los ingresos
    private static final Long SEDE_PRINCIPAL_ID = 1L;

    @Autowired
    public IngresoService(IngresoRepository ingresoRepository,
                         IngresoDetalleRepository ingresoDetalleRepository,
                         InventarioService inventarioService,
                         SedeRepository sedeRepository,
                         ProductoRepository productoRepository,
                         ProveedorRepository proveedorRepository) {
        this.ingresoRepository = ingresoRepository;
        this.ingresoDetalleRepository = ingresoDetalleRepository;
        this.inventarioService = inventarioService;
        this.sedeRepository = sedeRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<Ingreso> listarIngresos() {
        return ingresoRepository.findAllWithProveedores();
    }

    @Transactional(readOnly = true)
    public Optional<Ingreso> obtenerIngresoPorId(Long id) {
        return Optional.ofNullable(ingresoRepository.findByIdWithDetalles(id));
    }

    @Transactional(readOnly = true)
    public List<Ingreso> obtenerIngresosPorProveedor(Long proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));
        return ingresoRepository.findByProveedorOrderByFechaDesc(proveedor);
    }

    @Transactional(readOnly = true)
    public List<Ingreso> obtenerIngresosPorFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ingresoRepository.findByFechaBetweenOrderByFechaDesc(fechaInicio, fechaFin);
    }

    @Transactional(readOnly = true)
    public List<Ingreso> obtenerIngresosNoProcesados() {
        return ingresoRepository.findByProcesadoFalseOrderByFechaAsc();
    }

    public Ingreso guardarIngreso(Ingreso ingreso) {
        // Establecer fecha si no está definida
        if (ingreso.getFecha() == null) {
            ingreso.setFecha(LocalDateTime.now());
        }

        // Calcular totales
        ingreso.calcularTotal();
        
        // Establecer referencias bidireccionales
        for (IngresoDetalle detalle : ingreso.getDetalles()) {
            detalle.setIngreso(ingreso);
        }

        // Guardar el ingreso
        Ingreso ingresoGuardado = ingresoRepository.save(ingreso);

        // Procesar automáticamente el inventario
        procesarInventario(ingresoGuardado);

        return ingresoGuardado;
    }

    public Ingreso actualizarIngreso(Long id, Ingreso ingresoActualizado) {
        Ingreso ingresoExistente = ingresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));

        // Si ya fue procesado, no permitir cambios que afecten el inventario
        if (ingresoExistente.getProcesado()) {
            throw new RuntimeException("No se puede modificar un ingreso ya procesado");
        }

        // Actualizar campos
        ingresoExistente.setFecha(ingresoActualizado.getFecha());
        ingresoExistente.setProveedor(ingresoActualizado.getProveedor());
        ingresoExistente.setNumeroFactura(ingresoActualizado.getNumeroFactura());
        ingresoExistente.setObservaciones(ingresoActualizado.getObservaciones());

        // Actualizar detalles
        ingresoExistente.getDetalles().clear();
        for (IngresoDetalle detalle : ingresoActualizado.getDetalles()) {
            detalle.setIngreso(ingresoExistente);
            ingresoExistente.getDetalles().add(detalle);
        }

        ingresoExistente.calcularTotal();

        return ingresoRepository.save(ingresoExistente);
    }

    public void eliminarIngreso(Long id) {
        Ingreso ingreso = ingresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));

        if (ingreso.getProcesado()) {
            throw new RuntimeException("No se puede eliminar un ingreso ya procesado");
        }

        ingresoRepository.deleteById(id);
    }

    /**
     * Procesa el ingreso actualizando automáticamente el inventario en la sede principal
     */
    public void procesarInventario(Ingreso ingreso) {
        if (ingreso.getProcesado()) {
            throw new RuntimeException("El ingreso ya ha sido procesado");
        }

        // Obtener la sede principal
        Sede sedePrincipal = sedeRepository.findById(SEDE_PRINCIPAL_ID)
                .orElseThrow(() -> new RuntimeException("Sede principal no encontrada (ID: " + SEDE_PRINCIPAL_ID + ")"));

        // Procesar cada detalle del ingreso
        for (IngresoDetalle detalle : ingreso.getDetalles()) {
            Producto producto = detalle.getProducto();
            Integer cantidadIngresada = detalle.getCantidad();

            // Buscar o crear registro de inventario para este producto en la sede principal
            Optional<Inventario> inventarioExistente = inventarioService
                    .obtenerPorProductoYSede(producto.getId(), sedePrincipal.getId());

            if (inventarioExistente.isPresent()) {
                // Actualizar cantidad existente
                Inventario inventario = inventarioExistente.get();
                inventario.setCantidad(inventario.getCantidad() + cantidadIngresada);
                inventarioService.actualizar(inventario.getId(), inventario);
            } else {
                // Crear nuevo registro de inventario
                Inventario nuevoInventario = new Inventario();
                nuevoInventario.setProducto(producto);
                nuevoInventario.setSede(sedePrincipal);
                nuevoInventario.setCantidad(cantidadIngresada);
                inventarioService.guardar(nuevoInventario);
            }

            // Actualizar el costo del producto si es diferente
            Double costoActual = producto.getCosto();
            Double nuevoCosto = detalle.getCostoUnitario();
            if (costoActual == null || !costoActual.equals(nuevoCosto)) {
                producto.setCosto(nuevoCosto);
                productoRepository.save(producto);
            }
        }

        // Marcar el ingreso como procesado
        ingreso.setProcesado(true);
        ingresoRepository.save(ingreso);
    }

    /**
     * Reprocesa un ingreso (útil para correcciones)
     */
    public void reprocesarInventario(Long ingresoId) {
        Ingreso ingreso = ingresoRepository.findByIdWithDetalles(ingresoId);
        if (ingreso == null) {
            throw new RuntimeException("Ingreso no encontrado");
        }

        // Marcar como no procesado temporalmente para poder reprocesar
        ingreso.setProcesado(false);
        procesarInventario(ingreso);
    }
}