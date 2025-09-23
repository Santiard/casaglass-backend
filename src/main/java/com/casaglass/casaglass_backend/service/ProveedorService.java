package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Proveedor;
import com.casaglass.casaglass_backend.repository.ProveedorRepository;
import org.springframework.stereotype.Service;

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
}s