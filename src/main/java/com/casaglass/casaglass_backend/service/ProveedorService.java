package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Proveedor;
import com.casaglass.casaglass_backend.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAll();
    }

    /**
     * 🚀 LISTADO DE PROVEEDORES CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object listarProveedoresConFiltros(
            String nombre,
            String nit,
            String correo,
            String ciudad,
            Boolean activo,
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar y normalizar ordenamiento
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "nombre";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "ASC";
        }
        sortOrder = sortOrder.toUpperCase();
        if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
            sortOrder = "ASC";
        }
        
        // Buscar proveedores con filtros (correo y activo se ignoran porque el modelo no tiene esos campos)
        List<Proveedor> proveedores = proveedorRepository.buscarConFiltros(
            nombre, nit, ciudad
        );
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por nombre ASC)
        if (!sortBy.equals("nombre") || !sortOrder.equals("ASC")) {
            proveedores = aplicarOrdenamientoProveedores(proveedores, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200; // Límite máximo para proveedores
            
            long totalElements = proveedores.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, proveedores.size());
            
            if (fromIndex >= proveedores.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Proveedor> contenido = proveedores.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return proveedores;
    }
    
    /**
     * Aplica ordenamiento a la lista de proveedores según sortBy y sortOrder
     */
    private List<Proveedor> aplicarOrdenamientoProveedores(List<Proveedor> proveedores, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "nombre":
                proveedores.sort((a, b) -> {
                    int cmp = (a.getNombre() != null ? a.getNombre() : "").compareToIgnoreCase(b.getNombre() != null ? b.getNombre() : "");
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "nit":
                proveedores.sort((a, b) -> {
                    int cmp = (a.getNit() != null ? a.getNit() : "").compareToIgnoreCase(b.getNit() != null ? b.getNit() : "");
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por nombre ASC
                proveedores.sort((a, b) -> (a.getNombre() != null ? a.getNombre() : "").compareToIgnoreCase(b.getNombre() != null ? b.getNombre() : ""));
        }
        
        return proveedores;
    }

    public Optional<Proveedor> obtenerProveedorPorId(Long id) {
        return proveedorRepository.findById(id);
    }

    public Optional<Proveedor> obtenerProveedorPorNit(String nit) {
        return proveedorRepository.findByNit(nit);
    }

    public Proveedor guardarProveedor(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public Proveedor actualizarProveedor(Long id, Proveedor proveedor) {
        return proveedorRepository.findById(id).map(p -> {
            p.setNit(proveedor.getNit());
            p.setNombre(proveedor.getNombre());
            p.setDireccion(proveedor.getDireccion());
            p.setTelefono(proveedor.getTelefono());
            p.setCiudad(proveedor.getCiudad());
            return proveedorRepository.save(p);
        }).orElseThrow(() -> new RuntimeException("Proveedor no encontrado con id " + id));
    }

    public void eliminarProveedor(Long id) {
        proveedorRepository.deleteById(id);
    }
}