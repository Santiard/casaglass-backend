package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.dto.OrdenTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
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
    private final EntityManager entityManager;

    public OrdenService(OrdenRepository repo, EntityManager entityManager) { 
        this.repo = repo; 
        this.entityManager = entityManager;
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
        return repo.save(orden);
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
        
        // 📋 ITEMS COMPLETOS (manteniendo detalle como solicitado)
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
                nuevoItem.setProducto(entityManager.getReference(Producto.class, itemDTO.getProductoId()));
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

                itemExistente.setProducto(entityManager.getReference(Producto.class, itemDTO.getProductoId()));
                itemExistente.setDescripcion(itemDTO.getDescripcion());
                itemExistente.setCantidad(itemDTO.getCantidad());
                itemExistente.setPrecioUnitario(itemDTO.getPrecioUnitario());
                itemExistente.setTotalLinea(itemDTO.getTotalLinea());
            }
        }
    }
}