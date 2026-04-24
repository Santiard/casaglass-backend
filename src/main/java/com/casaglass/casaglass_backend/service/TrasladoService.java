package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.CatalogoProductoTrasladoDTO;
import com.casaglass.casaglass_backend.dto.CatalogoProductosTrasladoResponseDTO;
import com.casaglass.casaglass_backend.dto.TrasladoDetalleBatchDTO;
import com.casaglass.casaglass_backend.exception.InventarioInsuficienteException;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.CatalogoProductoTrasladoProjection;
import com.casaglass.casaglass_backend.repository.CorteRepository;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
// no LocalDateTime/LocalTime needed
import java.util.*;

/**
 * Traslados entre sedes. Entre <strong>Centro (2) y Patios (3)</strong>, las líneas cuyo
 * {@code producto} es un {@link com.casaglass.casaglass_backend.model.Corte} mueven
 * stock en {@code inventario_cortes}; el resto de pares o productos no-corte usan
 * {@link Inventario} como hasta ahora.
 */
@Service
public class TrasladoService {

    private static final long SEDE_INSULA_ID = 1L;
    private static final long SEDE_CENTRO_ID = 2L;
    private static final long SEDE_PATIOS_ID = 3L;

    private final TrasladoRepository repo;
    private final TrasladoDetalleRepository detalleRepo;
    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final CorteRepository corteRepository;
    private final InventarioService inventarioService;
    private final InventarioCorteService inventarioCorteService;
    private final EntityManager em;

