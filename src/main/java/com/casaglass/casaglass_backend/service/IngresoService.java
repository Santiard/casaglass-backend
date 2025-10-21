package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
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
    public List<Ingreso> obtenerIngresosPorFecha(LocalDate fechaInicio, LocalDate fechaFin) {
        return ingresoRepository.findByFechaBetweenOrderByFechaDesc(fechaInicio, fechaFin);
    }

    @Transactional(readOnly = true)
    public List<Ingreso> obtenerIngresosNoProcesados() {
        return ingresoRepository.findByProcesadoFalseOrderByFechaAsc();
    }

    public Ingreso guardarIngreso(Ingreso ingreso) {
        // Establecer fecha si no está definida
        if (ingreso.getFecha() == null) {
            ingreso.setFecha(LocalDate.now());
        }

        // ARREGLO: Buscar entidad gestionada para proveedor
        if (ingreso.getProveedor() != null && ingreso.getProveedor().getId() != null) {
            Proveedor proveedor = proveedorRepository.findById(ingreso.getProveedor().getId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + ingreso.getProveedor().getId()));
            ingreso.setProveedor(proveedor);
        } else {
            throw new RuntimeException("El proveedor es obligatorio");
        }

        // ARREGLO: Buscar entidades gestionadas para productos en detalles
        for (IngresoDetalle detalle : ingreso.getDetalles()) {
            if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
                Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + detalle.getProducto().getId()));
                detalle.setProducto(producto);
            } else {
                throw new RuntimeException("Todos los detalles deben tener un producto válido");
            }
            detalle.setIngreso(ingreso);
        }

        // Calcular totales
        ingreso.calcularTotal();
        
        // Guardar el ingreso
        Ingreso ingresoGuardado = ingresoRepository.save(ingreso);

        // Procesar automáticamente el inventario
        procesarInventario(ingresoGuardado);

        return ingresoGuardado;
    }

    public Ingreso actualizarIngreso(Long id, Ingreso ingresoActualizado) {
        System.out.println("🔧 Servicio - Actualizando ingreso ID: " + id);
        
        // 🔧 ARREGLO: Usar consulta con FETCH para evitar proxies lazy
        Ingreso ingresoExistente = ingresoRepository.findByIdWithDetalles(id);
        if (ingresoExistente == null) {
            throw new RuntimeException("Ingreso no encontrado");
        }

        System.out.println("📋 Ingreso encontrado - Procesado: " + ingresoExistente.getProcesado());

        // Si ya fue procesado, no permitir cambios que afecten el inventario
        if (ingresoExistente.getProcesado()) {
            throw new RuntimeException("No se puede modificar un ingreso ya procesado");
        }

        // Actualizar campos básicos
        ingresoExistente.setFecha(ingresoActualizado.getFecha());
        ingresoExistente.setNumeroFactura(ingresoActualizado.getNumeroFactura());
        ingresoExistente.setObservaciones(ingresoActualizado.getObservaciones());

        // ARREGLO: Buscar entidad gestionada para proveedor
        if (ingresoActualizado.getProveedor() != null && ingresoActualizado.getProveedor().getId() != null) {
            Proveedor proveedor = proveedorRepository.findById(ingresoActualizado.getProveedor().getId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + ingresoActualizado.getProveedor().getId()));
            ingresoExistente.setProveedor(proveedor);
        } else {
            throw new RuntimeException("El proveedor es obligatorio");
        }

        // Actualizar detalles - MANEJO CORRECTO DE ENTIDADES
        ingresoExistente.getDetalles().clear();
        for (IngresoDetalle detalleActualizado : ingresoActualizado.getDetalles()) {
            // Crear nuevo detalle para evitar problemas de estado de entidad
            IngresoDetalle nuevoDetalle = new IngresoDetalle();
            
            // ARREGLO: Buscar entidad gestionada para producto
            if (detalleActualizado.getProducto() != null && detalleActualizado.getProducto().getId() != null) {
                Producto producto = productoRepository.findById(detalleActualizado.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + detalleActualizado.getProducto().getId()));
                nuevoDetalle.setProducto(producto);
            } else {
                throw new RuntimeException("Todos los detalles deben tener un producto válido");
            }
            
            // Copiar valores básicos
            nuevoDetalle.setCantidad(detalleActualizado.getCantidad());
            nuevoDetalle.setCostoUnitario(detalleActualizado.getCostoUnitario());
            nuevoDetalle.setIngreso(ingresoExistente);
            
            // Calcular total de línea manualmente
            nuevoDetalle.setTotalLinea(detalleActualizado.getCantidad() * detalleActualizado.getCostoUnitario());
            
            ingresoExistente.getDetalles().add(nuevoDetalle);
        }

        System.out.println("📊 Calculando total...");
        ingresoExistente.calcularTotal();

        System.out.println("💾 Guardando ingreso actualizado...");
        Ingreso resultado = ingresoRepository.save(ingresoExistente);
        System.out.println("✅ Servicio - Ingreso guardado exitosamente ID: " + resultado.getId());
        
        // 🔧 ARREGLO: Forzar inicialización del proveedor para evitar proxy lazy en serialización
        if (resultado.getProveedor() != null) {
            resultado.getProveedor().getNombre(); // Acceder a una propiedad para inicializar el proxy
        }
        
        return resultado;
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
                .orElseThrow(() -> new RuntimeException("Sede principal no encontrada (ID: " + SEDE_PRINCIPAL_ID + "). Verifique que exista una sede con ID 1 en la base de datos."));

        // Procesar cada detalle del ingreso
        for (IngresoDetalle detalle : ingreso.getDetalles()) {
            Producto producto = detalle.getProducto();
            Integer cantidadIngresada = detalle.getCantidad();

            if (producto == null || producto.getId() == null) {
                throw new RuntimeException("Detalle de ingreso sin producto válido");
            }

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

    /**
     * Marca un ingreso como procesado sin actualizar el inventario
     */
    public Ingreso marcarComoProcesado(Long ingresoId) {
        Ingreso ingreso = ingresoRepository.findById(ingresoId)
                .orElseThrow(() -> new RuntimeException("Ingreso no encontrado"));

        if (ingreso.getProcesado()) {
            throw new RuntimeException("El ingreso ya está marcado como procesado");
        }

        ingreso.setProcesado(true);
        return ingresoRepository.save(ingreso);
    }
}