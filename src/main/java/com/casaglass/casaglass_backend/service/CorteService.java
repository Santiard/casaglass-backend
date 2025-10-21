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

    private final CorteRepository repository;
    private final CategoriaRepository categoriaRepository;

    public CorteService(CorteRepository repository, CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
    }

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
            // 🐛 DEBUG: Logging para debug
            System.out.println("=== GUARDANDO CORTE ===");
            System.out.println("Código: " + corte.getCodigo());
            System.out.println("Nombre: " + corte.getNombre());
            System.out.println("Largo CM: " + corte.getLargoCm());
            System.out.println("Precio: " + corte.getPrecio());
            System.out.println("Tipo: " + corte.getTipo());
            System.out.println("Color: " + corte.getColor());
            
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
            if (corte.getPrecio() == null || corte.getPrecio() <= 0) {
                throw new IllegalArgumentException("El precio debe ser mayor que 0");
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

            System.out.println("Categoría asignada: " + (corte.getCategoria() != null ? corte.getCategoria().getNombre() : "null"));
            
            Corte saved = repository.save(corte);
            System.out.println("Corte guardado exitosamente con ID: " + saved.getId());
            return saved;
            
        } catch (Exception e) {
            System.err.println("ERROR al guardar corte: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al guardar corte: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Corte actualizar(Long id, Corte corteActualizado) {
        return repository.findById(id)
                .map(corteExistente -> {
                    try {
                        // 🐛 DEBUG: Logging para debug
                        System.out.println("=== ACTUALIZANDO CORTE ===");
                        System.out.println("ID: " + id);
                        System.out.println("Nuevo código: " + corteActualizado.getCodigo());
                        System.out.println("Nuevo largo: " + corteActualizado.getLargoCm());
                        System.out.println("Nuevo precio: " + corteActualizado.getPrecio());
                        
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
                        if (corteActualizado.getPrecioEspecial() != null) {
                            corteExistente.setPrecioEspecial(corteActualizado.getPrecioEspecial());
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
                        if (corteActualizado.getPrecio() != null) {
                            if (corteActualizado.getPrecio() <= 0) {
                                throw new IllegalArgumentException("El precio debe ser mayor que 0");
                            }
                            corteExistente.setPrecio(corteActualizado.getPrecio());
                        }
                        if (corteActualizado.getObservacion() != null) {
                            corteExistente.setObservacion(corteActualizado.getObservacion());
                        }

                        System.out.println("Corte antes de guardar: " + corteExistente.getCodigo());
                        Corte saved = repository.save(corteExistente);
                        System.out.println("Corte actualizado exitosamente");
                        return saved;
                        
                    } catch (Exception e) {
                        System.err.println("ERROR al actualizar corte: " + e.getMessage());
                        e.printStackTrace();
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
        return repository.findByPrecioLessThanEqual(precioMaximo);
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
}