    public TrasladoService(TrasladoRepository repo,
                           TrasladoDetalleRepository detalleRepo,
                           SedeRepository sedeRepository,
                           ProductoRepository productoRepository,
                           TrabajadorRepository trabajadorRepository,
                           CorteRepository corteRepository,
                           InventarioService inventarioService,
                           InventarioCorteService inventarioCorteService,
                           EntityManager em) {
        this.repo = repo;
        this.detalleRepo = detalleRepo;
        this.sedeRepository = sedeRepository;
        this.productoRepository = productoRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.corteRepository = corteRepository;
        this.inventarioService = inventarioService;
        this.inventarioCorteService = inventarioCorteService;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public CatalogoProductosTrasladoResponseDTO obtenerCatalogoParaTraslado(
            Long sedeOrigenId,
            String q,
            Long categoriaId,
            String color,
            Integer page,
            Integer size,
            Long trabajadorId) {

        if (sedeOrigenId == null || sedeOrigenId <= 0) {
            throw new IllegalArgumentException("El parámetro sedeOrigenId es obligatorio");
        }

        if (!sedeRepository.existsById(sedeOrigenId)) {
            throw new NoSuchElementException("Sede no encontrada con ID: " + sedeOrigenId);
        }

        if (trabajadorId != null) {
            Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                    .orElseThrow(() -> new NoSuchElementException("Trabajador no encontrado con ID: " + trabajadorId));

            if (trabajador.getRol() == Rol.VENDEDOR) {
                Long sedeTrabajadorId = trabajador.getSede() != null ? trabajador.getSede().getId() : null;
                if (sedeTrabajadorId == null || !sedeTrabajadorId.equals(sedeOrigenId)) {
                    throw new SecurityException("El vendedor no tiene permiso para consultar esta sede");
                }
            }
        }

        int pageValue = (page != null && page > 0) ? page : 1;
        int sizeValue = (size != null && size > 0) ? Math.min(size, 200) : 50;

        Pageable pageable = PageRequest.of(pageValue - 1, sizeValue);

        String qNormalizado = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        ColorProducto colorEnum = null;
        if (color != null && !color.trim().isEmpty()) {
            try {
                colorEnum = ColorProducto.valueOf(color.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Color inválido: " + color);
            }
        }

        Page<CatalogoProductoTrasladoProjection> resultado = productoRepository.buscarCatalogoParaTraslado(
                sedeOrigenId,
                qNormalizado,
                categoriaId,
                colorEnum,
                pageable
        );

        List<CatalogoProductoTrasladoDTO> items = resultado.getContent().stream()
                .map(p -> new CatalogoProductoTrasladoDTO(
                        p.getId(),
                        p.getCodigo(),
                        p.getNombre(),
                        p.getCategoriaId(),
                        p.getCategoriaNombre(),
                        p.getColor() != null ? p.getColor().name() : null,
                        p.getCantidadSedeOrigen() != null ? p.getCantidadSedeOrigen() : 0.0,
                        p.getCantidadTotal() != null ? p.getCantidadTotal() : 0.0,
                        p.getPrecio1(),
                        p.getPrecio2(),
                        p.getPrecio3(),
                        true
                ))
                .toList();

        return new CatalogoProductosTrasladoResponseDTO(
                sedeOrigenId,
                items,
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                pageValue,
                sizeValue,
                resultado.hasNext(),
                resultado.hasPrevious()
        );
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
                if (d.getProductoInventarioADescontarSede1() != null
                        && d.getProductoInventarioADescontarSede1().getId() != null) {
                    Producto aDescontar = productoRepository.findById(
                                    d.getProductoInventarioADescontarSede1().getId())
                            .orElseThrow(() -> new RuntimeException("Producto a descontar (sede 1) no encontrado: "
                                    + d.getProductoInventarioADescontarSede1().getId()));
                    d.setProductoInventarioADescontarSede1(aDescontar);
                } else {
                    d.setProductoInventarioADescontarSede1(null);
                }
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
        if (traslado.getDetalles() == null) {
            return;
        }
        for (TrasladoDetalle detalle : traslado.getDetalles()) {
            aplicarMovimientoLinea(traslado, detalle);
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
        
        if (traslado.getDetalles() != null) {
            for (TrasladoDetalle detalle : traslado.getDetalles()) {
                revertirMovimientoLinea(traslado, detalle);
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
        if (payload.getProductoInventarioADescontarSede1() != null
                && payload.getProductoInventarioADescontarSede1().getId() != null) {
            payload.setProductoInventarioADescontarSede1(
                    em.getReference(Producto.class, payload.getProductoInventarioADescontarSede1().getId()));
        } else {
            payload.setProductoInventarioADescontarSede1(null);
        }
        
        TrasladoDetalle detalleGuardado = detalleRepo.save(payload);
        aplicarMovimientoLinea(t, detalleGuardado);
        
        return detalleGuardado;
    }

    @Transactional
    public TrasladoDetalle actualizarDetalle(Long trasladoId, Long detalleId, TrasladoDetalle payload) {
        TrasladoDetalle d = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        if (!Objects.equals(d.getTraslado().getId(), trasladoId))
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");

        Traslado traslado = d.getTraslado();
        revertirMovimientoLinea(traslado, d);
        
        if (payload.getProducto() != null && payload.getProducto().getId() != null) {
            d.setProducto(em.getReference(Producto.class, payload.getProducto().getId()));
        }
        if (payload.getCantidad() != null) {
            if (payload.getCantidad() < 1) {
                throw new IllegalArgumentException("cantidad debe ser >= 1");
            }
            d.setCantidad(payload.getCantidad());
        }
        if (payload.getProductoInventarioADescontarSede1() != null) {
            if (payload.getProductoInventarioADescontarSede1().getId() == null) {
                d.setProductoInventarioADescontarSede1(null);
            } else {
                d.setProductoInventarioADescontarSede1(
                        em.getReference(Producto.class, payload.getProductoInventarioADescontarSede1().getId()));
            }
        }
        d = detalleRepo.save(d);
        aplicarMovimientoLinea(traslado, d);
        return d;
    }

    @Transactional
    public void eliminarDetalle(Long trasladoId, Long detalleId) {
        TrasladoDetalle d = detalleRepo.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado"));
        if (!Objects.equals(d.getTraslado().getId(), trasladoId))
            throw new IllegalArgumentException("El detalle no pertenece al traslado indicado");
        
        Traslado traslado = d.getTraslado();
        revertirMovimientoLinea(traslado, d);
        
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
        
        // 2️⃣ ELIMINAR detalles (revertir inventario primero)
        if (batchDTO.getEliminar() != null && !batchDTO.getEliminar().isEmpty()) {
            for (Long detalleId : batchDTO.getEliminar()) {
                TrasladoDetalle detalle = detalleRepo.findById(detalleId)
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + detalleId));
                
                if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
                    throw new IllegalArgumentException("El detalle " + detalleId + " no pertenece al traslado " + trasladoId);
                }
                
                revertirMovimientoLinea(traslado, detalle);
                
                // ✅ Eliminar el detalle después de revertir el inventario
                // Usar consulta nativa DELETE para asegurar ejecución inmediata
                detalleRepo.deleteByIdNative(detalleId);
            }
            // Forzar flush de todas las eliminaciones juntas
            detalleRepo.flush();
            // Forzar flush a nivel de EntityManager para asegurar persistencia
            em.flush();
        }
        
        if (batchDTO.getActualizar() != null && !batchDTO.getActualizar().isEmpty()) {
            for (TrasladoDetalleBatchDTO.DetalleActualizarDTO dto : batchDTO.getActualizar()) {
                TrasladoDetalle detalle = detalleRepo.findById(dto.getDetalleId())
                        .orElseThrow(() -> new RuntimeException("Detalle no encontrado con id " + dto.getDetalleId()));
                
                if (!Objects.equals(detalle.getTraslado().getId(), trasladoId)) {
                    throw new IllegalArgumentException("El detalle " + dto.getDetalleId() + " no pertenece al traslado " + trasladoId);
                }
                revertirMovimientoLinea(traslado, detalle);
                if (dto.getProductoId() != null) {
                    detalle.setProducto(productoRepository.findById(dto.getProductoId())
                            .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + dto.getProductoId())));
                }
                if (dto.getCantidad() != null) {
                    if (dto.getCantidad() < 1) {
                        throw new IllegalArgumentException("La cantidad debe ser >= 1");
                    }
                    detalle.setCantidad(dto.getCantidad());
                }
                if (Boolean.TRUE.equals(dto.getLimpiarProductoInventarioADescontarSede1())) {
                    detalle.setProductoInventarioADescontarSede1(null);
                } else if (dto.getProductoInventarioADescontarSede1Id() != null) {
                    detalle.setProductoInventarioADescontarSede1(productoRepository.findById(dto.getProductoInventarioADescontarSede1Id())
                            .orElseThrow(() -> new RuntimeException("Producto a descontar (sede 1) no encontrado: "
                                    + dto.getProductoInventarioADescontarSede1Id())));
                }
                detalle = detalleRepo.save(detalle);
                aplicarMovimientoLinea(traslado, detalle);
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
                if (dto.getProductoInventarioADescontarSede1Id() != null) {
                    nuevoDetalle.setProductoInventarioADescontarSede1(productoRepository
                            .findById(dto.getProductoInventarioADescontarSede1Id())
                            .orElseThrow(() -> new RuntimeException("Producto a descontar (sede 1) no encontrado: "
                                    + dto.getProductoInventarioADescontarSede1Id())));
                }
                detalleRepo.save(nuevoDetalle);
                aplicarMovimientoLinea(traslado, nuevoDetalle);
            }
        }
        
        // 5️⃣ Retornar todos los detalles actualizados del traslado
        // Forzar flush final y limpiar caché antes de consultar para evitar problemas de caché
        em.flush();
        em.clear(); // Limpiar caché para forzar consulta fresca desde BD
        
        // Consultar detalles directamente desde BD (sin caché, después de limpiar)
        return detalleRepo.findByTrasladoId(trasladoId);
    }
    
