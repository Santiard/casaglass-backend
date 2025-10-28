package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    /**
     * Buscar factura por número único
     */
    Optional<Factura> findByNumero(String numero);

    /**
     * Buscar factura por orden (solo puede haber una)
     */
    Optional<Factura> findByOrdenId(Long ordenId);

    /**
     * Listar facturas por cliente
     */
    List<Factura> findByClienteId(Long clienteId);

    /**
     * Listar facturas por sede
     */
    List<Factura> findBySedeId(Long sedeId);

    /**
     * Listar facturas por trabajador
     */
    List<Factura> findByTrabajadorId(Long trabajadorId);

    /**
     * Listar facturas por estado
     */
    List<Factura> findByEstado(Factura.EstadoFactura estado);

    /**
     * Listar facturas por fecha
     */
    List<Factura> findByFecha(LocalDate fecha);

    /**
     * Listar facturas por rango de fechas
     */
    List<Factura> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /**
     * Listar facturas por cliente y estado
     */
    List<Factura> findByClienteIdAndEstado(Long clienteId, Factura.EstadoFactura estado);

    /**
     * Listar facturas por sede y estado
     */
    List<Factura> findBySedeIdAndEstado(Long sedeId, Factura.EstadoFactura estado);

    /**
     * Listar facturas por sede y fecha
     */
    List<Factura> findBySedeIdAndFechaBetween(Long sedeId, LocalDate desde, LocalDate hasta);

    /**
     * Listar facturas pagadas
     */
    List<Factura> findByEstadoAndFechaPagoNotNull(Factura.EstadoFactura estado);

    /**
     * Obtener el siguiente número de factura
     * Busca el número más alto y lo incrementa en 1
     * Usa query nativa porque HQL no soporta REGEXP
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(numero AS UNSIGNED)), 0) + 1 FROM facturas WHERE numero REGEXP '^[0-9]+$'", nativeQuery = true)
    Long obtenerSiguienteNumero();

    /**
     * Contar facturas por estado
     */
    long countByEstado(Factura.EstadoFactura estado);
}

