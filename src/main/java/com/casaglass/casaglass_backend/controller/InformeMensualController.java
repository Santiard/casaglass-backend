package com.casaglass.casaglass_backend.controller;

import com.casaglass.casaglass_backend.dto.InformeMensualCierreListItemDTO;
import com.casaglass.casaglass_backend.dto.InformeMensualCierreRequestDTO;
import com.casaglass.casaglass_backend.dto.InformeMensualResponseDTO;
import com.casaglass.casaglass_backend.service.InformeMensualService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/informes/mensual")
public class InformeMensualController {

    private final InformeMensualService informeMensualService;

    public InformeMensualController(InformeMensualService informeMensualService) {
        this.informeMensualService = informeMensualService;
    }

    @GetMapping("/preview")
    public ResponseEntity<InformeMensualResponseDTO> preview(
            @RequestParam Long sedeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(informeMensualService.calcularPreview(sedeId, year, month));
    }

    @GetMapping("/cierre")
    public ResponseEntity<InformeMensualResponseDTO> obtenerCierre(
            @RequestParam Long sedeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(informeMensualService.obtenerCierre(sedeId, year, month));
    }

    @PostMapping("/cierre")
    public ResponseEntity<InformeMensualResponseDTO> cerrar(
            @Valid @RequestBody InformeMensualCierreRequestDTO body) {
        return ResponseEntity.ok(informeMensualService.cerrarMes(body));
    }

    @GetMapping("/cierres")
    public ResponseEntity<List<InformeMensualCierreListItemDTO>> listar(
            @RequestParam Long sedeId,
            @RequestParam int year) {
        return ResponseEntity.ok(informeMensualService.listarCierresAnio(sedeId, year));
    }
}
