package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.dto.CatalogoProductosTrasladoResponseDTO;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.model.Rol;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.repository.CatalogoProductoTrasladoProjection;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import com.casaglass.casaglass_backend.repository.TrasladoDetalleRepository;
import com.casaglass.casaglass_backend.repository.TrasladoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrasladoServiceCatalogoTest {

    @Mock
    private TrasladoRepository trasladoRepository;

    @Mock
    private TrasladoDetalleRepository trasladoDetalleRepository;

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private TrabajadorRepository trabajadorRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TrasladoService trasladoService;

    @Test
    void adminConSedeValidaDebeRetornarCatalogo() {
        when(sedeRepository.existsById(2L)).thenReturn(true);
        when(trabajadorRepository.findById(10L)).thenReturn(Optional.of(crearTrabajador(10L, Rol.ADMINISTRADOR, 99L)));

        CatalogoProductoTrasladoProjection projection = projection(76L, "ENG001", "ENGRANCHE", 1L, "Herrajes", ColorProducto.NEGRO, 14.0, 20.0);
        Page<CatalogoProductoTrasladoProjection> page = new PageImpl<>(List.of(projection));
        when(productoRepository.buscarCatalogoParaTraslado(eq(2L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        CatalogoProductosTrasladoResponseDTO response = trasladoService.obtenerCatalogoParaTraslado(2L, null, null, null, 1, 20, 10L);

        assertEquals(2L, response.getSedeOrigenId());
        assertEquals(1, response.getItems().size());
        assertEquals("ENG001", response.getItems().get(0).getCodigo());
        assertEquals(14.0, response.getItems().get(0).getCantidadSedeOrigen());
        assertTrue(response.getItems().get(0).isEsTrasladable());
    }

    @Test
    void vendedorConSuSedeDebePoderConsultar() {
        when(sedeRepository.existsById(3L)).thenReturn(true);
        when(trabajadorRepository.findById(11L)).thenReturn(Optional.of(crearTrabajador(11L, Rol.VENDEDOR, 3L)));

        Page<CatalogoProductoTrasladoProjection> page = new PageImpl<>(List.of());
        when(productoRepository.buscarCatalogoParaTraslado(eq(3L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        CatalogoProductosTrasladoResponseDTO response = trasladoService.obtenerCatalogoParaTraslado(3L, null, null, null, 1, 20, 11L);

        assertEquals(3L, response.getSedeOrigenId());
        verify(productoRepository).buscarCatalogoParaTraslado(eq(3L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void vendedorIntentandoOtraSedeDebeRetornar403Logico() {
        when(sedeRepository.existsById(2L)).thenReturn(true);
        when(trabajadorRepository.findById(12L)).thenReturn(Optional.of(crearTrabajador(12L, Rol.VENDEDOR, 1L)));

        assertThrows(SecurityException.class,
                () -> trasladoService.obtenerCatalogoParaTraslado(2L, null, null, null, 1, 20, 12L));
    }

    @Test
    void sinSedeOrigenIdDebeRetornar400Logico() {
        assertThrows(IllegalArgumentException.class,
                () -> trasladoService.obtenerCatalogoParaTraslado(null, null, null, null, 1, 20, null));
    }

    @Test
    void productoSinInventarioEnSedeDebeDevolverCantidadCero() {
        when(sedeRepository.existsById(1L)).thenReturn(true);

        CatalogoProductoTrasladoProjection projectionSinInventario = projection(80L, "ENG002", "ENGRANCHE BLANCO", 1L, "Herrajes", ColorProducto.BLANCO, null, 3.0);
        Page<CatalogoProductoTrasladoProjection> page = new PageImpl<>(List.of(projectionSinInventario));
        when(productoRepository.buscarCatalogoParaTraslado(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        CatalogoProductosTrasladoResponseDTO response = trasladoService.obtenerCatalogoParaTraslado(1L, null, null, null, 1, 20, null);

        assertEquals(0.0, response.getItems().get(0).getCantidadSedeOrigen());
    }

    @Test
    void filtrosQCategoriaColorDebenAplicarse() {
        when(sedeRepository.existsById(2L)).thenReturn(true);
        Page<CatalogoProductoTrasladoProjection> page = new PageImpl<>(List.of());
        when(productoRepository.buscarCatalogoParaTraslado(eq(2L), eq("eng"), eq(5L), eq(ColorProducto.NEGRO), any(Pageable.class))).thenReturn(page);

        trasladoService.obtenerCatalogoParaTraslado(2L, "  eng  ", 5L, "negro", 2, 30, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productoRepository).buscarCatalogoParaTraslado(eq(2L), eq("eng"), eq(5L), eq(ColorProducto.NEGRO), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(30, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void catalogoExcluyeCortesDesdeQueryEspecializada() {
        when(sedeRepository.existsById(2L)).thenReturn(true);
        when(productoRepository.buscarCatalogoParaTraslado(eq(2L), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        trasladoService.obtenerCatalogoParaTraslado(2L, null, null, null, 1, 20, null);

        // El método de repositorio usado tiene WHERE TYPE(p) != Corte.
        verify(productoRepository).buscarCatalogoParaTraslado(eq(2L), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    private Trabajador crearTrabajador(Long id, Rol rol, Long sedeId) {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(id);
        trabajador.setRol(rol);
        Sede sede = new Sede();
        sede.setId(sedeId);
        trabajador.setSede(sede);
        return trabajador;
    }

    private CatalogoProductoTrasladoProjection projection(Long id,
                                                          String codigo,
                                                          String nombre,
                                                          Long categoriaId,
                                                          String categoriaNombre,
                                                          ColorProducto color,
                                                          Double cantidadSedeOrigen,
                                                          Double cantidadTotal) {
        return new CatalogoProductoTrasladoProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getCodigo() {
                return codigo;
            }

            @Override
            public String getNombre() {
                return nombre;
            }

            @Override
            public Long getCategoriaId() {
                return categoriaId;
            }

            @Override
            public String getCategoriaNombre() {
                return categoriaNombre;
            }

            @Override
            public ColorProducto getColor() {
                return color;
            }

            @Override
            public Double getCantidadSedeOrigen() {
                return cantidadSedeOrigen;
            }

            @Override
            public Double getCantidadTotal() {
                return cantidadTotal;
            }

            @Override
            public Double getPrecio1() {
                return 1000.0;
            }

            @Override
            public Double getPrecio2() {
                return 1200.0;
            }

            @Override
            public Double getPrecio3() {
                return 1400.0;
            }
        };
    }
}
