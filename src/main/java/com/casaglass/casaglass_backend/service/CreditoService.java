package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.model.EntregaClienteEspecial;
import com.casaglass.casaglass_backend.repository.CreditoRepository;
import com.casaglass.casaglass_backend.repository.FacturaRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CreditoService {

    private final CreditoRepository creditoRepo;
    private final EntityManager entityManager;
    private final FacturaRepository facturaRepository;
    private final EntregaClienteEspecialService entregaClienteEspecialService;
    private final SedeRepository sedeRepository;

    public CreditoService(CreditoRepository creditoRepo,
                          EntityManager entityManager,
                          FacturaRepository facturaRepository,
                          EntregaClienteEspecialService entregaClienteEspecialService,
                          SedeRepository sedeRepository) {
        this.creditoRepo = creditoRepo;
        this.entityManager = entityManager;
        this.facturaRepository = facturaRepository;
        this.entregaClienteEspecialService = entregaClienteEspecialService;
        this.sedeRepository = sedeRepository;
    }

    /* ---------- Helpers de dinero (redondeado a 2 decimales) ---------- */

    private Double normalize(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }

    /* --------------------- Operaciones de negocio --------------------- */

    public Optional<Credito> obtener(Long id) { 
        return creditoRepo.findById(id); 
    }

    public Optional<Credito> obtenerPorOrden(Long ordenId) { 
        return creditoRepo.findByOrdenId(ordenId); 
    }

    public List<Credito> listar() { 
        return creditoRepo.findAll(); 
    }

    public List<Credito> listarPorCliente(Long clienteId) { 
        return creditoRepo.findByClienteId(clienteId); 
    }
    
    /**
     * ⭐ ESTADO DE CUENTA DEL CLIENTE ESPECIAL
     * Retorna SOLO créditos ABIERTOS con saldo pendiente > 0 del cliente especial (ID 499)
     * 
     * @param sedeId (Opcional) ID de la sede para filtrar
     * @return Lista de créditos activos con deuda pendiente
     */
    @Transactional(readOnly = true)
    public List<com.casaglass.casaglass_backend.dto.CreditoResponseDTO> obtenerEstadoCuentaClienteEspecial(
            Long sedeId) {
        
        // Usar método específico para cliente especial
        List<Credito> creditos = creditoRepo.buscarClienteEspecial(
            sedeId,
            Credito.EstadoCredito.ABIERTO,
            null,
            null
        );
        
        // Filtrar solo créditos con saldo pendiente > 0
        // Ordenar por fecha de inicio (más antiguos primero)
        return creditos.stream()
                .filter(c -> c.getSaldoPendiente() != null && c.getSaldoPendiente() > 0)
                .sorted((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .map(com.casaglass.casaglass_backend.dto.CreditoResponseDTO::new)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 📊 ESTADO DE CUENTA DE UN CLIENTE
     * Retorna SOLO créditos ABIERTOS con saldo pendiente > 0
     * ⚠️ EXCLUYE al cliente especial (ID 499)
     * Ordenados por fecha de inicio (más antiguos primero)
     * 
     * @param clienteId ID del cliente
     * @param sedeId (Opcional) ID de la sede para filtrar
     * @return Lista de créditos activos con deuda pendiente
     */
    @Transactional(readOnly = true)
    public List<com.casaglass.casaglass_backend.dto.CreditoResponseDTO> obtenerEstadoCuenta(
            Long clienteId, 
            Long sedeId) {
        
        List<Credito> creditos;
        
        if (sedeId != null) {
            // Filtrar por cliente y sede
            creditos = creditoRepo.buscarConFiltros(
                clienteId, 
                sedeId, 
                Credito.EstadoCredito.ABIERTO, 
                null, 
                null
            );
        } else {
            // Solo filtrar por cliente
            creditos = creditoRepo.buscarConFiltros(
                clienteId, 
                null, 
                Credito.EstadoCredito.ABIERTO, 
                null, 
                null
            );
        }
        
        // Filtrar solo créditos con saldo pendiente > 0
        // Ordenar por fecha de inicio (más antiguos primero)
        return creditos.stream()
                .filter(c -> c.getSaldoPendiente() != null && c.getSaldoPendiente() > 0)
                .sorted((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .map(com.casaglass.casaglass_backend.dto.CreditoResponseDTO::new)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 📅 LISTAR CRÉDITOS DE UN CLIENTE CON FILTROS DE FECHA
     * Retorna todos los créditos del cliente, opcionalmente filtrados por fecha de la orden
     * @param clienteId ID del cliente
     * @param fechaDesde Fecha desde (inclusive) - filtra por orden.fecha
     * @param fechaHasta Fecha hasta (inclusive) - filtra por orden.fecha
     * @return Lista de créditos mapeados a CreditoResponseDTO
     */
    @Transactional(readOnly = true)
    public List<com.casaglass.casaglass_backend.dto.CreditoResponseDTO> listarCreditosClienteConFiltros(
            Long clienteId,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        
        List<Credito> creditos = creditoRepo.findByClienteId(clienteId);
        
        // Filtrar por fecha de la orden si se proporcionan fechas
        if (fechaDesde != null || fechaHasta != null) {
            creditos = creditos.stream()
                .filter(credito -> {
                    LocalDate fechaOrden = credito.getOrden().getFecha();
                    
                    if (fechaDesde != null && fechaHasta != null) {
                        // Ambas fechas: entre fechaDesde y fechaHasta (inclusive)
                        return !fechaOrden.isBefore(fechaDesde) && !fechaOrden.isAfter(fechaHasta);
                    } else if (fechaDesde != null) {
                        // Solo fechaDesde: >= fechaDesde
                        return !fechaOrden.isBefore(fechaDesde);
                    } else {
                        // Solo fechaHasta: <= fechaHasta
                        return !fechaOrden.isAfter(fechaHasta);
                    }
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Mapear a DTO
        return creditos.stream()
            .map(com.casaglass.casaglass_backend.dto.CreditoResponseDTO::new)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 🚀 LISTADO DE CRÉDITOS CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    /**
     * ⭐ Listar SOLO créditos del cliente especial (ID 499 - JAIRO JAVIER VELANDIA)
     */
    public Object listarClienteEspecial(
            Long sedeId,
            Credito.EstadoCredito estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
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
        
        // Buscar créditos SOLO del cliente especial
        List<Credito> creditos = creditoRepo.buscarClienteEspecial(
            sedeId, estado, fechaDesde, fechaHasta
        );
        
        // Aplicar ordenamiento
        creditos = aplicarOrdenamientoCreditos(creditos, sortBy, sortOrder);
        
        // Si se solicita paginación
        if (page != null && size != null) {
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200;
            
            long totalElements = creditos.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, creditos.size());
            
            if (fromIndex >= creditos.size()) {
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            List<Credito> creditosPagina = creditos.subList(fromIndex, toIndex);
            return com.casaglass.casaglass_backend.dto.PageResponse.of(creditosPagina, totalElements, page, size);
        }
        
        return creditos;
    }
    
    /**
     * 📋 Listar créditos con filtros (EXCLUYE al cliente especial ID 499)
     */
    public Object listarConFiltros(
            Long clienteId,
            Long sedeId,
            Credito.EstadoCredito estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
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
        
        // Buscar créditos con filtros
        List<Credito> creditos = creditoRepo.buscarConFiltros(
            clienteId, sedeId, estado, fechaDesde, fechaHasta
        );
        
        // Aplicar ordenamiento
        creditos = aplicarOrdenamientoCreditos(creditos, sortBy, sortOrder);
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200; // Límite máximo para créditos
            
            long totalElements = creditos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, creditos.size());
            
            if (fromIndex >= creditos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Credito> creditosPagina = creditos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(creditosPagina, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return creditos;
    }
    
    /**
     * Aplica ordenamiento a la lista de créditos según sortBy y sortOrder
     */
    private List<Credito> aplicarOrdenamientoCreditos(List<Credito> creditos, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                creditos.sort((a, b) -> {
                    int cmp = a.getFechaInicio().compareTo(b.getFechaInicio());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "montototal":
            case "monto_total":
            case "totalcredito":
            case "total_credito":
                creditos.sort((a, b) -> {
                    int cmp = Double.compare(a.getTotalCredito() != null ? a.getTotalCredito() : 0.0,
                                            b.getTotalCredito() != null ? b.getTotalCredito() : 0.0);
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "saldopendiente":
            case "saldo_pendiente":
                creditos.sort((a, b) -> {
                    int cmp = Double.compare(a.getSaldoPendiente() != null ? a.getSaldoPendiente() : 0.0,
                                            b.getSaldoPendiente() != null ? b.getSaldoPendiente() : 0.0);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                creditos.sort((a, b) -> b.getFechaInicio().compareTo(a.getFechaInicio()));
        }
        
        return creditos;
    }

    public List<Credito> listarPorEstado(Credito.EstadoCredito estado) { 
        return creditoRepo.findByEstado(estado); 
    }

    /**
     * 💳 CREAR CRÉDITO PARA UNA ORDEN
     * Se ejecuta cuando una orden se marca como crédito
     * 
     * @param ordenId ID de la orden
     * @param clienteId ID del cliente
     * @param totalOrden Total de la orden (total facturado CON IVA, sin restar retención)
     * @param retencionFuente Valor de la retención en la fuente (se resta del saldo pendiente inicial)
     * @param retencionIca Valor de la retención ICA (se resta del saldo pendiente inicial)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Credito crearCreditoParaOrden(Long ordenId, Long clienteId, Double totalOrden, Double retencionFuente, Double retencionIca) {
        try {
            // Verificar que no exista ya un crédito para esta orden
            Optional<Credito> existente = creditoRepo.findByOrdenId(ordenId);
            if (existente.isPresent()) {
                return existente.get(); // Devolver el existente en lugar de fallar
            }

            // ⚠️ OBTENER LA ORDEN COMPLETA PARA ESTABLECER RELACIÓN BIDIRECCIONAL
            Orden orden = entityManager.find(Orden.class, ordenId);
            if (orden == null) {
                throw new IllegalArgumentException("Orden no encontrada con ID: " + ordenId);
            }

            // ✅ VALIDAR QUE totalOrden NO SEA NULL O CERO
            if (totalOrden == null || totalOrden <= 0) {
                throw new IllegalArgumentException("El total de la orden debe ser mayor a 0. Total recibido: " + totalOrden);
            }

            // ✅ CALCULAR SALDO PENDIENTE INICIAL: Total orden - Retención de fuente - Retención ICA
            Double retencionFuenteValor = (retencionFuente != null && retencionFuente > 0) ? retencionFuente : 0.0;
            Double retencionIcaValor = (retencionIca != null && retencionIca > 0) ? retencionIca : 0.0;
            Double saldoPendienteInicial = totalOrden - retencionFuenteValor - retencionIcaValor;
            
            // ✅ VALIDAR QUE saldoPendienteInicial SEA POSITIVO
            if (saldoPendienteInicial <= 0) {
                // Aún así crear el crédito, pero con saldo mínimo de 0.01 para que aparezca en la consulta
                saldoPendienteInicial = 0.01;
            }

            Credito credito = new Credito();
            credito.setCliente(entityManager.getReference(Cliente.class, clienteId));
            credito.setOrden(orden); // Usar la orden completa, no una referencia
            credito.setFechaInicio(LocalDate.now());
            credito.setTotalCredito(normalize(totalOrden));
            credito.setTotalAbonado(0.0);
            credito.setSaldoPendiente(normalize(saldoPendienteInicial)); // ✅ Ahora resta la retención
            credito.setEstado(Credito.EstadoCredito.ABIERTO);

            // ⚡ ESTABLECER RELACIÓN BIDIRECCIONAL CORRECTAMENTE
            orden.setCreditoDetalle(credito);

            Credito creditoGuardado = creditoRepo.save(credito);
            return creditoGuardado;

        } catch (Exception e) {
            throw new RuntimeException("Error al crear crédito: " + e.getMessage(), e);
        }
    }

    /**
     * 🔄 ACTUALIZAR CRÉDITO PARA UNA ORDEN
     * Se ejecuta cuando se actualiza una orden que tiene crédito
     * 
     * @param creditoId ID del crédito a actualizar
     * @param nuevoTotalOrden Nuevo total de la orden (total facturado CON IVA, sin restar retención)
     * @param nuevaRetencionFuente Nueva retención de fuente (se resta del saldo pendiente)
     * @param nuevaRetencionIca Nueva retención ICA (se resta del saldo pendiente)
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Credito actualizarCreditoParaOrden(Long creditoId, Double nuevoTotalOrden, Double nuevaRetencionFuente, Double nuevaRetencionIca) {
        try {
            Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado con ID: " + creditoId));

            // Actualizar el total del crédito
            Double totalNormalizado = normalize(nuevoTotalOrden);
            credito.setTotalCredito(totalNormalizado);

            // ✅ RECALCULAR SALDO PENDIENTE CONSIDERANDO LAS RETENCIONES
            // El saldo pendiente inicial debe ser: totalOrden - retencionFuente - retencionIca
            Double retencionFuenteValor = (nuevaRetencionFuente != null && nuevaRetencionFuente > 0) ? nuevaRetencionFuente : 0.0;
            Double retencionIcaValor = (nuevaRetencionIca != null && nuevaRetencionIca > 0) ? nuevaRetencionIca : 0.0;
            Double saldoPendienteInicial = totalNormalizado - retencionFuenteValor - retencionIcaValor;

            // El saldo pendiente actual = saldo pendiente inicial - total abonado
            Double totalAbonado = credito.getTotalAbonado() != null ? credito.getTotalAbonado() : 0.0;
            Double nuevoSaldoPendiente = Math.max(0, saldoPendienteInicial - totalAbonado);

            credito.setSaldoPendiente(normalize(nuevoSaldoPendiente));

            // Actualizar estado si el saldo es 0
            if (nuevoSaldoPendiente <= 0.0) {
                credito.setEstado(Credito.EstadoCredito.CERRADO);
                if (credito.getFechaCierre() == null) {
                    credito.setFechaCierre(LocalDate.now());
                }
            } else if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
                // Si había estado cerrado pero ahora tiene saldo, reabrir
                credito.setEstado(Credito.EstadoCredito.ABIERTO);
                credito.setFechaCierre(null);
            }

            Credito creditoActualizado = creditoRepo.save(credito);
            return creditoActualizado;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar crédito: " + e.getMessage(), e);
        }
    }

    /**
     * 💰 REGISTRAR ABONO A UN CRÉDITO
     * Actualiza automáticamente los totales y el estado
     */
    @Transactional
    public Credito registrarAbono(Long creditoId, Double montoAbono) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
        }

        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito anulado");
        }

        Double montoNormalizado = normalize(montoAbono);
        if (montoNormalizado <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a 0");
        }

        // Actualizar totales
        credito.setTotalAbonado(normalize(credito.getTotalAbonado() + montoNormalizado));
        credito.actualizarSaldo();

        return creditoRepo.save(credito);
    }

    /**
     * 🔄 RECALCULAR TOTALES DE UN CRÉDITO
     * Útil para sincronizar después de cambios en abonos
     */
    @Transactional
    public Credito recalcularTotales(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        // Recalcular total abonado sumando todos los abonos
        Double totalAbonos = credito.getAbonos().stream()
                .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
                .sum();

        credito.setTotalAbonado(normalize(totalAbonos));
        credito.actualizarSaldo();
        sincronizarSaldoPosteriorEnAbonos(credito);
        return creditoRepo.save(credito);
    }

    /**
     * Reescribe {@link Abono#getSaldo()} en cada abono del crédito: es el saldo pendiente
     * justo después de aplicar ese abono, en orden cronológico (fecha, id).
     * Así, si se elimina o cambia un abono, los snapshots no quedan desactualizados.
     */
    private void sincronizarSaldoPosteriorEnAbonos(Credito credito) {
        if (credito.getAbonos() == null || credito.getAbonos().isEmpty()) {
            return;
        }
        double retFuente = 0.0;
        double retIca = 0.0;
        Orden orden = credito.getOrden();
        if (orden != null) {
            if (orden.isTieneRetencionFuente() && orden.getRetencionFuente() != null) {
                retFuente = orden.getRetencionFuente();
            }
            if (orden.isTieneRetencionIca() && orden.getRetencionIca() != null) {
                retIca = orden.getRetencionIca();
            }
        }
        double totalCredito = credito.getTotalCredito() != null ? credito.getTotalCredito() : 0.0;
        double running = normalize(totalCredito - retFuente - retIca);

        List<Abono> ordenados = new ArrayList<>(credito.getAbonos());
        ordenados.sort(Comparator
                .comparing(Abono::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Abono::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        for (Abono a : ordenados) {
            double monto = normalize(a.getTotal() != null ? a.getTotal() : 0.0);
            running = normalize(running - monto);
            a.setSaldo(running);
        }
    }

    /**
     * ❌ ANULAR CRÉDITO
     * Se ejecuta cuando se anula la orden asociada
     */
    @Transactional
    public Credito anularCredito(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        credito.setEstado(Credito.EstadoCredito.ANULADO);
        credito.setFechaCierre(LocalDate.now());

        return creditoRepo.save(credito);
    }

    /**
     * 🏁 CERRAR CRÉDITO MANUALMENTE
     * Para casos especiales donde se quiere cerrar sin estar completamente pagado
     */
    @Transactional
    public Credito cerrarCredito(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        credito.setEstado(Credito.EstadoCredito.CERRADO);
        credito.setFechaCierre(LocalDate.now());
        // Ajustar saldo a 0 si se cierra manualmente
        credito.setSaldoPendiente(0.0);

        return creditoRepo.save(credito);
    }

    @Transactional
    public void eliminar(Long creditoId) {
        creditoRepo.deleteById(creditoId);
    }

    /**
     * 💰 LISTAR CRÉDITOS PENDIENTES DE UN CLIENTE
     * 
     * Endpoint especializado para la página de abonos.
     * Retorna SOLO los créditos con saldo pendiente > 0 y estado ABIERTO.
     * 
     * Características:
     * - Filtra automáticamente: estado = ABIERTO AND saldoPendiente > 0
     * - Incluye todos los datos necesarios para registrar abonos
     * - Incluye información de retención de fuente
     * - Incluye subtotal (necesario para calcular retención)
     * - Ordenado por fecha de orden (más recientes primero)
     * 
     * @param clienteId ID del cliente
     * @param sedeId    Si no es {@code null}, solo créditos cuya {@code orden.sede} coincide (recomendado: sede del usuario en abonos).
     *                  Si es {@code null}, lista todos los pendientes del cliente (comportamiento anterior; sin filtro por sede).
     * @return Lista de CreditoPendienteDTO con toda la información necesaria
     */
    @Transactional(readOnly = true)
    public List<com.casaglass.casaglass_backend.dto.CreditoPendienteDTO> listarCreditosPendientes(Long clienteId,
                                                                                                   Long sedeId) {
        if (clienteId == null) {
            throw new IllegalArgumentException("clienteId es obligatorio");
        }
        if (sedeId != null && !sedeRepository.existsById(sedeId)) {
            throw new IllegalArgumentException("Sede no encontrada");
        }

        // ✅ Buscar todos y filtrar manualmente (evita problemas de precisión con Double en el repositorio)
        List<Credito> todosCreditos = creditoRepo.findByClienteId(clienteId);

        List<Credito> creditosFiltrados = todosCreditos.stream()
            .filter(c -> c.getEstado() == Credito.EstadoCredito.ABIERTO)
            .filter(c -> c.getSaldoPendiente() != null && c.getSaldoPendiente() > 0.001)
            .filter(c -> sedeId == null
                    || (c.getOrden() != null && c.getOrden().getSede() != null
                        && Objects.equals(c.getOrden().getSede().getId(), sedeId)))
            .collect(java.util.stream.Collectors.toList());

        // Convertir a DTO
        List<com.casaglass.casaglass_backend.dto.CreditoPendienteDTO> resultado = creditosFiltrados.stream()
            .map(credito -> {
                com.casaglass.casaglass_backend.dto.CreditoPendienteDTO dto = 
                    new com.casaglass.casaglass_backend.dto.CreditoPendienteDTO(credito);
                
                // Buscar número de factura si la orden existe
                if (credito.getOrden() != null && credito.getOrden().getId() != null) {
                    String numeroFactura = facturaRepository.findByOrdenId(credito.getOrden().getId())
                        .map(factura -> factura.getNumeroFactura())
                        .orElse("-");
                    dto.setNumeroFactura(numeroFactura);
                }
                
                return dto;
            })
            .sorted((a, b) -> {
                // Ordenar por fecha de orden (más recientes primero)
                if (a.getOrdenFecha() == null && b.getOrdenFecha() == null) return 0;
                if (a.getOrdenFecha() == null) return 1;
                if (b.getOrdenFecha() == null) return -1;
                return b.getOrdenFecha().compareTo(a.getOrdenFecha());
            })
            .collect(java.util.stream.Collectors.toList());

        return resultado;
    }

    /**
     * 💰 MARCAR CRÉDITOS DEL CLIENTE ESPECIAL COMO PAGADOS (SIN REGISTRO DE ABONOS)
     * 
     * Este método es específico para el cliente especial (ID 499 - JAIRO JAVIER VELANDIA)
     * que paga en persona sin necesidad de registro detallado de abonos.
     * 
     * Marca directamente los créditos como CERRADOS estableciendo:
     * - saldoPendiente = 0.0
     * - totalAbonado = totalCredito (menos retención si aplica)
     * - estado = CERRADO
     * - fechaCierre = fecha actual
     * 
     * @param creditoIds Lista de IDs de créditos a marcar como pagados
     * @return Número de créditos marcados como pagados
     * @throws IllegalArgumentException Si algún crédito no pertenece al cliente especial o no existe
     * @throws IllegalStateException Si algún crédito ya está cerrado
     */
    @Transactional
    public EntregaClienteEspecial marcarCreditosClienteEspecialComoPagados(List<Long> creditoIds,
                                                                            String ejecutadoPor,
                                                                            String observaciones) {
        if (creditoIds == null || creditoIds.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un ID de crédito");
        }

        List<EntregaClienteEspecialService.DetalleRegistro> registros = new ArrayList<>();

        for (Long creditoId : creditoIds) {
            Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado con ID: " + creditoId));

            if (credito.getCliente() == null || !credito.getCliente().getId().equals(499L)) {
                throw new IllegalArgumentException(
                    "El crédito con ID " + creditoId + " no pertenece al cliente especial. " +
                    "Este endpoint solo puede marcar como pagados los créditos de JAIRO JAVIER VELANDIA (ID 499)."
                );
            }

            if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
                throw new IllegalStateException(
                    "El crédito con ID " + creditoId + " ya está cerrado. " +
                    "No se puede marcar como pagado nuevamente."
                );
            }

            Double saldoAnterior = credito.getSaldoPendiente() != null ? credito.getSaldoPendiente() : 0.0;

            Double retencionFuente = 0.0;
            if (credito.getOrden() != null && 
                credito.getOrden().isTieneRetencionFuente() && 
                credito.getOrden().getRetencionFuente() != null) {
                retencionFuente = credito.getOrden().getRetencionFuente();
            }

            Double totalAAbonar = credito.getTotalCredito() - retencionFuente;
            
            credito.setTotalAbonado(normalize(totalAAbonar));
            credito.setSaldoPendiente(0.0);
            credito.setEstado(Credito.EstadoCredito.CERRADO);
            credito.setFechaCierre(LocalDate.now());

            creditoRepo.save(credito);

            registros.add(new EntregaClienteEspecialService.DetalleRegistro(credito, saldoAnterior));
        }

        return entregaClienteEspecialService.registrarEntrega(registros, ejecutadoPor, observaciones);
    }
}