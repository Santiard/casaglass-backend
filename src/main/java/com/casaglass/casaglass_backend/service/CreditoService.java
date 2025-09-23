package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Abono;
import com.casaglass.casaglass_backend.repository.CreditoRepository;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.AbonoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CreditoService {

    private final CreditoRepository creditoRepo;
    private final OrdenRepository ordenRepo;
    private final AbonoRepository abonoRepo;

    public CreditoService(CreditoRepository creditoRepo,
                          OrdenRepository ordenRepo,
                          AbonoRepository abonoRepo) {
        this.creditoRepo = creditoRepo;
        this.ordenRepo = ordenRepo;
        this.abonoRepo = abonoRepo;
    }

    /* ---------- Helpers de dinero (2 decimales, HALF_UP) ---------- */

    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RM) : v.setScale(2, RM);
    }

    private BigDecimal normalize(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RM) : v.setScale(2, RM);
    }

    private BigDecimal sumMoney(Collection<BigDecimal> values) {
        BigDecimal acc = BigDecimal.ZERO;
        for (BigDecimal v : values) acc = acc.add(v == null ? BigDecimal.ZERO : v);
        return acc.setScale(2, RM);
    }

    /* --------------------- Operaciones de negocio --------------------- */

    public Optional<Credito> obtener(Long id) { return creditoRepo.findById(id); }

    public Optional<Credito> obtenerPorCliente(Long clienteId) { return creditoRepo.findByClienteId(clienteId); }

    public List<Credito> listar() { return creditoRepo.findAll(); }

    public List<Credito> listarPorCliente(Long clienteId) { return creditoRepo.findAllByClienteId(clienteId); }

    @Transactional
    public Credito crear(Credito payload) {
        if (payload.getCliente() == null || payload.getCliente().getId() == null) {
            throw new IllegalArgumentException("Debe especificar cliente.id");
        }
        // Normaliza totalDeuda (si viene en el body, lo ignoramos y partimos en 0; se recalcula al agregar órdenes/abonos)
        payload.setTotalDeuda(normalize(BigDecimal.ZERO));
        // Asegura lista de órdenes y abonos no nulas
        if (payload.getOrdenes() == null) payload.setOrdenes(new ArrayList<>());
        if (payload.getAbonos() == null) payload.setAbonos(new ArrayList<>());
        return creditoRepo.save(payload);
    }

    /**
     * Agrega órdenes a un crédito y recalcula totalDeuda = SUM(orden.total) - SUM(abonos.total).
     * Valida que las órdenes pertenezcan al mismo cliente.
     */
    @Transactional
    public Credito agregarOrdenes(Long creditoId, List<Long> ordenIds) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));

        if (ordenIds == null || ordenIds.isEmpty()) return recalcular(creditoId);

        List<Orden> nuevas = ordenRepo.findAllById(ordenIds);
        if (nuevas.size() != ordenIds.size()) {
            throw new IllegalArgumentException("Alguna orden no existe");
        }

        Long clienteId = credito.getCliente().getId();
        for (Orden o : nuevas) {
            if (!Objects.equals(o.getCliente().getId(), clienteId)) {
                throw new IllegalArgumentException("La orden " + o.getId() + " pertenece a otro cliente");
            }
        }

        // Evita duplicados (DB también protege con unique en join-table)
        Set<Long> existentes = new HashSet<>();
        for (Orden o : credito.getOrdenes()) existentes.add(o.getId());
        for (Orden o : nuevas) {
            if (!existentes.contains(o.getId())) credito.getOrdenes().add(o);
        }
        creditoRepo.save(credito);
        return recalcular(creditoId);
    }

    @Transactional
    public Credito quitarOrden(Long creditoId, Long ordenId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
        credito.getOrdenes().removeIf(o -> Objects.equals(o.getId(), ordenId));
        creditoRepo.save(credito);
        return recalcular(creditoId);
    }

    /**
     * Recalcula totalDeuda = SUM(orden.total) - SUM(abonos.total), clamp a 0 mínimo.
     */
    @Transactional
    public Credito recalcular(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));

        // Suma órdenes (normalizadas)
        BigDecimal totalOrdenes = sumMoney(
                credito.getOrdenes().stream().map(Orden::getTotal).toList()
        );

        // Suma abonos (normalizados)
        BigDecimal totalAbonos = sumMoney(
                abonoRepo.findByCreditoId(creditoId).stream().map(Abono::getTotal).toList()
        );

        BigDecimal deuda = totalOrdenes.subtract(totalAbonos).setScale(2, RM);
        if (deuda.signum() < 0) deuda = BigDecimal.ZERO.setScale(2, RM); // evita negativos
        credito.setTotalDeuda(deuda);
        return creditoRepo.save(credito);
    }

    @Transactional
    public void eliminar(Long creditoId) {
        creditoRepo.deleteById(creditoId);
    }
}