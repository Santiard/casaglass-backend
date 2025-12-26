package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ProductoInventarioCompletoDTO;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.model.TipoProducto;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.repository.ProductoVidrioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventarioCompletoService {

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final ProductoVidrioRepository productoVidrioRepository;

    public InventarioCompletoService(ProductoRepository productoRepository, 
                                   InventarioRepository inventarioRepository,
                                   ProductoVidrioRepository productoVidrioRepository) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.productoVidrioRepository = productoVidrioRepository;
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompleto() {
        // 🔧 OBTENER TODOS LOS PRODUCTOS (incluyendo ProductoVidrio)
        // findAll() con herencia JOINED debería cargar automáticamente ProductoVidrio
        List<Producto> todosLosProductos = productoRepository.findAll();
        
        // ...existing code...
        
        // 🔧 FILTRAR CORTES MANUALMENTE EN JAVA (mantener ProductoVidrio)
        List<Producto> productos = todosLosProductos.stream()
            .filter(p -> !(p instanceof com.casaglass.casaglass_backend.model.Corte))
            .collect(Collectors.toList());
        
        // 🔧 USAR MÉTODO CON FETCH JOINS para evitar lazy loading
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findAllWithDetails().stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum // En caso de duplicados, sumar
                    )
                ));

        // 🐛 DEBUG: Logging para verificar los datos
        // ...existing code...
        
        // Contar productos vidrio
        long cantidadVidrios = productos.stream()
            .filter(p -> p instanceof ProductoVidrio)
            .count();
        // ...existing code...
        
        // Listar IDs de productos vidrio
        List<Long> idsVidrios = productos.stream()
            .filter(p -> p instanceof ProductoVidrio)
            .map(Producto::getId)
            .collect(Collectors.toList());
        // ...existing code...
        
        // ...existing code...

        // Convertir a DTOs (incluir TODOS los productos, incluso sin inventario)
        return productos.stream()
            .map(producto -> {
                Map<Long, Integer> inventarios = inventariosPorProductoYSede.get(producto.getId());
                ProductoInventarioCompletoDTO dto = convertirADTO(producto, inventarios);
                
                // 🐛 DEBUG: Log específico para productos vidrio
                if (producto instanceof ProductoVidrio) {
                    // ...existing code...
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompletoPorCategoria(Long categoriaId) {
        // 🔧 OBTENER PRODUCTOS DE CATEGORÍA ESPECÍFICA (excluir cortes)
        List<Producto> productos = productoRepository.findByCategoria_IdSinCortes(categoriaId);
        
        // Obtener inventarios para esos productos
        List<Long> productosIds = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findByProductoIdIn(productosIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));

        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
    }

    public List<ProductoInventarioCompletoDTO> buscarInventarioCompleto(String query) {
        // 🔧 BÚSQUEDA POR NOMBRE O CÓDIGO (excluir cortes)
        List<Producto> productos = productoRepository.findByNombreOrCodigoSinCortes(query, query);
        
        // Obtener inventarios para esos productos
        List<Long> productosIds = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findByProductoIdIn(productosIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));

        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO DE INVENTARIO COMPLETO CON FILTROS COMPLETOS
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object obtenerInventarioCompletoConFiltros(
            Long categoriaId,
            String categoriaNombre,
            String tipo,
            String color,
            String codigo,
            String nombre,
            Long sedeId,
            Boolean conStock,
            Boolean sinStock,
            Integer page,
            Integer size) {
        
        // Convertir tipo y color String a enum
        TipoProducto tipoEnum = null;
        if (tipo != null && !tipo.isEmpty()) {
            try {
                tipoEnum = TipoProducto.valueOf(tipo.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo inválido: " + tipo);
            }
        }
        
        ColorProducto colorEnum = null;
        if (color != null && !color.isEmpty()) {
            try {
                colorEnum = ColorProducto.valueOf(color.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Color inválido: " + color);
            }
        }
        
        // Buscar productos con filtros (excluye cortes)
        List<Producto> productos = productoRepository.buscarConFiltros(
            categoriaId, categoriaNombre, tipoEnum, colorEnum, codigo, nombre
        );
        
        // Obtener inventarios para esos productos
        List<Long> productosIds = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findByProductoIdIn(productosIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));
        
        // Convertir a DTOs
        List<ProductoInventarioCompletoDTO> dtos = productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
        
        // Filtrar por stock si se solicita
        if (sedeId != null) {
            if (conStock != null && conStock) {
                dtos = dtos.stream()
                    .filter(dto -> {
                        Integer cantidad = obtenerCantidadPorSede(dto, sedeId);
                        return cantidad != null && cantidad > 0;
                    })
                    .collect(Collectors.toList());
            } else if (sinStock != null && sinStock) {
                dtos = dtos.stream()
                    .filter(dto -> {
                        Integer cantidad = obtenerCantidadPorSede(dto, sedeId);
                        return cantidad == null || cantidad == 0;
                    })
                    .collect(Collectors.toList());
            }
        }
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 100;
            if (size > 500) size = 500; // Límite máximo para inventario
            
            long totalElements = dtos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, dtos.size());
            
            if (fromIndex >= dtos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new java.util.ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<ProductoInventarioCompletoDTO> contenido = dtos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return dtos;
    }
    
    /**
     * Obtiene la cantidad de un producto en una sede específica desde el DTO
     */
    private Integer obtenerCantidadPorSede(ProductoInventarioCompletoDTO dto, Long sedeId) {
        // IDs de sedes: Insula=1, Centro=2, Patios=3
        if (sedeId == 1L) return dto.getCantidadInsula();
        if (sedeId == 2L) return dto.getCantidadCentro();
        if (sedeId == 3L) return dto.getCantidadPatios();
        return null;
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompletoPorTipo(String tipoStr) {
        // Convertir String a enum
        TipoProducto tipo;
        try {
            tipo = TipoProducto.valueOf(tipoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de producto inválido: " + tipoStr);
        }

        // 🔧 OBTENER PRODUCTOS DE UN TIPO ESPECÍFICO (excluir cortes)
        List<Producto> productos = productoRepository.findByTipoSinCortes(tipo);
        
        // Obtener inventarios para esos productos
        List<Long> productosIds = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findByProductoIdIn(productosIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));

        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompletoPorColor(String colorStr) {
        // Convertir String a enum
        ColorProducto color;
        try {
            color = ColorProducto.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Color de producto inválido: " + colorStr);
        }

        // 🔧 OBTENER PRODUCTOS DE UN COLOR ESPECÍFICO (excluir cortes)
        List<Producto> productos = productoRepository.findByColorSinCortes(color);
        
        // Obtener inventarios para esos productos
        List<Long> productosIds = productos.stream().map(Producto::getId).collect(Collectors.toList());
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findByProductoIdIn(productosIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));

        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompletoPorSede(Long sedeId) {
        // Obtener inventarios de una sede específica
        List<Inventario> inventariosSede = inventarioRepository.findBySedeId(sedeId);
        
        // Obtener los productos únicos
        List<Long> productosIds = inventariosSede.stream()
            .map(inv -> inv.getProducto().getId())
            .distinct()
            .collect(Collectors.toList());
        
        // Obtener productos
        List<Producto> productos = productoRepository.findByIdIn(productosIds);
        
        // Crear mapa de inventarios por producto y sede (solo para esta sede)
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventariosSede.stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));

        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
    }

    private ProductoInventarioCompletoDTO convertirADTO(Producto producto, Map<Long, Integer> inventariosPorSede) {
        // 🔧 USAR IDS ESPECÍFICOS DE LAS SEDES (según los datos reales)
        Long insulaId = 1L;  // Sede ID 1 = Insula  
        Long centroId = 2L;  // Sede ID 2 = Centro
        Long patiosId = 3L;  // Sede ID 3 = Patios

        Integer cantidadInsula = inventariosPorSede != null ? inventariosPorSede.getOrDefault(insulaId, 0) : 0;
        Integer cantidadCentro = inventariosPorSede != null ? inventariosPorSede.getOrDefault(centroId, 0) : 0;
        Integer cantidadPatios = inventariosPorSede != null ? inventariosPorSede.getOrDefault(patiosId, 0) : 0;

        // Verificar si es vidrio y obtener datos específicos
        Boolean esVidrio = producto instanceof ProductoVidrio;
        Double mm = null;
        Double m1 = null;
        Double m2 = null;

        if (esVidrio) {
            ProductoVidrio vidrio = (ProductoVidrio) producto;
            mm = vidrio.getMm();
            m1 = vidrio.getM1();
            m2 = vidrio.getM2();
        }

        // ✅ Crear CategoriaDTO con id y nombre (unificado para todos los productos)
        com.casaglass.casaglass_backend.dto.CategoriaDTO categoriaDTO = null;
        if (producto.getCategoria() != null) {
            categoriaDTO = new com.casaglass.casaglass_backend.dto.CategoriaDTO(
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre()
            );
        }
        
        String tipoProducto = producto.getTipo() != null ? producto.getTipo().name() : null;
        String colorProducto = producto.getColor() != null ? producto.getColor().name() : null;

        return new ProductoInventarioCompletoDTO(
            producto.getId(),
            producto.getCodigo(),
            producto.getNombre(),
            producto.getDescripcion(),  // ✅ Incluir descripción
            categoriaDTO,  // ✅ Ahora es CategoriaDTO { id, nombre } en lugar de String
            tipoProducto,
            colorProducto,
            esVidrio,
            mm,
            m1,
            m2,
            cantidadInsula,
            cantidadCentro,
            cantidadPatios,
            producto.getCosto(),
            producto.getPrecio1(),
            producto.getPrecio2(),
            producto.getPrecio3()
        );
    }
    
    /**
     * 🪟 OBTENER INVENTARIO COMPLETO DE PRODUCTOS VIDRIO
     * Endpoint exclusivo para productos vidrio usando el repositorio específico
     * 
     * ESTRATEGIA ALTERNATIVA: Obtener IDs desde query nativo y luego cargar productos
     */
    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompletoVidrios() {
        // ...existing code...
        
        // ESTRATEGIA 1: Intentar con findAll() normal
        List<ProductoVidrio> productosVidrio = productoVidrioRepository.findAll();
        // ...existing code...
        
        // ESTRATEGIA 2: Si findAll() retorna 0, usar query nativo para obtener IDs directamente de productos_vidrio
        if (productosVidrio.isEmpty()) {
            List<Long> idsVidrios = productoVidrioRepository.findProductoVidrioIds();
            
            if (!idsVidrios.isEmpty()) {
                // Cargar productos por IDs usando el repositorio de ProductoVidrio
                productosVidrio = productoVidrioRepository.findAllById(idsVidrios);
                
                // Si aún está vacío después de cargar por IDs, puede ser un problema de herencia JOINED
                if (productosVidrio.isEmpty()) {
                    // ...existing code...
                    
                    // Intentar cargar como Producto y verificar si son ProductoVidrio
                    List<Producto> productos = productoRepository.findAllById(idsVidrios);
                    // ...existing code...
                    
                    // Filtrar solo los que son ProductoVidrio
                    productosVidrio = productos.stream()
                        .filter(p -> p instanceof ProductoVidrio)
                        .map(p -> (ProductoVidrio) p)
                        .collect(Collectors.toList());
                }
            }
        }
        
        // ESTRATEGIA 3: Si aún está vacío, intentar con query JPQL explícito
        if (productosVidrio.isEmpty()) {
            productosVidrio = productoVidrioRepository.findAllWithExplicitJoin();
        }
        
        if (productosVidrio.isEmpty()) {
            // ...existing code...
            return new java.util.ArrayList<>();
        }
        
        // ...existing code...
        
        // Obtener inventarios para esos productos
        List<Long> productosIds = productosVidrio.stream()
            .map(ProductoVidrio::getId)
            .collect(Collectors.toList());
        
        // ...existing code...
        
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findByProductoIdIn(productosIds).stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum
                    )
                ));
        
        // ...existing code...
        
        // Convertir a DTOs
        List<ProductoInventarioCompletoDTO> dtos = productosVidrio.stream()
            .map(producto -> {
                Map<Long, Integer> inventarios = inventariosPorProductoYSede.get(producto.getId());
                ProductoInventarioCompletoDTO dto = convertirADTO(producto, inventarios);
                return dto;
            })
            .collect(Collectors.toList());
        
        // ...existing code...
        return dtos;
    }
}