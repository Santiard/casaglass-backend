package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import com.casaglass.casaglass_backend.dto.IngresoCreateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 🚀 LISTADO DE INGRESOS CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object listarIngresosConFiltros(
            Long proveedorId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Boolean procesado,
            String numeroFactura,
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar fechas
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        
        // Validar y normalizar ordenamiento
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "fecha";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "DESC";
        }
        sortOrder = sortOrder.toUpperCase();
        if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
            sortOrder = "DESC";
        }
        
        // Buscar ingresos con filtros
        List<Ingreso> ingresos = ingresoRepository.buscarConFiltros(
            proveedorId, fechaDesde, fechaHasta, procesado, numeroFactura
        );
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por fecha DESC)
        if (!sortBy.equals("fecha") || !sortOrder.equals("DESC")) {
            ingresos = aplicarOrdenamientoIngresos(ingresos, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = ingresos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, ingresos.size());
            
            if (fromIndex >= ingresos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Ingreso> ingresosPagina = ingresos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(ingresosPagina, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return ingresos;
    }
    
    /**
     * Aplica ordenamiento a la lista de ingresos según sortBy y sortOrder
     */
    private List<Ingreso> aplicarOrdenamientoIngresos(List<Ingreso> ingresos, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                ingresos.sort((a, b) -> {
                    int cmp = a.getFecha().compareTo(b.getFecha());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "numerofactura":
            case "numero_factura":
                ingresos.sort((a, b) -> {
                    String numA = a.getNumeroFactura() != null ? a.getNumeroFactura() : "";
                    String numB = b.getNumeroFactura() != null ? b.getNumeroFactura() : "";
                    int cmp = numA.compareToIgnoreCase(numB);
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "totalcosto":
            case "total_costo":
                ingresos.sort((a, b) -> {
                    int cmp = Double.compare(a.getTotalCosto() != null ? a.getTotalCosto() : 0.0,
                                            b.getTotalCosto() != null ? b.getTotalCosto() : 0.0);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                ingresos.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        }
        
        return ingresos;
    }

    /**
     * Listar ingresos por sede
     * Nota: Los ingresos actualmente no tienen un campo sede directo.
     * Este método retorna todos los ingresos ya que todos se procesan en la sede principal.
     * Si en el futuro se agrega un campo sede a Ingreso, este método deberá ser actualizado.
     */
    @Transactional(readOnly = true)
    public List<Ingreso> listarIngresosPorSede(Long sedeId) {
        // TODO: Si se agrega campo sede a Ingreso, filtrar por sedeId
        // Por ahora, retornamos todos los ingresos ya que todos se procesan en la sede principal
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

    /**
     * Crea un ingreso desde un DTO que contiene solo IDs de proveedor y productos
     */
    public Ingreso crearIngresoDesdeDTO(IngresoCreateDTO ingresoDTO) {
        System.out.println("🔧 Servicio - Creando ingreso desde DTO");
        
        // Crear la entidad Ingreso
        Ingreso ingreso = new Ingreso();
        ingreso.setFecha(ingresoDTO.getFecha() != null ? ingresoDTO.getFecha() : LocalDate.now());
        ingreso.setNumeroFactura(ingresoDTO.getNumeroFactura());
        ingreso.setObservaciones(ingresoDTO.getObservaciones());
        // NO usar totalCosto del DTO directamente, se calculará automáticamente con calcularTotal()
        ingreso.setProcesado(ingresoDTO.getProcesado() != null ? ingresoDTO.getProcesado() : false);

        // Buscar el proveedor completo por ID
        if (ingresoDTO.getProveedor() != null && ingresoDTO.getProveedor().getId() != null) {
            Proveedor proveedorCompleto = proveedorRepository.findById(ingresoDTO.getProveedor().getId())
                .orElseThrow(() -> new RuntimeException("Proveedor con ID " + ingresoDTO.getProveedor().getId() + " no encontrado"));
            ingreso.setProveedor(proveedorCompleto);
        } else {
            throw new RuntimeException("El proveedor es obligatorio");
        }

        // Procesar los detalles de productos
        if (ingresoDTO.getDetalles() != null && !ingresoDTO.getDetalles().isEmpty()) {
            for (IngresoCreateDTO.IngresoDetalleCreateDTO detalleDTO : ingresoDTO.getDetalles()) {
                // Buscar el producto completo por ID
                if (detalleDTO.getProducto() != null && detalleDTO.getProducto().getId() != null) {
                    Producto productoCompleto = productoRepository.findById(detalleDTO.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto con ID " + detalleDTO.getProducto().getId() + " no encontrado"));
                    
                    // Crear el detalle del ingreso
                    IngresoDetalle detalle = new IngresoDetalle();
                    detalle.setProducto(productoCompleto);
                    detalle.setCantidad(detalleDTO.getCantidad());
                    detalle.setCostoUnitario(detalleDTO.getCostoUnitario()); // Costo original (para totalCosto y trazabilidad)
                    detalle.setCostoUnitarioPonderado(detalleDTO.getCostoUnitarioPonderado()); // Costo ponderado calculado por el frontend
                    // totalLinea se calcula automáticamente con @PrePersist usando costoUnitario
                    // Pero si viene del frontend, lo respetamos
                    if (detalleDTO.getTotalLinea() != null) {
                        detalle.setTotalLinea(detalleDTO.getTotalLinea());
                    }
                    detalle.setIngreso(ingreso);
                    
                    ingreso.getDetalles().add(detalle);
                } else {
                    throw new RuntimeException("Todos los detalles deben tener un producto válido");
                }
            }
        } else {
            throw new RuntimeException("El ingreso debe tener al menos un detalle");
        }

        // Calcular totales
        ingreso.calcularTotal();
        
        // Guardar el ingreso
        Ingreso ingresoGuardado = ingresoRepository.save(ingreso);

        // NO procesar automáticamente - el usuario debe hacerlo manualmente
        // procesarInventario(ingresoGuardado);

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
            nuevoDetalle.setCostoUnitario(detalleActualizado.getCostoUnitario()); // Costo original (para totalCosto y trazabilidad)
            nuevoDetalle.setCostoUnitarioPonderado(detalleActualizado.getCostoUnitarioPonderado()); // Costo ponderado calculado por el frontend
            nuevoDetalle.setIngreso(ingresoExistente);
            
            // Calcular total de línea manualmente usando costoUnitario (costo original)
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

            // Actualizar el costo del producto usando costoUnitarioPonderado (calculado por el frontend)
            // El frontend ya calculó el promedio ponderado antes de enviar el ingreso
            Double costoActual = producto.getCosto();
            Double costoPonderado = detalle.getCostoUnitarioPonderado(); // Viene calculado del frontend
            
            if (costoPonderado == null) {
                throw new RuntimeException("El campo costoUnitarioPonderado es obligatorio para el producto ID: " + producto.getId());
            }
            
            // Actualizar el costo del producto con el costoUnitarioPonderado recibido del frontend
            if (costoActual == null || !costoActual.equals(costoPonderado)) {
                producto.setCosto(costoPonderado);
                productoRepository.save(producto);
                System.out.println("✅ Costo del producto actualizado: Producto ID=" + producto.getId() + 
                                 ", Costo anterior=" + costoActual + 
                                 ", Costo nuevo (ponderado del frontend)=" + costoPonderado +
                                 ", Costo original del ingreso=" + detalle.getCostoUnitario());
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