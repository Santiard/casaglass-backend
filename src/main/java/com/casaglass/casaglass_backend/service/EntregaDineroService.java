package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ResumenMesDTO;
import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.EntregaDinero;
import com.casaglass.casaglass_backend.model.EntregaDetalle;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.ReembolsoVenta;
import com.casaglass.casaglass_backend.repository.AbonoRepository;
import com.casaglass.casaglass_backend.repository.CreditoRepository;
import com.casaglass.casaglass_backend.repository.EntregaDineroRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.ReembolsoVentaRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    @Autowired
    private ReembolsoVentaRepository reembolsoVentaRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private AbonoRepository abonoRepository;

    @Autowired
    private AbonoService abonoService;

    @Autowired
    private CreditoRepository creditoRepository;

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

    /**
     * 🚀 LISTADO DE ENTREGAS DE DINERO CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     * Nota: conDiferencias no está implementado actualmente (requiere cálculo adicional)
     */
    @Transactional(readOnly = true)
    public Object obtenerEntregasConFiltros(
            Long sedeId,
            Long empleadoId,
            EntregaDinero.EstadoEntrega estado,
            LocalDate desde,
            LocalDate hasta,
            Boolean conDiferencias, // No implementado actualmente
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar fechas
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
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
        
        // Buscar entregas con filtros
        List<EntregaDinero> entregas = entregaDineroRepository.buscarConFiltros(
            sedeId, empleadoId, estado, desde, hasta
        );
        
        // TODO: Filtrar por conDiferencias si se implementa
        // if (conDiferencias != null && conDiferencias) {
        //     entregas = entregas.stream()
        //         .filter(e -> tieneDiferencias(e))
        //         .collect(Collectors.toList());
        // }
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por fecha DESC)
        if (!sortBy.equals("fecha") || !sortOrder.equals("DESC")) {
            entregas = aplicarOrdenamientoEntregas(entregas, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = entregas.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, entregas.size());
            
            if (fromIndex >= entregas.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<EntregaDinero> contenido = entregas.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return entregas;
    }
    
    /**
     * Aplica ordenamiento a la lista de entregas según sortBy y sortOrder
     */
    private List<EntregaDinero> aplicarOrdenamientoEntregas(List<EntregaDinero> entregas, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                entregas.sort((a, b) -> {
                    int cmp = a.getFechaEntrega().compareTo(b.getFechaEntrega());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "id":
                entregas.sort((a, b) -> {
                    int cmp = Long.compare(a.getId(), b.getId());
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                entregas.sort((a, b) -> b.getFechaEntrega().compareTo(a.getFechaEntrega()));
        }
        
        return entregas;
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
        return crearEntregaConReembolsos(entrega, ordenIds, abonoIds, null);
    }

    @Transactional
    public EntregaDinero crearEntregaConReembolsos(EntregaDinero entrega, List<Long> ordenIds, List<Long> abonoIds, List<Long> reembolsoIds) {
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

        // Si no llegan IDs desde frontend, resolver automáticamente pendientes desde
        // la última entrega de la sede hasta la fecha de esta entrega.
        boolean sinOrdenes = ordenIds == null || ordenIds.isEmpty();
        boolean sinAbonos = abonoIds == null || abonoIds.isEmpty();
        boolean sinReembolsos = reembolsoIds == null || reembolsoIds.isEmpty();
        if (sinOrdenes && sinAbonos && sinReembolsos) {
            LocalDate[] rango = resolverRangoDesdeUltimaEntrega(entrega.getSede().getId(), entrega.getFechaEntrega());
            LocalDate fechaDesde = rango[0];
            LocalDate fechaHasta = rango[1];

            List<Orden> ordenesDisponibles = entregaDetalleService
                .obtenerOrdenesContadoDisponibles(entrega.getSede().getId(), fechaDesde, fechaHasta);
            List<Abono> abonosDisponibles = abonoService
                .obtenerAbonosDisponiblesParaEntrega(entrega.getSede().getId(), fechaDesde, fechaHasta);

            ordenIds = ordenesDisponibles.stream().map(Orden::getId).toList();
            abonoIds = abonosDisponibles.stream().map(Abono::getId).toList();
            reembolsoIds = new ArrayList<>();
        }

        // Validar órdenes
        if (ordenIds != null && !ordenIds.isEmpty()) {
            for (Long ordenId : ordenIds) {
                // ⚠️ VALIDAR: No permitir órdenes del cliente especial (ID 499)
                Orden orden = ordenRepository.findById(ordenId)
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID " + ordenId));
                
                if (orden.getCliente() != null && orden.getCliente().getId().equals(499L)) {
                    throw new RuntimeException("No se pueden crear entregas de dinero para el cliente especial. Las órdenes de JAIRO JAVIER VELANDIA se manejan de forma independiente.");
                }
                
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
                // ⚠️ VALIDAR: No permitir abonos del cliente especial (ID 499)
                Abono abono = abonoRepository.findById(abonoId)
                    .orElseThrow(() -> new RuntimeException("Abono no encontrado con ID " + abonoId));
                
                if (abono.getCliente() != null && abono.getCliente().getId().equals(499L)) {
                    throw new RuntimeException("No se pueden crear entregas de dinero para el cliente especial. Los abonos de JAIRO JAVIER VELANDIA se manejan de forma independiente.");
                }
                
                EntregaDetalle detalle = new EntregaDetalle();
                detalle.setEntrega(entregaGuardada);
                
                entregaDetalleService.crearDetalleDesdeAbono(detalle, abonoId);
            }
        }
        
        // 🆕 Crear detalles de entrega para cada REEMBOLSO (egresos)
        if (reembolsoIds != null && !reembolsoIds.isEmpty()) {
            for (Long reembolsoId : reembolsoIds) {
                ReembolsoVenta reembolso = reembolsoVentaRepository.findById(reembolsoId)
                    .orElseThrow(() -> new RuntimeException("Reembolso no encontrado con ID: " + reembolsoId));
                
                // Validar que el reembolso esté procesado
                if (!reembolso.getProcesado() || reembolso.getEstado() != ReembolsoVenta.EstadoReembolso.PROCESADO) {
                    throw new RuntimeException("El reembolso #" + reembolsoId + " no está procesado");
                }
                
                EntregaDetalle detalle = new EntregaDetalle();
                detalle.setEntrega(entregaGuardada);
                detalle.inicializarDesdeReembolso(reembolso);
                
                entregaDetalleService.crearDetalle(detalle);
            }
        }
        
        // Recalcular monto desde las órdenes/abonos/reembolsos
        if ((ordenIds != null && !ordenIds.isEmpty()) || 
            (abonoIds != null && !abonoIds.isEmpty()) || 
            (reembolsoIds != null && !reembolsoIds.isEmpty())) {
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

    @Transactional(readOnly = true)
    public LocalDate[] resolverRangoDesdeUltimaEntrega(Long sedeId, LocalDate fechaHastaSolicitada) {
        if (sedeId == null) {
            throw new IllegalArgumentException("La sede es obligatoria para calcular el rango automático");
        }

        LocalDate fechaHasta = fechaHastaSolicitada != null ? fechaHastaSolicitada : LocalDate.now();
        EntregaDinero ultimaEntrega = entregaDineroRepository.findFirstBySedeIdOrderByFechaEntregaDesc(sedeId);

        LocalDate fechaDesde;
        if (ultimaEntrega != null && ultimaEntrega.getFechaEntrega() != null) {
            // Importante: usar la misma fecha de la última entrega (no +1 día)
            // para no perder movimientos de la tarde cuando hubo una entrega en la mañana.
            fechaDesde = ultimaEntrega.getFechaEntrega();
        } else {
            // Primera entrega de la sede: incluir histórico sin depender de un día puntual.
            fechaDesde = LocalDate.of(2000, 1, 1);
        }

        if (fechaDesde.isAfter(fechaHasta)) {
            fechaDesde = fechaHasta;
        }

        return new LocalDate[]{fechaDesde, fechaHasta};
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
    
    /**
     * Calcula el resumen del mes de una entrega específica
     * @param fechaEntrega La fecha de la entrega para determinar el mes a analizar
     * @return ResumenMesDTO con totales de ventas y créditos activos del mes
     */
    @Transactional(readOnly = true)
    public ResumenMesDTO calcularResumenMes(EntregaDinero entrega) {
        if (entrega == null || entrega.getFechaEntrega() == null) {
            return null;
        }
        
        LocalDate fechaEntrega = entrega.getFechaEntrega();
        
        // Calcular primer y último día del mes
        LocalDate inicioMes = fechaEntrega.withDayOfMonth(1);
        LocalDate finMes = fechaEntrega.withDayOfMonth(fechaEntrega.lengthOfMonth());
        
        // 1. Calcular total de ventas del mes (órdenes con venta=true del mes activas)
        List<Orden> ordenesDelMes = ordenRepository.findByFechaBetween(inicioMes, finMes);
        Double totalVentasDelMes = ordenesDelMes.stream()
                .filter(orden -> orden.isVenta() && orden.getEstado() == Orden.EstadoOrden.ACTIVA)
                .mapToDouble(orden -> orden.getTotal() != null ? orden.getTotal() : 0.0)
                .sum();
        
        // 2. Calcular total de deudas (créditos activos iniciados en el mes)
        List<Credito> creditosActivos = creditoRepository.findByEstado(Credito.EstadoCredito.ABIERTO);
        Double totalDeudasDelMes = creditosActivos.stream()
                .filter(credito -> credito.getFechaInicio() != null && 
                        !credito.getFechaInicio().isBefore(inicioMes) && 
                        !credito.getFechaInicio().isAfter(finMes))
                .mapToDouble(credito -> credito.getSaldoPendiente() != null ? credito.getSaldoPendiente() : 0.0)
                .sum();
        
        // 3. Calcular total de abonos del mes
        List<Abono> abonos = abonoRepository.findByFechaBetween(inicioMes, finMes);
        Double totalAbonasDelMes = abonos.stream()
            .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
                .sum();
        
        // 4. Total entregado del mes (solo de la misma sede de la entrega)
           List<EntregaDinero> entregasDelMes = entrega.getSede() != null
               ? entregaDineroRepository.findBySedeIdAndFechaEntregaBetween(entrega.getSede().getId(), inicioMes, finMes)
               : entregaDineroRepository.findByFechaEntregaBetween(inicioMes, finMes);
        Double totalEntregadoDelMes = entregasDelMes.stream()
                .mapToDouble(ent -> ent.getMonto() != null ? ent.getMonto() : 0.0)
                .sum();
        
        // 5. Generar nombre del mes en formato ISO "2026-04"
        String mesISO = String.format("%04d-%02d", fechaEntrega.getYear(), fechaEntrega.getMonthValue());
        
        // 6. Generar nombre del mes en formato "febrero 2026"
        String mesNombre = fechaEntrega.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                + " " + fechaEntrega.getYear();
        
        // 7. Información de sede y trabajador
        String sedeNombre = entrega.getSede() != null ? entrega.getSede().getNombre() : "N/A";
        String trabajadorNombre = entrega.getEmpleado() != null ? entrega.getEmpleado().getNombre() : "N/A";
        
        // Crear y retornar el resumen
        ResumenMesDTO resumen = new ResumenMesDTO();
        resumen.setTotalVentasDelMes(totalVentasDelMes);
        resumen.setTotalDeudasDelMes(totalDeudasDelMes);
        resumen.setTotalAbonasDelMes(totalAbonasDelMes);
        resumen.setTotalEntregadoDelMes(totalEntregadoDelMes);
        resumen.setTotalEstaEntrega(entrega.getMonto() != null ? entrega.getMonto() : 0.0);
        resumen.setMes(mesISO);
        resumen.setSede(sedeNombre);
        resumen.setTrabajador(trabajadorNombre);
        resumen.setMesNombre(mesNombre);
        
        return resumen;
    }
    
    public ResumenMesDTO calcularResumenMes(LocalDate fechaEntrega) {
        if (fechaEntrega == null) {
            return null;
        }
        
        // Calcular primer y último día del mes
        LocalDate inicioMes = fechaEntrega.withDayOfMonth(1);
        LocalDate finMes = fechaEntrega.withDayOfMonth(fechaEntrega.lengthOfMonth());
        
        // 1. Calcular total de ventas del mes (órdenes con venta=true del mes)
        List<Orden> ordenesDelMes = ordenRepository.findByFechaBetween(inicioMes, finMes);
        Double totalVentasDelMes = ordenesDelMes.stream()
                .filter(orden -> orden.isVenta() && orden.getEstado() == Orden.EstadoOrden.ACTIVA)
                .mapToDouble(orden -> orden.getTotal() != null ? orden.getTotal() : 0.0)
                .sum();
        
        // 2. Calcular total de créditos activos del mes
        List<Credito> creditosActivos = creditoRepository.findByEstado(Credito.EstadoCredito.ABIERTO);
               List<Credito> creditosDelMes = creditoRepository.findByFechaInicioBetween(inicioMes, finMes);
               Double totalDeudasDelMes = creditosDelMes.stream()
                   .filter(credito -> credito.getEstado() == Credito.EstadoCredito.ABIERTO)
                .mapToDouble(credito -> credito.getSaldoPendiente() != null ? credito.getSaldoPendiente() : 0.0)
                .sum();
        
        // 3. Calcular total de abonos del mes
        List<Abono> abonos = abonoRepository.findByFechaBetween(inicioMes, finMes);
        Double totalAbonasDelMes = abonos.stream()
            .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
                .sum();
        
        // 4. Total entregado del mes
               List<EntregaDinero> entregasDelMes = entregaDineroRepository.findByFechaEntregaBetween(inicioMes, finMes);
        Double totalEntregadoDelMes = entregasDelMes.stream()
                .mapToDouble(ent -> ent.getMonto() != null ? ent.getMonto() : 0.0)
                .sum();
        
        // 5. Generar nombre del mes en formato ISO "2026-04"
        String mesISO = String.format("%04d-%02d", fechaEntrega.getYear(), fechaEntrega.getMonthValue());
        
        // 6. Generar nombre del mes en formato "febrero 2026"
        String mesNombre = fechaEntrega.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                + " " + fechaEntrega.getYear();
        
        ResumenMesDTO resumen = new ResumenMesDTO();
        resumen.setTotalVentasDelMes(totalVentasDelMes);
        resumen.setTotalDeudasDelMes(totalDeudasDelMes);
        resumen.setTotalAbonasDelMes(totalAbonasDelMes);
        resumen.setTotalEntregadoDelMes(totalEntregadoDelMes);
        resumen.setMes(mesISO);
        resumen.setMesNombre(mesNombre);
        
        return resumen;
    }
}