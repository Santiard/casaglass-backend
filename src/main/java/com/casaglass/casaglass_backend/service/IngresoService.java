package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import com.casaglass.casaglass_backend.dto.IngresoCreateDTO;
import com.casaglass.casaglass_backend.dto.IngresoTablaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class IngresoService {

    private static final Logger log = LoggerFactory.getLogger(IngresoService.class);

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
    public List<IngresoTablaDTO> listarIngresos() {
        return ingresoRepository.findAllWithProveedores().stream()
                .map(this::convertirAIngresoTablaDTO)
                .collect(Collectors.toList());
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
            
            // Convertir a DTOs para evitar LazyInitializationException
            List<IngresoTablaDTO> contenido = ingresosPagina.stream()
                    .map(this::convertirAIngresoTablaDTO)
                    .collect(Collectors.toList());
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa convertida a DTOs
        return ingresos.stream()
                .map(this::convertirAIngresoTablaDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 🔄 CONVERSOR: Ingreso Entity → IngresoTablaDTO optimizado
     * Extrae solo los campos necesarios para la tabla
     * NO incluye detalles para evitar LazyInitializationException
     */
    private IngresoTablaDTO convertirAIngresoTablaDTO(Ingreso ingreso) {
        IngresoTablaDTO dto = new IngresoTablaDTO();
        
        // 📝 CAMPOS PRINCIPALES DEL INGRESO
        dto.setId(ingreso.getId());
        dto.setFecha(ingreso.getFecha());
        dto.setNumeroFactura(ingreso.getNumeroFactura());
        dto.setObservaciones(ingreso.getObservaciones());
        dto.setTotalCosto(ingreso.getTotalCosto());
        dto.setProcesado(ingreso.getProcesado());
        
        // � CANTIDAD TOTAL (suma de cantidades de detalles)
        Integer cantidadTotal = ingresoRepository.calcularCantidadTotal(ingreso.getId());
        dto.setCantidadTotal(cantidadTotal != null ? cantidadTotal : 0);
        
        // �👤 PROVEEDOR SIMPLIFICADO
        if (ingreso.getProveedor() != null) {
            // Inicializar el proxy lazy accediendo a sus propiedades
            Proveedor proveedor = ingreso.getProveedor();
            IngresoTablaDTO.ProveedorTablaDTO proveedorDTO = new IngresoTablaDTO.ProveedorTablaDTO(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getNit()
            );
            dto.setProveedor(proveedorDTO);
        }
        
        return dto;
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
    public List<IngresoTablaDTO> listarIngresosPorSede(Long sedeId) {
        // TODO: Si se agrega campo sede a Ingreso, filtrar por sedeId
        // Por ahora, retornamos todos los ingresos ya que todos se procesan en la sede principal
        return ingresoRepository.findAllWithProveedores().stream()
                .map(this::convertirAIngresoTablaDTO)
                .collect(Collectors.toList());
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
        // ...existing code...
        
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
        // Usar consulta con FETCH para evitar proxies lazy
        Ingreso ingresoExistente = ingresoRepository.findByIdWithDetalles(id);
        if (ingresoExistente == null) {
            throw new RuntimeException("Ingreso no encontrado");
        }

        System.out.println("📋 Ingreso encontrado - Procesado: " + ingresoExistente.getProcesado());

        // Si el ingreso ya fue procesado, revertir el inventario antes de aplicar los nuevos detalles
        if (ingresoExistente.getProcesado()) {
            System.out.println("♻️ Revirtiendo inventario por edición de ingreso procesado");
            Sede sedePrincipal = sedeRepository.findById(SEDE_PRINCIPAL_ID)
                .orElseThrow(() -> new RuntimeException("Sede principal no encontrada (ID: " + SEDE_PRINCIPAL_ID + ")"));
            for (IngresoDetalle detalleOriginal : ingresoExistente.getDetalles()) {
                Producto producto = detalleOriginal.getProducto();
                Double cantidadOriginal = detalleOriginal.getCantidad();
                if (producto != null && producto.getId() != null) {
                    Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(producto.getId(), sedePrincipal.getId());
                    if (inventarioOpt.isPresent()) {
                        Inventario inventario = inventarioOpt.get();
                        double nuevaCantidad = inventario.getCantidad() - cantidadOriginal;
                        if (nuevaCantidad < 0) nuevaCantidad = 0.0;
                        inventario.setCantidad(nuevaCantidad);
                        inventarioService.actualizar(inventario.getId(), inventario);
                    }
                }
            }
        }

        // Actualizar campos básicos
        ingresoExistente.setFecha(ingresoActualizado.getFecha());
        ingresoExistente.setNumeroFactura(ingresoActualizado.getNumeroFactura());
        ingresoExistente.setObservaciones(ingresoActualizado.getObservaciones());

        // Buscar entidad gestionada para proveedor
        if (ingresoActualizado.getProveedor() != null && ingresoActualizado.getProveedor().getId() != null) {
            Proveedor proveedor = proveedorRepository.findById(ingresoActualizado.getProveedor().getId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + ingresoActualizado.getProveedor().getId()));
            ingresoExistente.setProveedor(proveedor);
        } else {
            throw new RuntimeException("El proveedor es obligatorio");
        }

        // Actualizar detalles - limpiar y agregar nuevos
        ingresoExistente.getDetalles().clear();
        for (IngresoDetalle detalleActualizado : ingresoActualizado.getDetalles()) {
            IngresoDetalle nuevoDetalle = new IngresoDetalle();
            if (detalleActualizado.getProducto() != null && detalleActualizado.getProducto().getId() != null) {
                Producto producto = productoRepository.findById(detalleActualizado.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + detalleActualizado.getProducto().getId()));
                nuevoDetalle.setProducto(producto);
            } else {
                throw new RuntimeException("Todos los detalles deben tener un producto válido");
            }
            nuevoDetalle.setCantidad(detalleActualizado.getCantidad());
            nuevoDetalle.setCostoUnitario(detalleActualizado.getCostoUnitario());
            nuevoDetalle.setCostoUnitarioPonderado(detalleActualizado.getCostoUnitarioPonderado());
            nuevoDetalle.setIngreso(ingresoExistente);
            nuevoDetalle.setTotalLinea(detalleActualizado.getCantidad() * detalleActualizado.getCostoUnitario());
            ingresoExistente.getDetalles().add(nuevoDetalle);
        }

        ingresoExistente.calcularTotal();
        Ingreso resultado = ingresoRepository.save(ingresoExistente);

        // Si el ingreso estaba procesado, volver a procesar el inventario con los nuevos detalles
        if (ingresoExistente.getProcesado()) {
            System.out.println("🔁 Reaplicando inventario por edición de ingreso procesado");
            Sede sedePrincipal = sedeRepository.findById(SEDE_PRINCIPAL_ID)
                .orElseThrow(() -> new RuntimeException("Sede principal no encontrada (ID: " + SEDE_PRINCIPAL_ID + ")"));
            for (IngresoDetalle detalleNuevo : ingresoExistente.getDetalles()) {
                Producto producto = detalleNuevo.getProducto();
                Double cantidadNueva = detalleNuevo.getCantidad();
                if (producto != null && producto.getId() != null) {
                    Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(producto.getId(), sedePrincipal.getId());
                    if (inventarioOpt.isPresent()) {
                        Inventario inventario = inventarioOpt.get();
                        double nuevaCantidad = inventario.getCantidad() + cantidadNueva;
                        inventario.setCantidad(nuevaCantidad);
                        inventarioService.actualizar(inventario.getId(), inventario);
                    } else {
                        // Si no existe inventario, crearlo
                        Inventario nuevoInventario = new Inventario();
                        nuevoInventario.setProducto(producto);
                        nuevoInventario.setSede(sedePrincipal);
                        nuevoInventario.setCantidad(cantidadNueva);
                        inventarioService.guardar(nuevoInventario);
                    }
                }
            }
        }

        // Forzar inicialización del proveedor para evitar proxy lazy en serialización
        if (resultado.getProveedor() != null) {
            resultado.getProveedor().getNombre();
        }
        return resultado;
    }

    /**
     * ✅ ELIMINAR INGRESO (con soporte para ingresos procesados)
     * 
     * Si el ingreso está procesado, primero revierte el inventario automáticamente
     * antes de eliminarlo. Esto permite corregir errores de duplicación.
     * 
     * @param id ID del ingreso a eliminar
     */
    public void eliminarIngreso(Long id) {
        // Usar consulta con FETCH para cargar detalles si es necesario
        Ingreso ingreso = ingresoRepository.findByIdWithDetalles(id);
        if (ingreso == null) {
            throw new RuntimeException("Ingreso no encontrado");
        }

        // Si el ingreso está procesado, desprocesarlo primero (revertir inventario)
        if (ingreso.getProcesado()) {
            log.info("⚠️ Eliminando ingreso procesado ID: {}. Revirtiendo inventario automáticamente...", id);
            desprocesarIngreso(ingreso);
        }

        // Eliminar el ingreso (los detalles se eliminan en cascada)
        ingresoRepository.deleteById(id);
        log.info("✅ Ingreso ID: {} eliminado correctamente", id);
    }

    /**
     * 🔄 DESPROCESAR INGRESO (revertir cambios en inventario)
     * 
     * Revierte los cambios que se hicieron al procesar el ingreso:
     * - Resta las cantidades del inventario
     * 
     * NOTA: El costo del producto NO se revierte porque es un promedio ponderado
     * calculado desde múltiples ingresos. Si se necesita recalcular el costo,
     * se debe hacer manualmente o mediante un proceso de recálculo global.
     * 
     * @param ingreso Ingreso a desprocesar
     */
    public void desprocesarIngreso(Ingreso ingreso) {
        if (!ingreso.getProcesado()) {
            log.warn("⚠️ Intento de desprocesar un ingreso que no está procesado. ID: {}", ingreso.getId());
            return;
        }

        // Obtener la sede principal
        Sede sedePrincipal = sedeRepository.findById(SEDE_PRINCIPAL_ID)
                .orElseThrow(() -> new RuntimeException("Sede principal no encontrada (ID: " + SEDE_PRINCIPAL_ID + "). Verifique que exista una sede con ID 1 en la base de datos."));

        // Revertir cada detalle del ingreso
        for (IngresoDetalle detalle : ingreso.getDetalles()) {
            Producto producto = detalle.getProducto();
            Double cantidadIngresada = detalle.getCantidad();

            if (producto == null || producto.getId() == null) {
                log.warn("⚠️ Detalle de ingreso sin producto válido. Se omite la reversión para este detalle.");
                continue;
            }

            // Buscar el inventario para este producto en la sede principal
            Optional<Inventario> inventarioOpt = inventarioService
                    .obtenerPorProductoYSede(producto.getId(), sedePrincipal.getId());

            if (inventarioOpt.isPresent()) {
                Inventario inventario = inventarioOpt.get();
                // Restar la cantidad que se sumó al procesar
                double nuevaCantidad = inventario.getCantidad() - cantidadIngresada;
                
                // Proteger contra cantidades negativas (no debería pasar, pero por seguridad)
                if (nuevaCantidad < 0) {
                    log.warn("⚠️ Al revertir ingreso, el inventario del producto ID: {} quedaría negativo (actual: {}, a restar: {}). Se establece en 0.", 
                            producto.getId(), inventario.getCantidad(), cantidadIngresada);
                    nuevaCantidad = 0.0;
                }
                
                inventario.setCantidad(nuevaCantidad);
                inventarioService.actualizar(inventario.getId(), inventario);
                log.debug("✅ Revertido inventario - Producto ID: {}, Cantidad restada: {}, Nueva cantidad: {}", 
                        producto.getId(), cantidadIngresada, nuevaCantidad);
            } else {
                log.warn("⚠️ No se encontró inventario para el producto ID: {} en la sede principal. No se puede revertir.", producto.getId());
            }

            // NOTA: No revertimos el costo del producto porque:
            // 1. El costo es un promedio ponderado calculado desde múltiples ingresos
            // 2. No tenemos el costo anterior guardado
            // 3. Recalcular el costo requeriría consultar todos los ingresos procesados del producto
            // Si se necesita recalcular el costo, se debe hacer manualmente o mediante un proceso de recálculo global
        }

        // Marcar el ingreso como no procesado
        ingreso.setProcesado(false);
        ingresoRepository.save(ingreso);
        log.info("✅ Ingreso ID: {} desprocesado correctamente", ingreso.getId());
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
            Double cantidadIngresada = detalle.getCantidad();

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
                try {
                    producto.setCosto(costoPonderado);
                    productoRepository.save(producto);
                    // ...existing code...
                } catch (jakarta.persistence.OptimisticLockException e) {
                    // ...existing code...
                    throw new RuntimeException(
                        String.format("⚠️ Otro usuario modificó el producto ID %d. Por favor, intente nuevamente.", producto.getId())
                    );
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    // ...existing code...
                    throw new RuntimeException(
                        String.format("⚠️ Otro usuario modificó el producto ID %d. Por favor, intente nuevamente.", producto.getId())
                    );
                }
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