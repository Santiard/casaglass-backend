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
        // Validaciones básicas
        if (corte.getCodigo() == null || corte.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("El código del corte es obligatorio");
        }
        if (corte.getLargoCm() == null || corte.getLargoCm() <= 0) {
            throw new IllegalArgumentException("El largo debe ser mayor que 0");
        }
        if (corte.getPrecio() == null || corte.getPrecio() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        }

        // Validar categoría si viene con ID
        if (corte.getCategoria() != null && corte.getCategoria().getId() != null) {
            Categoria cat = categoriaRepository.findById(corte.getCategoria().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
            corte.setCategoria(cat);
        } else {
            corte.setCategoria(null);
        }

        return repository.save(corte);
    }

    @Transactional
    public Corte actualizar(Long id, Corte corteActualizado) {
        return repository.findById(id)
                .map(corteExistente -> {
                    // Campos heredados de Producto
                    corteExistente.setCodigo(corteActualizado.getCodigo());
                    corteExistente.setNombre(corteActualizado.getNombre());
                    corteExistente.setColor(corteActualizado.getColor());
                    corteExistente.setDescripcion(corteActualizado.getDescripcion());
                    corteExistente.setCantidad(corteActualizado.getCantidad());
                    
                    // Actualizar categoría si se envía
                    if (corteActualizado.getCategoria() != null && corteActualizado.getCategoria().getId() != null) {
                        Categoria cat = categoriaRepository.findById(corteActualizado.getCategoria().getId())
                                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                        corteExistente.setCategoria(cat);
                    } else {
                        corteExistente.setCategoria(null);
                    }
                    
                    // Convertir BigDecimal a Double para campos heredados si es necesario
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
                    
                    // Campos específicos de Corte
                    corteExistente.setLargoCm(corteActualizado.getLargoCm());
                    corteExistente.setPrecio(corteActualizado.getPrecio());
                    corteExistente.setObservacion(corteActualizado.getObservacion());
                    
                    return repository.save(corteExistente);
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
}