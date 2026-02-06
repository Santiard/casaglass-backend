package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.EntregaDetalle;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.repository.EntregaDetalleRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.AbonoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EntregaDetalleService {

    @Autowired
    private EntregaDetalleRepository entregaDetalleRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private AbonoService abonoService;
    
    @Autowired
    private AbonoRepository abonoRepository;

    public List<EntregaDetalle> obtenerTodos() {
        return entregaDetalleRepository.findAll();
    }

    public Optional<EntregaDetalle> obtenerPorId(Long id) {
        return entregaDetalleRepository.findById(id);
    }

    public List<EntregaDetalle> obtenerPorEntrega(Long entregaId) {
        return entregaDetalleRepository.findByEntregaId(entregaId);
    }

    public List<EntregaDetalle> obtenerPorOrden(Long ordenId) {
        return entregaDetalleRepository.findByOrdenId(ordenId);
    }

    public Optional<EntregaDetalle> obtenerPorEntregaYOrden(Long entregaId, Long ordenId) {
        return entregaDetalleRepository.findByEntregaIdAndOrdenId(entregaId, ordenId);
    }

    public boolean existeOrdenEnEntrega(Long entregaId, Long ordenId) {
        return entregaDetalleRepository.existsByEntregaIdAndOrdenId(entregaId, ordenId);
    }

    public long contarDetallesPorEntrega(Long entregaId) {
        return entregaDetalleRepository.countByEntregaId(entregaId);
    }

    public EntregaDetalle crearDetalle(EntregaDetalle detalle) {
        // Validar que la orden existe
        if (detalle.getOrden() == null || !ordenRepository.existsById(detalle.getOrden().getId())) {
            throw new RuntimeException("La orden especificada no existe");
        }

        // ✅ VALIDACIÓN MEJORADA: Permitir la misma orden en diferentes tipos de transacciones
        // (abono + reembolso), pero evitar duplicados del MISMO tipo
        if (detalle.getEntrega() != null) {
            Long entregaId = detalle.getEntrega().getId();
            Long ordenId = detalle.getOrden().getId();
            
            // Obtener todos los detalles existentes de esta orden en esta entrega
            List<EntregaDetalle> detallesExistentes = entregaDetalleRepository.findByEntregaId(entregaId).stream()
                .filter(d -> d.getOrden() != null && d.getOrden().getId().equals(ordenId))
                .collect(java.util.stream.Collectors.toList());
            
            // Si hay detalles existentes, validar que no sean del mismo tipo
            for (EntregaDetalle existente : detallesExistentes) {
                // Si ambos son órdenes a contado (sin abono ni reembolso): duplicado
                if (existente.getAbono() == null && existente.getReembolsoVenta() == null &&
                    detalle.getAbono() == null && detalle.getReembolsoVenta() == null) {
                    throw new RuntimeException("La orden ya está incluida en esta entrega");
                }
                
                // Si ambos son abonos: verificar que no sea el mismo abono
                // ✅ Validar que los Abonos existan antes de acceder (maneja referencias huérfanas)
                if (existente.getAbono() != null && detalle.getAbono() != null) {
                    try {
                        Long abonoExistenteId = existente.getAbono().getId();
                        Long abonoDetalleId = detalle.getAbono().getId();
                        if (abonoExistenteId != null && abonoDetalleId != null && 
                            abonoExistenteId.equals(abonoDetalleId)) {
                            throw new RuntimeException("Este abono ya está incluido en esta entrega");
                        }
                    } catch (jakarta.persistence.EntityNotFoundException e) {
                        // Uno de los Abonos fue eliminado (referencia huérfana), continuar sin error
                        // No es el mismo abono si uno no existe
                    }
                }
                
                // Si ambos son reembolsos: verificar que no sea el mismo reembolso
                if (existente.getReembolsoVenta() != null && detalle.getReembolsoVenta() != null) {
                    if (existente.getReembolsoVenta().getId().equals(detalle.getReembolsoVenta().getId())) {
                        throw new RuntimeException("Este reembolso ya está incluido en esta entrega");
                    }
                }
            }
        }

        // Capturar datos de la orden en el momento de la entrega
        Orden orden = ordenRepository.findById(detalle.getOrden().getId())
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // ✅ VALIDACIÓN 1: Verificar que la orden no esté ya incluida en OTRA entrega
        if (orden.isIncluidaEntrega()) {
            // Verificar si está en esta entrega o en otra
            if (detalle.getEntrega() == null || 
                !entregaDetalleRepository.existsByEntregaIdAndOrdenId(detalle.getEntrega().getId(), orden.getId())) {
                throw new RuntimeException("La orden ya está incluida en otra entrega");
            }
        }

        // ✅ VALIDACIÓN 2: Si es orden a crédito, validar que el crédito esté abierto
        // IMPORTANTE: Esta validación SOLO aplica a abonos (ingresos), NO a reembolsos (egresos)
        if (orden.isCredito() && detalle.getReembolsoVenta() == null) {
            // Verificar que el crédito no esté cerrado (completamente saldado)
            if (orden.getCreditoDetalle() != null) {
                if (orden.getCreditoDetalle().getEstado() == com.casaglass.casaglass_backend.model.Credito.EstadoCredito.CERRADO) {
                    throw new RuntimeException("No se puede agregar una orden a crédito completamente saldada. El dinero ya fue entregado en entregas anteriores.");
                }
            }
        }

        // ✅ VALIDACIÓN 3: Si es un reembolso, verificar que no esté ya incluido en otra entrega
        if (detalle.getReembolsoVenta() != null) {
            Long reembolsoId = detalle.getReembolsoVenta().getId();
            
            // Buscar si este reembolso ya está en alguna entrega
            List<EntregaDetalle> detallesConReembolso = entregaDetalleRepository.findAll().stream()
                .filter(d -> d.getReembolsoVenta() != null && d.getReembolsoVenta().getId().equals(reembolsoId))
                .collect(java.util.stream.Collectors.toList());
            
            for (EntregaDetalle existente : detallesConReembolso) {
                // Si el reembolso está en otra entrega (no en esta), rechazar
                if (detalle.getEntrega() == null || 
                    !existente.getEntrega().getId().equals(detalle.getEntrega().getId())) {
                    throw new RuntimeException("Este reembolso ya está incluido en otra entrega de dinero");
                }
            }
        }

        // Establecer la orden en el detalle para que inicializarDesdeOrden() funcione
        detalle.setOrden(orden);
        
        // Inicializar todos los campos snapshot desde la orden (incluye clienteNombre y ventaCredito)
        detalle.inicializarDesdeOrden();

        EntregaDetalle detalleCreado = entregaDetalleRepository.save(detalle);

        // ✅ IMPORTANTE: Solo marcar la orden como incluida si es ORDEN A CONTADO
        // Para órdenes a crédito, NO se marca como incluida porque se pueden agregar múltiples abonos
        if (!orden.isCredito()) {
            orden.setIncluidaEntrega(true);
            ordenRepository.save(orden);
        }

        return detalleCreado;
    }
    
    /**
     * Crea un detalle de entrega desde un ABONO específico
     * Permite agregar abonos individuales de órdenes a crédito a diferentes entregas
     */
    public EntregaDetalle crearDetalleDesdeAbono(EntregaDetalle detalle, Long abonoId) {
        // Validar que el abono existe
        Abono abono = abonoRepository.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado con ID: " + abonoId));
        
        // Validar que el abono tiene orden asociada
        if (abono.getOrden() == null) {
            throw new RuntimeException("El abono no tiene una orden asociada");
        }
        
        Orden orden = abono.getOrden();
        
        // Verificar que la orden no esté ya incluida en esta entrega (para evitar duplicados)
        if (detalle.getEntrega() != null && 
            entregaDetalleRepository.existsByEntregaIdAndOrdenId(detalle.getEntrega().getId(), orden.getId())) {
            // Verificar si ya existe un detalle con este mismo abono
            // ✅ Validar que el Abono exista antes de acceder (maneja referencias huérfanas)
            boolean existeAbono = entregaDetalleRepository.findByEntregaId(detalle.getEntrega().getId()).stream()
                    .anyMatch(d -> {
                        if (d.getAbono() == null) return false;
                        try {
                            return d.getAbono().getId() != null && d.getAbono().getId().equals(abonoId);
                        } catch (jakarta.persistence.EntityNotFoundException e) {
                            // El Abono fue eliminado (referencia huérfana), no es el mismo
                            return false;
                        }
                    });
            if (existeAbono) {
                throw new RuntimeException("Este abono ya está incluido en esta entrega");
            }
        }
        
        // Validar que la orden tenga un crédito asociado (solo para verificar estructura)
        if (orden.getCreditoDetalle() == null) {
            throw new RuntimeException("La orden no tiene un crédito asociado");
        }
        
        // ✅ NO validamos el estado del crédito porque:
        // - El abono ya fue realizado y necesita ser entregado
        // - El abono apareció en la lista de disponibles
        // - El estado del crédito no impide entregar un abono ya realizado
        
        // Ya no validamos el período de la entrega, solo verificamos que el abono no esté duplicado
        
        // Establecer el abono y la orden en el detalle
        detalle.setAbono(abono);
        detalle.setOrden(orden);
        
        // Inicializar desde el abono
        detalle.inicializarDesdeAbono(abono);
        
        EntregaDetalle detalleCreado = entregaDetalleRepository.save(detalle);
        
        // ✅ NO marcar la orden como incluida - permite agregar otros abonos de la misma orden
        
        return detalleCreado;
    }

    public EntregaDetalle actualizarDetalle(Long id, EntregaDetalle detalleActualizado) {
        return entregaDetalleRepository.findById(id)
                .map(detalle -> {
                    // Solo permitir actualizar ciertos campos, los datos de snapshot no cambian
                    if (detalleActualizado.getObservaciones() != null) {
                        detalle.setObservaciones(detalleActualizado.getObservaciones());
                    }

                    return entregaDetalleRepository.save(detalle);
                })
                .orElseThrow(() -> new RuntimeException("Detalle de entrega no encontrado con id: " + id));
    }

    public void eliminarDetalle(Long id) {
        EntregaDetalle detalle = entregaDetalleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Detalle de entrega no encontrado con id: " + id));

        // Desmarcar la orden como no incluida en entrega SOLO si es orden a contado
        // Para órdenes a crédito con abonos, no se marca como incluida, así que no hay que desmarcar
        if (detalle.getOrden() != null && !detalle.getOrden().isCredito()) {
            Orden orden = detalle.getOrden();
            orden.setIncluidaEntrega(false);
            ordenRepository.save(orden);
        }

        entregaDetalleRepository.deleteById(id);
    }

    public void eliminarDetallesPorEntrega(Long entregaId) {
        List<EntregaDetalle> detalles = entregaDetalleRepository.findByEntregaId(entregaId);
        
        // Desmarcar todas las órdenes como no incluidas SOLO si son órdenes a contado
        // Para órdenes a crédito con abonos, no se marca como incluida, así que no hay que desmarcar
        for (EntregaDetalle detalle : detalles) {
            if (detalle.getOrden() != null && !detalle.getOrden().isCredito()) {
                Orden orden = detalle.getOrden();
                orden.setIncluidaEntrega(false);
                ordenRepository.save(orden);
            }
        }

        entregaDetalleRepository.deleteByEntregaId(entregaId);
    }

    public boolean validarOrdenParaEntrega(Long ordenId) {
        Optional<Orden> orden = ordenRepository.findById(ordenId);
        if (!orden.isPresent()) {
            return false;
        }
        
        Orden ordenObj = orden.get();
        
        // ✅ VALIDACIÓN 1: No debe estar incluida en otra entrega
        if (ordenObj.isIncluidaEntrega()) {
            return false;
        }
        
        // ✅ VALIDACIÓN 2: Si es orden a crédito, debe tener crédito abierto y abonos
        if (ordenObj.isCredito()) {
            if (ordenObj.getCreditoDetalle() == null) {
                return false; // No tiene crédito asociado
            }
            
            // No debe estar completamente saldada
            if (ordenObj.getCreditoDetalle().getEstado() == com.casaglass.casaglass_backend.model.Credito.EstadoCredito.CERRADO) {
                return false; // Ya está completamente pagada
            }
        }
        
        return true;
    }

    public Double calcularMontoTotalEntrega(Long entregaId) {
        return entregaDetalleRepository.calcularMontoTotalPorEntrega(entregaId);
    }

    /**
     * 💰 CALCULA EL DINERO REAL A ENTREGAR
     * - Órdenes A CONTADO: Monto completo de la orden (montoOrden)
     * - Órdenes A CRÉDITO: Monto del abono específico (si hay abono) o montoOrden
     */
    public Double calcularDineroRealEntrega(Long entregaId) {
        List<EntregaDetalle> detalles = entregaDetalleRepository.findByEntregaId(entregaId);
        Double total = 0.0;
        
        for (EntregaDetalle detalle : detalles) {
            // Usar el montoOrden que ya está capturado en el snapshot del detalle
            // Para créditos con abono específico, el montoOrden ya contiene el monto del abono
            total += (detalle.getMontoOrden() != null ? detalle.getMontoOrden() : 0.0);
        }
        
        return Math.round(total * 100.0) / 100.0; // Redondear a 2 decimales
    }

    /**
     * 📋 OBTIENE ÓRDENES A CONTADO DISPONIBLES PARA ENTREGA
     * Solo órdenes que NO son crédito y NO han sido incluidas en entregas
     */
    public List<Orden> obtenerOrdenesContadoDisponibles(Long sedeId, java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta) {
        // Buscar órdenes que cumplan:
        // 1. De la sede especificada
        // 2. En el rango de fechas
        // 3. credito = false (venta a contado)
        // 4. incluidaEntrega = false (no incluida en otra entrega)
        // 5. estado = ACTIVA
        
        return ordenRepository.findOrdenesContadoDisponiblesParaEntrega(sedeId, fechaDesde, fechaHasta);
    }

    /**
     * 📋 OBTIENE ÓRDENES A CRÉDITO CON ABONOS EN EL PERÍODO
     * Solo órdenes a crédito que tienen abonos en el período especificado
     */
    public List<Orden> obtenerOrdenesConAbonosEnPeriodo(Long sedeId, java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta) {
        // Buscar órdenes que cumplan:
        // 1. De la sede especificada
        // 2. credito = true (venta a crédito)
        // 3. Que tengan abonos en el período especificado
        // 4. estado = ACTIVA
        
        return ordenRepository.findOrdenesConAbonosEnPeriodo(sedeId, fechaDesde, fechaHasta);
    }
}