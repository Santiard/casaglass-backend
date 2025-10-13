package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.ProductoInventarioCompletoDTO;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.repository.InventarioRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
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
    private final SedeRepository sedeRepository;

    // Cache de sedes por nombre
    private Map<String, Long> sedeIds = null;

    public InventarioCompletoService(ProductoRepository productoRepository, 
                                   InventarioRepository inventarioRepository,
                                   SedeRepository sedeRepository) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.sedeRepository = sedeRepository;
    }

    private void inicializarSedeIds() {
        if (sedeIds == null) {
            sedeIds = sedeRepository.findAll().stream()
                .collect(Collectors.toMap(
                    sede -> sede.getNombre().toLowerCase(),
                    Sede::getId
                ));
        }
    }

    private Long obtenerSedeId(String nombreSede) {
        inicializarSedeIds();
        return sedeIds.get(nombreSede.toLowerCase());
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompleto() {
        // Obtener todos los productos con sus categorías
        List<Producto> productos = productoRepository.findAll();
        
        // Obtener inventarios agrupados por producto y sede
        Map<Long, Map<Long, Integer>> inventariosPorProductoYSede = 
            inventarioRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                    inv -> inv.getProducto().getId(),
                    Collectors.toMap(
                        inv -> inv.getSede().getId(),
                        Inventario::getCantidad,
                        Integer::sum // En caso de duplicados, sumar
                    )
                ));

        // Convertir a DTOs
        return productos.stream()
            .map(producto -> convertirADTO(producto, inventariosPorProductoYSede.get(producto.getId())))
            .collect(Collectors.toList());
    }

    public List<ProductoInventarioCompletoDTO> obtenerInventarioCompletoPorCategoria(Long categoriaId) {
        // Obtener productos de una categoría específica
        List<Producto> productos = productoRepository.findByCategoria_Id(categoriaId);
        
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
        // Búsqueda por nombre o código
        List<Producto> productos = productoRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(query, query);
        
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

    private ProductoInventarioCompletoDTO convertirADTO(Producto producto, Map<Long, Integer> inventariosPorSede) {
        // Obtener cantidades por sede (0 si no existe)
        Long insulaId = obtenerSedeId("insula");
        Long centroId = obtenerSedeId("centro");
        Long patiosId = obtenerSedeId("patios");

        Integer cantidadInsula = inventariosPorSede != null && insulaId != null ? inventariosPorSede.getOrDefault(insulaId, 0) : 0;
        Integer cantidadCentro = inventariosPorSede != null && centroId != null ? inventariosPorSede.getOrDefault(centroId, 0) : 0;
        Integer cantidadPatios = inventariosPorSede != null && patiosId != null ? inventariosPorSede.getOrDefault(patiosId, 0) : 0;

        // Verificar si es vidrio y obtener datos específicos
        Boolean esVidrio = producto instanceof ProductoVidrio;
        Double mm = null;
        Double m1m2 = null;
        Integer laminas = null;

        if (esVidrio) {
            ProductoVidrio vidrio = (ProductoVidrio) producto;
            mm = vidrio.getMm();
            m1m2 = vidrio.getM1m2();
            laminas = vidrio.getLaminas();
        }

        // Obtener nombre de la categoría
        String categoriaNombre = producto.getCategoria() != null ? producto.getCategoria().getNombre() : null;

        return new ProductoInventarioCompletoDTO(
            producto.getId(),
            producto.getCodigo(),
            producto.getNombre(),
            categoriaNombre,
            esVidrio,
            mm,
            m1m2,
            laminas,
            cantidadInsula,
            cantidadCentro,
            cantidadPatios,
            producto.getPrecio1(),
            producto.getPrecio2(),
            producto.getPrecio3(),
            producto.getPrecioEspecial()
        );
    }
}