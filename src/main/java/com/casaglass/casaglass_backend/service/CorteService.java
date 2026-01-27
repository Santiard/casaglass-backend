package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Categoria;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.repository.CategoriaRepository;
import com.casaglass.casaglass_backend.repository.CorteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CorteService {

    private final com.casaglass.casaglass_backend.service.InventarioCorteService inventarioCorteService;
    private final com.casaglass.casaglass_backend.service.SedeService sedeService;

    public CorteService(CorteRepository repository, CategoriaRepository categoriaRepository,
                        com.casaglass.casaglass_backend.service.InventarioCorteService inventarioCorteService,
                        com.casaglass.casaglass_backend.service.SedeService sedeService) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioCorteService = inventarioCorteService;
        this.sedeService = sedeService;
    }

    private final CorteRepository repository;
    private final CategoriaRepository categoriaRepository;

    // Operaciones básicas CRUD
    public List<Corte> listar() {
        return repository.findAll();
    }

    public Optional<Corte> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    public Optional<Corte> obtenerPorCodigo(String codigo) {
        return repository.findByCodigo(codigo);
    }

    @Transactional
    public Corte guardar(Corte corte) {
        try {
            
            // Validaciones básicas
            if (corte.getCodigo() == null || corte.getCodigo().trim().isEmpty()) {
                throw new IllegalArgumentException("El código del corte es obligatorio");
            }
            if (corte.getNombre() == null || corte.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del corte es obligatorio");
            }
            if (corte.getLargoCm() == null || corte.getLargoCm() <= 0) {
                throw new IllegalArgumentException("El largo debe ser mayor que 0");
            }
            if (corte.getPrecio1() == null || corte.getPrecio1() <= 0) {
                throw new IllegalArgumentException("El precio1 debe ser mayor que 0");
            }

            // 🔧 Configurar valores por defecto si no vienen
            if (corte.getCantidad() == null) {
                corte.setCantidad(0);
            }
            if (corte.getCosto() == null) {
                corte.setCosto(0.0);
            }

            // Validar y asignar categoría si viene con ID
            if (corte.getCategoria() != null && corte.getCategoria().getId() != null) {
                Categoria cat = categoriaRepository.findById(corte.getCategoria().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + corte.getCategoria().getId()));
                corte.setCategoria(cat);
            } else {
                corte.setCategoria(null);
            }

            // ...existing code...
            
            Corte saved = repository.save(corte);
            return saved;
            
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar corte: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Corte actualizar(Long id, Corte corteActualizado) {
        return repository.findById(id)
                .map(corteExistente -> {
                    try {
                        // ...existing code...
                        
                        // Campos heredados de Producto - SOLO si no son null
                        if (corteActualizado.getPosicion() != null) {
                            corteExistente.setPosicion(corteActualizado.getPosicion());
                        }
                        if (corteActualizado.getCodigo() != null) {
                            corteExistente.setCodigo(corteActualizado.getCodigo());
                        }
                        if (corteActualizado.getNombre() != null) {
                            corteExistente.setNombre(corteActualizado.getNombre());
                        }
                        if (corteActualizado.getTipo() != null) {
                            corteExistente.setTipo(corteActualizado.getTipo());
                        }
                        if (corteActualizado.getColor() != null) {
                            corteExistente.setColor(corteActualizado.getColor());
                        }
                        if (corteActualizado.getDescripcion() != null) {
                            corteExistente.setDescripcion(corteActualizado.getDescripcion());
                        }
                        if (corteActualizado.getCantidad() != null) {
                            corteExistente.setCantidad(corteActualizado.getCantidad());
                        }

                        // Campos de precios heredados
                        if (corteActualizado.getCosto() != null) {
                            corteExistente.setCosto(corteActualizado.getCosto());
                        }
                        if (corteActualizado.getPrecio1() != null) {
                            corteExistente.setPrecio1(corteActualizado.getPrecio1());
                        }
                        if (corteActualizado.getPrecio2() != null) {
                            corteExistente.setPrecio2(corteActualizado.getPrecio2());
                        }
                        if (corteActualizado.getPrecio3() != null) {
                            corteExistente.setPrecio3(corteActualizado.getPrecio3());
                        }

                        // Actualizar categoría si se envía
                        if (corteActualizado.getCategoria() != null && corteActualizado.getCategoria().getId() != null) {
                            Categoria cat = categoriaRepository.findById(corteActualizado.getCategoria().getId())
                                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + corteActualizado.getCategoria().getId()));
                            corteExistente.setCategoria(cat);
                        }

                        // Campos específicos de Corte - OBLIGATORIOS al actualizar
                        if (corteActualizado.getLargoCm() != null) {
                            if (corteActualizado.getLargoCm() <= 0) {
                                throw new IllegalArgumentException("El largo debe ser mayor que 0");
                            }
                            corteExistente.setLargoCm(corteActualizado.getLargoCm());
                        }
                        if (corteActualizado.getPrecio1() != null) {
                            if (corteActualizado.getPrecio1() <= 0) {
                                throw new IllegalArgumentException("El precio1 debe ser mayor que 0");
                            }
                            corteExistente.setPrecio1(corteActualizado.getPrecio1());
                        }
                        if (corteActualizado.getObservacion() != null) {
                            corteExistente.setObservacion(corteActualizado.getObservacion());
                        }

                        Corte saved = repository.save(corteExistente);
                        return saved;
                        
                    } catch (Exception e) {
                        throw new RuntimeException("Error al actualizar corte: " + e.getMessage(), e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Corte no encontrado con ID: " + id));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Corte no encontrado con ID: " + id);
        }
        repository.deleteById(id);
    }

    // Búsquedas especializadas
    public List<Corte> listarPorCategoriaId(Long categoriaId) {
        return repository.findByCategoria_Id(categoriaId);
    }

    public List<Corte> listarPorCategoria(String categoriaNombre) {
        return repository.findByCategoria_NombreIgnoreCase(categoriaNombre);
    }

    public List<Corte> buscar(String query) {
        return repository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(query, query);
    }

    public List<Corte> listarPorRangoLargo(Double largoMin, Double largoMax) {
        return repository.findByLargoRange(largoMin, largoMax);
    }

    public List<Corte> listarPorRangoPrecio(Double precioMin, Double precioMax) {
        return repository.findByPrecioRange(precioMin, precioMax);
    }

    public List<Corte> listarPorLargoMinimo(Double largoMinimo) {
        return repository.findByLargoCmGreaterThanEqual(largoMinimo);
    }

    public List<Corte> listarPorPrecioMaximo(Double precioMaximo) {
        return repository.findByPrecio1LessThanEqual(precioMaximo);
    }

    public List<Corte> listarConObservaciones() {
        return repository.findCortesWithObservaciones();
    }

    // 🆕 NUEVOS MÉTODOS PARA FILTROS AVANZADOS
    public List<Corte> listarPorTipo(String tipoStr) {
        try {
            com.casaglass.casaglass_backend.model.TipoProducto tipo = 
                com.casaglass.casaglass_backend.model.TipoProducto.valueOf(tipoStr.toUpperCase());
            return repository.findByTipo(tipo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de producto inválido: " + tipoStr);
        }
    }

    public List<Corte> listarPorColor(String colorStr) {
        try {
            com.casaglass.casaglass_backend.model.ColorProducto color = 
                com.casaglass.casaglass_backend.model.ColorProducto.valueOf(colorStr.toUpperCase());
            return repository.findByColor(color);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Color de producto inválido: " + colorStr);
        }
    }

    public List<Corte> listarPorCategoriaYTipo(Long categoriaId, String tipoStr) {
        try {
            com.casaglass.casaglass_backend.model.TipoProducto tipo = 
                com.casaglass.casaglass_backend.model.TipoProducto.valueOf(tipoStr.toUpperCase());
            return repository.findByCategoria_IdAndTipo(categoriaId, tipo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de producto inválido: " + tipoStr);
        }
    }

    public List<Corte> listarPorCategoriaYColor(Long categoriaId, String colorStr) {
        try {
            com.casaglass.casaglass_backend.model.ColorProducto color = 
                com.casaglass.casaglass_backend.model.ColorProducto.valueOf(colorStr.toUpperCase());
            return repository.findByCategoria_IdAndColor(categoriaId, color);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Color de producto inválido: " + colorStr);
        }
    }

    public List<Corte> listarPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(ids);
    }

    /**
     * Une dos cortes en inventario si cumplen las condiciones de suma 600cm, mismo producto, color y categoría.
     * Resta 1 unidad de cada corte y suma 1 unidad a la barra completa (600cm).
     */
    @Transactional
    public void unirCortes(Long corteId1, Long corteId2, Long sedeId) {
        if (corteId1 == null || corteId2 == null || sedeId == null) {
            throw new IllegalArgumentException("Debe enviar los dos cortes y la sede.");
        }
        if (corteId1.equals(corteId2)) {
            throw new IllegalArgumentException("No puede unir el mismo corte consigo mismo.");
        }
        Corte corte1 = repository.findById(corteId1).orElseThrow(() -> new IllegalArgumentException("Corte 1 no encontrado."));
        Corte corte2 = repository.findById(corteId2).orElseThrow(() -> new IllegalArgumentException("Corte 2 no encontrado."));
        if (!corte1.getCodigo().equals(corte2.getCodigo()) ||
            !corte1.getColor().equals(corte2.getColor()) ||
            (corte1.getCategoria() != null && corte2.getCategoria() != null && !corte1.getCategoria().getId().equals(corte2.getCategoria().getId()))) {
            throw new IllegalArgumentException("Ambos cortes deben ser del mismo producto, color y categoría.");
        }
        double suma = corte1.getLargoCm() + corte2.getLargoCm();
        if (Math.abs(suma - 600.0) > 0.1) {
            throw new IllegalArgumentException("La suma de los largos de los cortes debe ser exactamente 600cm.");
        }
        // Validar stock suficiente
        var inv1 = inventarioCorteService.obtenerPorCorteYSede(corteId1, sedeId).orElseThrow(() -> new IllegalArgumentException("No hay inventario del corte 1 en la sede."));
        var inv2 = inventarioCorteService.obtenerPorCorteYSede(corteId2, sedeId).orElseThrow(() -> new IllegalArgumentException("No hay inventario del corte 2 en la sede."));
        if (inv1.getCantidad() < 1 || inv2.getCantidad() < 1) {
            throw new IllegalArgumentException("Debe haber al menos 1 unidad de cada corte en inventario.");
        }
        // Buscar barra completa (600cm) existente o crearla
        Corte barraCompleta = repository.findExistingByCodigoAndSpecs(
            corte1.getCodigo(), 600.0, corte1.getCategoria() != null ? corte1.getCategoria().getId() : null, corte1.getColor()
        ).orElseGet(() -> {
            Corte nueva = new Corte();
            nueva.setCodigo(corte1.getCodigo());
            nueva.setNombre(corte1.getNombre().replaceAll(" Corte de \\d+cm", ""));
            nueva.setLargoCm(600.0);
            nueva.setCategoria(corte1.getCategoria());
            nueva.setColor(corte1.getColor());
            nueva.setTipo(corte1.getTipo());
            nueva.setPrecio1(corte1.getPrecio1());
            nueva.setPrecio2(corte1.getPrecio2());
            nueva.setPrecio3(corte1.getPrecio3());
            nueva.setCosto(corte1.getCosto());
            nueva.setCantidad(0);
            return repository.save(nueva);
        });
        // Restar 1 unidad de cada corte
        inventarioCorteService.decrementarStock(corteId1, sedeId, 1.0);
        inventarioCorteService.decrementarStock(corteId2, sedeId, 1.0);
        // Sumar 1 unidad a la barra completa
        inventarioCorteService.incrementarStock(barraCompleta.getId(), sedeId, 1.0);
    }
}