    private static boolean esParCentroPatios(Long sedeA, Long sedeB) {
        if (sedeA == null || sedeB == null) {
            return false;
        }
        return (sedeA.equals(SEDE_CENTRO_ID) && sedeB.equals(SEDE_PATIOS_ID))
                || (sedeA.equals(SEDE_PATIOS_ID) && sedeB.equals(SEDE_CENTRO_ID));
    }

    private static boolean esSedeCentroOPatios(Long sedeId) {
        return sedeId != null && (sedeId.equals(SEDE_CENTRO_ID) || sedeId.equals(SEDE_PATIOS_ID));
    }

    /**
     * Reglas: {@code productoInventarioADescontarSede1} solo si origen=1, destino=2|3, línea corte, y
     * el producto a descontar no es corte y hay stock en sede 1. Si se envía en otro contexto, 400.
     */
    private void validarReglasProductoInventarioADescontarSede1(Traslado t, TrasladoDetalle d) {
        if (d.getProductoInventarioADescontarSede1() == null) {
            return;
        }
        long o = t.getSedeOrigen().getId();
        long dest = t.getSedeDestino().getId();
        boolean parUnoAResto = (o == SEDE_INSULA_ID) && (dest == SEDE_CENTRO_ID || dest == SEDE_PATIOS_ID);
        if (!parUnoAResto) {
            throw new IllegalArgumentException(
                    "productoInventarioADescontarSede1 solo aplica a traslados cuyo origen es Insula (1) "
                            + "y el destino es Centro (2) o Patios (3).");
        }
        if (d.getProducto() == null || d.getProducto().getId() == null) {
            throw new IllegalArgumentException("La línea requiere producto para validar el descuento en sede 1.");
        }
        if (!corteRepository.existsById(d.getProducto().getId())) {
            throw new IllegalArgumentException(
                    "productoInventarioADescontarSede1 solo aplica cuando la línea traslada un corte (producto corte).");
        }
        Long pDesc = d.getProductoInventarioADescontarSede1().getId();
        if (corteRepository.existsById(pDesc)) {
            throw new IllegalArgumentException(
                    "El producto a descontar en Insula no puede ser un corte; use un producto entero (id=" + pDesc + ").");
        }
        double need = d.getCantidad() != null ? d.getCantidad() : 0.0;
        if (need <= 0) {
            return;
        }
        double disponible = inventarioService.obtenerPorProductoYSede(pDesc, SEDE_INSULA_ID)
                .map(Inventario::getCantidad)
                .orElse(0.0);
        if (disponible < need) {
            throw new InventarioInsuficienteException(
                    "Stock insuficiente en Insula del producto a descontar (id " + pDesc
                            + "). Disponible: " + disponible + ", requerido: " + need,
                    disponible, need, pDesc, SEDE_INSULA_ID);
        }
    }

