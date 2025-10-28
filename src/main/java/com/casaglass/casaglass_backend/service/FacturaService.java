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

    public FacturaService(
            FacturaRepository facturaRepo,
            OrdenRepository ordenRepository,
            ClienteRepository clienteRepository,
            SedeRepository sedeRepository,
            TrabajadorRepository trabajadorRepository,
            EntityManager entityManager) {
        this.facturaRepo = facturaRepo;
        this.ordenRepository = ordenRepository;
        this.clienteRepository = clienteRepository;
        this.sedeRepository = sedeRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.entityManager = entityManager;
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

        // Buscar cliente
        Cliente cliente = clienteRepository.findById(facturaDTO.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + facturaDTO.getClienteId()));

        // Buscar sede
        Sede sede = sedeRepository.findById(facturaDTO.getSedeId())
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada con ID: " + facturaDTO.getSedeId()));

        // Buscar trabajador (opcional)
        Trabajador trabajador = null;
        if (facturaDTO.getTrabajadorId() != null) {
            trabajador = trabajadorRepository.findById(facturaDTO.getTrabajadorId())
                    .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con ID: " + facturaDTO.getTrabajadorId()));
        }

        // Crear factura
        Factura factura = new Factura();
        factura.setOrden(orden);
        factura.setCliente(cliente);
        factura.setSede(sede);
        factura.setTrabajador(trabajador);
        factura.setFecha(facturaDTO.getFecha() != null ? facturaDTO.getFecha() : LocalDate.now());
        factura.setSubtotal(facturaDTO.getSubtotal());
        factura.setDescuentos(facturaDTO.getDescuentos() != null ? facturaDTO.getDescuentos() : 0.0);
        factura.setIva(facturaDTO.getIva() != null ? facturaDTO.getIva() : 0.0);
        factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
        factura.setOtrosImpuestos(facturaDTO.getOtrosImpuestos() != null ? facturaDTO.getOtrosImpuestos() : 0.0);
        factura.setFormaPago(facturaDTO.getFormaPago());
        factura.setObservaciones(facturaDTO.getObservaciones());
        factura.setEstado(Factura.EstadoFactura.PENDIENTE);

        // Calcular total automáticamente
        if (facturaDTO.getTotal() != null) {
            factura.setTotal(facturaDTO.getTotal());
        } else {
            factura.calcularTotal();
        }

        // Generar número de factura único
        Long siguienteNumero = generarNumeroFactura();
        factura.setNumero(String.valueOf(siguienteNumero));

        // Guardar factura
        Factura facturaGuardada = facturaRepo.save(factura);

        System.out.println("✅ Factura creada exitosamente - Número: " + facturaGuardada.getNumero());

        return facturaGuardada;
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

                if (!facturaRepo.findByNumero(String.valueOf(siguienteNumero)).isPresent()) {
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
    public Optional<Factura> obtenerPorNumero(String numero) {
        return facturaRepo.findByNumero(numero);
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
     * Listar facturas por cliente
     */
    @Transactional(readOnly = true)
    public List<Factura> listarPorCliente(Long clienteId) {
        return facturaRepo.findByClienteId(clienteId);
    }

    /**
     * Listar facturas por sede
     */
    @Transactional(readOnly = true)
    public List<Factura> listarPorSede(Long sedeId) {
        return facturaRepo.findBySedeId(sedeId);
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
        factura.setIva(facturaDTO.getIva() != null ? facturaDTO.getIva() : 0.0);
        factura.setRetencionFuente(facturaDTO.getRetencionFuente() != null ? facturaDTO.getRetencionFuente() : 0.0);
        factura.setOtrosImpuestos(facturaDTO.getOtrosImpuestos() != null ? facturaDTO.getOtrosImpuestos() : 0.0);
        factura.setFormaPago(facturaDTO.getFormaPago());
        factura.setObservaciones(facturaDTO.getObservaciones());

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

        facturaRepo.deleteById(facturaId);
    }

    /**
     * Convertir Factura a FacturaTablaDTO
     */
    private FacturaTablaDTO convertirAFacturaTablaDTO(Factura factura) {
        FacturaTablaDTO dto = new FacturaTablaDTO();

        dto.setId(factura.getId());
        dto.setNumero(factura.getNumero());
        dto.setFecha(factura.getFecha());
        dto.setSubtotal(factura.getSubtotal());
        dto.setDescuentos(factura.getDescuentos());
        dto.setIva(factura.getIva());
        dto.setRetencionFuente(factura.getRetencionFuente());
        dto.setOtrosImpuestos(factura.getOtrosImpuestos());
        dto.setTotal(factura.getTotal());
        dto.setFormaPago(factura.getFormaPago());
        dto.setEstado(convertirEstado(factura.getEstado()));
        dto.setFechaPago(factura.getFechaPago());
        dto.setObservaciones(factura.getObservaciones());

        // Obra desde la orden
        if (factura.getOrden() != null) {
            dto.setObra(factura.getOrden().getObra());
            dto.setOrden(new FacturaTablaDTO.OrdenTabla(factura.getOrden().getNumero()));
        }

        // Cliente
        if (factura.getCliente() != null) {
            dto.setCliente(new FacturaTablaDTO.ClienteTabla(factura.getCliente().getNombre()));
        }

        // Sede
        if (factura.getSede() != null) {
            dto.setSede(new FacturaTablaDTO.SedeTabla(factura.getSede().getNombre()));
        }

        // Trabajador
        if (factura.getTrabajador() != null) {
            dto.setTrabajador(new FacturaTablaDTO.TrabajadorTabla(factura.getTrabajador().getNombre()));
        }

        return dto;
    }

    /**
     * Convertir EstadoFactura enum a String
     */
    private FacturaTablaDTO.EstadoFactura convertirEstado(Factura.EstadoFactura estado) {
        return FacturaTablaDTO.EstadoFactura.valueOf(estado.name());
    }
}

