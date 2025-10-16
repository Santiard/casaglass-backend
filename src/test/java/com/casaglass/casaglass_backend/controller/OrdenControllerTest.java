package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.service.OrdenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdenController.class)
public class OrdenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdenService ordenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCrearOrdenConTrabajador() throws Exception {
        // Arrange
        Orden ordenRequest = new Orden();
        ordenRequest.setFecha(LocalDate.now());
        ordenRequest.setVenta(true);
        ordenRequest.setCredito(false);

        // Cliente mock
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Test");
        ordenRequest.setCliente(cliente);

        // Sede mock
        Sede sede = new Sede();
        sede.setId(1L);
        sede.setNombre("Sede Test");
        ordenRequest.setSede(sede);

        // 🆕 Trabajador mock
        Trabajador trabajador = new Trabajador();
        trabajador.setId(1L);
        trabajador.setNombre("Trabajador Test");
        ordenRequest.setTrabajador(trabajador);

        // Response mock
        Orden ordenResponse = new Orden();
        ordenResponse.setId(1L);
        ordenResponse.setNumero(1001L);
        ordenResponse.setFecha(LocalDate.now());
        ordenResponse.setVenta(true);
        ordenResponse.setCredito(false);
        ordenResponse.setCliente(cliente);
        ordenResponse.setSede(sede);
        ordenResponse.setTrabajador(trabajador);

        when(ordenService.crear(any(Orden.class))).thenReturn(ordenResponse);

        // Act & Assert
        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ordenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numero").value(1001))
                .andExpect(jsonPath("$.trabajador.id").value(1))
                .andExpect(jsonPath("$.trabajador.nombre").value("Trabajador Test"));
    }

    @Test
    public void testListarPorTrabajador() throws Exception {
        // Arrange
        Long trabajadorId = 1L;
        
        Orden orden1 = new Orden();
        orden1.setId(1L);
        orden1.setNumero(1001L);
        
        Orden orden2 = new Orden();
        orden2.setId(2L);
        orden2.setNumero(1002L);
        
        List<Orden> ordenes = Arrays.asList(orden1, orden2);
        
        when(ordenService.listarPorTrabajador(trabajadorId)).thenReturn(ordenes);

        // Act & Assert
        mockMvc.perform(get("/api/ordenes/trabajador/{trabajadorId}", trabajadorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].numero").value(1001))
                .andExpect(jsonPath("$[1].numero").value(1002));
    }

    @Test
    public void testListarConParametroTrabajador() throws Exception {
        // Arrange
        Long trabajadorId = 1L;
        
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setNumero(1001L);
        
        List<Orden> ordenes = Arrays.asList(orden);
        
        when(ordenService.listarPorTrabajador(trabajadorId)).thenReturn(ordenes);

        // Act & Assert
        mockMvc.perform(get("/api/ordenes")
                .param("trabajadorId", trabajadorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(1001));
    }
}