    /**
     * Aplica el movimiento de stock de una línea al crear/confirmar detalle (saca origen, entra en destino).
     */
    private void aplicarMovimientoLinea(Traslado t, TrasladoDetalle d) {
        validarReglasProductoInventarioADescontarSede1(t, d);
        long o = t.getSedeOrigen().getId();
        long dest = t.getSedeDestino().getId();
        Long pid = d.getProducto().getId();
        double q = d.getCantidad();
        boolean corte = corteRepository.existsById(pid);

        if (corte && esParCentroPatios(o, dest)) {
            ajustarInventarioCorte(pid, o, -q, "origen");
            ajustarInventarioCorte(pid, dest, +q, "destino");
            return;
        }
        if (corte && o == SEDE_INSULA_ID && esSedeCentroOPatios(dest)) {
            if (d.getProductoInventarioADescontarSede1() != null) {
                Long pDesc = d.getProductoInventarioADescontarSede1().getId();
                ajustarInventarioProductoNormal(pDesc, SEDE_INSULA_ID, -q, "sede1-producto-entero");
            }
            ajustarInventarioCorte(pid, dest, +q, "destino");
            // Nombre canónico del corte (misma lógica que OrdenService al crear/ajustar cortes)
            actualizarNombreCorteTrasladoInsulaADestino(d);
            return;
        }
        if (corte && esSedeCentroOPatios(o) && dest == SEDE_INSULA_ID) {
            ajustarInventarioCorte(pid, o, -q, "origen");
            return;
        }
        ajustarInventarioProductoNormal(pid, o, -q, "origen");
        ajustarInventarioProductoNormal(pid, dest, +q, "destino");
    }

