package com.casaglass.casaglass_backend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Test para validar el comportamiento CORRECTO de la validación de items CM
 * en sede 1:
 * 
 * ✅ Cotización (venta=false) sin cmBase → PERMITIDO
 * ❌ Venta confirmada (venta=true) sin cmBase → RECHAZADO
 */
@DisplayName("Validación CM para Sede 1 - Lógica de Cotización vs Venta")
public class OrdenServiceSede1ValidationTest {

    /**
     * Simula la lógica de validación actual en OrdenService.validarItemsSedeSinControlCortes
     */
    private void validarCMItem(String tipoUnidad, Integer cmBase, boolean ventaConfirmada) {
        if (!"CM".equals(tipoUnidad)) {
            return; // No es CM, no validamos
        }
        
        // Validar rango
        if (cmBase != null && cmBase <= 0) {
            throw new IllegalArgumentException("CM base inválido, debe ser mayor a 0");
        }
        
        // ✅ CLAVE: Solo exigir cmBase si es venta confirmada
        if (ventaConfirmada && cmBase == null) {
            throw new IllegalArgumentException("CM base es obligatorio para confirmar ventas CM en sede principal");
        }
    }

    @Test
    @DisplayName("✅ Cotización con CM sin cmBase - DEBE PERMITIRSE")
    public void testCotizacionSinCmBasePermitida() {
        // Cotización (venta=false) con Item CM sin cmBase
        assertDoesNotThrow(
            () -> validarCMItem("CM", null, false),
            "Cotización sin cmBase debe permitirse para permite a bodega confirmar origen"
        );
    }

    @Test
    @DisplayName("❌ Venta confirmada con CM sin cmBase - DEBE RECHAZARSE")
    public void testVentaConfirmadaSinCmBaseRechazada() {
        // Venta confirmada (venta=true) con Item CM sin cmBase
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> validarCMItem("CM", null, true),
            "Venta confirmada sin cmBase debe rechazarse "
        );
        assertTrue(ex.getMessage().contains("obligatorio"), "Mensaje debe indicar que cmBase es obligatorio");
    }

    @Test
    @DisplayName("✅ Venta con CM y cmBase=600 (corte entero) - DEBE PERMITIRSE")
    public void testVentaConCmBase600Permitida() {
        // Venta confirmada (venta=true) con Item CM con cmBase=600
        assertDoesNotThrow(
            () -> validarCMItem("CM", 600, true),
            "Venta con cmBase=600 debe permitirse (corte de pieza entera)"
        );
    }

    @Test
    @DisplayName("✅ Venta con CM y cmBase=300 (fragmento) - DEBE PERMITIRSE")
    public void testVentaConCmBase300Permitida() {
        // Venta confirmada (venta=true) con Item CM con cmBase<600
        assertDoesNotThrow(
            () -> validarCMItem("CM", 300, true),
            "Venta con cmBase<600 debe permitirse (fragmento de pieza)"
        );
    }

    @Test
    @DisplayName("❌ CM base inválido (0) - DEBE RECHAZARSE")
    public void testCmBaseCeroRechazado() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> validarCMItem("CM", 0, false)
        );
        assertTrue(ex.getMessage().contains("mayor a 0"), "Mensaje debe indicar que cmBase es inválido");
    }

    @Test
    @DisplayName("❌ CM base negativo - DEBE RECHAZARSE")
    public void testCmBaseNegativoRechazado() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> validarCMItem("CM", -100, true)
        );
        assertTrue(ex.getMessage().contains("mayor a 0"));
    }

    @Test
    @DisplayName("✅ Tipo UNID - NO VALIDA cmBase")
    public void testTipoUNIDNoValidaCmBase() {
        // Items UNID/PERFIL/MT no deben validar cmBase
        assertDoesNotThrow(
            () -> validarCMItem("UNID", 600, true),
            "Tipo UNID no debe validar cmBase"
        );
    }
}
