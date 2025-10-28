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
     * Buscar factura por número de factura público
     */
    Optional<Factura> findByNumeroFactura(String numeroFactura);

    /**
     * Buscar factura por orden (solo puede haber una)
     */
    Optional<Factura> findByOrdenId(Long ordenId);

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
     * Listar facturas pagadas
     */
    List<Factura> findByEstadoAndFechaPagoNotNull(Factura.EstadoFactura estado);

    /**
     * Obtener el siguiente número de factura
     * Busca el número más alto y lo incrementa en 1
     * Usa query nativa porque HQL no soporta REGEXP
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(numero_factura AS UNSIGNED)), 0) + 1 FROM facturas WHERE numero_factura REGEXP '^[0-9]+$'", nativeQuery = true)
    Long obtenerSiguienteNumero();

    /**
     * Contar facturas por estado
     */
    long countByEstado(Factura.EstadoFactura estado);
}

