package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.repository.OrdenItemRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrdenItemService {

    private final OrdenItemRepository itemRepo;
    private final OrdenRepository ordenRepo;
    private final EntityManager em;

    public OrdenItemService(OrdenItemRepository itemRepo,
                            OrdenRepository ordenRepo,
                            EntityManager em) {
        this.itemRepo = itemRepo;
        this.ordenRepo = ordenRepo;
        this.em = em;
    }

    public List<OrdenItem> listarPorOrden(Long ordenId) {
        return itemRepo.findByOrdenId(ordenId);
    }

    public Optional<OrdenItem> obtener(Long itemId) {
        return itemRepo.findById(itemId);
    }

    @Transactional
    public OrdenItem crear(Long ordenId, OrdenItem payload) {
        Orden orden = ordenRepo.findById(ordenId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + ordenId));

        if (payload.getCantidad() == null || payload.getCantidad() < 1) {
            throw new IllegalArgumentException("La cantidad debe ser >= 1");
        }
        if (payload.getPrecioUnitario() == null) {
            throw new IllegalArgumentException("El precioUnitario es obligatorio");
        }

        OrdenItem item = new OrdenItem();
        item.setOrden(orden);

        // Producto opcional
        if (payload.getProducto() != null && payload.getProducto().getId() != null) {
            Producto prodRef = em.getReference(Producto.class, payload.getProducto().getId());
            item.setProducto(prodRef);
            // ✅ Campo descripcion eliminado - los datos del producto se obtienen mediante la relación
        }

        // Normalizar dinero
        Double precio = normalize(payload.getPrecioUnitario());
        item.setPrecioUnitario(precio);

        item.setCantidad(payload.getCantidad());
        item.setTotalLinea(Math.round(precio * item.getCantidad() * 100.0) / 100.0);

        OrdenItem guardado = itemRepo.save(item);
        recalcularTotales(orden);
        return guardado;
    }

    @Transactional
    public OrdenItem actualizar(Long ordenId, Long itemId, OrdenItem payload) {
        OrdenItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("OrdenItem no encontrado: " + itemId));

        if (!Objects.equals(item.getOrden().getId(), ordenId)) {
            throw new IllegalArgumentException("El ítem no pertenece a la orden indicada");
        }

        if (payload.getProducto() != null && payload.getProducto().getId() != null) {
            Producto prodRef = em.getReference(Producto.class, payload.getProducto().getId());
            item.setProducto(prodRef);
        }
        // ✅ Campo descripcion eliminado - los datos del producto se obtienen mediante la relación
        if (payload.getCantidad() != null) {
            if (payload.getCantidad() < 1) throw new IllegalArgumentException("La cantidad debe ser >= 1");
            item.setCantidad(payload.getCantidad());
        }
        if (payload.getPrecioUnitario() != null) {
            item.setPrecioUnitario(normalize(payload.getPrecioUnitario()));
        }

        // Recalcular total de línea con valores actuales
        item.setTotalLinea(Math.round(item.getPrecioUnitario() * item.getCantidad() * 100.0) / 100.0);

        OrdenItem guardado = itemRepo.save(item);
        recalcularTotales(item.getOrden());
        return guardado;
    }

    @Transactional
    public void eliminar(Long ordenId, Long itemId) {
        OrdenItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("OrdenItem no encontrado: " + itemId));
        if (!Objects.equals(item.getOrden().getId(), ordenId)) {
            throw new IllegalArgumentException("El ítem no pertenece a la orden indicada");
        }
        Orden orden = item.getOrden();
        itemRepo.delete(item);
        recalcularTotales(orden);
    }

    /* ----------------- Helpers ----------------- */

    private Double normalize(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }

    private void recalcularTotales(Orden orden) {
        List<OrdenItem> items = itemRepo.findByOrdenId(orden.getId());
        double subtotal = 0.0;
        for (OrdenItem it : items) {
            subtotal += it.getTotalLinea() != null ? it.getTotalLinea() : 0.0;
        }
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        orden.setSubtotal(subtotal);
        orden.setTotal(subtotal); // impuestos/desc. podrían sumarse aquí más adelante
        ordenRepo.save(orden);
    }
}
