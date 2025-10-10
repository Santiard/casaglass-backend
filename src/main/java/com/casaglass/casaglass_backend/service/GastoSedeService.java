package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.GastoSede;
import com.casaglass.casaglass_backend.repository.GastoSedeRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class GastoSedeService {

    @Autowired
    private GastoSedeRepository gastoSedeRepository;

    @Autowired
    private SedeRepository sedeRepository;

    public List<GastoSede> obtenerTodos() {
        return gastoSedeRepository.findAll();
    }

    public Optional<GastoSede> obtenerPorId(Long id) {
        return gastoSedeRepository.findById(id);
    }

    public List<GastoSede> obtenerPorSede(Long sedeId) {
        return gastoSedeRepository.findBySedeId(sedeId);
    }

    public List<GastoSede> obtenerPorTipo(GastoSede.TipoGasto tipo) {
        return gastoSedeRepository.findByTipo(tipo);
    }

    public List<GastoSede> obtenerPorSedeYTipo(Long sedeId, GastoSede.TipoGasto tipo) {
        return gastoSedeRepository.findBySedeIdAndTipo(sedeId, tipo);
    }

    public List<GastoSede> obtenerPorPeriodo(LocalDate desde, LocalDate hasta) {
        return gastoSedeRepository.findByFechaGastoBetween(desde, hasta);
    }

    public List<GastoSede> obtenerPorSedeYPeriodo(Long sedeId, LocalDate desde, LocalDate hasta) {
        return gastoSedeRepository.findBySedeIdAndFechaGastoBetween(sedeId, desde, hasta);
    }

    public List<GastoSede> obtenerGastosAprobados() {
        return gastoSedeRepository.findByAprobado(true);
    }

    public List<GastoSede> obtenerGastosPendientes() {
        return gastoSedeRepository.findByAprobado(false);
    }

    public List<GastoSede> obtenerGastosPendientesAprobacion(Long sedeId) {
        return gastoSedeRepository.findBySedeIdAndAprobado(sedeId, false);
    }

    public List<GastoSede> obtenerGastosSinEntrega(Long sedeId) {
        return gastoSedeRepository.findGastosSinEntregaBySede(sedeId);
    }

    public Double obtenerTotalGastosSedeEnPeriodo(Long sedeId, LocalDate desde, LocalDate hasta) {
        Double total = gastoSedeRepository.getTotalGastosBySedeAndPeriodo(sedeId, desde, hasta);
        return total != null ? total : 0.0;
    }

    public Double obtenerTotalGastosSedeYTipoEnPeriodo(Long sedeId, GastoSede.TipoGasto tipo, LocalDate desde, LocalDate hasta) {
        Double total = gastoSedeRepository.getTotalGastosBySedeAndTipoAndPeriodo(sedeId, tipo, desde, hasta);
        return total != null ? total : 0.0;
    }

    public List<GastoSede> buscarPorConcepto(String concepto) {
        return gastoSedeRepository.findByConceptoContaining(concepto);
    }

    public List<Object[]> obtenerResumenGastosPorConcepto(Long sedeId, LocalDate desde, LocalDate hasta) {
        return gastoSedeRepository.getResumenGastosByConcepto(sedeId, desde, hasta);
    }

    public GastoSede crearGasto(GastoSede gasto) {
        // Validar que la sede existe
        if (gasto.getSede() == null || !sedeRepository.existsById(gasto.getSede().getId())) {
            throw new RuntimeException("La sede especificada no existe");
        }

        // Establecer valores por defecto
        if (gasto.getFechaGasto() == null) {
            gasto.setFechaGasto(LocalDate.now());
        }

        if (gasto.getAprobado() == null) {
            gasto.setAprobado(false);
        }

        return gastoSedeRepository.save(gasto);
    }

    public GastoSede actualizarGasto(Long id, GastoSede gastoActualizado) {
        return gastoSedeRepository.findById(id)
                .map(gasto -> {
                    gasto.setConcepto(gastoActualizado.getConcepto());
                    gasto.setDescripcion(gastoActualizado.getDescripcion());
                    gasto.setMonto(gastoActualizado.getMonto());
                    gasto.setTipo(gastoActualizado.getTipo());
                    gasto.setComprobante(gastoActualizado.getComprobante());
                    gasto.setAprobado(gastoActualizado.getAprobado());
                    
                    if (gastoActualizado.getSede() != null && sedeRepository.existsById(gastoActualizado.getSede().getId())) {
                        gasto.setSede(gastoActualizado.getSede());
                    }

                    return gastoSedeRepository.save(gasto);
                })
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + id));
    }

    public GastoSede aprobarGasto(Long id) {
        return gastoSedeRepository.findById(id)
                .map(gasto -> {
                    gasto.setAprobado(true);
                    return gastoSedeRepository.save(gasto);
                })
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + id));
    }

    public GastoSede rechazarGasto(Long id) {
        return gastoSedeRepository.findById(id)
                .map(gasto -> {
                    gasto.setAprobado(false);
                    return gastoSedeRepository.save(gasto);
                })
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado con id: " + id));
    }

    public void eliminarGasto(Long id) {
        if (!gastoSedeRepository.existsById(id)) {
            throw new RuntimeException("Gasto no encontrado con id: " + id);
        }
        gastoSedeRepository.deleteById(id);
    }

    public boolean validarGastoParaEntrega(Long gastoId) {
        Optional<GastoSede> gasto = gastoSedeRepository.findById(gastoId);
        return gasto.isPresent() && gasto.get().getAprobado() && gasto.get().getEntrega() == null;
    }

    public List<GastoSede> obtenerPorEntrega(Long entregaId) {
        return gastoSedeRepository.findByEntregaId(entregaId);
    }
}