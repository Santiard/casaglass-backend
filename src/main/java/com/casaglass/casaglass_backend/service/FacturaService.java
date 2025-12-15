package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.FacturaCreateDTO;
import com.casaglass.casaglass_backend.dto.FacturaTablaDTO;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepo;
    private final OrdenRepository ordenRepository;
    private final ClienteRepository clienteRepository;
    private final SedeRepository sedeRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final EntityManager entityManager;
    private final BusinessSettingsRepository businessSettingsRepository;

    public FacturaService(
            FacturaRepository facturaRepo,
            OrdenRepository ordenRepository,
            ClienteRepository clienteRepository,
            SedeRepository sedeRepository,
            TrabajadorRepository trabajadorRepository,
            EntityManager entityManager,
            BusinessSettingsRepository businessSettingsRepository) {
        this.facturaRepo = facturaRepo;
        this.ordenRepository = ordenRepository;
        this.clienteRepository = clienteRepository;
        this.sedeRepository = sedeRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.entityManager = entityManager;
        this.businessSettingsRepository = businessSettingsRepository;
    }

    /**
     * 🧾 CREAR FACTURA DESDE DTO
     * Recibe solo IDs y busca las entidades completas
     */
    @Transactional
    public Factura crearFactura(FacturaCreateDTO facturaDTO) {
        System.out.println("🧾 Creando factura para orden ID: " + facturaDTO.getOrdenId());

        // Validar que no exista ya una factura para esta orden
        Optional<Factura> facturaExistente = facturaRepo.findByOrdenId(facturaDTO.getOrdenId());
        if (facturaExistente.isPresent()) {
            throw new IllegalArgumentException("Ya existe una factura para la orden " + facturaDTO.getOrdenId());
        }

        // Buscar orden existente
        Orden orden = ordenRepository.findById(facturaDTO.getOrdenId())
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + facturaDTO.getOrdenId()));

        // Verificar que la orden esté activa
        if (orden.getEstado() == Orden.EstadoOrden.ANULADA) {
            throw new IllegalArgumentException("No se puede facturar una orden anulada");
        }

        // Buscar cliente (opcional - si no se proporciona, se usa el de la orden)
        Cliente cliente = null;
        if (facturaDTO.getClienteId() != null) {
            cliente = clienteRepository.findById(facturaDTO.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + facturaDTO.getClienteId()));
        }

        // Crear factura
        Factura factura = new Factura();
        factura.setOrden(orden);
        // Si se proporciona un cliente, usarlo; si no, usar el de la orden (o null, se manejará en los DTOs)
        factura.setCliente(cliente);
        factura.setFecha(facturaDTO.getFecha() != null ? facturaDTO.getFecha() : LocalDate.now());
        factura.setSubtotal(facturaDTO.getSubtotal());
        factura.setDescuentos(facturaDTO.getDescuentos() != null ? facturaDTO.getDescuentos() : 0.0);
        // Calcular IVA: si viene en el DTO se usa, si no se calcula desde el subtotal
        if (facturaDTO.getIva() != null && facturaDTO.getIva() > 0) {
            factura.setIva(facturaDTO.getIva());
        } else {
            // Calcular IVA automáticamente desde el subtotal (que ya incluye IVA)
            Double ivaCalculado = calcularIvaDesdeSubtotal(facturaDTO.getSubtotal());
            factura.setIva(ivaCalculado);
        }
        factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
        factura.setFormaPago(facturaDTO.getFormaPago());
        factura.setObservaciones(facturaDTO.getObservaciones());
        factura.setEstado(Factura.EstadoFactura.PENDIENTE);

        // Calcular total automáticamente
        if (facturaDTO.getTotal() != null) {
            factura.setTotal(facturaDTO.getTotal());
        } else {
            factura.calcularTotal();
        }

        // Generar o usar número de factura
        if (facturaDTO.getNumeroFactura() != null && !facturaDTO.getNumeroFactura().isEmpty()) {
            factura.setNumeroFactura(facturaDTO.getNumeroFactura());
        } else {
            Long siguienteNumero = generarNumeroFactura();
            factura.setNumeroFactura(String.valueOf(siguienteNumero));
        }

        // Guardar factura
        Factura facturaGuardada = facturaRepo.save(factura);

        // Asegurar consistencia bidireccional: enlazar en la orden
        try {
            orden.setFactura(facturaGuardada);
            ordenRepository.save(orden);
        } catch (Exception ignore) {
            // Si el mapeo no es propietario, ignoramos el fallo; el cálculo de 'facturada' usa repo
        }

        System.out.println("✅ Factura creada exitosamente - Número: " + facturaGuardada.getNumeroFactura());

        return facturaGuardada;
    }

    /**
     * 💰 OBTENER TASA DE IVA DESDE CONFIGURACIÓN
     * Obtiene el IVA rate desde BusinessSettings, con fallback a 19% si no existe
     */
    private Double obtenerIvaRate() {
        try {
            // Buscar la primera configuración (debería haber solo una)
            List<BusinessSettings> settings = businessSettingsRepository.findAll();
            if (!settings.isEmpty() && settings.get(0).getIvaRate() != null) {
                Double ivaRate = settings.get(0).getIvaRate();
                System.out.println("💰 IVA Rate obtenido desde configuración: " + ivaRate + "%");
                return ivaRate;
            }
        } catch (Exception e) {
            System.err.println("⚠️ WARNING: No se pudo obtener IVA rate desde configuración: " + e.getMessage());
        }
        // Fallback a 19% por defecto
        System.out.println("💰 IVA Rate usando valor por defecto: 19.0%");
        return 19.0;
    }

    /**
     * 💰 CALCULAR IVA DESDE SUBTOTAL (QUE YA INCLUYE IVA)
     * Extrae el IVA del subtotal que ya lo incluye
     * Fórmula: IVA = Subtotal * (tasa_iva / (100 + tasa_iva))
     * Ejemplo con 19%: IVA = Subtotal * 0.19 / 1.19
     * 
     * @param subtotal Subtotal que ya incluye IVA
     * @return Valor del IVA extraído del subtotal
     */
    private Double calcularIvaDesdeSubtotal(Double subtotal) {
        if (subtotal == null || subtotal <= 0) {
            return 0.0;
        }
        Double ivaRate = obtenerIvaRate();
        // Fórmula: IVA = Subtotal * (tasa / (100 + tasa))
        // Ejemplo: Si subtotal = 119 y tasa = 19%, entonces IVA = 119 * 0.19 / 1.19 = 19
        Double iva = subtotal * (ivaRate / (100.0 + ivaRate));
        // Redondear a 2 decimales
        return Math.round(iva * 100.0) / 100.0;
    }

    /**
     * Genera el siguiente número de factura de forma thread-safe
     */
    private Long generarNumeroFactura() {
        int maxIntentos = 5;
        int intento = 0;

        while (intento < maxIntentos) {
            try {
                Long siguienteNumero = facturaRepo.obtenerSiguienteNumero();

                if (!facturaRepo.findByNumeroFactura(String.valueOf(siguienteNumero)).isPresent()) {
                    return siguienteNumero;
                }

                intento++;
                Thread.sleep(10);

            } catch (Exception e) {
                intento++;
                if (intento >= maxIntentos) {
                    throw new RuntimeException("Error generando número de factura después de " + maxIntentos + " intentos", e);
                }
            }
        }

        throw new RuntimeException("No se pudo generar un número de factura único después de " + maxIntentos + " intentos");
    }

    /**
     * Obtener factura por ID
     */
    @Transactional(readOnly = true)
    public Optional<Factura> obtenerPorId(Long id) {
        return facturaRepo.findById(id);
    }

    /**
     * Obtener factura por número
     */
    @Transactional(readOnly = true)
    public Optional<Factura> obtenerPorNumeroFactura(String numeroFactura) {
        return facturaRepo.findByNumeroFactura(numeroFactura);
    }

    /**
     * Obtener factura por orden
     */
    @Transactional(readOnly = true)
    public Optional<Factura> obtenerPorOrden(Long ordenId) {
        return facturaRepo.findByOrdenId(ordenId);
    }

    /**
     * Listar todas las facturas
     */
    @Transactional(readOnly = true)
    public List<Factura> listar() {
        return facturaRepo.findAll();
    }

    /**
     * Listar facturas para tabla (optimizado)
     */
    @Transactional(readOnly = true)
    public List<FacturaTablaDTO> listarParaTabla() {
        return facturaRepo.findAll().stream()
                .map(this::convertirAFacturaTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar facturas para tabla filtradas por sede
     * Filtra por la sede de la orden relacionada
     */
    @Transactional(readOnly = true)
    public List<FacturaTablaDTO> listarParaTablaPorSede(Long sedeId) {
        return facturaRepo.findAll().stream()
                .filter(factura -> {
                    if (factura.getOrden() != null && 
                        factura.getOrden().getSede() != null) {
                        return factura.getOrden().getSede().getId().equals(sedeId);
                    }
                    return false;
                })
                .map(this::convertirAFacturaTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Listar facturas por estado
     */
    @Transactional(readOnly = true)
    public List<Factura> listarPorEstado(Factura.EstadoFactura estado) {
        return facturaRepo.findByEstado(estado);
    }

    /**
     * Listar facturas por fecha
     */
    @Transactional(readOnly = true)
    public List<Factura> listarPorFecha(LocalDate fecha) {
        return facturaRepo.findByFecha(fecha);
    }

    /**
     * Listar facturas por rango de fechas
     */
    @Transactional(readOnly = true)
    public List<Factura> listarPorRangoFechas(LocalDate desde, LocalDate hasta) {
        return facturaRepo.findByFechaBetween(desde, hasta);
    }

    /**
     * Marcar factura como pagada
     */
    @Transactional
    public Factura marcarComoPagada(Long facturaId, LocalDate fechaPago) {
        Factura factura = facturaRepo.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + facturaId));

        if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            throw new IllegalArgumentException("No se puede pagar una factura anulada");
        }

        factura.setEstado(Factura.EstadoFactura.PAGADA);
        factura.setFechaPago(fechaPago != null ? fechaPago : LocalDate.now());

        return facturaRepo.save(factura);
    }

    /**
     * Anular factura
     */
    @Transactional
    public Factura anularFactura(Long facturaId) {
        Factura factura = facturaRepo.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + facturaId));

        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            throw new IllegalArgumentException("No se puede anular una factura pagada");
        }

        factura.setEstado(Factura.EstadoFactura.ANULADA);

        return facturaRepo.save(factura);
    }

    /**
     * Actualizar factura
     */
    @Transactional
    public Factura actualizarFactura(Long facturaId, FacturaCreateDTO facturaDTO) {
        Factura factura = facturaRepo.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + facturaId));

        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            throw new IllegalArgumentException("No se puede actualizar una factura pagada");
        }

        if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            throw new IllegalArgumentException("No se puede actualizar una factura anulada");
        }

        // Actualizar campos
        factura.setFecha(facturaDTO.getFecha() != null ? facturaDTO.getFecha() : factura.getFecha());
        factura.setSubtotal(facturaDTO.getSubtotal());
        factura.setDescuentos(facturaDTO.getDescuentos() != null ? facturaDTO.getDescuentos() : 0.0);
        // Calcular IVA: si viene en el DTO se usa, si no se calcula desde el subtotal
        if (facturaDTO.getIva() != null && facturaDTO.getIva() > 0) {
            factura.setIva(facturaDTO.getIva());
        } else {
            // Calcular IVA automáticamente desde el subtotal (que ya incluye IVA)
            Double ivaCalculado = calcularIvaDesdeSubtotal(facturaDTO.getSubtotal());
            factura.setIva(ivaCalculado);
        }
        factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
        factura.setFormaPago(facturaDTO.getFormaPago());
        factura.setObservaciones(facturaDTO.getObservaciones());
        
        // Actualizar cliente si se proporciona
        if (facturaDTO.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(facturaDTO.getClienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + facturaDTO.getClienteId()));
            factura.setCliente(cliente);
        }

        // Recalcular total
        factura.calcularTotal();

        return facturaRepo.save(factura);
    }

    /**
     * Eliminar factura (solo si no está pagada)
     */
    @Transactional
    public void eliminarFactura(Long facturaId) {
        Factura factura = facturaRepo.findById(facturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada con ID: " + facturaId));

        if (factura.getEstado() == Factura.EstadoFactura.PAGADA) {
            throw new IllegalArgumentException("No se puede eliminar una factura pagada");
        }

        // Romper vínculo en la orden si existe
        Orden orden = factura.getOrden();
        if (orden != null) {
            try {
                orden.setFactura(null);
                ordenRepository.save(orden);
            } catch (Exception ignore) {}
        }

        facturaRepo.deleteById(facturaId);
    }

    /**
     * Convertir Factura a FacturaTablaDTO
     */
    private FacturaTablaDTO convertirAFacturaTablaDTO(Factura factura) {
        FacturaTablaDTO dto = new FacturaTablaDTO();

        dto.setId(factura.getId());
        dto.setNumeroFactura(factura.getNumeroFactura());
        dto.setFecha(factura.getFecha());
        dto.setSubtotal(factura.getSubtotal());
        dto.setDescuentos(factura.getDescuentos());
        dto.setIva(factura.getIva());
        dto.setRetencionFuente(factura.getRetencionFuente());
        dto.setTotal(factura.getTotal());
        dto.setFormaPago(factura.getFormaPago());
        dto.setEstado(convertirEstado(factura.getEstado()));
        dto.setFechaPago(factura.getFechaPago());
        dto.setObservaciones(factura.getObservaciones());

        // Obra y datos básicos de la orden
        if (factura.getOrden() != null) {
            dto.setObra(factura.getOrden().getObra());
            dto.setOrden(new FacturaTablaDTO.OrdenTabla(
                    factura.getOrden().getId(),      // ID de la orden para \"Ver detalles\"
                    factura.getOrden().getNumero()   // Número legible de la orden
            ));
        }

        // Cliente: usar el de la factura si existe, sino el de la orden
        Cliente clienteFactura = factura.getCliente();
        if (clienteFactura != null) {
            dto.setCliente(new FacturaTablaDTO.ClienteTabla(
                clienteFactura.getNombre(),
                clienteFactura.getNit()
            ));
        } else if (factura.getOrden() != null && factura.getOrden().getCliente() != null) {
            Cliente clienteOrden = factura.getOrden().getCliente();
            dto.setCliente(new FacturaTablaDTO.ClienteTabla(
                clienteOrden.getNombre(),
                clienteOrden.getNit()
            ));
        }


        return dto;
    }

    /**
     * Convertir EstadoFactura enum a String
     */
    private FacturaTablaDTO.EstadoFactura convertirEstado(Factura.EstadoFactura estado) {
        return FacturaTablaDTO.EstadoFactura.valueOf(estado.name());
    }

    /**
     * 🚀 LISTADO DE FACTURAS CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object listarFacturasConFiltros(
            Long clienteId,
            Long sedeId,
            Factura.EstadoFactura estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String numeroFactura,
            Long ordenId,
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
        
        // Buscar facturas con filtros
        List<Factura> facturas = facturaRepo.buscarConFiltros(
            clienteId, sedeId, estado, fechaDesde, fechaHasta, numeroFactura, ordenId
        );
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por fecha DESC)
        if (!sortBy.equals("fecha") || !sortOrder.equals("DESC")) {
            facturas = aplicarOrdenamientoFacturas(facturas, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = facturas.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, facturas.size());
            
            if (fromIndex >= facturas.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Factura> facturasPagina = facturas.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(facturasPagina, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return facturas;
    }

    /**
     * 🚀 LISTADO DE FACTURAS PARA TABLA CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object listarParaTablaConFiltros(
            Long clienteId,
            Long sedeId,
            Factura.EstadoFactura estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer page,
            Integer size) {
        
        // Validar fechas
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        
        // Buscar facturas con filtros
        List<Factura> facturas = facturaRepo.buscarConFiltros(
            clienteId, sedeId, estado, fechaDesde, fechaHasta, null, null
        );
        
        // Convertir a DTOs
        List<FacturaTablaDTO> dtos = facturas.stream()
                .map(this::convertirAFacturaTablaDTO)
                .collect(Collectors.toList());
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = dtos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, dtos.size());
            
            if (fromIndex >= dtos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<FacturaTablaDTO> contenido = dtos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return dtos;
    }
    
    /**
     * Aplica ordenamiento a la lista de facturas según sortBy y sortOrder
     */
    private List<Factura> aplicarOrdenamientoFacturas(List<Factura> facturas, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                facturas.sort((a, b) -> {
                    int cmp = a.getFecha().compareTo(b.getFecha());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "numerofactura":
            case "numero_factura":
                facturas.sort((a, b) -> {
                    int cmp = a.getNumeroFactura().compareToIgnoreCase(b.getNumeroFactura());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "total":
                facturas.sort((a, b) -> {
                    int cmp = Double.compare(a.getTotal() != null ? a.getTotal() : 0.0,
                                            b.getTotal() != null ? b.getTotal() : 0.0);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                facturas.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        }
        
        return facturas;
    }
}

