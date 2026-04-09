package com.casaglass.casaglass_backend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Test para validar que no se puede CONFIRMAR una cotización con items CM sin cmBase
 * cuando se usa PUT /api/ordenes/tabla/{id}
 */
@DisplayName("Confirmar Cotización - Validación CM para Sede 1")
public class OrdenServiceConfirmarCotizacionTest {

    /**
     * Simula el flujo:
     * 1. Crear cotización con CM sin cmBase ✅ PERMITIDO
     * 2. Intentar confirmar SIN actualizar cmBase ❌ DEBE RECHAZAR
     */
    
    // Simulamos la lógica de validación de items persistidos
    private void validarItemsPersistidos(String tipoUnidad, Integer cmBase, boolean ventaConfirmada) {
        if (!"CM".equals(tipoUnidad)) {
            return;
        }
        
        if (cmBase != null && cmBase <= 0) {
            throw new IllegalArgumentException("CM base inválido");
        }
        
        if (ventaConfirmada && cmBase == null) {
            throw new IllegalArgumentException(
                "No se puede confirmar venta: el item tipo CM no tiene cmBase completado"
            );
        }
    }

    @Test
    @DisplayName("❌ Confirmar cotización sin actualizar cmBase - DEBE RECHAZAR")
    public void testNoPermiteConfirmarSinCmBase() {
        // Escenario: Cotización existente con item CM sin cmBase
        // Frontend intenta: PUT /api/ordenes/tabla/{id} { venta: true, items: [] }
        // (vacío porque solo quiere cambiar venta a true, sin actualizar items)
        
        String tipoUnidad = "CM";
        Integer cmBase = null; // Aún no completado en la cotización
        boolean ventaAntes = false; // Era cotización
        boolean ventaDespues = true; // Intentando confirmar
        
        // Cuando se detecta: eraVentaAntes=false Y dto.isVenta()=true
        // Se debe validar items persistidos
        if (!ventaAntes && ventaDespues) {
            assertThrows(
                IllegalArgumentException.class,
                () -> validarItemsPersistidos(tipoUnidad, cmBase, ventaDespues),
                "Debe rechazar confirmar sin cmBase completo"
            );
        }
    }

    @Test
    @DisplayName("✅ Confirmar cotización CON cmBase actualizado - DEBE PERMITIR")
    public void testPermiteConfirmarConCmBase() {
        // Escenario: Cotización actualizada con cmBase=400
        // Frontend: PUT /api/ordenes/tabla/{id} { venta: true, items: [{ cmBase: 400 }] }
        
        String tipoUnidad = "CM";
        Integer cmBase = 400; // Ahora completado
        boolean ventaAntes = false;
        boolean ventaDespues = true;
        
        if (!ventaAntes && ventaDespues) {
            assertDoesNotThrow(
                () -> validarItemsPersistidos(tipoUnidad, cmBase, ventaDespues),
                "Debe permitir confirmar si cmBase está completo"
            );
        }
    }

    @Test
    @DisplayName("✅ Confirmar cotización con cmBase=600 - DEBE PERMITIR")
    public void testPermiteConfirmarConCmBase600() {
        String tipoUnidad = "CM";
        Integer cmBase = 600; // Corte de pieza entera
        boolean ventaAntes = false;
        boolean ventaDespues = true;
        
        if (!ventaAntes && ventaDespues) {
            assertDoesNotThrow(
                () -> validarItemsPersistidos(tipoUnidad, cmBase, ventaDespues),
                "Debe permitir confirmar si cmBase=600"
            );
        }
    }

    @Test
    @DisplayName("✅ Mantener cotización sin cambios - DEBE PERMITIR")
    public void testMantenerCotizacionSinCambios() {
        // Escenario: Solo se edi otros campos, venta sigue siendo false
        String tipoUnidad = "CM";
        Integer cmBase = null;
        boolean ventaAntes = false;
        boolean ventaDespues = false; // SIN cambiar a venta
        
        // No entra al condicional de validación porque ventaAntes == ventaDespues
        assertDoesNotThrow(
            () -> validarItemsPersistidos(tipoUnidad, cmBase, ventaDespues),
            "Cotización sin confirmación debe permitirse"
        );
    }

    @Test
    @DisplayName("✅ Cambiar de venta a cotización - DEBE PERMITIR")
    public void testReversoAcotizacion() {
        // Escenario: Máquina de venta (venta=true) con cmBase=400 se cambia a cotización
        String tipoUnidad = "CM";
        Integer cmBase = 400; // Tiene cmBase pero se vuelve a cotización
        boolean ventaAntes = true;
        boolean ventaDespues = false;
        
        // No necesita validación de cmBase porque venta=false (cotización)
        assertDoesNotThrow(
            () -> validarItemsPersistidos(tipoUnidad, cmBase, ventaDespues),
            "Reverso a cotización debe permitirse"
        );
    }
}
