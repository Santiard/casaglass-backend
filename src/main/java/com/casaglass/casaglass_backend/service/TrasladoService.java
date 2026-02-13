package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.TrasladoDetalleBatchDTO;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.service.InventarioService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
// no LocalDateTime/LocalTime needed
import java.util.*;

@Service
public class TrasladoService {

    private final TrasladoRepository repo;
    private final TrasladoDetalleRepository detalleRepo;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final EntityManager em;

    public TrasladoService(TrasladoRepository repo,
                           TrasladoDetalleRepository detalleRepo,
                           SedeRepository sedeRepository,
                           ProductoRepository productoRepository,
                           InventarioService inventarioService,
                           EntityManager em) {
        this.repo = repo;
        this.detalleRepo = detalleRepo;
        this.sedeRepository = sedeRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.em = em;
    }

    /* ---------------- Consultas ---------------- */

    public List<Traslado> listar() { 
        return repo.findAllWithDetails(); // 🔁 Usar query optimizada con JOIN FETCH
    }

    public Optional<Traslado> obtener(Long id) { return repo.findById(id); }

    public List<Traslado> listarPorSedeOrigen(Long sedeOrigenId) { return repo.findBySedeOrigenId(sedeOrigenId); }

    public List<Traslado> listarPorSedeDestino(Long sedeDestinoId) { return repo.findBySedeDestinoId(sedeDestinoId); }

    public List<Traslado> listarPorFecha(LocalDate fecha) {
        return repo.findByFechaBetween(fecha, fecha);
    }

    public List<Traslado> listarPorRango(LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByFechaBetween(desdeDia, hastaDia);
    }

    /* ---------------- Comandos (cabecera) ---------------- */

    @Transactional
    public Traslado crear(Traslado payload) {
        if (payload.getSedeOrigen() == null || payload.getSedeOrigen().getId() == null)
            throw new IllegalArgumentException("Debe especificar sedeOrigen.id");
        if (payload.getSedeDestino() == null || payload.getSedeDestino().getId() == null)
            throw new IllegalArgumentException("Debe especificar sedeDestino.id");
        if (Objects.equals(payload.getSedeOrigen().getId(), payload.getSedeDestino().getId()))
            throw new IllegalArgumentException("La sede de origen y destino no pueden ser la misma");

        // ARREGLO: Buscar entidades completas en lugar de usar proxies
        Sede sedeOrigen = sedeRepository.findById(payload.getSedeOrigen().getId())
            .orElseThrow(() -> new RuntimeException("Sede origen no encontrada con ID: " + payload.getSedeOrigen().getId()));
        Sede sedeDestino = sedeRepository.findById(payload.getSedeDestino().getId())
            .orElseThrow(() -> new RuntimeException("Sede destino no encontrada con ID: " + payload.getSedeDestino().getId()));
        
        payload.setSedeOrigen(sedeOrigen);
        payload.setSedeDestino(sedeDestino);

        if (payload.getFecha() == null) payload.setFecha(LocalDate.now());

        // detalles (si vienen en el payload)
        if (payload.getDetalles() != null) {
            for (TrasladoDetalle d : payload.getDetalles()) {
                if (d.getProducto() == null || d.getProducto().getId() == null)
                    throw new IllegalArgumentException("Cada detalle requiere producto.id");
                if (d.getCantidad() == null || d.getCantidad() < 1)
                    throw new IllegalArgumentException("Cada detalle requiere cantidad >= 1");
                
                // ARREGLO: Buscar producto completo en lugar de usar proxy
                Producto producto = productoRepository.findById(d.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + d.getProducto().getId()));
                
                d.setTraslado(payload);
                d.setProducto(producto);
            }
        }

        Traslado resultado = repo.save(payload);
        
        // 🔄 ACTUALIZAR INVENTARIO: Restar de origen y sumar a destino
        actualizarInventarioTraslado(resultado);
        
