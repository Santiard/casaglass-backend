package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.CorteInventarioCompletoDTO;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.model.InventarioCorte;
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
            sedeIds = new java.util.HashMap<>();
            // Buscar sedes por nombre parcial (case-insensitive)
            sedeRepository.findByNombreContainingIgnoreCase("insula")
                .stream()
                .findFirst()
                .ifPresent(sede -> sedeIds.put("insula", sede.getId()));
            
            sedeRepository.findByNombreContainingIgnoreCase("centro")
                .stream()
                .findFirst()
                .ifPresent(sede -> sedeIds.put("centro", sede.getId()));
            
            sedeRepository.findByNombreContainingIgnoreCase("patios")
                .stream()
                .findFirst()
                .ifPresent(sede -> sedeIds.put("patios", sede.getId()));
            
            // Log removido para producción
        }
    }

    private Long obtenerSedeId(String nombreSede) {
        inicializarSedeIds();
        Long id = sedeIds.get(nombreSede.toLowerCase());
        if (id == null) {
            // Log removido para producción
        }
        return id;
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompleto() {
        // Para listados de selección, solo retornar cortes que tengan stock disponible.
        List<InventarioCorte> todosLosInventarios = inventarioCorteRepository.findAllWithStock();

        List<Long> cortesIds = todosLosInventarios.stream()
            .map(inv -> inv.getCorte().getId())
            .distinct()
            .collect(Collectors.toList());

        if (cortesIds.isEmpty()) {
            return List.of();
        }

        List<Corte> cortes = corteRepository.findByIdIn(cortesIds);

        // Obtener inventarios agrupados por corte y sede
        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = 
            todosLosInventarios.stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum // En caso de duplicados, sumar
                    )
                ));

        // Convertir a DTOs
        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), null))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorCategoria(Long categoriaId) {
        // Obtener cortes de una categoría específica
        List<Corte> cortes = corteRepository.findByCategoria_Id(categoriaId);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), null))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> buscarInventarioCompleto(String query) {
        // Búsqueda por nombre o código
        List<Corte> cortes = corteRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(query, query);

        if (cortes.isEmpty()) {
            return List.of();
        }

        // Obtener inventarios para esos cortes con stock disponible
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        List<InventarioCorte> inventariosConStock = inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
            .filter(inv -> inv.getCantidad() != null && inv.getCantidad() > 0)
            .collect(Collectors.toList());

        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = inventariosConStock.stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum
                    )
                ));

        return cortes.stream()
            .filter(corte -> inventariosPorCorteYSede.containsKey(corte.getId()))
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), null))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> buscarInventarioCompletoPorSede(String query, Long sedeId) {
        List<Corte> cortes = corteRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(query, query);
        if (cortes.isEmpty()) {
            return List.of();
        }

        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        List<InventarioCorte> inventariosSede = inventarioCorteRepository.findBySedeIdAndCorteIdInWithStock(sedeId, cortesIds);
        if (inventariosSede.isEmpty()) {
            return List.of();
        }

        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = inventariosSede.stream()
            .collect(Collectors.groupingBy(
                inv -> inv.getCorte().getId(),
                Collectors.toMap(
                    inv -> inv.getSede().getId(),
                    InventarioCorte::getCantidad,
                    Double::sum
                )
            ));

        return cortes.stream()
            .filter(corte -> inventariosPorCorteYSede.containsKey(corte.getId()))
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), sedeId))
            .collect(Collectors.toList());
    }

    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorRangoLargo(Double largoMin, Double largoMax) {
        // Búsqueda por rango de largo
        List<Corte> cortes = corteRepository.findByLargoCmBetween(largoMin, largoMax);
        
        // Obtener inventarios para esos cortes
        List<Long> cortesIds = cortes.stream().map(Corte::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), null))
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
        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), null))
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
        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = 
            inventarioCorteRepository.findByCorteIdIn(cortesIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), null))
            .collect(Collectors.toList());
    }

    /**
     * Cortes con stock estrictamente en {@code sedeId} (cantidad &gt; 0 vía
     * {@link InventarioCorteRepository#findBySedeIdWithStock(long)}). El DTO sigue
     * trayendo el desglose tri-sede; solo la columna de esta sede tendrá cantidad.
     */
    public List<CorteInventarioCompletoDTO> obtenerInventarioCompletoPorSede(Long sedeId) {
        List<InventarioCorte> inventariosSede = inventarioCorteRepository.findBySedeIdWithStock(sedeId);
        
        // Obtener los cortes únicos
        List<Long> cortesIds = inventariosSede.stream()
            .map(inv -> inv.getCorte().getId())
            .distinct()
            .collect(Collectors.toList());
        
        // Obtener cortes
        List<Corte> cortes = corteRepository.findByIdIn(cortesIds);
        
        // Crear mapa de inventarios por corte y sede (solo para esta sede)
        Map<Long, Map<Long, Double>> inventariosPorCorteYSede = 
            inventariosSede.stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getCorte().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        InventarioCorte::getCantidad,
                        Double::sum
                    )
                ));

        return cortes.stream()
            .map(corte -> convertirADTO(corte, inventariosPorCorteYSede.get(corte.getId()), sedeId))
            .collect(Collectors.toList());
    }

    /**
     * {@code filtroSedeInventario} = cuando no es {@code null}: se solicitó
     * {@link #obtenerInventarioCompletoPorSede(long)} o {@code ?sedeId=}; el mapa solo trae
     * filas de esa sede, pero las claves reales de {@code inventario_cortes} deben mapear a
     * Insula/Centro/Patios usando el <strong>id de sede de la query</strong> (1,2,3), no solo
     * {@link #obtenerSedeId(String)} por nombre (si no coincide, antes quedaba en 0 o se mezclaba
     * con otra sede y el total parecía “suma de todo”).
     */
    private CorteInventarioCompletoDTO convertirADTO(
            Corte corte, Map<Long, Double> inventariosPorSede, Long filtroSedeInventario) {
        double insU;
        double cenU;
        double patU;
        if (filtroSedeInventario != null) {
            double v = 0.0;
            if (inventariosPorSede != null) {
                if (inventariosPorSede.containsKey(filtroSedeInventario)) {
                    v = inventariosPorSede.get(filtroSedeInventario);
                } else if (inventariosPorSede.size() == 1) {
                    v = inventariosPorSede.values().iterator().next();
                } else {
                    for (Map.Entry<Long, Double> e : inventariosPorSede.entrySet()) {
                        v += e.getValue() != null ? e.getValue() : 0.0;
                    }
                }
            }
            insU = filtroSedeInventario.equals(1L) ? v : 0.0;
            cenU = filtroSedeInventario.equals(2L) ? v : 0.0;
            patU = filtroSedeInventario.equals(3L) ? v : 0.0;
            if (filtroSedeInventario != 1L && filtroSedeInventario != 2L && filtroSedeInventario != 3L) {
                Long ins = obtenerSedeId("insula");
                Long cen = obtenerSedeId("centro");
                Long pat = obtenerSedeId("patios");
                insU = ins != null && filtroSedeInventario.equals(ins) ? v : 0.0;
                cenU = cen != null && filtroSedeInventario.equals(cen) ? v : 0.0;
                patU = pat != null && filtroSedeInventario.equals(pat) ? v : 0.0;
            }
        } else {
            Long insulaId = obtenerSedeId("insula");
            Long centroId = obtenerSedeId("centro");
            Long patiosId = obtenerSedeId("patios");
            insU = inventariosPorSede != null && insulaId != null ? inventariosPorSede.getOrDefault(insulaId, 0.0) : 0;
            cenU = inventariosPorSede != null && centroId != null ? inventariosPorSede.getOrDefault(centroId, 0.0) : 0;
            patU = inventariosPorSede != null && patiosId != null ? inventariosPorSede.getOrDefault(patiosId, 0.0) : 0;
        }

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
            insU,
            cenU,
            patU,
            corte.getPrecio1(),
            corte.getPrecio2(),
            corte.getPrecio3()
        );
    }
}
