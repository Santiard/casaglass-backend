package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.*;
import com.casaglass.casaglass_backend.model.CierreInformeMensualSede;
import com.casaglass.casaglass_backend.model.EntregaDinero;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.CierreInformeMensualSedeRepository;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

@Service
public class InformeMensualService {

    public static final String CRITERIO_RANGO_ORDENES =
            "Ordenes con venta=true, fecha dentro del mes calendario en la sede; min/max sobre ese conjunto (puede haber huecos entre numeros).";

    private final SedeRepository sedeRepository;
    private final OrdenRepository ordenRepository;
    private final InventarioRepository inventarioRepository;
    private final CierreInformeMensualSedeRepository cierreInformeRepository;
    private final EntregaDineroService entregaDineroService;

    public InformeMensualService(
            SedeRepository sedeRepository,
            OrdenRepository ordenRepository,
            InventarioRepository inventarioRepository,
            CierreInformeMensualSedeRepository cierreInformeRepository,
            EntregaDineroService entregaDineroService) {
        this.sedeRepository = sedeRepository;
        this.ordenRepository = ordenRepository;
        this.inventarioRepository = inventarioRepository;
        this.cierreInformeRepository = cierreInformeRepository;
        this.entregaDineroService = entregaDineroService;
    }

    private static void validarMes(int anio, int mesVal) {
        if (anio < 2000 || anio > 2100 || mesVal < 1 || mesVal > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mes o año inválidos");
        }
    }

    private InformeMensualMesPeriodoDTO crearPeriodoDto(int anio, int mesVal) {
        LocalDate primer = LocalDate.of(anio, mesVal, 1);
        String mesIso = String.format("%04d-%02d", anio, mesVal);
        String mesNombre = primer.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO"))
                + " " + anio;
        return new InformeMensualMesPeriodoDTO(anio, mesVal, mesIso, mesNombre);
    }

    /**
     * Misma agregación que el módulo de entregas en periodo {@link EntregaDineroService#obtenerTotalEntregadoPorSedeEnPeriodo(Long, LocalDate, LocalDate)}:
     * {@code SUM(entregas_dinero.monto)} sobre la sede y fechas (inclusive), todos los estados de entrega registrados así en BD.
     */
    private double calcularDineroRecogidoMes(Long sedeId, LocalDate inicio, LocalDate fin) {
        return round2(entregaDineroService.obtenerTotalEntregadoPorSedeEnPeriodo(sedeId, inicio, fin));
    }

    private InformeMensualRangoOrdenesDTO calcularRangoOrdenes(Long sedeId, LocalDate inicio, LocalDate fin) {
        List<Orden> ventas = ordenRepository.findBySedeIdAndFechaBetweenAndVentaTrue(sedeId, inicio, fin);
        int cantidad = ventas.size();
        OptionalLong min = ventas.stream()
                .map(Orden::getNumero)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .min();
        OptionalLong max = ventas.stream()
                .map(Orden::getNumero)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max();
        Long nmin = min.isPresent() ? min.getAsLong() : null;
        Long nmax = max.isPresent() ? max.getAsLong() : null;
        return new InformeMensualRangoOrdenesDTO(nmin, nmax, cantidad, CRITERIO_RANGO_ORDENES);
    }

    /** Valor inventario productos: Σ cantidad × costo ({@link Inventario}); no incluye cortes en v1. */
    private Double valorInventarioPorSede(Long sedeId) {
        List<Inventario> rows = inventarioRepository.findBySedeId(sedeId);
        double v =
                rows.stream()
                        .mapToDouble(inv -> nz(inv.getCantidad()) * nz(inv.getProducto() != null ? inv.getProducto().getCosto() : null))
                        .sum();
        return round2(v);
    }

    private InformeMensualCierreListItemDTO aItemLista(CierreInformeMensualSede c) {
        return InformeMensualCierreListItemDTO.builder()
                .id(c.getId())
                .year(c.getAnio())
                .month(c.getMes())
                .mesIso(String.format("%04d-%02d", c.getAnio(), c.getMes()))
                .ventasMes(c.getVentasMes())
                .dineroRecogidoMes(c.getDineroRecogidoMes())
                .build();
    }

    private static double nz(Double v) {
        return v != null ? v : 0.0;
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    private static Double round2Nz(Double x) {
        if (x == null) {
            return null;
        }
        return round2(x.doubleValue());
    }

    @Transactional(readOnly = true)
    public InformeMensualResponseDTO calcularPreview(Long sedeId, int anio, int mesVal) {
        validarMes(anio, mesVal);
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));

        LocalDate inicio = LocalDate.of(anio, mesVal, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());

        EntregaDinero marco = new EntregaDinero();
        marco.setSede(sede);
        marco.setFechaEntrega(inicio);
        ResumenMesDTO resumenMes = entregaDineroService.calcularResumenMes(marco);

