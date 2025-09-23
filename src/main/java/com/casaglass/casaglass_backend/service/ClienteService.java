package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public Optional<Cliente> obtenerClientePorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> obtenerClientePorNit(String nit) {
        return clienteRepository.findByNit(nit);
    }

    public Cliente guardarCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Cliente actualizarCliente(Long id, Cliente cliente) {
        return clienteRepository.findById(id).map(c -> {
            c.setNit(cliente.getNit());
            c.setNombre(cliente.getNombre());
            c.setDireccion(cliente.getDireccion());
            c.setTelefono(cliente.getTelefono());
            c.setCiudad(cliente.getCiudad());
            c.setCorreo(cliente.getCorreo());
            c.setCredito(cliente.getCredito());
            return clienteRepository.save(c);
        }).orElseThrow(() -> new RuntimeException("Cliente no encontrado con id " + id));
    }

    public void eliminarCliente(Long id) {
        clienteRepository.deleteById(id);
    }
}