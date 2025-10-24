package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Credito;
import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.repository.CreditoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CreditoService {

    private final CreditoRepository creditoRepo;
    private final EntityManager entityManager;

    public CreditoService(CreditoRepository creditoRepo, EntityManager entityManager) {
        this.creditoRepo = creditoRepo;
        this.entityManager = entityManager;
    }

    /* ---------- Helpers de dinero (redondeado a 2 decimales) ---------- */

    private Double normalize(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }

    /* --------------------- Operaciones de negocio --------------------- */

    public Optional<Credito> obtener(Long id) { 
        return creditoRepo.findById(id); 
    }

    public Optional<Credito> obtenerPorOrden(Long ordenId) { 
        return creditoRepo.findByOrdenId(ordenId); 
    }

    public List<Credito> listar() { 
        return creditoRepo.findAll(); 
    }

    public List<Credito> listarPorCliente(Long clienteId) { 
        return creditoRepo.findByClienteId(clienteId); 
    }

    public List<Credito> listarPorEstado(Credito.EstadoCredito estado) { 
        return creditoRepo.findByEstado(estado); 
    }

    /**
     * 💳 CREAR CRÉDITO PARA UNA ORDEN
     * Se ejecuta cuando una orden se marca como crédito
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Credito crearCreditoParaOrden(Long ordenId, Long clienteId, Double totalOrden) {
        try {
            System.out.println("🔍 DEBUG: Verificando si ya existe crédito para orden " + ordenId);
            
            // Verificar que no exista ya un crédito para esta orden
            Optional<Credito> existente = creditoRepo.findByOrdenId(ordenId);
            if (existente.isPresent()) {
                System.out.println("⚠️ WARNING: Ya existe crédito para orden " + ordenId);
                return existente.get(); // Devolver el existente en lugar de fallar
            }

            System.out.println("🔍 DEBUG: Creando nuevo crédito...");
            
            // ⚠️ OBTENER LA ORDEN COMPLETA PARA ESTABLECER RELACIÓN BIDIRECCIONAL
            Orden orden = entityManager.find(Orden.class, ordenId);
            if (orden == null) {
                throw new IllegalArgumentException("Orden no encontrada con ID: " + ordenId);
            }
            
            Credito credito = new Credito();
            credito.setCliente(entityManager.getReference(Cliente.class, clienteId));
            credito.setOrden(orden); // Usar la orden completa, no una referencia
            credito.setFechaInicio(LocalDate.now());
            credito.setTotalCredito(normalize(totalOrden));
            credito.setTotalAbonado(0.0);
            credito.setSaldoPendiente(normalize(totalOrden));
            credito.setEstado(Credito.EstadoCredito.ABIERTO);

            // ⚡ ESTABLECER RELACIÓN BIDIRECCIONAL CORRECTAMENTE
            orden.setCreditoDetalle(credito);
            
            Credito creditoGuardado = creditoRepo.save(credito);
            System.out.println("✅ DEBUG: Crédito guardado con ID: " + creditoGuardado.getId());
            return creditoGuardado;
            
        } catch (Exception e) {
            // Log del error para debugging
            System.err.println("❌ ERROR al crear crédito: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al crear crédito: " + e.getMessage(), e);
        }
    }

    /**
     * 🔄 ACTUALIZAR CRÉDITO PARA UNA ORDEN
     * Se ejecuta cuando se actualiza una orden que tiene crédito
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Credito actualizarCreditoParaOrden(Long creditoId, Double nuevoTotalOrden) {
        try {
            System.out.println("🔄 DEBUG: Actualizando crédito ID: " + creditoId + " con nuevo total: " + nuevoTotalOrden);
            
            Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado con ID: " + creditoId));

            // Actualizar el total del crédito
            Double totalNormalizado = normalize(nuevoTotalOrden);
            credito.setTotalCredito(totalNormalizado);
            
            // Recalcular el saldo pendiente
            credito.actualizarSaldo();
            
            Credito creditoActualizado = creditoRepo.save(credito);
            System.out.println("✅ DEBUG: Crédito actualizado - Total: " + totalNormalizado + ", Saldo: " + creditoActualizado.getSaldoPendiente());
            
            return creditoActualizado;
            
        } catch (Exception e) {
            System.err.println("❌ ERROR al actualizar crédito: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar crédito: " + e.getMessage(), e);
        }
    }

    /**
     * 💰 REGISTRAR ABONO A UN CRÉDITO
     * Actualiza automáticamente los totales y el estado
     */
    @Transactional
    public Credito registrarAbono(Long creditoId, Double montoAbono) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        if (credito.getEstado() == Credito.EstadoCredito.CERRADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito cerrado");
        }

        if (credito.getEstado() == Credito.EstadoCredito.ANULADO) {
            throw new IllegalArgumentException("No se pueden agregar abonos a un crédito anulado");
        }

        Double montoNormalizado = normalize(montoAbono);
        if (montoNormalizado <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a 0");
        }

        // Actualizar totales
        credito.setTotalAbonado(normalize(credito.getTotalAbonado() + montoNormalizado));
        credito.actualizarSaldo();

        return creditoRepo.save(credito);
    }

    /**
     * 🔄 RECALCULAR TOTALES DE UN CRÉDITO
     * Útil para sincronizar después de cambios en abonos
     */
    @Transactional
    public Credito recalcularTotales(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        // Recalcular total abonado sumando todos los abonos
        Double totalAbonos = credito.getAbonos().stream()
                .mapToDouble(abono -> abono.getTotal() != null ? abono.getTotal() : 0.0)
                .sum();

        credito.setTotalAbonado(normalize(totalAbonos));
        credito.actualizarSaldo();

        return creditoRepo.save(credito);
    }

    /**
     * ❌ ANULAR CRÉDITO
     * Se ejecuta cuando se anula la orden asociada
     */
    @Transactional
    public Credito anularCredito(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        credito.setEstado(Credito.EstadoCredito.ANULADO);
        credito.setFechaCierre(LocalDate.now());

        return creditoRepo.save(credito);
    }

    /**
     * 🏁 CERRAR CRÉDITO MANUALMENTE
     * Para casos especiales donde se quiere cerrar sin estar completamente pagado
     */
    @Transactional
    public Credito cerrarCredito(Long creditoId) {
        Credito credito = creditoRepo.findById(creditoId)
                .orElseThrow(() -> new IllegalArgumentException("Crédito no encontrado"));

        credito.setEstado(Credito.EstadoCredito.CERRADO);
        credito.setFechaCierre(LocalDate.now());
        // Ajustar saldo a 0 si se cierra manualmente
        credito.setSaldoPendiente(0.0);

        return creditoRepo.save(credito);
    }

    @Transactional
    public void eliminar(Long creditoId) {
        creditoRepo.deleteById(creditoId);
    }
}