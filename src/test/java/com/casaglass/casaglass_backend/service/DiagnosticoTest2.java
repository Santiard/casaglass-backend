package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("dev")
public class DiagnosticoTest2 {

    @Autowired
    private OrdenRepository repository;

    @Test
    public void diagnosticoOrden33() {
        System.out.println("=================================================");
        System.out.println("DIAGNOSTICO ORDEN 33");
        System.out.println("=================================================");
        
        Optional<Orden> opt = repository.findByNumero(33L);
        if (opt.isPresent()) {
            Orden orden = opt.get();
            System.out.println("ID: " + orden.getId());
            System.out.println("Numero: " + orden.getNumero());
            System.out.println("Total: " + orden.getTotal());
            System.out.println("Monto Efectivo: " + orden.getMontoEfectivo());
            System.out.println("Monto Transferencia: " + orden.getMontoTransferencia());
            System.out.println("Monto Cheque: " + orden.getMontoCheque());
            System.out.println("Descripcion: " + orden.getDescripcion());
        } else {
            System.out.println("No se encontró la orden con número 33.");
        }
        System.out.println("=================================================");
    }
}
