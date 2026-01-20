package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.AbonoDTO;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.AbonoRepository;
import com.casaglass.casaglass_backend.repository.CreditoRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AbonoService {

    private final AbonoRepository abonoRepo;
    private final CreditoRepository creditoRepo;
    private final OrdenRepository ordenRepo;
    private final SedeRepository sedeRepo;
    private final CreditoService creditoService;

    public AbonoService(AbonoRepository abonoRepo,
                        CreditoRepository creditoRepo,
                        OrdenRepository ordenRepo,
                        SedeRepository sedeRepo,
                        CreditoService creditoService) {
        this.abonoRepo = abonoRepo;
        this.creditoRepo = creditoRepo;
        this.ordenRepo = ordenRepo;
        this.sedeRepo = sedeRepo;
        this.creditoService = creditoService;
    }

    /* -------- Helpers de dinero (redondeado a 2 decimales) -------- */

    private Double norm(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }

    /* ------------------- Consultas ------------------- */

    public Optional<Abono> obtener(Long abonoId) {
        return abonoRepo.findById(abonoId);
    }

    public List<Abono> listarPorCredito(Long creditoId) {
        return abonoRepo.findByCreditoId(creditoId);
    }

    public List<Abono> listarPorCliente(Long clienteId) {
        return abonoRepo.findByClienteId(clienteId);
    }

    /**
     * Lista abonos de un cliente con filtros opcionales de fecha
     * Optimizado para mejorar rendimiento al filtrar en la base de datos
     */
    public List<Abono> listarPorClienteConFiltros(Long clienteId, LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null) {
            return abonoRepo.findByClienteIdAndFechaBetween(clienteId, fechaDesde, fechaHasta);
        }
        return abonoRepo.findByClienteId(clienteId);
    }

    public List<Abono> listarPorOrden(Long ordenId) {
        return abonoRepo.findByOrdenId(ordenId);
    }

    /* ----------------- Crear / Actualizar / Eliminar ----------------- */

    /**
     * 💰 CREAR ABONO DESDE DTO (MÉTODO SIMPLIFICADO PARA FRONTEND)
     * Crea un abono usando solo los datos mínimos del frontend
     */
    @Transactional
    public Abono crearDesdeDTO(Long creditoId, AbonoDTO abonoDTO) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado: " + creditoId));

        // Validar que el crédito esté abierto
        if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
        }
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito anulado");
        }

        // Normalizar y validar monto
        Double monto = norm(abonoDTO.getTotal());
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        // Validar que no exceda el saldo pendiente
        if (monto > credito.getSaldoPendiente()) {
            throw new IllegalArgumentException(
                String.format("El abono ($%.2f) excede el saldo pendiente ($%.2f)", 
                            monto, credito.getSaldoPendiente())
            );
        }

        // ✅ VALIDAR Y OBTENER LA SEDE DONDE SE REGISTRA EL ABONO
        if (abonoDTO.getSedeId() == null) {
            throw new IllegalArgumentException("El ID de la sede es obligatorio");
        }
        
        Sede sedeAbono = sedeRepo.findById(abonoDTO.getSedeId())
            .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + abonoDTO.getSedeId()));

        // Obtener la orden del crédito (para mantener referencia, pero la sede del abono puede ser diferente)
        Orden ordenCredito = credito.getOrden();

        // Crear el abono con los datos del DTO
        Abono abono = new Abono();
        abono.setCredito(credito);
        abono.setCliente(credito.getCliente());
        abono.setSede(sedeAbono); // ✅ Sede donde se registra el pago (puede ser diferente a la sede de la orden)
        abono.setOrden(ordenCredito); // Mantener referencia a la orden original
        if (ordenCredito != null) {
            abono.setNumeroOrden(ordenCredito.getNumero());
        }
        abono.setFecha(abonoDTO.getFecha());
        abono.setMetodoPago(abonoDTO.getMetodoPago());
        abono.setFactura(abonoDTO.getFactura());
        abono.setTotal(monto);
        
        // 💰 MONTOS POR MÉTODO DE PAGO
        abono.setMontoEfectivo(abonoDTO.getMontoEfectivo() != null ? abonoDTO.getMontoEfectivo() : 0.0);
        abono.setMontoTransferencia(abonoDTO.getMontoTransferencia() != null ? abonoDTO.getMontoTransferencia() : 0.0);
        abono.setMontoCheque(abonoDTO.getMontoCheque() != null ? abonoDTO.getMontoCheque() : 0.0);
        abono.setMontoRetencion(abonoDTO.getMontoRetencion() != null ? abonoDTO.getMontoRetencion() : 0.0);
        
        // ✅ VALIDAR QUE LA SUMA DE MÉTODOS DE PAGO IGUALA EL TOTAL
        Double sumaMetodos = abono.getMontoEfectivo() + abono.getMontoTransferencia() + abono.getMontoCheque();
        if (Math.abs(sumaMetodos - monto) > 0.01) { // Tolerancia de 1 centavo por redondeo
            throw new IllegalArgumentException(
                String.format("La suma de los métodos de pago ($%.2f) no coincide con el monto total ($%.2f)", 
                            sumaMetodos, monto)
            );
        }

        // Calcular saldo posterior al abono
        Double saldoPosterior = norm(credito.getSaldoPendiente() - monto);
        abono.setSaldo(saldoPosterior);

        // Guardar abono
        Abono guardado = abonoRepo.save(abono);

        // Actualizar totales del crédito
        credito.setTotalAbonado(norm(credito.getTotalAbonado() + monto));
        credito.setSaldoPendiente(saldoPosterior);
        
        // Actualizar estado si se pagó completamente
        if (saldoPosterior <= 0.0) {
            credito.setEstado(Credito.EstadoCredito.CERRADO);
            credito.setFechaCierre(LocalDate.now());
        }
        
        creditoRepo.save(credito);

        return guardado;
    }

    /**
     * 💰 CREAR ABONO PARA UN CRÉDITO
     * Crea un abono y actualiza automáticamente los totales del crédito
     */
    @Transactional
    public Abono crear(Long creditoId, Abono payload) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado: " + creditoId));

        // Validar que el crédito esté abierto
        if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
        }
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito anulado");
        }

        // Normalizar y validar monto
        if (payload.getTotal() == null) {
            throw new IllegalArgumentException("El monto (total) es obligatorio");
        }
        Double monto = norm(payload.getTotal());
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        // Validar que no exceda el saldo pendiente
        if (monto > credito.getSaldoPendiente()) {
            throw new IllegalArgumentException(
                String.format("El abono ($%.2f) excede el saldo pendiente ($%.2f)", 
                            monto, credito.getSaldoPendiente())
            );
        }

        // Cliente: usar el del crédito
        Cliente clienteCredito = credito.getCliente();
        if (payload.getCliente() != null && payload.getCliente().getId() != null &&
            !Objects.equals(payload.getCliente().getId(), clienteCredito.getId())) {
            throw new IllegalArgumentException("El cliente del abono no coincide con el del crédito");
        }

        // Orden: debe ser la orden del crédito (si se especifica)
        Orden orden = credito.getOrden(); // En el nuevo modelo, cada crédito tiene una orden específica
        if (payload.getOrden() != null && payload.getOrden().getId() != null) {
            if (!Objects.equals(payload.getOrden().getId(), orden.getId())) {
                throw new IllegalArgumentException("El abono debe aplicarse a la orden del crédito");
            }
        }

        // ✅ Sede: usar la del payload si existe, sino usar la de la orden como fallback
        Sede sedeAbono;
        if (payload.getSede() != null && payload.getSede().getId() != null) {
            sedeAbono = sedeRepo.findById(payload.getSede().getId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + payload.getSede().getId()));
        } else if (orden != null && orden.getSede() != null) {
            // Fallback: usar la sede de la orden si no se especifica
            sedeAbono = orden.getSede();
        } else {
            throw new IllegalArgumentException("La sede es obligatoria para crear un abono");
        }

        // Crear el abono
        Abono abono = new Abono();
        abono.setCredito(credito);
        abono.setCliente(clienteCredito);
        abono.setSede(sedeAbono); // ✅ Sede donde se registra el pago
        abono.setOrden(orden);
        if (orden != null) {
            abono.setNumeroOrden(orden.getNumero());
        }
        abono.setFecha(payload.getFecha() != null ? payload.getFecha() : LocalDate.now());
        abono.setMetodoPago(payload.getMetodoPago() != null ? payload.getMetodoPago() : "TRANSFERENCIA");
        abono.setFactura(payload.getFactura());
        abono.setTotal(monto);

        // Calcular saldo posterior al abono
        Double saldoPosterior = norm(credito.getSaldoPendiente() - monto);
        abono.setSaldo(saldoPosterior);

        // Guardar abono
        Abono guardado = abonoRepo.save(abono);

        // Actualizar crédito usando el método del crédito
        credito.agregarAbono(guardado);
        creditoRepo.save(credito);

        return guardado;
    }

    /**
     * ✏️ ACTUALIZAR ABONO EXISTENTE
     */
    @Transactional
    public Abono actualizar(Long creditoId, Long abonoId, Abono payload) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));

        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }

        Credito credito = abono.getCredito();
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden modificar abonos de un crédito anulado");
        }

        // Permitir edición de fecha, método de pago, factura
        if (payload.getFecha() != null) abono.setFecha(payload.getFecha());
        if (payload.getMetodoPago() != null) abono.setMetodoPago(payload.getMetodoPago());
        if (payload.getFactura() != null) abono.setFactura(payload.getFactura());

        // Si se cambia el monto, validar y recalcular
        if (payload.getTotal() != null) {
            Double nuevoMonto = norm(payload.getTotal());
            if (nuevoMonto <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor a 0");
            }

            Double montoAnterior = abono.getTotal();
            Double diferencia = nuevoMonto - montoAnterior;
            Double nuevoSaldoPendiente = credito.getSaldoPendiente() + montoAnterior - nuevoMonto;

            if (nuevoSaldoPendiente < 0) {
                throw new IllegalArgumentException("El nuevo monto haría que se exceda el total del crédito");
            }

            abono.setTotal(nuevoMonto);
            abono.setSaldo(nuevoSaldoPendiente);
        }

        Abono actualizado = abonoRepo.save(abono);
        
        // Recalcular totales del crédito
        creditoService.recalcularTotales(creditoId);
        
        return actualizado;
    }

    /**
     * 🆕 ACTUALIZAR ABONO DESDE DTO (CON VALIDACIÓN DE CAMPOS NUMÉRICOS)
     * Método recomendado que valida la suma de métodos de pago
     */
    @Transactional
    public Abono actualizarDesdeDTO(Long creditoId, Long abonoId, AbonoDTO abonoDTO) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));

        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }

        Credito credito = abono.getCredito();
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden modificar abonos de un crédito anulado");
        }

        // Actualizar fecha, método de pago, factura
        if (abonoDTO.getFecha() != null) abono.setFecha(abonoDTO.getFecha());
        if (abonoDTO.getMetodoPago() != null) abono.setMetodoPago(abonoDTO.getMetodoPago());
        if (abonoDTO.getFactura() != null) abono.setFactura(abonoDTO.getFactura());
        
        // ✅ ACTUALIZAR SEDE (si se proporciona)
        if (abonoDTO.getSedeId() != null) {
            Sede nuevaSede = sedeRepo.findById(abonoDTO.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + abonoDTO.getSedeId()));
            abono.setSede(nuevaSede);
        }

        // ✅ ACTUALIZAR CAMPOS NUMÉRICOS
        abono.setMontoEfectivo(abonoDTO.getMontoEfectivo() != null ? abonoDTO.getMontoEfectivo() : 0.0);
        abono.setMontoTransferencia(abonoDTO.getMontoTransferencia() != null ? abonoDTO.getMontoTransferencia() : 0.0);
        abono.setMontoCheque(abonoDTO.getMontoCheque() != null ? abonoDTO.getMontoCheque() : 0.0);
        abono.setMontoRetencion(abonoDTO.getMontoRetencion() != null ? abonoDTO.getMontoRetencion() : 0.0);

        // Si se cambia el monto, validar y recalcular
        if (abonoDTO.getTotal() != null) {
            Double nuevoMonto = norm(abonoDTO.getTotal());
            if (nuevoMonto <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor a 0");
            }

            // ✅ VALIDAR QUE LA SUMA DE MÉTODOS COINCIDA CON EL TOTAL
            Double sumaMetodos = abono.getMontoEfectivo() + abono.getMontoTransferencia() + abono.getMontoCheque();
            if (Math.abs(sumaMetodos - nuevoMonto) > 0.01) {
                throw new IllegalArgumentException(
                    String.format("La suma de los métodos de pago ($%.2f) no coincide con el monto total ($%.2f)", 
                                sumaMetodos, nuevoMonto)
                );
            }

            Double montoAnterior = abono.getTotal();
            Double diferencia = nuevoMonto - montoAnterior;
            Double nuevoSaldoPendiente = credito.getSaldoPendiente() + montoAnterior - nuevoMonto;

            if (nuevoSaldoPendiente < 0) {
                throw new IllegalArgumentException("El nuevo monto haría que se exceda el total del crédito");
            }

            abono.setTotal(nuevoMonto);
            abono.setSaldo(nuevoSaldoPendiente);
        }

        Abono actualizado = abonoRepo.save(abono);
        
        // Recalcular totales del crédito
        creditoService.recalcularTotales(creditoId);
        
        return actualizado;
    }

    /**
     * 🗑️ ELIMINAR ABONO
     */
    @Transactional
    public void eliminar(Long creditoId, Long abonoId) {
        Abono abono = abonoRepo.findById(abonoId)
                .orElseThrow(() -> new RuntimeException("Abono no encontrado: " + abonoId));
        
        if (!abono.getCredito().getId().equals(creditoId)) {
            throw new IllegalArgumentException("El abono no pertenece al crédito indicado");
        }

        Credito credito = abono.getCredito();
        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden eliminar abonos de un crédito anulado");
        }

        abonoRepo.delete(abono);
        
        // Recalcular totales del crédito
        creditoService.recalcularTotales(creditoId);
    }

    /**
     * 💰 CALCULA ABONOS DE UNA ORDEN EN UN PERÍODO ESPECÍFICO
     * Usado para entregas de dinero - solo cuenta abonos realizados en el período
     */
    public Double calcularAbonosOrdenEnPeriodo(Long ordenId, LocalDate fechaDesde, LocalDate fechaHasta) {
        if (ordenId == null || fechaDesde == null || fechaHasta == null) {
            return 0.0;
        }
        
        List<Abono> abonos = abonoRepo.findByOrdenIdAndFechaBetween(ordenId, fechaDesde, fechaHasta);
        
        return abonos.stream()
                .filter(Objects::nonNull)
                .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
                .sum();
    }
    
    /**
     * 📋 OBTIENE ABONOS DISPONIBLES PARA ENTREGA
     * Solo abonos de órdenes a crédito que no han sido incluidos en entregas
     */
    public List<Abono> obtenerAbonosDisponiblesParaEntrega(Long sedeId, LocalDate fechaDesde, LocalDate fechaHasta) {
        return abonoRepo.findAbonosDisponiblesParaEntrega(sedeId, fechaDesde, fechaHasta);
    }

    /**
     * 🚀 LISTADO DE ABONOS CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object listarAbonosConFiltros(
            Long clienteId,
            Long creditoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String metodoPago,
            Long sedeId,
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
        
        // Buscar abonos con filtros
        List<Abono> abonos = abonoRepo.buscarConFiltros(
            clienteId, creditoId, fechaDesde, fechaHasta, metodoPago, sedeId
        );
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por fecha DESC)
        if (!sortBy.equals("fecha") || !sortOrder.equals("DESC")) {
            abonos = aplicarOrdenamientoAbonos(abonos, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200; // Límite máximo para abonos
            
            long totalElements = abonos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, abonos.size());
            
            if (fromIndex >= abonos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Abono> abonosPagina = abonos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(abonosPagina, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return abonos;
    }
    
    /**
     * Aplica ordenamiento a la lista de abonos según sortBy y sortOrder
     */
    private List<Abono> aplicarOrdenamientoAbonos(List<Abono> abonos, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                abonos.sort((a, b) -> {
                    int cmp = a.getFecha().compareTo(b.getFecha());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "total":
                abonos.sort((a, b) -> {
                    int cmp = Double.compare(a.getTotal() != null ? a.getTotal() : 0.0,
                                            b.getTotal() != null ? b.getTotal() : 0.0);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                abonos.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        }
        
        return abonos;
    }
}