    /**
     * Revierte el movimiento de stock de una línea (anular traslado o editar/borrar detalle).
     */
    private void revertirMovimientoLinea(Traslado t, TrasladoDetalle d) {
        long o = t.getSedeOrigen().getId();
        long dest = t.getSedeDestino().getId();
        Long pid = d.getProducto().getId();
        double q = d.getCantidad();
        boolean corte = corteRepository.existsById(pid);

        if (corte && esParCentroPatios(o, dest)) {
            ajustarInventarioCorte(pid, o, +q, "origen-revertir");
            ajustarInventarioCorte(pid, dest, -q, "destino-revertir");
            return;
        }
        if (corte && o == SEDE_INSULA_ID && esSedeCentroOPatios(dest)) {
            if (d.getProductoInventarioADescontarSede1() != null) {
                Long pDesc = d.getProductoInventarioADescontarSede1().getId();
                ajustarInventarioProductoNormal(pDesc, SEDE_INSULA_ID, +q, "sede1-producto-revertir");
            }
            ajustarInventarioCorte(pid, dest, -q, "destino-revertir");
            return;
        }
        if (corte && esSedeCentroOPatios(o) && dest == SEDE_INSULA_ID) {
            ajustarInventarioCorte(pid, o, +q, "origen-revertir");
            return;
        }
        ajustarInventarioProductoNormal(pid, o, +q, "origen-revertir");
        ajustarInventarioProductoNormal(pid, dest, -q, "destino-revertir");
    }

    /**
     * Traslado Insula (1) → Centro (2) o Patios (3) con línea corte: al acreditar corte en destino,
     * fija {@link Corte#getNombre()} usando el <strong>producto entero</strong> descontado en 1
     * y la medida del corte, mismo criterio que en {@code OrdenService}:
     * {@code [base entero] Corte de {medida} CMS}.
     * <p>El {@code nombre} está en la fila del {@code Corte} (id único a nivel app); afecta listados
     * en todas las sedes. Solo corre si la línea trae {@code productoInventarioADescontarSede1}.</p>
     */
    private void actualizarNombreCorteTrasladoInsulaADestino(TrasladoDetalle d) {
        if (d.getProducto() == null || d.getProducto().getId() == null) {
            return;
        }
        if (d.getProductoInventarioADescontarSede1() == null
                || d.getProductoInventarioADescontarSede1().getId() == null) {
            return;
        }
        Long corteId = d.getProducto().getId();
        if (!corteRepository.existsById(corteId)) {
            return;
        }
        Corte corte = corteRepository.findById(corteId).orElse(null);
        if (corte == null) {
            return;
        }
        Long enteroId = d.getProductoInventarioADescontarSede1().getId();
        Producto entero = productoRepository.findById(enteroId).orElse(null);
        if (entero == null) {
            return;
        }
        String nombreEntero = entero.getNombre() != null ? entero.getNombre() : "";
        int idx = nombreEntero.indexOf(" Corte de ");
        String baseNombre = idx != -1 ? nombreEntero.substring(0, idx).trim() : nombreEntero.trim();
        if (baseNombre.isEmpty()) {
            baseNombre = entero.getCodigo() != null && !entero.getCodigo().isBlank()
                    ? entero.getCodigo().trim() : "Corte";
        }
        double largo = corte.getLargoCm() != null ? corte.getLargoCm() : 0.0;
        int medida = (int) Math.round(largo);
        if (medida <= 0) {
            return;
        }
        String nombreFinal = baseNombre + " Corte de " + medida + " CMS";
        if (nombreFinal.equals(corte.getNombre())) {
            return;
        }
        corte.setNombre(nombreFinal);
        corteRepository.save(corte);
    }

    private void ajustarInventarioCorte(Long corteId, Long sedeId, Double ajuste, String tipo) {
        if (ajuste == null || ajuste == 0) {
            return;
        }
        if (ajuste < 0) {
            double aSacar = -ajuste;
            double disponible = inventarioCorteService
                    .obtenerPorCorteYSede(corteId, sedeId)
                    .map(InventarioCorte::getCantidad)
                    .orElse(0.0);
            if (disponible < aSacar) {
                throw new InventarioInsuficienteException(
                        "No hay suficiente stock de corte en sede " + tipo
                                + ". Disponible: " + disponible + ", requerido: " + aSacar,
                        disponible, aSacar, corteId, sedeId);
            }
            inventarioCorteService.decrementarStock(corteId, sedeId, aSacar);
        } else {
            inventarioCorteService.incrementarStock(corteId, sedeId, ajuste);
        }
    }

    private void ajustarInventarioProductoNormal(Long productoId, Long sedeId, Double ajuste, String tipo) {
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
