package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.CorteInventarioCompletoDTO;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.model.InventarioCorte;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.TipoProducto;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.repository.CorteRepository;
import com.casaglass.casaglass_backend.repository.InventarioCorteRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CorteInventarioCompletoService {

    private final CorteRepository corteRepository;
    private final InventarioCorteRepository inventarioCorteRepository;
    private final SedeRepository sedeRepository;

    // Cache de sedes por nombre
    private Map<String, Long> sedeIds = null;

    public CorteInventarioCompletoService(CorteRepository corteRepository,
                                        InventarioCorteRepository inventarioCorteRepository,
                                        SedeRepository sedeRepository) {
        this.corteRepository = corteRepository;
        this.inventarioCorteRepository = inventarioCorteRepository;
        this.sedeRepository = sedeRepository;
    }

    private void inicializarSedeIds() {
        if (sedeIds == null) {
            sedeIds = sedeRepository.findAll().stream()
                .collect(Collectors.toMap(
                    sede -> sede.getNombre().toLowerCase(),
                    Sede::getId
                ));
        }
    }

    private Long obtenerSedeId(String nombreSede) {
        inicializarSedeIds();
        return sedeIds.get(nombreSede.toLowerCase());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompleto() {
        // Obtener todos los cortes con sus categorías
        List<Corte> cortes = corteRepository.findAll();
        
        // Obtener inventarios agrupados por corte y sede
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum // En caso de duplicados, sumar
                    )
                ));

        // Convertir a DTOs
        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorCategoria(Long categoriaId) {
        // Obtener cortes de una categoría específica
        List<Corte> cortes = corteRepository.findByCategoria_Id(categoriaId);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> buscarInventarioCompleto(String query) {
        // Búsqueda por nombre o código
        List<Corte> cortes = corteRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(query, query);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorRangoLargo(Double largoMin, Double largoMax) {
        // Búsqueda por rango de largo
        List<Corte> cortes = corteRepository.findByLargoCmBetween(largoMin, largoMax);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorTipo(String tipoStr) {
        // Convertir String a enum
        TipoProducto tipo;
        try {
            tipo = TipoProducto.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de producto inválido: " + tipoStr);
        }

        // Obtener cortes de un tipo específico
        List<Corte> cortes = corteRepository.findByTipo(tipo);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorColor(String colorStr) {
        // Convertir String a enum
        ColorProducto color;
        try {
            color = ColorProducto.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Color de producto inválido: " + colorStr);
        }

        // Obtener cortes de un color específico
        List<Corte> cortes = corteRepository.findByColor(color);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorSede(Long sedeId) {
        // Obtener inventarios de cortes para una sede específica
        List<InventarioCorte> inventariosSede = inventarioCorteRepository.findBySedeId(sedeId);
        
        // Obtener los cortes únicos
        List<Long> cortesIds = inventariosSede.stream()
            .map(inv -> inv.getCorte().getId())
            .distinct()
            .collect(Collectors.toList());
        
        // Obtener cortes
        List<Corte> cortes = corteRepository.findByIdIn(cortesIds);
        
        // Crear mapa de inventarios por corte y sede (solo para esta sede)
        Map<Long, Map<Long, Integer>> inventariosPorCorteYSede = 
            inventariosSede.stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Integer::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId())))
            .collect(Collectors.toList());
    }

    private CorteInventarioCompletoDTO convertirADTO(Corte corte, Map<Long, Integer> inventariosPorSede) {
        // Obtener cantidades por sede (0 si no existe)
        Long insulaId = obtenerSedeId("insula");
        Long centroId = obtenerSedeId("centro");
        Long patiosId = obtenerSedeId("patios");

        Integer cantidadInsula = inventariosPorSede != null && insulaId != null ? inventariosPorSede.getOrDefault(insulaId, 0) : 0;
        Integer cantidadCentro = inventariosPorSede != null && centroId != null ? inventariosPorSede.getOrDefault(centroId, 0) : 0;
        Integer cantidadPatios = inventariosPorSede != null && patiosId != null ? inventariosPorSede.getOrDefault(patiosId, 0) : 0;

        // Obtener nombre de la categoría, tipo y color
        String categoriaNombre = corte.getCategoria() != null ? corte.getCategoria().getNombre() : null;
        String tipoProducto = corte.getTipo() != null ? corte.getTipo().name() : null;
        String colorProducto = corte.getColor() != null ? corte.getColor().name() : null;

        return new CorteInventarioCompletoDTO(
            corte.getId(),
            corte.getCodigo(),
            corte.getNombre(),
            categoriaNombre,
            tipoProducto,
            colorProducto,
            corte.getLargoCm(),
            corte.getPrecio(),
            corte.getObservacion(),
            cantidadInsula,
            cantidadCentro,
            cantidadPatios,
            corte.getPrecio1(),
            corte.getPrecio2(),
            corte.getPrecio3()
        );
    }
}