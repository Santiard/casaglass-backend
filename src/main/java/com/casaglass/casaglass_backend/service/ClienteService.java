package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 🚀 LISTADO DE CLIENTES CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     * Nota: conCredito requiere verificar créditos pendientes, se filtra después
     */
    @Transactional(readOnly = true)
    public Object listarClientesConFiltros(
            String nombre,
            String nit,
            String correo,
            String ciudad,
            Boolean activo,
            Boolean conCredito,
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
        
        // Buscar clientes con filtros (activo se ignora porque el modelo no tiene ese campo)
        List<Cliente> clientes = clienteRepository.buscarConFiltros(
            nombre, nit, correo, ciudad
        );
        
        // Filtrar por conCredito si se solicita (requiere verificar créditos)
        if (conCredito != null && conCredito) {
            // TODO: Filtrar clientes que tienen créditos pendientes
            // Por ahora, filtrar por campo credito=true
            clientes = clientes.stream()
                    .filter(c -> c.getCredito() != null && c.getCredito())
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // Aplicar ordenamiento adicional si es necesario (el query ya ordena por nombre ASC)
        if (!sortBy.equals("nombre") || !sortOrder.equals("ASC")) {
            clientes = aplicarOrdenamientoClientes(clientes, sortBy, sortOrder);
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200; // Límite máximo para clientes
            
            long totalElements = clientes.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, clientes.size());
            
            if (fromIndex >= clientes.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Cliente> contenido = clientes.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return clientes;
    }
    
    /**
     * Aplica ordenamiento a la lista de clientes según sortBy y sortOrder
     */
    private List<Cliente> aplicarOrdenamientoClientes(List<Cliente> clientes, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "nombre":
                clientes.sort((a, b) -> {
                    int cmp = (a.getNombre() != null ? a.getNombre() : "").compareToIgnoreCase(b.getNombre() != null ? b.getNombre() : "");
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "nit":
                clientes.sort((a, b) -> {
                    int cmp = (a.getNit() != null ? a.getNit() : "").compareToIgnoreCase(b.getNit() != null ? b.getNit() : "");
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "ciudad":
                clientes.sort((a, b) -> {
                    String ciudadA = a.getCiudad() != null ? a.getCiudad() : "";
                    String ciudadB = b.getCiudad() != null ? b.getCiudad() : "";
                    int cmp = ciudadA.compareToIgnoreCase(ciudadB);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por nombre ASC
                clientes.sort((a, b) -> (a.getNombre() != null ? a.getNombre() : "").compareToIgnoreCase(b.getNombre() != null ? b.getNombre() : ""));
        }
        
        return clientes;
    }

    public Optional<Cliente> obtenerClientePorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> obtenerClientePorNit(String nit) {
        return clienteRepository.findByNit(nit);
    }

    /**
     * Normaliza un string: convierte "-", strings vacíos o solo espacios a NULL
     * Útil para campos opcionales que pueden venir como "-" desde el frontend
     */
    private String normalizarStringOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        if (trimmed.isEmpty() || trimmed.equals("-")) {
            return null;
        }
        return trimmed;
    }
    
    /**
     * Normaliza el NIT: solo elimina espacios, nunca lo convierte a NULL (es obligatorio)
     */
    private String normalizarNit(String nit) {
        if (nit == null) {
            return null;
        }
        return nit.trim();
    }

    public Cliente guardarCliente(Cliente cliente) {
        // Normalizar NIT: eliminar espacios en blanco (es obligatorio, no se convierte a NULL)
        cliente.setNit(normalizarNit(cliente.getNit()));
        
        // Normalizar campos opcionales: convertir "-", vacíos o solo espacios a NULL
        cliente.setCorreo(normalizarStringOpcional(cliente.getCorreo()));
        cliente.setDireccion(normalizarStringOpcional(cliente.getDireccion()));
        cliente.setTelefono(normalizarStringOpcional(cliente.getTelefono()));
        cliente.setCiudad(normalizarStringOpcional(cliente.getCiudad()));
        
        // Validar que el NIT no esté duplicado
        if (cliente.getNit() != null && !cliente.getNit().isEmpty()) {
            Optional<Cliente> clienteExistente = clienteRepository.findByNit(cliente.getNit());
            if (clienteExistente.isPresent()) {
                throw new IllegalArgumentException("El NIT '" + cliente.getNit() + "' ya está registrado.");
            }
        }
        
        // Validar que el correo no esté duplicado (solo si no es NULL)
        if (cliente.getCorreo() != null) {
            Optional<Cliente> clienteConCorreo = clienteRepository.findByCorreo(cliente.getCorreo());
            if (clienteConCorreo.isPresent()) {
                throw new IllegalArgumentException("El correo electrónico '" + cliente.getCorreo() + "' ya está registrado.");
            }
        }
        
        return clienteRepository.save(cliente);
    }

    public Cliente actualizarCliente(Long id, Cliente cliente) {
        return clienteRepository.findById(id).map(c -> {
            // Normalizar NIT: eliminar espacios (es obligatorio, no se convierte a NULL)
            String nitNormalizado = normalizarNit(cliente.getNit());
            
            // Normalizar campos opcionales: convertir "-", vacíos o solo espacios a NULL
            String correoNormalizado = normalizarStringOpcional(cliente.getCorreo());
            String direccionNormalizada = normalizarStringOpcional(cliente.getDireccion());
            String telefonoNormalizado = normalizarStringOpcional(cliente.getTelefono());
            String ciudadNormalizada = normalizarStringOpcional(cliente.getCiudad());
            
            // Validar que el NIT no esté duplicado (solo si cambió y no es el mismo cliente)
            if (nitNormalizado != null && !nitNormalizado.isEmpty() && !nitNormalizado.equals(c.getNit())) {
                Optional<Cliente> clienteExistente = clienteRepository.findByNit(nitNormalizado);
                if (clienteExistente.isPresent() && !clienteExistente.get().getId().equals(id)) {
                    throw new IllegalArgumentException("El NIT '" + nitNormalizado + "' ya está registrado.");
                }
            }
            
            // Validar que el correo no esté duplicado (solo si cambió y no es NULL)
            if (correoNormalizado != null && !correoNormalizado.equals(c.getCorreo())) {
                Optional<Cliente> clienteConCorreo = clienteRepository.findByCorreo(correoNormalizado);
                if (clienteConCorreo.isPresent() && !clienteConCorreo.get().getId().equals(id)) {
                    throw new IllegalArgumentException("El correo electrónico '" + correoNormalizado + "' ya está registrado.");
                }
            }
            
            c.setNit(nitNormalizado);
            c.setNombre(cliente.getNombre());
            c.setDireccion(direccionNormalizada);
            c.setTelefono(telefonoNormalizado);
            c.setCiudad(ciudadNormalizada);
            c.setCorreo(correoNormalizado);
            c.setCredito(cliente.getCredito());
            return clienteRepository.save(c);
        }).orElseThrow(() -> new RuntimeException("Cliente no encontrado con id " + id));
    }

    public void eliminarCliente(Long id) {
        clienteRepository.deleteById(id);
    }
}