        InformeMensualMesPeriodoDTO periodo = crearPeriodoDto(anio, mesVal);

        double dineroRecogido = calcularDineroRecogidoMes(sedeId, inicio, fin);
        Double valorInv = valorInventarioPorSede(sedeId);
        InformeMensualRangoOrdenesDTO rango = calcularRangoOrdenes(sedeId, inicio, fin);

        return InformeMensualResponseDTO.builder()
                .origen("PREVIEW")
                .sede(new SedeSimpleDTO(sede))
                .periodo(periodo)
                .ventasMes(round2Nz(resumenMes != null ? resumenMes.getTotalDelMes() : null))
                .dineroRecogidoMes(round2Nz(dineroRecogido))
                .deudasMes(round2Nz(resumenMes != null ? resumenMes.getTotalDeudasMensuales() : null))
                .deudasActivasTotales(round2Nz(resumenMes != null ? resumenMes.getTotalCreditosActivosHistorico() : null))
                .valorInventario(round2Nz(valorInv))
                .ordenesVentasMes(rango)
                .build();
    }

    @Transactional(readOnly = true)
    public InformeMensualResponseDTO obtenerCierre(Long sedeId, int anio, int mesVal) {
        validarMes(anio, mesVal);
        CierreInformeMensualSede guardado = cierreInformeRepository.findBySedeIdAndAnioAndMes(sedeId, anio, mesVal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay cierre mensual guardado"));

        return InformeMensualResponseDTO.builder()
                .origen("CERRADO")
                .sede(new SedeSimpleDTO(guardado.getSede()))
                .periodo(crearPeriodoDto(guardado.getAnio(), guardado.getMes()))
                .ventasMes(round2Nz(guardado.getVentasMes()))
                .dineroRecogidoMes(round2Nz(guardado.getDineroRecogidoMes()))
                .deudasMes(round2Nz(guardado.getDeudasMes()))
                .deudasActivasTotales(round2Nz(guardado.getDeudasActivasTotales()))
                .valorInventario(round2Nz(guardado.getValorInventario()))
                .ordenesVentasMes(new InformeMensualRangoOrdenesDTO(
                        guardado.getOrdenNumeroMin(),
                        guardado.getOrdenNumeroMax(),
                        guardado.getCantidadOrdenesVentasMes(),
                        CRITERIO_RANGO_ORDENES))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InformeMensualCierreListItemDTO> listarCierresAnio(Long sedeId, int anio) {
        if (anio < 2000 || anio > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anio fuera de rango");
        }
        if (!sedeRepository.existsById(sedeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada");
        }
        return cierreInformeRepository.findBySedeIdAndAnioOrderByMesAsc(sedeId, anio).stream()
                .map(this::aItemLista)
                .collect(Collectors.toList());
    }

    @Transactional
    public InformeMensualResponseDTO cerrarMes(InformeMensualCierreRequestDTO req) {
        Boolean confirm = req.getConfirmar();
        if (Boolean.FALSE.equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe confirmar el cierre (confirmar=true) para registrar el snapshot");
        }
        validarMes(req.getYear(), req.getMonth());
        if (!sedeRepository.existsById(req.getSedeId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada");
        }
        if (cierreInformeRepository.findBySedeIdAndAnioAndMes(req.getSedeId(), req.getYear(), req.getMonth()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un informe cerrado para esta sede y mes");
        }

        InformeMensualResponseDTO preview = calcularPreview(req.getSedeId(), req.getYear(), req.getMonth());

        Sede sedeRef = sedeRepository.getReferenceById(req.getSedeId());
        CierreInformeMensualSede row = new CierreInformeMensualSede();
        row.setSede(sedeRef);
        row.setAnio(req.getYear());
        row.setMes(req.getMonth());
        row.setEstado(CierreInformeMensualSede.EstadoInforme.CERRADO);

        row.setVentasMes(preview.getVentasMes());
        row.setDineroRecogidoMes(preview.getDineroRecogidoMes());
        row.setDeudasMes(preview.getDeudasMes());
        row.setDeudasActivasTotales(preview.getDeudasActivasTotales());
        row.setValorInventario(preview.getValorInventario());

        if (preview.getOrdenesVentasMes() != null) {
            row.setOrdenNumeroMin(preview.getOrdenesVentasMes().getNumeroMin());
            row.setOrdenNumeroMax(preview.getOrdenesVentasMes().getNumeroMax());
            row.setCantidadOrdenesVentasMes(preview.getOrdenesVentasMes().getCantidad());
        }

        cierreInformeRepository.save(row);
        return obtenerCierre(req.getSedeId(), req.getYear(), req.getMonth());
    }
}
