package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.dto.OrdenTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaDTO;
import com.casaglass.casaglass_backend.dto.CreditoTablaDTO;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.ClienteRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
// no need for LocalDateTime/LocalTime
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class OrdenService {

    private final OrdenRepository repo;
    private final ClienteRepository clienteRepository;
    private final SedeRepository sedeRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ProductoRepository productoRepository;
    private final EntityManager entityManager;
    private final InventarioService inventarioService;
    private final CreditoService creditoService;

    public OrdenService(OrdenRepository repo, 
                       ClienteRepository clienteRepository,
                       SedeRepository sedeRepository,
                       TrabajadorRepository trabajadorRepository,
                       ProductoRepository productoRepository,
                       EntityManager entityManager, 
                       InventarioService inventarioService, 
                       CreditoService creditoService) { 
        this.repo = repo; 
        this.clienteRepository = clienteRepository;
        this.sedeRepository = sedeRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.productoRepository = productoRepository;
        this.entityManager = entityManager;
        this.inventarioService = inventarioService;
        this.creditoService = creditoService;
    }

    @Transactional
    public Orden crear(Orden orden) {
        if (orden.getFecha() == null) orden.setFecha(LocalDate.now());

        // Validar que tenga sede asignada
        if (orden.getSede() == null || orden.getSede().getId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria para la orden");
        }

        // Usar referencia ligera para la sede
        orden.setSede(entityManager.getReference(Sede.class, orden.getSede().getId()));

        // Manejar trabajador encargado (opcional)
        if (orden.getTrabajador() != null && orden.getTrabajador().getId() != null) {
            orden.setTrabajador(entityManager.getReference(Trabajador.class, orden.getTrabajador().getId()));
        }

        // GENERACION AUTOMATICA DE NUMERO (THREAD-SAFE)
        // El número se ignora si viene del frontend - siempre se genera automáticamente
        Long numeroGenerado = generarNumeroOrden();
        orden.setNumero(numeroGenerado);

        double subtotal = 0.0;
        if (orden.getItems() != null) {
            for (OrdenItem it : orden.getItems()) {
                it.setOrden(orden); // amarra relación
                Double linea = it.getPrecioUnitario() * it.getCantidad();
                it.setTotalLinea(linea);
                subtotal += linea;

                if ((it.getDescripcion() == null || it.getDescripcion().isBlank())
                        && it.getProducto() != null) {
                    it.setDescripcion(it.getProducto().getNombre());
                }
            }
        }
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal); // impuestos/desc. si aplica más adelante
        
        // Establecer estado activa por defecto
        orden.setEstado(Orden.EstadoOrden.ACTIVA);
        
        // Guardar la orden primero
        Orden ordenGuardada = repo.save(orden);
        
        // Actualizar inventario (restar productos vendidos)
        actualizarInventarioPorVenta(ordenGuardada);
        
        return ordenGuardada;
    }

    /**
     * 🛒 CREAR ORDEN DE VENTA - Método optimizado para ventas reales
     * Valida todos los campos necesarios y maneja inventario automáticamente
     */
    @Transactional
    public Orden crearOrdenVenta(OrdenVentaDTO ventaDTO) {
        // 🔍 VALIDACIONES DE NEGOCIO
        validarDatosVenta(ventaDTO);
        
        // 📝 CREAR ENTIDAD ORDEN
        Orden orden = new Orden();
        orden.setFecha(ventaDTO.getFecha() != null ? ventaDTO.getFecha() : LocalDate.now());
        orden.setObra(ventaDTO.getObra());
        orden.setVenta(ventaDTO.isVenta());
        orden.setCredito(ventaDTO.isCredito());
        orden.setIncluidaEntrega(ventaDTO.isIncluidaEntrega());
        orden.setEstado(Orden.EstadoOrden.ACTIVA);
        
        // 🔗 ESTABLECER RELACIONES (usando referencias ligeras)
        orden.setCliente(clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + ventaDTO.getClienteId())));
        orden.setSede(sedeRepository.findById(ventaDTO.getSedeId())
            .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + ventaDTO.getSedeId())));
        
        if (ventaDTO.getTrabajadorId() != null) {
            orden.setTrabajador(trabajadorRepository.findById(ventaDTO.getTrabajadorId())
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + ventaDTO.getTrabajadorId())));
        }
        
        // 📋 PROCESAR ITEMS DE VENTA
        List<OrdenItem> items = new ArrayList<>();
        double subtotal = 0.0;
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            OrdenItem item = new OrdenItem();
            item.setOrden(orden);
            item.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
            item.setDescripcion(itemDTO.getDescripcion());
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            
            // Calcular total de línea
            double totalLinea = itemDTO.getCantidad() * itemDTO.getPrecioUnitario();
            item.setTotalLinea(totalLinea);
            subtotal += totalLinea;
            
            items.add(item);
        }
        
        orden.setItems(items);
        orden.setSubtotal(Math.round(subtotal * 100.0) / 100.0);
        orden.setTotal(orden.getSubtotal()); // Por ahora sin impuestos/descuentos
        
        // 🔢 GENERAR NÚMERO AUTOMÁTICO
        orden.setNumero(generarNumeroOrden());
        
        // 💾 GUARDAR ORDEN
        Orden ordenGuardada = repo.save(orden);
        
        // 📦 ACTUALIZAR INVENTARIO
        actualizarInventarioPorVenta(ordenGuardada);
        
        return ordenGuardada;
    }

    /**
     * 💳 CREAR ORDEN DE VENTA CON CRÉDITO - Método unificado sin transacciones anidadas
     */
    @Transactional
    public Orden crearOrdenVentaConCredito(OrdenVentaDTO ventaDTO) {
        System.out.println("🔍 DEBUG: Iniciando creación de orden con crédito");
        
        // 🔍 VALIDACIONES DE NEGOCIO
        validarDatosVenta(ventaDTO);
        
        // 📝 CREAR ENTIDAD ORDEN
        Orden orden = new Orden();
        orden.setFecha(ventaDTO.getFecha() != null ? ventaDTO.getFecha() : LocalDate.now());
        orden.setObra(ventaDTO.getObra());
        orden.setVenta(ventaDTO.isVenta());
        orden.setCredito(ventaDTO.isCredito());
        orden.setIncluidaEntrega(ventaDTO.isIncluidaEntrega());
        orden.setEstado(Orden.EstadoOrden.ACTIVA);
        
        // 🔗 ESTABLECER RELACIONES (usando referencias ligeras)
        orden.setCliente(clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + ventaDTO.getClienteId())));
        orden.setSede(sedeRepository.findById(ventaDTO.getSedeId())
            .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + ventaDTO.getSedeId())));
        
        if (ventaDTO.getTrabajadorId() != null) {
            orden.setTrabajador(trabajadorRepository.findById(ventaDTO.getTrabajadorId())
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + ventaDTO.getTrabajadorId())));
        }
        
        // 📋 PROCESAR ITEMS DE VENTA
        List<OrdenItem> items = new ArrayList<>();
        double subtotal = 0.0;
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            OrdenItem item = new OrdenItem();
            item.setOrden(orden);
            item.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
            item.setDescripcion(itemDTO.getDescripcion());
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            
            // Calcular total de línea
            double totalLinea = itemDTO.getCantidad() * itemDTO.getPrecioUnitario();
            item.setTotalLinea(totalLinea);
            subtotal += totalLinea;
            
            items.add(item);
        }
        
        orden.setItems(items);
        orden.setSubtotal(Math.round(subtotal * 100.0) / 100.0);
        orden.setTotal(orden.getSubtotal()); // Por ahora sin impuestos/descuentos
        
        // 🔢 GENERAR NÚMERO AUTOMÁTICO
        orden.setNumero(generarNumeroOrden());
        
        // 💾 GUARDAR ORDEN PRIMERO
        Orden ordenGuardada = repo.save(orden);
        System.out.println("✅ DEBUG: Orden guardada con ID: " + ordenGuardada.getId());
        
        // 💳 CREAR CRÉDITO SI ES NECESARIO (en la misma transacción)
        if (ventaDTO.isCredito()) {
            System.out.println("🔍 DEBUG: Creando crédito para orden " + ordenGuardada.getId());
            creditoService.crearCreditoParaOrden(
                ordenGuardada.getId(), 
                ventaDTO.getClienteId(), 
                ordenGuardada.getTotal()
            );
        }
        
        // 📦 ACTUALIZAR INVENTARIO AL FINAL
        actualizarInventarioPorVenta(ordenGuardada);
        
        return ordenGuardada;
    }

    /**
     * 🔍 VALIDACIONES PARA ORDENES DE VENTA
     */
    private void validarDatosVenta(OrdenVentaDTO ventaDTO) {
        // Cliente obligatorio
        if (ventaDTO.getClienteId() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio para realizar una venta");
        }
        
        // Sede obligatoria
        if (ventaDTO.getSedeId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria para realizar una venta");
        }
        
        // Items obligatorios
        if (ventaDTO.getItems() == null || ventaDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un producto en la venta");
        }
        
        // Validar cada item
        for (int i = 0; i < ventaDTO.getItems().size(); i++) {
            OrdenVentaDTO.OrdenItemVentaDTO item = ventaDTO.getItems().get(i);
            String posicion = "Item " + (i + 1);
            
            if (item.getProductoId() == null) {
                throw new IllegalArgumentException(posicion + ": El producto es obligatorio");
            }
            
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException(posicion + ": La cantidad debe ser mayor a 0");
            }
            
            if (item.getPrecioUnitario() == null || item.getPrecioUnitario() <= 0) {
                throw new IllegalArgumentException(posicion + ": El precio unitario debe ser mayor a 0");
            }
        }
    }

    /**
     * Genera el siguiente número de orden de forma thread-safe
     * Maneja automáticamente la concurrencia entre múltiples usuarios
     */
    private Long generarNumeroOrden() {
        int maxIntentos = 5;
        int intento = 0;
        
        while (intento < maxIntentos) {
            try {
                // Obtener el siguiente número disponible
                Long siguienteNumero = repo.obtenerSiguienteNumero();
                
                // Verificar que no exista (por si hubo concurrencia)
                if (!repo.findByNumero(siguienteNumero).isPresent()) {
                    return siguienteNumero;
                }
                
                // Si existe, incrementar y volver a intentar
                intento++;
                Thread.sleep(10); // Pausa muy breve para evitar colisiones
                
            } catch (Exception e) {
                intento++;
                if (intento >= maxIntentos) {
                    throw new RuntimeException("Error generando número de orden después de " + maxIntentos + " intentos", e);
                }
            }
        }
        
        throw new RuntimeException("No se pudo generar un número de orden único después de " + maxIntentos + " intentos");
    }

    @Transactional(readOnly = true)
    public Optional<Orden> obtenerPorId(Long id) { return repo.findById(id); }

    @Transactional(readOnly = true)
    public Optional<Orden> obtenerPorNumero(Long numero) { return repo.findByNumero(numero); }

    @Transactional(readOnly = true)
    public List<Orden> listar() {
        // Usar findAll() simple ya que las relaciones son EAGER
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorCliente(Long clienteId) { return repo.findByClienteId(clienteId); }

    @Transactional(readOnly = true)
    public List<Orden> listarPorVenta(boolean venta) { return repo.findByVenta(venta); }

    @Transactional(readOnly = true)
    public List<Orden> listarPorCredito(boolean credito) { return repo.findByCredito(credito); }

    /** Órdenes de un día (00:00:00 a 23:59:59.999999999) */
    @Transactional(readOnly = true)
    public List<Orden> listarPorFecha(LocalDate fecha) {
        return repo.findByFechaBetween(fecha, fecha);
    }

    /** Órdenes en rango [desde, hasta] (ambos inclusive por día) */
    @Transactional(readOnly = true)
    public List<Orden> listarPorRangoFechas(LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByFechaBetween(desdeDia, hastaDia);
    }

    // Métodos nuevos para manejar sede
    @Transactional(readOnly = true)
    public List<Orden> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorClienteYSede(Long clienteId, Long sedeId) {
        return repo.findByClienteIdAndSedeId(clienteId, sedeId);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYVenta(Long sedeId, boolean venta) {
        return repo.findBySedeIdAndVenta(sedeId, venta);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYCredito(Long sedeId, boolean credito) {
        return repo.findBySedeIdAndCredito(sedeId, credito);
    }

    /** Órdenes de una sede en un día específico */
    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYFecha(Long sedeId, LocalDate fecha) {
        return repo.findBySedeIdAndFechaBetween(sedeId, fecha, fecha);
    }

    /** Órdenes de una sede en rango [desde, hasta] (ambos inclusive por día) */
    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYRangoFechas(Long sedeId, LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findBySedeIdAndFechaBetween(sedeId, desdeDia, hastaDia);
    }

    // 🆕 MÉTODOS PARA FILTRAR POR TRABAJADOR
    /** Todas las órdenes de un trabajador */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajador(Long trabajadorId) {
        return repo.findByTrabajadorId(trabajadorId);
    }

    /** Órdenes de un trabajador filtradas por venta/cotización */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajadorYVenta(Long trabajadorId, boolean venta) {
        return repo.findByTrabajadorIdAndVenta(trabajadorId, venta);
    }

    /** Órdenes de un trabajador en un día específico */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajadorYFecha(Long trabajadorId, LocalDate fecha) {
        return repo.findByTrabajadorIdAndFechaBetween(trabajadorId, fecha, fecha);
    }

    /** Órdenes de un trabajador en rango [desde, hasta] */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajadorYRangoFechas(Long trabajadorId, LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByTrabajadorIdAndFechaBetween(trabajadorId, desdeDia, hastaDia);
    }

    /** Órdenes de una sede y trabajador específicos */
    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYTrabajador(Long sedeId, Long trabajadorId) {
        return repo.findBySedeIdAndTrabajadorId(sedeId, trabajadorId);
    }

    /**
     * Obtiene el próximo número de orden que se asignará
     * Útil para mostrar en el frontend como referencia (número provisional)
     */
    @Transactional(readOnly = true)
    public Long obtenerProximoNumero() {
        return repo.obtenerSiguienteNumero();
    }

    // 🎯 ================================
    // 🎯 MÉTODOS OPTIMIZADOS PARA TABLA
    // 🎯 ================================

    /**
     * 🚀 LISTADO OPTIMIZADO PARA TABLA DE ÓRDENES
     * Retorna solo los campos necesarios para mejorar rendimiento
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarParaTabla() {
        return repo.findAll().stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO OPTIMIZADO POR SEDE PARA TABLA
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarPorSedeParaTabla(Long sedeId) {
        return repo.findBySedeId(sedeId).stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO OPTIMIZADO POR TRABAJADOR PARA TABLA
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarPorTrabajadorParaTabla(Long trabajadorId) {
        return repo.findByTrabajadorId(trabajadorId).stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO OPTIMIZADO POR CLIENTE PARA TABLA
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarPorClienteParaTabla(Long clienteId) {
        return repo.findByClienteId(clienteId).stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🔄 CONVERSOR: Orden Entity → OrdenTablaDTO optimizado
     * Extrae solo los campos necesarios para la tabla
     */
    private OrdenTablaDTO convertirAOrdenTablaDTO(Orden orden) {
        OrdenTablaDTO dto = new OrdenTablaDTO();
        
        // 📝 CAMPOS PRINCIPALES DE LA ORDEN
        dto.setId(orden.getId());
        dto.setNumero(orden.getNumero());
        dto.setFecha(orden.getFecha());
        dto.setObra(orden.getObra());
        dto.setVenta(orden.isVenta());
        dto.setCredito(orden.isCredito());
        dto.setEstado(orden.getEstado());
        
        // 👤 CLIENTE SIMPLIFICADO
        if (orden.getCliente() != null) {
            dto.setCliente(new OrdenTablaDTO.ClienteTablaDTO(orden.getCliente().getNombre()));
        }
        
        // 👷 TRABAJADOR SIMPLIFICADO  
        if (orden.getTrabajador() != null) {
            dto.setTrabajador(new OrdenTablaDTO.TrabajadorTablaDTO(orden.getTrabajador().getNombre()));
        }
        
        // 🏢 SEDE SIMPLIFICADA
        if (orden.getSede() != null) {
            dto.setSede(new OrdenTablaDTO.SedeTablaDTO(orden.getSede().getNombre()));
        }
        
        // 💳 INFORMACIÓN DEL CRÉDITO (si existe)
        if (orden.getCreditoDetalle() != null) {
            CreditoTablaDTO creditoDTO = new CreditoTablaDTO();
            creditoDTO.setId(orden.getCreditoDetalle().getId());
            creditoDTO.setFechaInicio(orden.getCreditoDetalle().getFechaInicio());
            creditoDTO.setTotalCredito(orden.getCreditoDetalle().getTotalCredito());
            creditoDTO.setSaldoPendiente(orden.getCreditoDetalle().getSaldoPendiente());
            creditoDTO.setEstado(orden.getCreditoDetalle().getEstado());
            creditoDTO.setTotalAbonado(orden.getCreditoDetalle().getTotalAbonado());
            dto.setCreditoDetalle(creditoDTO);
        }
        
        // �📋 ITEMS COMPLETOS (manteniendo detalle como solicitado)
        if (orden.getItems() != null) {
            List<OrdenTablaDTO.OrdenItemTablaDTO> itemsDTO = orden.getItems().stream()
                    .map(this::convertirAOrdenItemTablaDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemsDTO);
        }
        
        return dto;
    }

    /**
     * 🔄 CONVERSOR: OrdenItem Entity → OrdenItemTablaDTO  
     */
    private OrdenTablaDTO.OrdenItemTablaDTO convertirAOrdenItemTablaDTO(OrdenItem item) {
        OrdenTablaDTO.OrdenItemTablaDTO itemDTO = new OrdenTablaDTO.OrdenItemTablaDTO();
        
        itemDTO.setId(item.getId());
        itemDTO.setDescripcion(item.getDescripcion());
        itemDTO.setCantidad(item.getCantidad());
        itemDTO.setPrecioUnitario(item.getPrecioUnitario());
        itemDTO.setTotalLinea(item.getTotalLinea());
        
        // 🎯 PRODUCTO SIMPLIFICADO (solo código y nombre)
        if (item.getProducto() != null) {
            OrdenTablaDTO.ProductoTablaDTO productoDTO = new OrdenTablaDTO.ProductoTablaDTO(
                item.getProducto().getCodigo(),
                item.getProducto().getNombre()
            );
            itemDTO.setProducto(productoDTO);
        }
        
        return itemDTO;
    }

    // 🔄 ================================
    // 🔄 MÉTODO DE ACTUALIZACIÓN
    // 🔄 ================================

    /**
     * 🔄 ACTUALIZAR ORDEN COMPLETA desde tabla
     * Maneja actualización de orden + items (crear, actualizar, eliminar)
     */
    @Transactional
    public OrdenTablaDTO actualizarOrden(Long ordenId, OrdenActualizarDTO dto) {
        // 1️⃣ Buscar orden existente
        Orden orden = repo.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + ordenId));

        // 2️⃣ Actualizar campos básicos de la orden
        orden.setFecha(dto.getFecha());
        orden.setObra(dto.getObra());
        orden.setVenta(dto.isVenta());
        orden.setCredito(dto.isCredito());

        // 3️⃣ Actualizar referencias de entidades
        if (dto.getClienteId() != null) {
            orden.setCliente(entityManager.getReference(Cliente.class, dto.getClienteId()));
        }
        if (dto.getTrabajadorId() != null) {
            orden.setTrabajador(entityManager.getReference(Trabajador.class, dto.getTrabajadorId()));
        }
        if (dto.getSedeId() != null) {
            orden.setSede(entityManager.getReference(Sede.class, dto.getSedeId()));
        }

        // 4️⃣ Manejar items: eliminar, actualizar, crear
        if (dto.getItems() != null) {
            actualizarItemsDeOrden(orden, dto.getItems());
        }

        // 5️⃣ Guardar orden actualizada
        Orden ordenActualizada = repo.save(orden);

        // 6️⃣ Retornar DTO optimizado para tabla
        return convertirAOrdenTablaDTO(ordenActualizada);
    }

    /**
     * 🔄 ACTUALIZAR ITEMS DE UNA ORDEN
     * Maneja crear, actualizar y eliminar items
     */
    private void actualizarItemsDeOrden(Orden orden, List<OrdenActualizarDTO.OrdenItemActualizarDTO> itemsDTO) {
        
        // 🗑️ Eliminar items marcados para eliminación
        orden.getItems().removeIf(item -> 
            itemsDTO.stream().anyMatch(dto -> 
                dto.getId() != null && dto.getId().equals(item.getId()) && dto.isEliminar()
            )
        );

        for (OrdenActualizarDTO.OrdenItemActualizarDTO itemDTO : itemsDTO) {
            if (itemDTO.isEliminar()) {
                continue; // Ya eliminado arriba
            }

            if (itemDTO.getId() == null) {
                // 🆕 CREAR NUEVO ITEM
                OrdenItem nuevoItem = new OrdenItem();
                nuevoItem.setOrden(orden);
                nuevoItem.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
                nuevoItem.setDescripcion(itemDTO.getDescripcion());
                nuevoItem.setCantidad(itemDTO.getCantidad());
                nuevoItem.setPrecioUnitario(itemDTO.getPrecioUnitario());
                nuevoItem.setTotalLinea(itemDTO.getTotalLinea());
                
                orden.getItems().add(nuevoItem);
                
            } else {
                // 🔄 ACTUALIZAR ITEM EXISTENTE
                OrdenItem itemExistente = orden.getItems().stream()
                    .filter(item -> item.getId().equals(itemDTO.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Item no encontrado: " + itemDTO.getId()));

                itemExistente.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
                itemExistente.setDescripcion(itemDTO.getDescripcion());
                itemExistente.setCantidad(itemDTO.getCantidad());
                itemExistente.setPrecioUnitario(itemDTO.getPrecioUnitario());
                itemExistente.setTotalLinea(itemDTO.getTotalLinea());
            }
        }
    }

    /**
     * Actualiza el inventario restando los productos vendidos
     * Se ejecuta cuando se crea una nueva orden (venta)
     */
    /**
     * 📦 ACTUALIZAR INVENTARIO POR VENTA - CON MANEJO DE CONCURRENCIA
     * 
     * Mejoras implementadas:
     * - Validación de stock con lock pesimista
     * - Manejo de concurrencia con reintentos
     * - Mensajes de error específicos
     * - Transaccional para consistencia
     */
    @Transactional
    private void actualizarInventarioPorVenta(Orden orden) {
        if (orden.getItems() == null || orden.getItems().isEmpty()) {
            return;
        }

        System.out.println("🔄 Actualizando inventario para orden ID: " + orden.getId());
        
        // Obtener la sede de la orden (donde se realiza la venta)
        Long sedeId = orden.getSede().getId();

        for (OrdenItem item : orden.getItems()) {
            if (item.getProducto() != null && item.getCantidad() != null && item.getCantidad() > 0) {
                Long productoId = item.getProducto().getId();
                Integer cantidadVendida = item.getCantidad();
                
                System.out.println("📦 Procesando producto ID: " + productoId + ", cantidad: " + cantidadVendida);
                
                // 🔒 VALIDACIÓN Y ACTUALIZACIÓN CONCURRENTE SEGURA
                actualizarInventarioConcurrente(productoId, sedeId, cantidadVendida);
            }
        }
        
        System.out.println("✅ Inventario actualizado correctamente para orden ID: " + orden.getId());
    }

    /**
     * 🔒 ACTUALIZAR INVENTARIO CON MANEJO DE CONCURRENCIA
     * 
     * Implementa:
     * - Lock pesimista para evitar race conditions
     * - Validación de stock en tiempo real
     * - Manejo de errores específicos
     */
    @Transactional
    private void actualizarInventarioConcurrente(Long productoId, Long sedeId, Integer cantidadVendida) {
        try {
            // 🔍 BUSCAR INVENTARIO CON LOCK PESIMISTA
            Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSedeConLock(productoId, sedeId);
            
            if (!inventarioOpt.isPresent()) {
                throw new IllegalArgumentException(
                    String.format("❌ No existe inventario para producto ID %d en sede ID %d", productoId, sedeId)
                );
            }
            
            Inventario inventario = inventarioOpt.get();
            int cantidadActual = inventario.getCantidad();
            
            System.out.println("📊 Stock actual: " + cantidadActual + ", cantidad a vender: " + cantidadVendida);
            
            // 🛡️ VALIDAR STOCK SUFICIENTE
            if (cantidadActual < cantidadVendida) {
                throw new IllegalArgumentException(
                    String.format("❌ Stock insuficiente para producto ID %d en sede ID %d. Disponible: %d, Requerido: %d", 
                                productoId, sedeId, cantidadActual, cantidadVendida)
                );
            }
            
            // ➖ ACTUALIZAR CANTIDAD CON VALIDACIÓN ADICIONAL
            int nuevaCantidad = cantidadActual - cantidadVendida;
            
            // 🔒 DOBLE VERIFICACIÓN PARA CONCURRENCIA
            if (nuevaCantidad < 0) {
                throw new IllegalArgumentException(
                    String.format("❌ Error de concurrencia: Stock insuficiente después de validación. Disponible: %d, Requerido: %d", 
                                cantidadActual, cantidadVendida)
                );
            }
            
            inventario.setCantidad(nuevaCantidad);
            inventarioService.actualizar(inventario.getId(), inventario);
            
            System.out.println("✅ Stock actualizado: " + cantidadActual + " → " + nuevaCantidad);
            
        } catch (IllegalArgumentException e) {
            // Re-lanzar errores de validación
            throw e;
        } catch (org.springframework.dao.PessimisticLockingFailureException e) {
            // Error específico de lock pesimista (timeout o deadlock)
            System.err.println("❌ Error de lock pesimista: " + e.getMessage());
            throw new RuntimeException(
                String.format("❌ Conflicto de concurrencia: Otro proceso está usando el inventario del producto ID %d. Espere unos segundos e intente nuevamente.", productoId)
            );
        } catch (org.springframework.dao.DataAccessException e) {
            // Otros errores de base de datos
            System.err.println("❌ Error de base de datos: " + e.getMessage());
            throw new RuntimeException(
                String.format("❌ Error de base de datos al actualizar inventario del producto ID %d. Intente nuevamente.", productoId)
            );
        } catch (Exception e) {
            // Manejar otros errores de concurrencia
            System.err.println("❌ Error inesperado en inventario: " + e.getMessage());
            throw new RuntimeException(
                String.format("❌ Error inesperado al actualizar inventario del producto ID %d. Intente nuevamente.", productoId)
            );
        }
    }

    /**
     * Restaura el inventario sumando los productos de una orden anulada
     * Se ejecuta cuando se anula una orden
     */
    private void restaurarInventarioPorAnulacion(Orden orden) {
        if (orden.getItems() == null || orden.getItems().isEmpty()) {
            return;
        }

        // Obtener la sede de la orden
        Long sedeId = orden.getSede().getId();

        for (OrdenItem item : orden.getItems()) {
            if (item.getProducto() != null && item.getCantidad() != null && item.getCantidad() > 0) {
                Long productoId = item.getProducto().getId();
                
                // Buscar inventario del producto en la sede
                Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(productoId, sedeId);
                
                if (inventarioOpt.isPresent()) {
                    Inventario inventario = inventarioOpt.get();
                    int cantidadActual = inventario.getCantidad();
                    int cantidadARestaurar = item.getCantidad();
                    
                    // Sumar cantidad restaurada usando método seguro
                    inventarioService.actualizarInventarioVenta(productoId, sedeId, cantidadActual + cantidadARestaurar);
                } else {
                    // Si no existe inventario, crearlo con la cantidad restaurada usando método seguro
                    inventarioService.actualizarInventarioVenta(productoId, sedeId, item.getCantidad());
                }
            }
        }
    }

    /**
     * Anula una orden y restaura el inventario
     */
    @Transactional
    public Orden anularOrden(Long id) {
        Optional<Orden> ordenOpt = repo.findById(id);
        if (!ordenOpt.isPresent()) {
            throw new IllegalArgumentException("Orden no encontrada con ID: " + id);
        }

        Orden orden = ordenOpt.get();
        
        // Verificar que la orden esté activa
        if (orden.getEstado() == Orden.EstadoOrden.ANULADA) {
            throw new IllegalArgumentException("La orden ya está anulada");
        }

        // Restaurar inventario antes de anular
        restaurarInventarioPorAnulacion(orden);

        // 💳 ANULAR CRÉDITO ASOCIADO SI EXISTE
        if (orden.getCreditoDetalle() != null) {
            try {
                creditoService.anularCredito(orden.getCreditoDetalle().getId());
            } catch (Exception e) {
                // Si falla la anulación del crédito, registrar el error pero continuar con la anulación de la orden
                System.err.println("Error al anular crédito para orden " + orden.getId() + ": " + e.getMessage());
            }
        }

        // Cambiar estado a anulada
        orden.setEstado(Orden.EstadoOrden.ANULADA);
        
        return repo.save(orden);
    }
}