        return resultado;
    }

    /**
     * Actualiza el inventario cuando se crea un traslado:
     * - Resta la cantidad de la sede origen
     * - Suma la cantidad a la sede destino
     */
    private void actualizarInventarioTraslado(Traslado traslado) {
        Long sedeOrigenId = traslado.getSedeOrigen().getId();
        Long sedeDestinoId = traslado.getSedeDestino().getId();
        
        for (TrasladoDetalle detalle : traslado.getDetalles()) {
            Long productoId = detalle.getProducto().getId();
            Double cantidad = detalle.getCantidad();
            
            // 1. RESTAR de sede origen
            Optional<Inventario> inventarioOrigen = inventarioService.obtenerPorProductoYSede(productoId, sedeOrigenId);
            if (inventarioOrigen.isPresent()) {
                Inventario invOrigen = inventarioOrigen.get();
                double nuevaCantidadOrigen = invOrigen.getCantidad() - cantidad;
                
                if (nuevaCantidadOrigen < 0) {
                    throw new InventarioInsuficienteException(
                        "No hay suficiente stock en sede origen. Disponible: " + 
                        invOrigen.getCantidad() + ", requerido: " + cantidad,
                        invOrigen.getCantidad(), cantidad, productoId, sedeOrigenId);
                }
                
                invOrigen.setCantidad(nuevaCantidadOrigen);
                inventarioService.actualizar(invOrigen.getId(), invOrigen);
            } else {
                throw new InventarioInsuficienteException(
                    "No existe inventario del producto ID " + productoId + 
                    " en sede origen ID " + sedeOrigenId,
                    0.0, cantidad, productoId, sedeOrigenId);
            }
            
            // 2. SUMAR a sede destino
            Optional<Inventario> inventarioDestino = inventarioService.obtenerPorProductoYSede(productoId, sedeDestinoId);
            if (inventarioDestino.isPresent()) {
                // Actualizar inventario existente
                Inventario invDestino = inventarioDestino.get();
                invDestino.setCantidad(invDestino.getCantidad() + cantidad);
                inventarioService.actualizar(invDestino.getId(), invDestino);
            } else {
                // Crear nuevo inventario en sede destino
                Inventario nuevoInventario = new Inventario();
                nuevoInventario.setProducto(detalle.getProducto());
                nuevoInventario.setSede(traslado.getSedeDestino());
                nuevoInventario.setCantidad(cantidad);
                inventarioService.guardar(nuevoInventario);
            }
        }
    }

    @Transactional
    public Traslado actualizarCabecera(Long id, Traslado cambios) {
        return repo.findById(id).map(t -> {
            if (cambios.getSedeOrigen() != null && cambios.getSedeOrigen().getId() != null) {
                t.setSedeOrigen(em.getReference(Sede.class, cambios.getSedeOrigen().getId()));
            }
            if (cambios.getSedeDestino() != null && cambios.getSedeDestino().getId() != null) {
                t.setSedeDestino(em.getReference(Sede.class, cambios.getSedeDestino().getId()));
            }
            if (t.getSedeOrigen() != null && t.getSedeDestino() != null &&
                Objects.equals(t.getSedeOrigen().getId(), t.getSedeDestino().getId())) {
                throw new IllegalArgumentException("La sede de origen y destino no pueden ser la misma");
            }
            if (cambios.getFecha() != null) t.setFecha(cambios.getFecha());
            if (cambios.getTrabajadorConfirmacion() != null && cambios.getTrabajadorConfirmacion().getId() != null) {
                t.setTrabajadorConfirmacion(em.getReference(Trabajador.class, cambios.getTrabajadorConfirmacion().getId()));
            }
            if (cambios.getFechaConfirmacion() != null) {
                t.setFechaConfirmacion(cambios.getFechaConfirmacion());
            }
            return repo.save(t);
        }).orElseThrow(() -> new RuntimeException("Traslado no encontrado con id " + id));
    }

    @Transactional
    public Traslado confirmarLlegada(Long trasladoId, Long trabajadorId) {
        Traslado t = repo.findById(trasladoId)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado"));
        t.setTrabajadorConfirmacion(em.getReference(Trabajador.class, trabajadorId));
        t.setFechaConfirmacion(LocalDate.now()); // ⚡ Establecer fecha de confirmación automáticamente
        return repo.save(t);
    }

    @Transactional
    public void eliminar(Long id) {
        // 1️⃣ Buscar el traslado antes de eliminarlo
        Traslado traslado = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado con id " + id));
        
        // 2️⃣ Revertir el inventario de todos los detalles antes de eliminar
        if (traslado.getDetalles() != null && !traslado.getDetalles().isEmpty()) {
            Long sedeOrigenId = traslado.getSedeOrigen().getId();
            Long sedeDestinoId = traslado.getSedeDestino().getId();
            
            for (TrasladoDetalle detalle : traslado.getDetalles()) {
                Long productoId = detalle.getProducto().getId();
                Double cantidad = detalle.getCantidad();
                
                // Revertir inventario: devolver a origen y restar de destino
                ajustarInventario(productoId, sedeOrigenId, cantidad, "origen");
                ajustarInventario(productoId, sedeDestinoId, -cantidad, "destino");
            }
        }
        
        // 3️⃣ Eliminar el traslado (los detalles se eliminan en cascada)
        repo.deleteById(id);
    }

    /* ---------------- Detalles (anidados) ---------------- */

    public List<TrasladoDetalle> listarDetalles(Long trasladoId) {
        return detalleRepo.findByTrasladoId(trasladoId);
    }

    @Transactional
    public TrasladoDetalle agregarDetalle(Long trasladoId, TrasladoDetalle payload) {
        Traslado t = repo.findById(trasladoId)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado"));
        if (payload.getProducto() == null || payload.getProducto().getId() == null)
            throw new IllegalArgumentException("producto.id requerido");
        if (payload.getCantidad() == null || payload.getCantidad() < 1)
            throw new IllegalArgumentException("cantidad debe ser >= 1");

        payload.setTraslado(t);
        payload.setProducto(em.getReference(Producto.class, payload.getProducto().getId()));
        
        TrasladoDetalle detalleGuardado = detalleRepo.save(payload);
        
        // 🔄 ACTUALIZAR INVENTARIO: Restar de origen y sumar a destino
        Long sedeOrigenId = t.getSedeOrigen().getId();
        Long sedeDestinoId = t.getSedeDestino().getId();
        Long productoId = payload.getProducto().getId();
        Double cantidad = payload.getCantidad();
        
        ajustarInventario(productoId, sedeOrigenId, -cantidad, "origen");
        ajustarInventario(productoId, sedeDestinoId, cantidad, "destino");
        
        return detalleGuardado;
    }

    @Transactional
    public TrasladoDetalle actualizarDetalle(Long trasladoId, Long detalleId, TrasladoDetalle payload) {
        TrasladoDetalle d = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        if (!Objects.equals(d.getTraslado().getId(), trasladoId))
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");

        Traslado traslado = d.getTraslado();
        Long sedeOrigenId = traslado.getSedeOrigen().getId();
        Long sedeDestinoId = traslado.getSedeDestino().getId();
        
        // 🔄 CAMBIO DE PRODUCTO: Revertir inventario del producto anterior y aplicar el nuevo
        if (payload.getProducto() != null && payload.getProducto().getId() != null 
            && !Objects.equals(d.getProducto().getId(), payload.getProducto().getId())) {
            
            Long productoAnteriorId = d.getProducto().getId();
            Double cantidadAnterior = d.getCantidad();
            
            // Revertir movimiento del producto anterior
            ajustarInventario(productoAnteriorId, sedeOrigenId, cantidadAnterior, "origen");
            ajustarInventario(productoAnteriorId, sedeDestinoId, -cantidadAnterior, "destino");
            
            // Aplicar movimiento del nuevo producto
            Long productoNuevoId = payload.getProducto().getId();
            Double cantidadNueva = (payload.getCantidad() != null) ? payload.getCantidad() : cantidadAnterior;
            
            ajustarInventario(productoNuevoId, sedeOrigenId, -cantidadNueva, "origen");
            ajustarInventario(productoNuevoId, sedeDestinoId, cantidadNueva, "destino");
            
            d.setProducto(em.getReference(Producto.class, productoNuevoId));
            if (payload.getCantidad() != null) {
                if (payload.getCantidad() < 1) throw new IllegalArgumentException("cantidad debe ser >= 1");
                d.setCantidad(payload.getCantidad());
            }
        }
        // 🔄 CAMBIO DE CANTIDAD: Ajustar solo la diferencia
        else if (payload.getCantidad() != null && !Objects.equals(d.getCantidad(), payload.getCantidad())) {
            if (payload.getCantidad() < 1) throw new IllegalArgumentException("cantidad debe ser >= 1");
            
            Long productoId = d.getProducto().getId();
            Double cantidadAnterior = d.getCantidad();
            Double cantidadNueva = payload.getCantidad();
            Double diferencia = cantidadNueva - cantidadAnterior;
            
            // Ajustar inventario por la diferencia
            ajustarInventario(productoId, sedeOrigenId, -diferencia, "origen");
            ajustarInventario(productoId, sedeDestinoId, diferencia, "destino");
            
            d.setCantidad(cantidadNueva);
        }
        
        return detalleRepo.save(d);
    }

    @Transactional
    public void eliminarDetalle(Long trasladoId, Long detalleId) {
        TrasladoDetalle d = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        if (!Objects.equals(d.getTraslado().getId(), trasladoId))
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");
        
        // 🔄 REVERTIR INVENTARIO: Devolver a origen y restar de destino
        Traslado traslado = d.getTraslado();
        Long sedeOrigenId = traslado.getSedeOrigen().getId();
        Long sedeDestinoId = traslado.getSedeDestino().getId();
        Long productoId = d.getProducto().getId();
        Double cantidad = d.getCantidad();
        
        // Devolver cantidad a sede origen
        ajustarInventario(productoId, sedeOrigenId, cantidad, "origen");
        // Restar cantidad de sede destino
        ajustarInventario(productoId, sedeDestinoId, -cantidad, "destino");
        
        detalleRepo.delete(d);
    }
    
    /**
     * 🔄 ACTUALIZAR MÚLTIPLES DETALLES EN BATCH (ATÓMICO)
     * Permite crear, actualizar y eliminar detalles en una sola transacción.
     * Esto evita problemas de concurrencia cuando se hacen múltiples cambios simultáneos.
     * 
     * @param trasladoId ID del traslado
     * @param batchDTO DTO con los cambios a aplicar
     * @return Lista de todos los detalles del traslado después de los cambios
     */
    @Transactional
    public List<TrasladoDetalle> actualizarDetallesBatch(Long trasladoId, TrasladoDetalleBatchDTO batchDTO) {
        // 1️⃣ Validar que el traslado existe
        Traslado traslado = repo.findById(trasladoId)
                .orElseThrow(() -> new RuntimeException("Traslado no encontrado con id " + trasladoId));
        
        Long sedeOrigenId = traslado.getSedeOrigen().getId();
        Long sedeDestinoId = traslado.getSedeDestino().getId();
        
        // 2️⃣ ELIMINAR detalles (revertir inventario primero)
        if (batchDTO.getEliminar() != null && !batchDTO.getEliminar().isEmpty()) {
            for (Long detalleId : batchDTO.getEliminar()) {
                TrasladoDetalle detalle = detalleRepo.findById(detalleId)
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + detalleId));
                
                if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
                    throw new IllegalArgumentException("El detalle " + detalleId + " no pertenece al traslado " + trasladoId);
                }
                
                // 🔄 REVERTIR INVENTARIO: Devolver a origen y restar de destino (igual que eliminarDetalle)
                Long productoId = detalle.getProducto().getId();
                Double cantidad = detalle.getCantidad();
                
                // Devolver cantidad a sede origen (sumar)
                ajustarInventario(productoId, sedeOrigenId, cantidad, "origen");
                // Restar cantidad de sede destino
                ajustarInventario(productoId, sedeDestinoId, -cantidad, "destino");
                
                // ✅ Eliminar el detalle después de revertir el inventario
                // Usar consulta nativa DELETE para asegurar ejecución inmediata
                detalleRepo.deleteByIdNative(detalleId);
            }
            // Forzar flush de todas las eliminaciones juntas
            detalleRepo.flush();
            // Forzar flush a nivel de EntityManager para asegurar persistencia
            em.flush();
        }
        
        // 3️⃣ ACTUALIZAR detalles existentes
        if (batchDTO.getActualizar() != null && !batchDTO.getActualizar().isEmpty()) {
            for (TrasladoDetalleBatchDTO.DetalleActualizarDTO dto : batchDTO.getActualizar()) {
                TrasladoDetalle detalle = detalleRepo.findById(dto.getDetalleId())
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + dto.getDetalleId()));
                
                if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
                    throw new IllegalArgumentException("El detalle " + dto.getDetalleId() + " no pertenece al traslado " + trasladoId);
                }
                
                // Cambiar producto si se especifica
                if (dto.getProductoId() != null && !Objects.equals(detalle.getProducto().getId(), dto.getProductoId())) {
                    Long productoAnteriorId = detalle.getProducto().getId();
                    Double cantidadAnterior = detalle.getCantidad();
                    
                    // Revertir inventario del producto anterior
                    ajustarInventario(productoAnteriorId, sedeOrigenId, cantidadAnterior, "origen");
                    ajustarInventario(productoAnteriorId, sedeDestinoId, -cantidadAnterior, "destino");
                    
                    // Aplicar inventario del nuevo producto
                    Double cantidadNueva = (dto.getCantidad() != null) ? dto.getCantidad() : cantidadAnterior;
                    ajustarInventario(dto.getProductoId(), sedeOrigenId, -cantidadNueva, "origen");
                    ajustarInventario(dto.getProductoId(), sedeDestinoId, cantidadNueva, "destino");
                    
                    detalle.setProducto(productoRepository.findById(dto.getProductoId())
                            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + dto.getProductoId())));
                    detalle.setCantidad(cantidadNueva);
                }
                // Cambiar cantidad si se especifica (y no cambió el producto)
                else if (dto.getCantidad() != null && !Objects.equals(detalle.getCantidad(), dto.getCantidad())) {
                    if (dto.getCantidad() < 1) {
                        throw new IllegalArgumentException("La cantidad debe ser >= 1");
                    }
                    
                    Long productoId = detalle.getProducto().getId();
                    Double cantidadAnterior = detalle.getCantidad();
                    Double cantidadNueva = dto.getCantidad();
                    Double diferencia = cantidadNueva - cantidadAnterior;
                    
                    // Ajustar inventario por la diferencia
                    ajustarInventario(productoId, sedeOrigenId, -diferencia, "origen");
                    ajustarInventario(productoId, sedeDestinoId, diferencia, "destino");
                    
                    detalle.setCantidad(cantidadNueva);
                }
                
                detalleRepo.save(detalle);
            }
        }
        
        // 4️⃣ CREAR nuevos detalles
        if (batchDTO.getCrear() != null && !batchDTO.getCrear().isEmpty()) {
            for (TrasladoDetalleBatchDTO.DetalleCrearDTO dto : batchDTO.getCrear()) {
                if (dto.getProductoId() == null) {
                    throw new IllegalArgumentException("El producto es obligatorio para crear un detalle");
                }
                if (dto.getCantidad() == null || dto.getCantidad() < 1) {
                    throw new IllegalArgumentException("La cantidad debe ser >= 1");
                }
                
                Producto producto = productoRepository.findById(dto.getProductoId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + dto.getProductoId()));
                
                TrasladoDetalle nuevoDetalle = new TrasladoDetalle();
                nuevoDetalle.setTraslado(traslado);
                nuevoDetalle.setProducto(producto);
                nuevoDetalle.setCantidad(dto.getCantidad());
                
                detalleRepo.save(nuevoDetalle);
                
                // Aplicar inventario
                ajustarInventario(dto.getProductoId(), sedeOrigenId, -dto.getCantidad(), "origen");
                ajustarInventario(dto.getProductoId(), sedeDestinoId, dto.getCantidad(), "destino");
            }
        }
        
        // 5️⃣ Retornar todos los detalles actualizados del traslado
        // Forzar flush final y limpiar caché antes de consultar para evitar problemas de caché
        em.flush();
        em.clear(); // Limpiar caché para forzar consulta fresca desde BD
        
        // Consultar detalles directamente desde BD (sin caché, después de limpiar)
        return detalleRepo.findByTrasladoId(trasladoId);
    }
    
    /**
     * Método auxiliar para ajustar inventario de forma segura
     * @param productoId ID del producto
     * @param sedeId ID de la sede
     * @param ajuste Cantidad a ajustar (positivo = suma, negativo = resta)
     * @param tipo "origen" o "destino" para mensajes de error
     */
    private void ajustarInventario(Long productoId, Long sedeId, Double ajuste, String tipo) {
        Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(productoId, sedeId);
        
        if (inventarioOpt.isPresent()) {
            Inventario inv = inventarioOpt.get();
            double nuevaCantidad = inv.getCantidad() + ajuste;
            
            if (nuevaCantidad < 0) {
                throw new RuntimeException("Stock insuficiente en sede " + tipo + ". " +
                    "Disponible: " + inv.getCantidad() + ", ajuste solicitado: " + ajuste);
            }
            
            inv.setCantidad(nuevaCantidad);
            inventarioService.actualizar(inv.getId(), inv);
        } else {
            // Si no existe inventario y el ajuste es positivo, crear nuevo registro
            if (ajuste > 0) {
                Inventario nuevoInventario = new Inventario();
                nuevoInventario.setProducto(productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado")));
                nuevoInventario.setSede(sedeRepository.findById(sedeId)
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada")));
                nuevoInventario.setCantidad(ajuste);
                inventarioService.guardar(nuevoInventario);
            } else {
                throw new InventarioInsuficienteException(
                    "No existe inventario del producto ID " + productoId + 
                    " en sede " + tipo + " ID " + sedeId);
            }
        }
    }
}
