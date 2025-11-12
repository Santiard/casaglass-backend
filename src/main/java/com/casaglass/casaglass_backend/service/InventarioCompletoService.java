package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ProductoInventarioCompletoDTO;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.model.TipoProducto;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
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

    public InventarioCompletoService(ProductoRepository productoRepository, 
                                   InventarioRepository inventarioRepository) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompleto() {
        // 🔧 TEMPORALMENTE: Usar findAll() para debug
        List<Producto> todosLosProductos = productoRepository.findAll();
        
        // 🔧 FILTRAR CORTES MANUALMENTE EN JAVA
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
        System.out.println("=== DEBUG INVENTARIO COMPLETO (FILTRO MANUAL) ===");
        System.out.println("Todos los productos: " + todosLosProductos.size());
        System.out.println("Productos sin cortes: " + productos.size());
        System.out.println("Inventarios por producto: " + inventariosPorProductoYSede.size());
        inventariosPorProductoYSede.forEach((productoId, sedes) -> {
            System.out.println("Producto " + productoId + " -> " + sedes);
        });
        System.out.println("================================");

        // Convertir a DTOs
        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
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

        // Obtener nombre de la categoría, tipo y color
        String categoriaNombre = producto.getCategoria() != null ? producto.getCategoria().getNombre() : null;
        String tipoProducto = producto.getTipo() != null ? producto.getTipo().name() : null;
        String colorProducto = producto.getColor() != null ? producto.getColor().name() : null;

        return new ProductoInventarioCompletoDTO(
            producto.getId(),
            producto.getCodigo(),
            producto.getNombre(),
            categoriaNombre,
            tipoProducto,
            colorProducto,
            esVidrio,
            mm,
            m1,
            m2,
            cantidadInsula,
            cantidadCentro,
            cantidadPatios,
            producto.getPrecio1(),
            producto.getPrecio2(),
            producto.getPrecio3()
        );
    }
}