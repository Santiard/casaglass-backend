package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.EntregaDinero;
import com.casaglass.casaglass_backend.model.EntregaDetalle;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.repository.EntregaDineroRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EntregaDineroService {

    @Autowired
    private EntregaDineroRepository entregaDineroRepository;

    @Autowired
    private EntregaDetalleService entregaDetalleService;

    @Autowired
    private SedeRepository sedeRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerTodas() {
        return entregaDineroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<EntregaDinero> obtenerPorId(Long id) {
        return entregaDineroRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerPorSede(Long sedeId) {
        return entregaDineroRepository.findBySedeId(sedeId);
    }

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerPorEmpleado(Long empleadoId) {
        return entregaDineroRepository.findByEmpleadoId(empleadoId);
    }

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerPorEstado(EntregaDinero.EstadoEntrega estado) {
        return entregaDineroRepository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerPorSedeYEstado(Long sedeId, EntregaDinero.EstadoEntrega estado) {
        return entregaDineroRepository.findBySedeIdAndEstado(sedeId, estado);
    }

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerPorPeriodo(LocalDate desde, LocalDate hasta) {
        return entregaDineroRepository.findByFechaEntregaBetween(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<EntregaDinero> obtenerPorSedeYPeriodo(Long sedeId, LocalDate desde, LocalDate hasta) {
        return entregaDineroRepository.findBySedeIdAndFechaEntregaBetween(sedeId, desde, hasta);
    }

    public Double obtenerTotalEntregadoPorSedeEnPeriodo(Long sedeId, LocalDate desde, LocalDate hasta) {
        Double total = entregaDineroRepository.getTotalEntregadoBySedeAndPeriodo(sedeId, desde, hasta);
        return total != null ? total : 0.0;
    }


    @Transactional(readOnly = true)
    public List<Object[]> obtenerResumenPorEmpleado(Long sedeId, LocalDate desde, LocalDate hasta) {
        return entregaDineroRepository.getResumenByEmpleado(sedeId, desde, hasta);
    }

    @Transactional
    public EntregaDinero crearEntrega(EntregaDinero entrega, List<Long> ordenIds, List<Long> abonoIds) {
        // Validar que la sede existe
        if (entrega.getSede() == null || !sedeRepository.existsById(entrega.getSede().getId())) {
            throw new RuntimeException("La sede especificada no existe");
        }

        // Validar que el empleado exists
        if (entrega.getEmpleado() == null || !trabajadorRepository.existsById(entrega.getEmpleado().getId())) {
            throw new RuntimeException("El empleado especificado no existe");
        }

        // Establecer valores por defecto
        if (entrega.getFechaEntrega() == null) {
            entrega.setFechaEntrega(LocalDate.now());
        }

        if (entrega.getEstado() == null) {
            entrega.setEstado(EntregaDinero.EstadoEntrega.PENDIENTE);
        }

        // Validar órdenes
        if (ordenIds != null && !ordenIds.isEmpty()) {
            for (Long ordenId : ordenIds) {
                if (!entregaDetalleService.validarOrdenParaEntrega(ordenId)) {
                    throw new RuntimeException("La orden con ID " + ordenId + " no es válida para entrega");
                }
            }
        }

        // Normalizar desgloses
        entrega.setMontoEfectivo(entrega.getMontoEfectivo() != null ? entrega.getMontoEfectivo() : 0.0);
        entrega.setMontoTransferencia(entrega.getMontoTransferencia() != null ? entrega.getMontoTransferencia() : 0.0);
        entrega.setMontoCheque(entrega.getMontoCheque() != null ? entrega.getMontoCheque() : 0.0);
        entrega.setMontoDeposito(entrega.getMontoDeposito() != null ? entrega.getMontoDeposito() : 0.0);
        
        // Si no se proporciona monto, calcularlo desde el desglose
        if (entrega.getMonto() == null || entrega.getMonto() == 0.0) {
            Double sumaDesglose = entrega.getMontoEfectivo() + entrega.getMontoTransferencia() + 
                                   entrega.getMontoCheque() + entrega.getMontoDeposito();
            entrega.setMonto(sumaDesglose);
        } else {
            // Validar que el monto coincida con la suma del desglose
            Double sumaDesglose = entrega.getMontoEfectivo() + entrega.getMontoTransferencia() + 
                                   entrega.getMontoCheque() + entrega.getMontoDeposito();
            if (Math.abs(sumaDesglose - entrega.getMonto()) > 0.01) {
                throw new IllegalArgumentException("La suma del desglose no coincide con el monto");
            }
        }

        // Guardar la entrega primero
        EntregaDinero entregaGuardada = entregaDineroRepository.save(entrega);

        // Crear detalles de entrega para cada orden A CONTADO
        if (ordenIds != null && !ordenIds.isEmpty()) {
            for (Long ordenId : ordenIds) {
                EntregaDetalle detalle = new EntregaDetalle();
                detalle.setEntrega(entregaGuardada);
                
                // Buscar la orden y asignarla
                Orden orden = new Orden();
                orden.setId(ordenId);
                detalle.setOrden(orden);
                
                entregaDetalleService.crearDetalle(detalle);
            }
        }
        
        // Crear detalles de entrega para cada ABONO (órdenes a crédito)
        if (abonoIds != null && !abonoIds.isEmpty()) {
            for (Long abonoId : abonoIds) {
                EntregaDetalle detalle = new EntregaDetalle();
                detalle.setEntrega(entregaGuardada);
                
                entregaDetalleService.crearDetalleDesdeAbono(detalle, abonoId);
            }
        }
        
        // Recalcular monto desde las órdenes/abonos
        if ((ordenIds != null && !ordenIds.isEmpty()) || (abonoIds != null && !abonoIds.isEmpty())) {
            Double montoCalculado = entregaDetalleService.calcularMontoTotalEntrega(
                entregaGuardada.getId()
            );
            entregaGuardada.setMonto(montoCalculado != null ? montoCalculado : 0.0);
            
            // Actualizar el desglose si el monto fue calculado y no hay desglose
            if (entregaGuardada.getMontoEfectivo() == 0.0 && 
                entregaGuardada.getMontoTransferencia() == 0.0 &&
                entregaGuardada.getMontoCheque() == 0.0 &&
                entregaGuardada.getMontoDeposito() == 0.0) {
                // Por defecto, todo en efectivo
                entregaGuardada.setMontoEfectivo(entregaGuardada.getMonto());
            }
        }

        return entregaDineroRepository.save(entregaGuardada);
    }

    @Transactional
    public EntregaDinero actualizarEntrega(Long id, EntregaDinero entregaActualizada) {
        return entregaDineroRepository.findById(id)
                .map(entrega -> {
                    entrega.setFechaEntrega(entregaActualizada.getFechaEntrega());
                    entrega.setMontoEfectivo(entregaActualizada.getMontoEfectivo() != null ? entregaActualizada.getMontoEfectivo() : 0.0);
                    entrega.setMontoTransferencia(entregaActualizada.getMontoTransferencia() != null ? entregaActualizada.getMontoTransferencia() : 0.0);
                    entrega.setMontoCheque(entregaActualizada.getMontoCheque() != null ? entregaActualizada.getMontoCheque() : 0.0);
                    entrega.setMontoDeposito(entregaActualizada.getMontoDeposito() != null ? entregaActualizada.getMontoDeposito() : 0.0);
                    
                    // Calcular monto desde el desglose o usar el proporcionado
                    if (entregaActualizada.getMonto() != null) {
                        entrega.setMonto(entregaActualizada.getMonto());
                        Double sumaDesglose = entrega.getMontoEfectivo() + entrega.getMontoTransferencia() + 
                                             entrega.getMontoCheque() + entrega.getMontoDeposito();
                        if (Math.abs(sumaDesglose - entrega.getMonto()) > 0.01) {
                            throw new IllegalArgumentException("La suma del desglose no coincide con el monto");
                        }
                    } else {
                        // Calcular monto desde el desglose
                        Double sumaDesglose = entrega.getMontoEfectivo() + entrega.getMontoTransferencia() + 
                                             entrega.getMontoCheque() + entrega.getMontoDeposito();
                        entrega.setMonto(sumaDesglose);
                    }
                    
                    if (entregaActualizada.getSede() != null && sedeRepository.existsById(entregaActualizada.getSede().getId())) {
                        entrega.setSede(entregaActualizada.getSede());
                    }

                    if (entregaActualizada.getEmpleado() != null && trabajadorRepository.existsById(entregaActualizada.getEmpleado().getId())) {
                        entrega.setEmpleado(entregaActualizada.getEmpleado());
                    }

                    return entregaDineroRepository.save(entrega);
                })
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada con id: " + id));
    }

    @Transactional
    public EntregaDinero confirmarEntrega(Long id) {
        return entregaDineroRepository.findById(id)
                .map(entrega -> {
                    entrega.setEstado(EntregaDinero.EstadoEntrega.ENTREGADA);
                    
                    // Validar que el monto coincida con el desglose
                    Double sumaDesglose = (entrega.getMontoEfectivo() != null ? entrega.getMontoEfectivo() : 0.0)
                            + (entrega.getMontoTransferencia() != null ? entrega.getMontoTransferencia() : 0.0)
                            + (entrega.getMontoCheque() != null ? entrega.getMontoCheque() : 0.0)
                            + (entrega.getMontoDeposito() != null ? entrega.getMontoDeposito() : 0.0);
                    
                    // Asegurar que el monto coincida con el desglose
                    if (Math.abs(sumaDesglose - (entrega.getMonto() != null ? entrega.getMonto() : 0.0)) > 0.01) {
                        entrega.setMonto(sumaDesglose);
                    }
                    
                    return entregaDineroRepository.save(entrega);
                })
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada con id: " + id));
    }

    @Transactional
    public EntregaDinero cancelarEntrega(Long id) {
        return entregaDineroRepository.findById(id)
                .map(entrega -> {
                    entrega.setEstado(EntregaDinero.EstadoEntrega.RECHAZADA);
                    return entregaDineroRepository.save(entrega);
                })
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada con id: " + id));
    }

    @Transactional
    public void eliminarEntrega(Long id) {
        EntregaDinero entrega = entregaDineroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada con id: " + id));

        if (entrega.getEstado() == EntregaDinero.EstadoEntrega.ENTREGADA) {
            throw new RuntimeException("No se puede eliminar una entrega ya confirmada");
        }

        // Eliminar detalles (esto desmarcará las órdenes)
        entregaDetalleService.eliminarDetallesPorEntrega(id);

        entregaDineroRepository.deleteById(id);
    }

    public boolean validarEntregaCompleta(Long id) {
        EntregaDinero entrega = entregaDineroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entrega no encontrada con id: " + id));

        return entrega.getMonto() != null && entrega.getMonto() > 0.0 && 
               entrega.getEstado() == EntregaDinero.EstadoEntrega.ENTREGADA;
    }
}