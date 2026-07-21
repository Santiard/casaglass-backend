package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaResponseDTO;
import com.casaglass.casaglass_backend.model.*;
import com.casaglass.casaglass_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
public class DecimalVidrioTest {

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Test
    public void testCrearYEditarOrdenConDecimales() {
        System.out.println("=================================================");
        System.out.println("INICIANDO TEST DE CANTIDADES DECIMALES (VIDRIO)");
        System.out.println("=================================================");

        // 1. Buscar un producto, un cliente y una sede validos
        List<Producto> productos = productoRepository.findAll();
        Producto productoVidrio = productos.stream()
                .filter(p -> p.getCategoria() != null && p.getCategoria().getNombre() != null && p.getCategoria().getNombre().equalsIgnoreCase("vidrio"))
                .findFirst()
                .orElse(productos.get(0));

        Cliente cliente = clienteRepository.findAll().get(0);
        Long sedeId = 1L;

        System.out.println("Producto seleccionado: ID " + productoVidrio.getId() + " - " + productoVidrio.getNombre());
        
        // 2. Obtener inventario actual
        Double inventarioInicial = 0.0;
        Optional<Inventario> invOpt = inventarioService.obtenerPorProductoYSede(productoVidrio.getId(), sedeId);
        if (invOpt.isPresent()) {
            inventarioInicial = invOpt.get().getCantidad();
            System.out.println("Inventario Inicial: " + inventarioInicial);
        } else {
            System.out.println("Sin inventario previo, asumiendo 0.0");
        }

        // ==========================================
        // 3. PRUEBA DE CREACIÓN CON DECIMALES (1.5)
        // ==========================================
        OrdenVentaDTO ventaDTO = new OrdenVentaDTO();
        ventaDTO.setClienteId(cliente.getId());
        ventaDTO.setSedeId(sedeId);
        ventaDTO.setVenta(true);
        ventaDTO.setMontoEfectivo(10000.0);

        OrdenVentaDTO.OrdenItemVentaDTO itemDTO = new OrdenVentaDTO.OrdenItemVentaDTO();
        itemDTO.setProductoId(productoVidrio.getId());
        itemDTO.setCantidad(1.5); // DECIMAL!
        itemDTO.setPrecioUnitario(5000.0);
        
        List<OrdenVentaDTO.OrdenItemVentaDTO> items = new ArrayList<>();
        items.add(itemDTO);
        ventaDTO.setItems(items);

        System.out.println("Ejecutando CREACIÓN de Orden con cantidad: 1.5...");
        OrdenVentaResponseDTO response = ordenService.crearOrdenVenta(ventaDTO);
        Long nuevaOrdenId = response.getOrden().getId();
        System.out.println("Orden Creada Exitosamente con ID: " + nuevaOrdenId);

        // Verificar inventario tras creación (debe haber restado 1.5)
        Inventario invDespuesCreacion = inventarioService.obtenerPorProductoYSede(productoVidrio.getId(), sedeId).get();
        System.out.println("Inventario después de creación: " + invDespuesCreacion.getCantidad());
        System.out.println("Diferencia de inventario: " + (inventarioInicial - invDespuesCreacion.getCantidad()));
        assertEquals(1.5, inventarioInicial - invDespuesCreacion.getCantidad(), 0.001);

        // ==========================================
        // 4. PRUEBA DE EDICIÓN CON DECIMALES (2.7)
        // ==========================================
        // Obtener el ID del item creado
        Orden ordenCreada = ordenRepository.findById(nuevaOrdenId).get();
        Long itemId = ordenCreada.getItems().get(0).getId();

        OrdenActualizarDTO actualizarDTO = new OrdenActualizarDTO();
        actualizarDTO.setClienteId(cliente.getId());
        actualizarDTO.setSedeId(sedeId);
        actualizarDTO.setVenta(true);
        actualizarDTO.setMontoEfectivo(15000.0);

        OrdenActualizarDTO.OrdenItemActualizarDTO itemActualizar = new OrdenActualizarDTO.OrdenItemActualizarDTO();
        itemActualizar.setId(itemId);
        itemActualizar.setProductoId(productoVidrio.getId());
        itemActualizar.setCantidad(2.7); // DECIMAL ACTUALIZADO!
        itemActualizar.setPrecioUnitario(5000.0);

        List<OrdenActualizarDTO.OrdenItemActualizarDTO> itemsActualizar = new ArrayList<>();
        itemsActualizar.add(itemActualizar);
        actualizarDTO.setItems(itemsActualizar);

        System.out.println("Ejecutando EDICIÓN de Orden (Cambiando cantidad de 1.5 a 2.7)...");
        ordenService.actualizarOrden(nuevaOrdenId, actualizarDTO);
        System.out.println("Orden Editada Exitosamente");

        // Verificar inventario tras edición (debe haber restado 2.7 en total desde el inicio)
        Inventario invDespuesEdicion = inventarioService.obtenerPorProductoYSede(productoVidrio.getId(), sedeId).get();
        System.out.println("Inventario después de edición: " + invDespuesEdicion.getCantidad());
        System.out.println("Diferencia de inventario total respecto al inicial: " + (inventarioInicial - invDespuesEdicion.getCantidad()));
        assertEquals(2.7, inventarioInicial - invDespuesEdicion.getCantidad(), 0.001);
        
        // ==========================================
        // 5. LIMPIEZA DE DATOS (Anular orden para restaurar inventario)
        // ==========================================
        System.out.println("Anulando orden para no dejar basura en BD DEV...");
        ordenService.anularOrden(nuevaOrdenId);
        Inventario invFinal = inventarioService.obtenerPorProductoYSede(productoVidrio.getId(), sedeId).get();
        System.out.println("Inventario tras anulación (debe ser igual al inicial): " + invFinal.getCantidad());
        assertEquals(inventarioInicial, invFinal.getCantidad(), 0.001);

        System.out.println("=================================================");
        System.out.println("TEST FINALIZADO CON ÉXITO");
        System.out.println("=================================================");
    }
}
