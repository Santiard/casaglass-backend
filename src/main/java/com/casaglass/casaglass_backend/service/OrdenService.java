package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Orden;
import com.casaglass.casaglass_backend.model.OrdenItem;
import com.casaglass.casaglass_backend.model.Sede;
import com.casaglass.casaglass_backend.model.Trabajador;
import com.casaglass.casaglass_backend.model.Cliente;
import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.Inventario;
import com.casaglass.casaglass_backend.model.Corte;
import com.casaglass.casaglass_backend.service.CorteService;
import com.casaglass.casaglass_backend.service.InventarioCorteService;
import com.casaglass.casaglass_backend.dto.OrdenTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenActualizarDTO;
import com.casaglass.casaglass_backend.dto.OrdenVentaDTO;
import com.casaglass.casaglass_backend.dto.CreditoTablaDTO;
import com.casaglass.casaglass_backend.dto.OrdenCreditoDTO;
import com.casaglass.casaglass_backend.repository.OrdenRepository;
import com.casaglass.casaglass_backend.repository.FacturaRepository;
import com.casaglass.casaglass_backend.repository.ClienteRepository;
import com.casaglass.casaglass_backend.repository.SedeRepository;
import com.casaglass.casaglass_backend.repository.TrabajadorRepository;
import com.casaglass.casaglass_backend.repository.ProductoRepository;
import com.casaglass.casaglass_backend.repository.CorteRepository;
import com.casaglass.casaglass_backend.repository.BusinessSettingsRepository;
import com.casaglass.casaglass_backend.model.BusinessSettings;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
// no need for LocalDateTime/LocalTime
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@Service
public class OrdenService {

    private final OrdenRepository repo;
    private final ClienteRepository clienteRepository;
    private final SedeRepository sedeRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final ProductoRepository productoRepository;
    private final EntityManager entityManager;
    private final InventarioService inventarioService;
    private final CreditoService creditoService;
    private final CorteService corteService;
    private final InventarioCorteService inventarioCorteService;
    private final FacturaRepository facturaRepository;
    private final CorteRepository corteRepository;
    private final BusinessSettingsRepository businessSettingsRepository;

    public OrdenService(OrdenRepository repo, 
                       ClienteRepository clienteRepository,
                       SedeRepository sedeRepository,
                       TrabajadorRepository trabajadorRepository,
                       ProductoRepository productoRepository,
                       EntityManager entityManager, 
                       InventarioService inventarioService, 
                       CreditoService creditoService,
                       CorteService corteService,
                       InventarioCorteService inventarioCorteService,
                       FacturaRepository facturaRepository,
                       CorteRepository corteRepository,
                       BusinessSettingsRepository businessSettingsRepository) { 
        this.repo = repo; 
        this.clienteRepository = clienteRepository;
        this.sedeRepository = sedeRepository;
        this.trabajadorRepository = trabajadorRepository;
        this.productoRepository = productoRepository;
        this.entityManager = entityManager;
        this.inventarioService = inventarioService;
        this.creditoService = creditoService;
        this.corteService = corteService;
        this.inventarioCorteService = inventarioCorteService;
        this.facturaRepository = facturaRepository;
        this.corteRepository = corteRepository;
        this.businessSettingsRepository = businessSettingsRepository;
    }

    @Transactional
    public Orden crear(Orden orden) {
        if (orden.getFecha() == null) orden.setFecha(LocalDate.now());

        // Validar que tenga sede asignada
        if (orden.getSede() == null || orden.getSede().getId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria para la orden");
        }

        // Usar referencia ligera para la sede
        orden.setSede(entityManager.getReference(Sede.class, orden.getSede().getId()));

        // Manejar trabajador encargado (opcional)
        if (orden.getTrabajador() != null && orden.getTrabajador().getId() != null) {
            orden.setTrabajador(entityManager.getReference(Trabajador.class, orden.getTrabajador().getId()));
        }

        // GENERACION AUTOMATICA DE NUMERO (THREAD-SAFE)
        // El número se ignora si viene del frontend - siempre se genera automáticamente
        Long numeroGenerado = generarNumeroOrden();
        orden.setNumero(numeroGenerado);

        // Calcular subtotal facturado (suma de items con IVA incluido)
        double subtotalFacturado = 0.0;
        if (orden.getItems() != null) {
            for (OrdenItem it : orden.getItems()) {
                it.setOrden(orden); // amarra relación
                Double linea = it.getPrecioUnitario() * it.getCantidad();
                it.setTotalLinea(linea);
                subtotalFacturado += linea;

                if ((it.getDescripcion() == null || it.getDescripcion().isBlank())
                        && it.getProducto() != null) {
                    it.setDescripcion(it.getProducto().getNombre());
                }
            }
        }
        subtotalFacturado = Math.round(subtotalFacturado * 100.0) / 100.0;
        
        // Calcular descuentos (si no viene, usar 0.0)
        Double descuentos = orden.getDescuentos() != null ? orden.getDescuentos() : 0.0;
        orden.setDescuentos(descuentos);
        
        // Calcular todos los valores monetarios según la especificación
        Double[] valores = calcularValoresMonetariosOrden(subtotalFacturado, descuentos, orden.isTieneRetencionFuente());
        Double subtotalSinIva = valores[0];  // Base imponible sin IVA
        Double iva = valores[1];            // IVA calculado
        Double retencionFuente = valores[2]; // Retención de fuente
        Double total = valores[3];           // Total facturado
        
        // Guardar valores en la orden
        orden.setSubtotal(subtotalSinIva);        // Base sin IVA
        orden.setIva(iva);                        // IVA
        orden.setRetencionFuente(retencionFuente); // Retención
        orden.setTotal(total);                    // Total facturado
        
        // Establecer estado activa por defecto
        orden.setEstado(Orden.EstadoOrden.ACTIVA);
        
        // Guardar la orden primero
        Orden ordenGuardada = repo.save(orden);
        
        // ⚠️ SOLO descontar inventario si es una VENTA confirmada
        // Las cotizaciones (venta=false) NO afectan el stock
        if (ordenGuardada.isVenta()) {
            System.out.println("✅ VENTA CONFIRMADA - Descontando inventario...");
            actualizarInventarioPorVenta(ordenGuardada);
        } else {
            System.out.println("📋 COTIZACIÓN - Inventario NO afectado");
        }
        
        return ordenGuardada;
    }

    /**
     * 🛒 CREAR ORDEN DE VENTA - Método optimizado para ventas reales
     * Valida todos los campos necesarios y maneja inventario automáticamente
     */
    @Transactional
    public Orden crearOrdenVenta(OrdenVentaDTO ventaDTO) {
        // 🔍 VALIDACIONES DE NEGOCIO
        validarDatosVenta(ventaDTO);
        
        // 📝 CREAR ENTIDAD ORDEN
        Orden orden = new Orden();
        orden.setFecha(ventaDTO.getFecha() != null ? ventaDTO.getFecha() : LocalDate.now());
        orden.setObra(ventaDTO.getObra());
        orden.setDescripcion(ventaDTO.getDescripcion());
        orden.setVenta(ventaDTO.isVenta());
        orden.setCredito(ventaDTO.isCredito());
        orden.setIncluidaEntrega(ventaDTO.isIncluidaEntrega());
        orden.setTieneRetencionFuente(ventaDTO.isTieneRetencionFuente());
        orden.setEstado(Orden.EstadoOrden.ACTIVA);
        
        // 🔗 ESTABLECER RELACIONES (usando referencias ligeras)
        orden.setCliente(clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + ventaDTO.getClienteId())));
        orden.setSede(sedeRepository.findById(ventaDTO.getSedeId())
            .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + ventaDTO.getSedeId())));
        
        if (ventaDTO.getTrabajadorId() != null) {
            orden.setTrabajador(trabajadorRepository.findById(ventaDTO.getTrabajadorId())
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + ventaDTO.getTrabajadorId())));
        }
        
        // 📋 PROCESAR ITEMS DE VENTA
        List<OrdenItem> items = new ArrayList<>();
        double subtotalBruto = 0.0; // Subtotal con IVA incluido
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            OrdenItem item = new OrdenItem();
            item.setOrden(orden);
            // Si se envía reutilizarCorteSolicitadoId, el item vende ese CORTE específico
            if (itemDTO.getReutilizarCorteSolicitadoId() != null) {
                Corte corteReutilizado = corteRepository.findById(itemDTO.getReutilizarCorteSolicitadoId())
                    .orElseThrow(() -> new RuntimeException("Corte no encontrado con ID: " + itemDTO.getReutilizarCorteSolicitadoId()));
                item.setProducto(corteReutilizado);
                // ✅ Usar el nombre del corte de la BD (corregido) en lugar de la descripción del frontend
                item.setDescripcion(corteReutilizado.getNombre());
            } else {
                item.setProducto(productoRepository.findById(itemDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
                item.setDescripcion(itemDTO.getDescripcion());
            }
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            
            // Calcular total de línea (con IVA incluido)
            double totalLinea = itemDTO.getCantidad() * itemDTO.getPrecioUnitario();
            item.setTotalLinea(totalLinea);
            subtotalBruto += totalLinea;
            
            items.add(item);
        }
        
        orden.setItems(items);
        subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;
        
        // Calcular descuentos (si no viene, usar 0.0)
        Double descuentos = ventaDTO.getDescuentos() != null ? ventaDTO.getDescuentos() : 0.0;
        orden.setDescuentos(descuentos);
        
        // Calcular todos los valores monetarios según la especificación
        Double[] valores = calcularValoresMonetariosOrden(subtotalBruto, descuentos, ventaDTO.isTieneRetencionFuente());
        Double subtotalSinIva = valores[0];  // Base imponible sin IVA
        Double iva = valores[1];            // IVA calculado
        Double retencionFuente = valores[2]; // Retención de fuente
        Double total = valores[3];           // Total facturado
        
        // Guardar valores en la orden
        orden.setSubtotal(subtotalSinIva);        // Base sin IVA
        orden.setIva(iva);                        // IVA
        orden.setRetencionFuente(retencionFuente); // Retención
        orden.setTotal(total);                    // Total facturado
        
        // 🔢 GENERAR NÚMERO AUTOMÁTICO
        orden.setNumero(generarNumeroOrden());
        
        // 💾 GUARDAR ORDEN
        Orden ordenGuardada = repo.save(orden);
        
        // 🔪 PROCESAR CORTES SI EXISTEN (ANTES de actualizar inventario)
        // Esto crea los cortes nuevos y actualiza inventarios de sobrantes
        if (ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
            procesarCortes(ordenGuardada, ventaDTO.getCortes());
        }

        // ✅ INCREMENTAR INVENTARIO DE CORTES REUTILIZADOS (porque se están cortando de nuevo)
        // Lógica: Si se reutiliza un corte solicitado, su inventario debe incrementarse primero
        // porque se está haciendo el corte (inventario pasa a 1), y luego se vende (vuelve a 0)
        incrementarInventarioCortesReutilizados(ordenGuardada, ventaDTO);

        // 📦 ACTUALIZAR INVENTARIO (decrementar por venta)
        // ⚠️ Excluir productos que están en cortes[] porque procesarCortes() ya maneja su inventario
        actualizarInventarioPorVenta(ordenGuardada, ventaDTO);

        return ordenGuardada;
    }
    /**
     * 🗓️ VENTAS DEL DÍA POR SEDE
     * Devuelve todas las órdenes (contado y crédito) realizadas hoy en la sede indicada
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> ventasDelDiaPorSede(Long sedeId, LocalDate fecha) {
        List<Orden> ordenes = repo.findBySedeIdAndFechaBetween(sedeId, fecha, fecha);
        return ordenes.stream()
            .filter(Orden::isVenta)
            .map(this::convertirAOrdenTablaDTO)
            .collect(Collectors.toList());
    }

    /**
     * 🗓️ VENTAS DEL DÍA EN TODAS LAS SEDES
     * Devuelve todas las órdenes (contado y crédito) realizadas hoy en todas las sedes
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> ventasDelDiaTodasLasSedes(LocalDate fecha) {
        List<Orden> ordenes = repo.findByFechaBetween(fecha, fecha);
        return ordenes.stream()
            .filter(Orden::isVenta)
            .map(this::convertirAOrdenTablaDTO)
            .collect(Collectors.toList());
    }

    /**
     * 💳 CREAR ORDEN DE VENTA CON CRÉDITO - Método unificado sin transacciones anidadas
     */
    @Transactional
    public Orden crearOrdenVentaConCredito(OrdenVentaDTO ventaDTO) {
        // ...existing code...
        
        // 🔍 VALIDACIONES DE NEGOCIO
        validarDatosVenta(ventaDTO);
        
        // 📝 CREAR ENTIDAD ORDEN
        Orden orden = new Orden();
        orden.setFecha(ventaDTO.getFecha() != null ? ventaDTO.getFecha() : LocalDate.now());
        orden.setObra(ventaDTO.getObra());
        orden.setDescripcion(ventaDTO.getDescripcion());
        orden.setVenta(ventaDTO.isVenta());
        orden.setCredito(ventaDTO.isCredito());
        orden.setIncluidaEntrega(ventaDTO.isIncluidaEntrega());
        orden.setTieneRetencionFuente(ventaDTO.isTieneRetencionFuente());
        orden.setEstado(Orden.EstadoOrden.ACTIVA);
        
        // � MONTOS POR MÉTODO DE PAGO (solo para órdenes de contado)
        orden.setMontoEfectivo(ventaDTO.getMontoEfectivo() != null ? ventaDTO.getMontoEfectivo() : 0.0);
        orden.setMontoTransferencia(ventaDTO.getMontoTransferencia() != null ? ventaDTO.getMontoTransferencia() : 0.0);
        orden.setMontoCheque(ventaDTO.getMontoCheque() != null ? ventaDTO.getMontoCheque() : 0.0);
        
        // �🔗 ESTABLECER RELACIONES (usando referencias ligeras)
        Cliente cliente = clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + ventaDTO.getClienteId()));
        
        // 💳 ACTUALIZAR CLIENTE A CRÉDITO SI ES NECESARIO
        // Si se crea una venta a crédito, el cliente debe tener credito = true
        if (cliente.getCredito() == null || !cliente.getCredito()) {
            // ...existing code...
            cliente.setCredito(true);
            clienteRepository.save(cliente);
        }
        
        orden.setCliente(cliente);
        orden.setSede(sedeRepository.findById(ventaDTO.getSedeId())
            .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + ventaDTO.getSedeId())));
        
        if (ventaDTO.getTrabajadorId() != null) {
            orden.setTrabajador(trabajadorRepository.findById(ventaDTO.getTrabajadorId())
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + ventaDTO.getTrabajadorId())));
        }
        
        // 📋 PROCESAR ITEMS DE VENTA
        List<OrdenItem> items = new ArrayList<>();
        double subtotalBruto = 0.0; // Subtotal con IVA incluido
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            OrdenItem item = new OrdenItem();
            item.setOrden(orden);
            if (itemDTO.getReutilizarCorteSolicitadoId() != null) {
                Corte corteReutilizado = corteRepository.findById(itemDTO.getReutilizarCorteSolicitadoId())
                    .orElseThrow(() -> new RuntimeException("Corte no encontrado con ID: " + itemDTO.getReutilizarCorteSolicitadoId()));
                item.setProducto(corteReutilizado);
                // ✅ Usar el nombre del corte de la BD (corregido) en lugar de la descripción del frontend
                item.setDescripcion(corteReutilizado.getNombre());
            } else {
                item.setProducto(productoRepository.findById(itemDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
                item.setDescripcion(itemDTO.getDescripcion());
            }
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            
            // Calcular total de línea (con IVA incluido)
            double totalLinea = itemDTO.getCantidad() * itemDTO.getPrecioUnitario();
            item.setTotalLinea(totalLinea);
            subtotalBruto += totalLinea;
            
            items.add(item);
        }
        
        orden.setItems(items);
        subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;
        
        // Calcular descuentos (si no viene, usar 0.0)
        Double descuentos = ventaDTO.getDescuentos() != null ? ventaDTO.getDescuentos() : 0.0;
        orden.setDescuentos(descuentos);
        
        // Calcular todos los valores monetarios según la especificación
        Double[] valores = calcularValoresMonetariosOrden(subtotalBruto, descuentos, ventaDTO.isTieneRetencionFuente());
        Double subtotalSinIva = valores[0];  // Base imponible sin IVA
        Double iva = valores[1];            // IVA calculado
        Double retencionFuente = valores[2]; // Retención de fuente
        Double total = valores[3];           // Total facturado
        
        // Guardar valores en la orden
        orden.setSubtotal(subtotalSinIva);        // Base sin IVA
        orden.setIva(iva);                        // IVA
        orden.setRetencionFuente(retencionFuente); // Retención
        orden.setTotal(total);                    // Total facturado
        
        // 🔢 GENERAR NÚMERO AUTOMÁTICO
        orden.setNumero(generarNumeroOrden());
        
        // 💾 GUARDAR ORDEN PRIMERO
        Orden ordenGuardada = repo.save(orden);
        System.out.println("✅ DEBUG: Orden guardada con ID: " + ordenGuardada.getId());
            // ...existing code...
        
        // 💳 CREAR CRÉDITO SI ES NECESARIO (en la misma transacción)
        if (ventaDTO.isCredito()) {
            // ...existing code...
            // Reutilizar retencionFuente ya calculada arriba (línea 330)
            Double retencionParaCredito = ordenGuardada.getRetencionFuente() != null 
                ? ordenGuardada.getRetencionFuente() 
                : 0.0;
            creditoService.crearCreditoParaOrden(
                ordenGuardada.getId(), 
                ventaDTO.getClienteId(), 
                ordenGuardada.getTotal(),  // Total orden
                retencionParaCredito  // ✅ Pasar también la retención
            );
        }
        
        // 🔪 PROCESAR CORTES SI EXISTEN (ANTES de actualizar inventario)
        // Esto crea los cortes nuevos y actualiza inventarios
        if (ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
            // ...existing code...
            procesarCortes(ordenGuardada, ventaDTO.getCortes());
        }
        
        // ✅ INCREMENTAR INVENTARIO DE CORTES REUTILIZADOS (porque se están cortando de nuevo)
        // Lógica: Si se reutiliza un corte solicitado, su inventario debe incrementarse primero
        // porque se está haciendo el corte (inventario pasa a 1), y luego se vende (vuelve a 0)
        incrementarInventarioCortesReutilizados(ordenGuardada, ventaDTO);
        
        // 📦 ACTUALIZAR INVENTARIO (decrementar por venta)
        // ⚠️ Excluir productos que están en cortes[] porque procesarCortes() ya maneja su inventario
        actualizarInventarioPorVenta(ordenGuardada, ventaDTO);
        
        return ordenGuardada;
    }

    /**
     * 🔄 ACTUALIZAR ORDEN DE VENTA - Método optimizado para editar ventas
     * Maneja inventario automáticamente y procesa cortes
     */
    @Transactional
    public Orden actualizarOrdenVenta(Long ordenId, OrdenVentaDTO ventaDTO) {
        // ...existing code...
        
        // 🔍 VALIDACIONES DE NEGOCIO
        validarDatosVenta(ventaDTO);
        
        // 📝 BUSCAR ORDEN EXISTENTE
        Orden ordenExistente = repo.findById(ordenId)
            .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + ordenId));
        
        // 🔄 RESTAURAR INVENTARIO DE LA ORDEN ANTERIOR
        System.out.println("🔄 Restaurando inventario de la orden anterior...");
            // ...existing code...
        restaurarInventarioPorAnulacion(ordenExistente);
        
        // 📝 ACTUALIZAR CAMPOS BÁSICOS
        ordenExistente.setFecha(ventaDTO.getFecha() != null ? ventaDTO.getFecha() : LocalDate.now());
        ordenExistente.setObra(ventaDTO.getObra());
        ordenExistente.setDescripcion(ventaDTO.getDescripcion());
        ordenExistente.setVenta(ventaDTO.isVenta());
        ordenExistente.setCredito(ventaDTO.isCredito());
        ordenExistente.setIncluidaEntrega(ventaDTO.isIncluidaEntrega());
        
        // 🔗 ACTUALIZAR RELACIONES
        Cliente cliente = clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + ventaDTO.getClienteId()));
        
        // 💳 ACTUALIZAR CLIENTE A CRÉDITO SI ES NECESARIO
        // Si se actualiza a venta a crédito, el cliente debe tener credito = true
        if (ventaDTO.isCredito() && (cliente.getCredito() == null || !cliente.getCredito())) {
            // ...existing code...
            cliente.setCredito(true);
            clienteRepository.save(cliente);
        }
        
        ordenExistente.setCliente(cliente);
        ordenExistente.setSede(sedeRepository.findById(ventaDTO.getSedeId())
            .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + ventaDTO.getSedeId())));
        
        if (ventaDTO.getTrabajadorId() != null) {
            ordenExistente.setTrabajador(trabajadorRepository.findById(ventaDTO.getTrabajadorId())
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + ventaDTO.getTrabajadorId())));
        }
        
        // 📋 ACTUALIZAR ITEMS DE VENTA (manejo correcto de cascade)
        // Limpiar items existentes para evitar problemas de cascade
        ordenExistente.getItems().clear();
        
        double subtotalBruto = 0.0; // Subtotal con IVA incluido
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            OrdenItem item = new OrdenItem();
            item.setOrden(ordenExistente);
            item.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
            item.setDescripcion(itemDTO.getDescripcion());
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            
            // Calcular total de línea (con IVA incluido)
            double totalLinea = itemDTO.getCantidad() * itemDTO.getPrecioUnitario();
            item.setTotalLinea(totalLinea);
            subtotalBruto += totalLinea;
            
            // Agregar item a la lista existente
            ordenExistente.getItems().add(item);
        }
        
        subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;
        
        // Calcular descuentos (si no viene, usar el valor actual o 0.0)
        Double descuentos = ventaDTO.getDescuentos() != null ? ventaDTO.getDescuentos() : (ordenExistente.getDescuentos() != null ? ordenExistente.getDescuentos() : 0.0);
        ordenExistente.setDescuentos(descuentos);
        ordenExistente.setTieneRetencionFuente(ventaDTO.isTieneRetencionFuente());
        
        // Calcular todos los valores monetarios según la especificación
        Double[] valores = calcularValoresMonetariosOrden(subtotalBruto, descuentos, ventaDTO.isTieneRetencionFuente());
        Double subtotalSinIva = valores[0];  // Base imponible sin IVA
        Double iva = valores[1];            // IVA calculado
        Double retencionFuente = valores[2]; // Retención de fuente
        Double total = valores[3];           // Total facturado
        
        // Guardar valores en la orden
        ordenExistente.setSubtotal(subtotalSinIva);        // Base sin IVA
        ordenExistente.setIva(iva);                        // IVA
        ordenExistente.setRetencionFuente(retencionFuente); // Retención
        ordenExistente.setTotal(total);                    // Total facturado
        
        // 💾 GUARDAR ORDEN ACTUALIZADA
        Orden ordenActualizada = repo.save(ordenExistente);
        
        // 📦 ACTUALIZAR INVENTARIO CON LOS NUEVOS ITEMS
        actualizarInventarioPorVenta(ordenActualizada);
        
        // 🔪 PROCESAR CORTES SI EXISTEN
        if (ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
            // ...existing code...
            procesarCortes(ordenActualizada, ventaDTO.getCortes());
        }
        
        System.out.println("✅ Orden actualizada exitosamente: " + ordenActualizada.getId());
            // ...existing code...
        return ordenActualizada;
    }

    /**
     * 💳 ACTUALIZAR ORDEN DE VENTA CON CRÉDITO - Método para editar ventas a crédito
     */
    @Transactional
    public Orden actualizarOrdenVentaConCredito(Long ordenId, OrdenVentaDTO ventaDTO) {
        // ...existing code...
            // ...existing code...
        
        // 🔍 VALIDACIONES DE NEGOCIO
        validarDatosVenta(ventaDTO);
        
        // 📝 BUSCAR ORDEN EXISTENTE
        Orden ordenExistente = repo.findById(ordenId)
            .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + ordenId));
        
        // 🔄 RESTAURAR INVENTARIO DE LA ORDEN ANTERIOR
        System.out.println("🔄 Restaurando inventario de la orden anterior...");
        restaurarInventarioPorAnulacion(ordenExistente);
        
        // 📝 ACTUALIZAR CAMPOS BÁSICOS
        ordenExistente.setFecha(ventaDTO.getFecha() != null ? ventaDTO.getFecha() : LocalDate.now());
        ordenExistente.setObra(ventaDTO.getObra());
        ordenExistente.setDescripcion(ventaDTO.getDescripcion());
        ordenExistente.setVenta(ventaDTO.isVenta());
        ordenExistente.setCredito(ventaDTO.isCredito());
        ordenExistente.setIncluidaEntrega(ventaDTO.isIncluidaEntrega());
        
        // 🔗 ACTUALIZAR RELACIONES
        Cliente cliente = clienteRepository.findById(ventaDTO.getClienteId())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + ventaDTO.getClienteId()));
        
        // 💳 ACTUALIZAR CLIENTE A CRÉDITO SI ES NECESARIO
        // Si se actualiza a venta a crédito, el cliente debe tener credito = true
        if (ventaDTO.isCredito() && (cliente.getCredito() == null || !cliente.getCredito())) {
            // ...existing code...
            cliente.setCredito(true);
            clienteRepository.save(cliente);
        }
        
        ordenExistente.setCliente(cliente);
        ordenExistente.setSede(sedeRepository.findById(ventaDTO.getSedeId())
            .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + ventaDTO.getSedeId())));
        
        if (ventaDTO.getTrabajadorId() != null) {
            ordenExistente.setTrabajador(trabajadorRepository.findById(ventaDTO.getTrabajadorId())
                .orElseThrow(() -> new RuntimeException("Trabajador no encontrado con ID: " + ventaDTO.getTrabajadorId())));
        }
        
        // 📋 ACTUALIZAR ITEMS DE VENTA (manejo correcto de cascade)
        // Limpiar items existentes para evitar problemas de cascade
        ordenExistente.getItems().clear();
        
        double subtotalBruto = 0.0; // Subtotal con IVA incluido
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            OrdenItem item = new OrdenItem();
            item.setOrden(ordenExistente);
            item.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
            item.setDescripcion(itemDTO.getDescripcion());
            item.setCantidad(itemDTO.getCantidad());
            item.setPrecioUnitario(itemDTO.getPrecioUnitario());
            
            // Calcular total de línea (con IVA incluido)
            double totalLinea = itemDTO.getCantidad() * itemDTO.getPrecioUnitario();
            item.setTotalLinea(totalLinea);
            subtotalBruto += totalLinea;
            
            // Agregar item a la lista existente
            ordenExistente.getItems().add(item);
        }
        
        subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;
        
        // Calcular descuentos (si no viene, usar el valor actual o 0.0)
        Double descuentos = ventaDTO.getDescuentos() != null ? ventaDTO.getDescuentos() : (ordenExistente.getDescuentos() != null ? ordenExistente.getDescuentos() : 0.0);
        ordenExistente.setDescuentos(descuentos);
        ordenExistente.setTieneRetencionFuente(ventaDTO.isTieneRetencionFuente());
        
        // Calcular todos los valores monetarios según la especificación
        Double[] valores = calcularValoresMonetariosOrden(subtotalBruto, descuentos, ventaDTO.isTieneRetencionFuente());
        Double subtotalSinIva = valores[0];  // Base imponible sin IVA
        Double iva = valores[1];            // IVA calculado
        Double retencionFuente = valores[2]; // Retención de fuente
        Double total = valores[3];           // Total facturado
        
        // Guardar valores en la orden
        ordenExistente.setSubtotal(subtotalSinIva);        // Base sin IVA
        ordenExistente.setIva(iva);                        // IVA
        ordenExistente.setRetencionFuente(retencionFuente); // Retención
        ordenExistente.setTotal(total);                    // Total facturado
        
        // 💾 GUARDAR ORDEN ACTUALIZADA PRIMERO
        Orden ordenActualizada = repo.save(ordenExistente);
        System.out.println("✅ DEBUG: Orden actualizada con ID: " + ordenActualizada.getId());
            // ...existing code...
        
        // 💳 ACTUALIZAR CRÉDITO SI ES NECESARIO
        if (ventaDTO.isCredito()) {
            // ...existing code...
            
            // Si ya existe crédito, actualizarlo
            if (ordenActualizada.getCreditoDetalle() != null) {
                // Reutilizar retencionFuente ya calculada arriba (línea 568)
                creditoService.actualizarCreditoParaOrden(
                    ordenActualizada.getCreditoDetalle().getId(),
                    ordenActualizada.getTotal(),  // Total orden
                    retencionFuente  // ✅ Pasar también la retención (ya calculada)
                );
            } else {
                // Si no existe crédito, crearlo
                creditoService.crearCreditoParaOrden(
                    ordenActualizada.getId(), 
                    ventaDTO.getClienteId(), 
                    ordenActualizada.getTotal(),  // Total orden
                    retencionFuente  // ✅ Pasar también la retención (ya calculada)
                );
            }
        } else {
            // Si se cambió de crédito a contado, anular el crédito existente
            if (ordenActualizada.getCreditoDetalle() != null) {
                System.out.println("🔄 DEBUG: Anulando crédito existente...");
                            // ...existing code...
                creditoService.anularCredito(ordenActualizada.getCreditoDetalle().getId());
            }
        }
        
        // 📦 ACTUALIZAR INVENTARIO CON LOS NUEVOS ITEMS
        actualizarInventarioPorVenta(ordenActualizada);
        
        // 🔪 PROCESAR CORTES SI EXISTEN
        if (ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
            // ...existing code...
            procesarCortes(ordenActualizada, ventaDTO.getCortes());
        }
        
        System.out.println("✅ Orden con crédito actualizada exitosamente: " + ordenActualizada.getId());
            // ...existing code...
        return ordenActualizada;
    }

    /**
     * 💰 OBTENER TASA DE IVA DESDE CONFIGURACIÓN
     * Obtiene el IVA rate desde BusinessSettings, con fallback a 19% si no existe
     */
    private Double obtenerIvaRate() {
        try {
            // Buscar la primera configuración (debería haber solo una)
            List<BusinessSettings> settings = businessSettingsRepository.findAll();
            if (!settings.isEmpty() && settings.get(0).getIvaRate() != null) {
                Double ivaRate = settings.get(0).getIvaRate();
                return ivaRate;
            }
        } catch (Exception e) {
            // ...existing code...
        }
        // Fallback a 19% por defecto
        return 19.0;
    }

    /**
     * 💰 CALCULAR IVA DESDE SUBTOTAL (QUE YA INCLUYE IVA)
     * Extrae el IVA del subtotal que ya lo incluye
     * Fórmula: IVA = Subtotal * (tasa_iva / (100 + tasa_iva))
     * Ejemplo con 19%: IVA = Subtotal * 0.19 / 1.19
     * 
     * @param subtotal Subtotal que ya incluye IVA
     * @return Valor del IVA extraído del subtotal
     */
    public Double calcularIvaDesdeSubtotal(Double subtotal) {
        if (subtotal == null || subtotal <= 0) {
            return 0.0;
        }
        Double ivaRate = obtenerIvaRate();
        // Fórmula: IVA = Subtotal * (tasa / (100 + tasa))
        // Ejemplo: Si subtotal = 119 y tasa = 19%, entonces IVA = 119 * 0.19 / 1.19 = 19
        Double iva = subtotal * (ivaRate / (100.0 + ivaRate));
        // Redondear a 2 decimales
        return Math.round(iva * 100.0) / 100.0;
    }

    /**
     * 💰 CALCULAR VALORES MONETARIOS DE LA ORDEN
     * Calcula subtotal (base sin IVA), IVA, retención de fuente y total
     * según la especificación del frontend
     * 
     * @param subtotalFacturado Suma de (precioUnitario × cantidad) de todos los items (CON IVA incluido)
     * @param descuentos Monto de descuentos aplicados
     * @param tieneRetencionFuente Boolean que indica si aplica retención de fuente
     * @return Array con [subtotalSinIva, iva, retencionFuente, total]
     */
    private Double[] calcularValoresMonetariosOrden(Double subtotalFacturado, Double descuentos, boolean tieneRetencionFuente) {
        if (subtotalFacturado == null || subtotalFacturado <= 0) {
            return new Double[]{0.0, 0.0, 0.0, 0.0};
        }
        
        // Asegurar que descuentos no sea null
        if (descuentos == null) {
            descuentos = 0.0;
        }
        
        // Paso 1: Calcular base imponible (total facturado - descuentos)
        Double baseConIva = subtotalFacturado - descuentos;
        if (baseConIva <= 0) {
            return new Double[]{0.0, 0.0, 0.0, 0.0};
        }
        
        // Paso 2: Calcular subtotal sin IVA (base imponible / 1.19)
        Double ivaRate = obtenerIvaRate();
        Double subtotalSinIva = baseConIva / (1.0 + (ivaRate / 100.0));
        subtotalSinIva = Math.round(subtotalSinIva * 100.0) / 100.0;
        
        // Paso 3: Calcular IVA
        Double iva = baseConIva - subtotalSinIva;
        iva = Math.round(iva * 100.0) / 100.0;
        
        // Paso 4: Calcular retención de fuente (sobre subtotal sin IVA)
        Double retencionFuente = 0.0;
        if (tieneRetencionFuente) {
            BusinessSettings config = obtenerConfiguracionRetencion();
            Double reteRate = config.getReteRate() != null ? config.getReteRate() : 2.5;
            Long reteThreshold = config.getReteThreshold() != null ? config.getReteThreshold() : 1_000_000L;
            
            // Verificar si supera el umbral
            if (subtotalSinIva >= reteThreshold) {
                retencionFuente = subtotalSinIva * (reteRate / 100.0);
                retencionFuente = Math.round(retencionFuente * 100.0) / 100.0;
            }
        }
        
        // Paso 5: Calcular total (total facturado - descuentos, sin restar retención)
        Double total = subtotalFacturado - descuentos;
        total = Math.round(total * 100.0) / 100.0;
        
        return new Double[]{subtotalSinIva, iva, retencionFuente, total};
    }

    /**
     * 💰 OBTENER CONFIGURACIÓN DE RETENCIÓN DESDE BUSINESS SETTINGS
     * Obtiene la tasa y umbral de retención desde BusinessSettings
     */
    private BusinessSettings obtenerConfiguracionRetencion() {
        try {
            List<BusinessSettings> settings = businessSettingsRepository.findAll();
            if (!settings.isEmpty()) {
                return settings.get(0);
            }
        } catch (Exception e) {
            // ...existing code...
        }
        // Fallback a valores por defecto
        BusinessSettings defaultSettings = new BusinessSettings();
        defaultSettings.setReteRate(2.5);
        defaultSettings.setReteThreshold(1_000_000L);
        return defaultSettings;
    }

    /**
     * 💰 CALCULAR RETENCIÓN EN LA FUENTE
     * Calcula la retención en la fuente si aplica según la configuración
     * 
     * @param subtotal Subtotal de la orden (con IVA incluido)
     * @param descuentos Descuentos aplicados
     * @param tieneRetencionFuente Si la orden tiene retención de fuente habilitada
     * @return Valor de la retención (0.0 si no aplica)
     */
    private Double calcularRetencionFuente(Double subtotal, Double descuentos, boolean tieneRetencionFuente) {
        if (!tieneRetencionFuente || subtotal == null || subtotal <= 0) {
            return 0.0;
        }
        
        // Calcular base imponible (subtotal - descuentos)
        Double baseImponible = subtotal - (descuentos != null ? descuentos : 0.0);
        
        if (baseImponible <= 0) {
            return 0.0;
        }
        
        // Obtener configuración de retención
        BusinessSettings config = obtenerConfiguracionRetencion();
        Double reteRate = config.getReteRate() != null ? config.getReteRate() : 2.5;
        Long reteThreshold = config.getReteThreshold() != null ? config.getReteThreshold() : 1_000_000L;
        
        // Verificar si supera el umbral
        if (baseImponible < reteThreshold) {
            return 0.0;
        }
        
        // Calcular retención: baseImponible * (reteRate / 100)
        Double retencion = baseImponible * (reteRate / 100.0);
        
        // Redondear a 2 decimales
        return Math.round(retencion * 100.0) / 100.0;
    }

    /**
     * 🔍 VALIDACIONES PARA ORDENES DE VENTA
     */
    private void validarDatosVenta(OrdenVentaDTO ventaDTO) {
        // Cliente obligatorio
        if (ventaDTO.getClienteId() == null) {
            throw new IllegalArgumentException("El cliente es obligatorio para realizar una venta");
        }
        
        // Sede obligatoria
        if (ventaDTO.getSedeId() == null) {
            throw new IllegalArgumentException("La sede es obligatoria para realizar una venta");
        }
        
        // Items obligatorios
        if (ventaDTO.getItems() == null || ventaDTO.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un producto en la venta");
        }
        
        // Validar cada item
        for (int i = 0; i < ventaDTO.getItems().size(); i++) {
            OrdenVentaDTO.OrdenItemVentaDTO item = ventaDTO.getItems().get(i);
            String posicion = "Item " + (i + 1);
            
            if (item.getProductoId() == null) {
                throw new IllegalArgumentException(posicion + ": El producto es obligatorio");
            }
            
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException(posicion + ": La cantidad debe ser mayor a 0");
            }
            
            if (item.getPrecioUnitario() == null || item.getPrecioUnitario() <= 0) {
                throw new IllegalArgumentException(posicion + ": El precio unitario debe ser mayor a 0");
            }
        }
    }

    /**
     * Genera el siguiente número de orden de forma thread-safe
     * Maneja automáticamente la concurrencia entre múltiples usuarios
     */
    private Long generarNumeroOrden() {
        int maxIntentos = 5;
        int intento = 0;
        
        while (intento < maxIntentos) {
            try {
                // Obtener el siguiente número disponible
                Long siguienteNumero = repo.obtenerSiguienteNumero();
                
                // Verificar que no exista (por si hubo concurrencia)
                if (!repo.findByNumero(siguienteNumero).isPresent()) {
                    return siguienteNumero;
                }
                
                // Si existe, incrementar y volver a intentar
                intento++;
                Thread.sleep(10); // Pausa muy breve para evitar colisiones
                
            } catch (Exception e) {
                intento++;
                if (intento >= maxIntentos) {
                    throw new RuntimeException("Error generando número de orden después de " + maxIntentos + " intentos", e);
                }
            }
        }
        
        throw new RuntimeException("No se pudo generar un número de orden único después de " + maxIntentos + " intentos");
    }

    @Transactional(readOnly = true)
    public Optional<Orden> obtenerPorId(Long id) { return repo.findById(id); }

    @Transactional(readOnly = true)
    public Optional<Orden> obtenerPorNumero(Long numero) { return repo.findByNumero(numero); }

    @Transactional(readOnly = true)
    public List<Orden> listar() {
        // Usar findAll() simple ya que las relaciones son EAGER
        return repo.findAll();
    }

    /**
     * 🚀 LISTADO DE ÓRDENES CON FILTROS COMPLETOS
     * Similar a listarParaTablaConFiltros pero retorna entidades Orden completas
     */
    @Transactional(readOnly = true)
    public Object listarConFiltros(
            Long clienteId,
            Long sedeId,
            Orden.EstadoOrden estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Boolean venta,
            Boolean credito,
            Boolean facturada,
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar fechas
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        
        // Validar y normalizar ordenamiento
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "fecha";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "DESC";
        }
        sortOrder = sortOrder.toUpperCase();
        if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
            sortOrder = "DESC";
        }
        
        // Buscar órdenes con filtros
        List<Orden> ordenes = repo.buscarConFiltros(
            clienteId, sedeId, estado, fechaDesde, fechaHasta, venta, credito, facturada
        );
        
        // Aplicar ordenamiento
        ordenes = aplicarOrdenamiento(ordenes, sortBy, sortOrder);
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = ordenes.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, ordenes.size());
            
            if (fromIndex >= ordenes.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Orden> ordenesPagina = ordenes.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(ordenesPagina, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return ordenes;
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorCliente(Long clienteId) { return repo.findByClienteId(clienteId); }

    /**
     * Lista órdenes de un cliente con filtros opcionales de fecha
     * Optimizado para mejorar rendimiento al filtrar en la base de datos
     */
    @Transactional(readOnly = true)
    public List<Orden> listarPorClienteConFiltros(Long clienteId, LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null) {
            return repo.findByClienteIdAndFechaBetween(clienteId, fechaDesde, fechaHasta);
        }
        return repo.findByClienteId(clienteId);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorVenta(boolean venta) { return repo.findByVenta(venta); }

    @Transactional(readOnly = true)
    public List<Orden> listarPorCredito(boolean credito) { return repo.findByCredito(credito); }

    /** Órdenes de un día (00:00:00 a 23:59:59.999999999) */
    @Transactional(readOnly = true)
    public List<Orden> listarPorFecha(LocalDate fecha) {
        return repo.findByFechaBetween(fecha, fecha);
    }

    /** Órdenes en rango [desde, hasta] (ambos inclusive por día) */
    @Transactional(readOnly = true)
    public List<Orden> listarPorRangoFechas(LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByFechaBetween(desdeDia, hastaDia);
    }

    // Métodos nuevos para manejar sede
    @Transactional(readOnly = true)
    public List<Orden> listarPorSede(Long sedeId) {
        return repo.findBySedeId(sedeId);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorClienteYSede(Long clienteId, Long sedeId) {
        return repo.findByClienteIdAndSedeId(clienteId, sedeId);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYVenta(Long sedeId, boolean venta) {
        return repo.findBySedeIdAndVenta(sedeId, venta);
    }

    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYCredito(Long sedeId, boolean credito) {
        return repo.findBySedeIdAndCredito(sedeId, credito);
    }

    /** Órdenes de una sede en un día específico */
    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYFecha(Long sedeId, LocalDate fecha) {
        return repo.findBySedeIdAndFechaBetween(sedeId, fecha, fecha);
    }

    /** Órdenes de una sede en rango [desde, hasta] (ambos inclusive por día) */
    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYRangoFechas(Long sedeId, LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findBySedeIdAndFechaBetween(sedeId, desdeDia, hastaDia);
    }

    // 🆕 MÉTODOS PARA FILTRAR POR TRABAJADOR
    /** Todas las órdenes de un trabajador */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajador(Long trabajadorId) {
        return repo.findByTrabajadorId(trabajadorId);
    }

    /** Órdenes de un trabajador filtradas por venta/cotización */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajadorYVenta(Long trabajadorId, boolean venta) {
        return repo.findByTrabajadorIdAndVenta(trabajadorId, venta);
    }

    /** Órdenes de un trabajador en un día específico */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajadorYFecha(Long trabajadorId, LocalDate fecha) {
        return repo.findByTrabajadorIdAndFechaBetween(trabajadorId, fecha, fecha);
    }

    /** Órdenes de un trabajador en rango [desde, hasta] */
    @Transactional(readOnly = true)
    public List<Orden> listarPorTrabajadorYRangoFechas(Long trabajadorId, LocalDate desdeDia, LocalDate hastaDia) {
        return repo.findByTrabajadorIdAndFechaBetween(trabajadorId, desdeDia, hastaDia);
    }

    /** Órdenes de una sede y trabajador específicos */
    @Transactional(readOnly = true)
    public List<Orden> listarPorSedeYTrabajador(Long sedeId, Long trabajadorId) {
        return repo.findBySedeIdAndTrabajadorId(sedeId, trabajadorId);
    }

    /**
     * Obtiene el próximo número de orden que se asignará
     * Útil para mostrar en el frontend como referencia (número provisional)
     */
    @Transactional(readOnly = true)
    public Long obtenerProximoNumero() {
        return repo.obtenerSiguienteNumero();
    }

    // 🎯 ================================
    // 🎯 MÉTODOS OPTIMIZADOS PARA TABLA
    // 🎯 ================================

    /**
     * 🚀 LISTADO OPTIMIZADO PARA TABLA DE ÓRDENES
     * Retorna solo los campos necesarios para mejorar rendimiento
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarParaTabla() {
        return repo.findAll().stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO PAGINADO PARA TABLA DE ÓRDENES
     * Retorna solo los campos necesarios con paginación para mejorar rendimiento
     * 
     * @param page Número de página (1-indexed, default: 1)
     * @param size Tamaño de página (default: 20, máximo: 100)
     * @return Respuesta paginada con órdenes
     */
    @Transactional(readOnly = true)
    public com.casaglass.casaglass_backend.dto.PageResponse<OrdenTablaDTO> listarParaTablaPaginado(int page, int size) {
        // Validar y ajustar parámetros
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Límite máximo
        
        // Obtener todas las órdenes (por ahora, luego optimizar con query específica)
        List<Orden> todasLasOrdenes = repo.findAll();
        long totalElements = todasLasOrdenes.size();
        
        // Calcular índices para paginación (0-indexed para sublist)
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, todasLasOrdenes.size());
        
        // Obtener solo la página solicitada
        List<Orden> ordenesPagina = todasLasOrdenes.subList(fromIndex, toIndex);
        
        // Convertir a DTOs
        List<OrdenTablaDTO> contenido = ordenesPagina.stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
        
        return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
    }

    /**
     * 🚀 LISTADO OPTIMIZADO CON FILTROS COMPLETOS PARA TABLA
     * Acepta múltiples filtros opcionales y retorna lista o respuesta paginada
     */
    @Transactional(readOnly = true)
    public Object listarParaTablaConFiltros(
            Long clienteId,
            Long sedeId,
            Orden.EstadoOrden estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Boolean venta,
            Boolean credito,
            Boolean facturada,
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder) {
        
        // Validar fechas
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        
        // Validar y normalizar ordenamiento
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "fecha";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "DESC";
        }
        sortOrder = sortOrder.toUpperCase();
        if (!sortOrder.equals("ASC") && !sortOrder.equals("DESC")) {
            sortOrder = "DESC";
        }
        
        // Buscar órdenes con filtros
        List<Orden> ordenes = repo.buscarConFiltros(
            clienteId, sedeId, estado, fechaDesde, fechaHasta, venta, credito, facturada
        );
        
        // Aplicar ordenamiento
        ordenes = aplicarOrdenamiento(ordenes, sortBy, sortOrder);
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 20;
            if (size > 100) size = 100; // Límite máximo
            
            long totalElements = ordenes.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, ordenes.size());
            
            if (fromIndex >= ordenes.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<Orden> ordenesPagina = ordenes.subList(fromIndex, toIndex);
            
            // Convertir a DTOs
            List<OrdenTablaDTO> contenido = ordenesPagina.stream()
                    .map(this::convertirAOrdenTablaDTO)
                    .collect(Collectors.toList());
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return ordenes.stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Aplica ordenamiento a la lista de órdenes según sortBy y sortOrder
     */
    private List<Orden> aplicarOrdenamiento(List<Orden> ordenes, String sortBy, String sortOrder) {
        boolean ascendente = "ASC".equals(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "fecha":
                ordenes.sort((a, b) -> {
                    int cmp = a.getFecha().compareTo(b.getFecha());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "numero":
                ordenes.sort((a, b) -> {
                    int cmp = Long.compare(a.getNumero(), b.getNumero());
                    return ascendente ? cmp : -cmp;
                });
                break;
            case "total":
                ordenes.sort((a, b) -> {
                    int cmp = Double.compare(a.getTotal() != null ? a.getTotal() : 0.0,
                                            b.getTotal() != null ? b.getTotal() : 0.0);
                    return ascendente ? cmp : -cmp;
                });
                break;
            default:
                // Por defecto ordenar por fecha DESC
                ordenes.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        }
        
        return ordenes;
    }

    /**
     * 🚀 LISTADO OPTIMIZADO POR SEDE PARA TABLA
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarPorSedeParaTabla(Long sedeId) {
        return repo.findBySedeId(sedeId).stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO PAGINADO POR SEDE PARA TABLA
     * 
     * @param sedeId ID de la sede
     * @param page Número de página (1-indexed, default: 1)
     * @param size Tamaño de página (default: 20, máximo: 100)
     * @return Respuesta paginada con órdenes de la sede
     */
    @Transactional(readOnly = true)
    public com.casaglass.casaglass_backend.dto.PageResponse<OrdenTablaDTO> listarPorSedeParaTablaPaginado(Long sedeId, int page, int size) {
        // Validar y ajustar parámetros
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Límite máximo
        
        // Obtener todas las órdenes de la sede
        List<Orden> todasLasOrdenes = repo.findBySedeId(sedeId);
        long totalElements = todasLasOrdenes.size();
        
        // Calcular índices para paginación (0-indexed para sublist)
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, todasLasOrdenes.size());
        
        // Obtener solo la página solicitada
        List<Orden> ordenesPagina = todasLasOrdenes.subList(fromIndex, toIndex);
        
        // Convertir a DTOs
        List<OrdenTablaDTO> contenido = ordenesPagina.stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
        
        return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
    }

    /**
     * 🚀 LISTADO OPTIMIZADO POR TRABAJADOR PARA TABLA
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarPorTrabajadorParaTabla(Long trabajadorId) {
        return repo.findByTrabajadorId(trabajadorId).stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 🚀 LISTADO OPTIMIZADO POR CLIENTE PARA TABLA
     */
    @Transactional(readOnly = true)
    public List<OrdenTablaDTO> listarPorClienteParaTabla(Long clienteId) {
        return repo.findByClienteId(clienteId).stream()
                .map(this::convertirAOrdenTablaDTO)
                .collect(Collectors.toList());
    }

    /**
     * 💳 LISTADO DE ÓRDENES A CRÉDITO POR CLIENTE
     * Retorna solo órdenes a crédito con información del crédito
     * Usado en GET /api/ordenes/credito?clienteId=X
     */
    @Transactional(readOnly = true)
    public List<OrdenCreditoDTO> listarOrdenesCreditoPorCliente(Long clienteId) {
        return repo.findByClienteId(clienteId).stream()
                .filter(Orden::isCredito)  // Solo órdenes a crédito
                .map(this::convertirAOrdenCreditoDTO)
                .collect(Collectors.toList());
    }

    /**
     * 💳 LISTADO DE ÓRDENES A CRÉDITO POR CLIENTE CON FILTROS
     * Retorna solo órdenes a crédito con filtros opcionales de fecha, estado y paginación
     */
    @Transactional(readOnly = true)
    public Object listarOrdenesCreditoPorClienteConFiltros(
            Long clienteId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            com.casaglass.casaglass_backend.model.Credito.EstadoCredito estadoCredito,
            Integer page,
            Integer size) {
        
        // Validar fechas
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        
        // Obtener órdenes del cliente
        List<Orden> ordenes = repo.findByClienteId(clienteId);
        
        // Filtrar solo órdenes a crédito
        ordenes = ordenes.stream()
                .filter(Orden::isCredito)
                .collect(Collectors.toList());
        
        // Aplicar filtro de fecha si se proporciona
        if (fechaDesde != null || fechaHasta != null) {
            ordenes = ordenes.stream()
                    .filter(o -> {
                        if (fechaDesde != null && o.getFecha().isBefore(fechaDesde)) return false;
                        if (fechaHasta != null && o.getFecha().isAfter(fechaHasta)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        // Aplicar filtro de estado del crédito si se proporciona
        if (estadoCredito != null) {
            ordenes = ordenes.stream()
                    .filter(o -> o.getCreditoDetalle() != null && 
                               o.getCreditoDetalle().getEstado() == estadoCredito)
                    .collect(Collectors.toList());
        }
        
        // Ordenar por fecha DESC (más recientes primero)
        ordenes.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        
        // Convertir a DTOs
        List<OrdenCreditoDTO> dtos = ordenes.stream()
                .map(this::convertirAOrdenCreditoDTO)
                .collect(Collectors.toList());
        
        // Si se solicita paginación
        if (page != null && size != null) {
            // Validar y ajustar parámetros
            if (page < 1) page = 1;
            if (size < 1) size = 50;
            if (size > 200) size = 200; // Límite máximo para créditos
            
            long totalElements = dtos.size();
            
            // Calcular índices para paginación
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, dtos.size());
            
            if (fromIndex >= dtos.size()) {
                // Página fuera de rango, retornar lista vacía
                return com.casaglass.casaglass_backend.dto.PageResponse.of(
                    new ArrayList<>(), totalElements, page, size
                );
            }
            
            // Obtener solo la página solicitada
            List<OrdenCreditoDTO> contenido = dtos.subList(fromIndex, toIndex);
            
            return com.casaglass.casaglass_backend.dto.PageResponse.of(contenido, totalElements, page, size);
        }
        
        // Sin paginación: retornar lista completa
        return dtos;
    }

    /**
     * 🔄 CONVERSOR: Orden Entity → OrdenCreditoDTO
     * Convierte una orden a crédito al DTO específico
     */
    private OrdenCreditoDTO convertirAOrdenCreditoDTO(Orden orden) {
        OrdenCreditoDTO dto = new OrdenCreditoDTO();
        
        dto.setId(orden.getId());
        dto.setNumero(orden.getNumero());
        dto.setFecha(orden.getFecha());
        dto.setTotal(orden.getTotal());
        dto.setCredito(orden.isCredito());
        
        // Número de factura (si existe)
        String numeroFactura = "-";
        if (orden.getFactura() != null) {
            numeroFactura = orden.getFactura().getNumeroFactura();
        } else if (orden.getId() != null) {
            Optional<com.casaglass.casaglass_backend.model.Factura> facturaOpt = facturaRepository.findByOrdenId(orden.getId());
            if (facturaOpt.isPresent()) {
                numeroFactura = facturaOpt.get().getNumeroFactura();
            }
        }
        dto.setNumeroFactura(numeroFactura);
        
        // Información del crédito
        if (orden.getCreditoDetalle() != null) {
            OrdenCreditoDTO.CreditoDetalleDTO creditoDTO = new OrdenCreditoDTO.CreditoDetalleDTO();
            creditoDTO.setCreditoId(orden.getCreditoDetalle().getId());
            creditoDTO.setSaldoPendiente(orden.getCreditoDetalle().getSaldoPendiente());
            dto.setCreditoDetalle(creditoDTO);
        }
        
        return dto;
    }

    /**
     * 🔄 CONVERSOR: Orden Entity → OrdenTablaDTO optimizado
     * Extrae solo los campos necesarios para la tabla
     */
    private OrdenTablaDTO convertirAOrdenTablaDTO(Orden orden) {
        OrdenTablaDTO dto = new OrdenTablaDTO();
        
        // 📝 CAMPOS PRINCIPALES DE LA ORDEN
        dto.setId(orden.getId());
        dto.setNumero(orden.getNumero());
        dto.setFecha(orden.getFecha());
        dto.setObra(orden.getObra());
        dto.setDescripcion(orden.getDescripcion());
        dto.setVenta(orden.isVenta());
        dto.setCredito(orden.isCredito());
        dto.setTieneRetencionFuente(orden.isTieneRetencionFuente());
        dto.setRetencionFuente(orden.getRetencionFuente() != null ? orden.getRetencionFuente() : 0.0);
        dto.setEstado(orden.getEstado());
        dto.setSubtotal(orden.getSubtotal());
        dto.setIva(orden.getIva() != null ? orden.getIva() : 0.0);
        dto.setDescuentos(orden.getDescuentos());
        dto.setTotal(orden.getTotal());
        // Facturada si existe relación en memoria o en BD
        boolean tieneFactura = (orden.getFactura() != null);
        String numeroFactura = null;
        if (!tieneFactura && orden.getId() != null) {
            Optional<com.casaglass.casaglass_backend.model.Factura> facturaOpt = facturaRepository.findByOrdenId(orden.getId());
            tieneFactura = facturaOpt.isPresent();
            if (tieneFactura) {
                numeroFactura = facturaOpt.get().getNumeroFactura();
            }
        } else if (tieneFactura) {
            numeroFactura = orden.getFactura().getNumeroFactura();
        }
        dto.setFacturada(tieneFactura);
        dto.setNumeroFactura(numeroFactura != null ? numeroFactura : "-");
        
        // 👤 CLIENTE COMPLETO (todos los campos para facturación)
        if (orden.getCliente() != null) {
            OrdenTablaDTO.ClienteTablaDTO clienteDTO = new OrdenTablaDTO.ClienteTablaDTO(
                orden.getCliente().getId(),
                orden.getCliente().getNit(),
                orden.getCliente().getNombre(),
                orden.getCliente().getCorreo(),
                orden.getCliente().getCiudad(),
                orden.getCliente().getDireccion(),
                orden.getCliente().getTelefono()
            );
            dto.setCliente(clienteDTO);
        }
        
        // 👷 TRABAJADOR SIMPLIFICADO  
        if (orden.getTrabajador() != null) {
            dto.setTrabajador(new OrdenTablaDTO.TrabajadorTablaDTO(orden.getTrabajador().getNombre()));
        }
        
        // 🏢 SEDE SIMPLIFICADA
        if (orden.getSede() != null) {
            dto.setSede(new OrdenTablaDTO.SedeTablaDTO(orden.getSede().getNombre()));
        }
        
        // 💳 INFORMACIÓN DEL CRÉDITO (si existe)
        if (orden.getCreditoDetalle() != null) {
            CreditoTablaDTO creditoDTO = new CreditoTablaDTO();
            creditoDTO.setId(orden.getCreditoDetalle().getId());
            creditoDTO.setFechaInicio(orden.getCreditoDetalle().getFechaInicio());
            creditoDTO.setTotalCredito(orden.getCreditoDetalle().getTotalCredito());
            creditoDTO.setSaldoPendiente(orden.getCreditoDetalle().getSaldoPendiente());
            creditoDTO.setEstado(orden.getCreditoDetalle().getEstado());
            creditoDTO.setTotalAbonado(orden.getCreditoDetalle().getTotalAbonado());
            dto.setCreditoDetalle(creditoDTO);
        }
        
        // �📋 ITEMS COMPLETOS (manteniendo detalle como solicitado)
        if (orden.getItems() != null) {
            List<OrdenTablaDTO.OrdenItemTablaDTO> itemsDTO = orden.getItems().stream()
                    .map(this::convertirAOrdenItemTablaDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemsDTO);
        }
        
        return dto;
    }

    /**
     * 🔄 CONVERSOR: OrdenItem Entity → OrdenItemTablaDTO  
     */
    private OrdenTablaDTO.OrdenItemTablaDTO convertirAOrdenItemTablaDTO(OrdenItem item) {
        OrdenTablaDTO.OrdenItemTablaDTO itemDTO = new OrdenTablaDTO.OrdenItemTablaDTO();
        
        itemDTO.setId(item.getId());
        itemDTO.setDescripcion(item.getDescripcion());
        itemDTO.setCantidad(item.getCantidad());
        itemDTO.setPrecioUnitario(item.getPrecioUnitario());
        itemDTO.setTotalLinea(item.getTotalLinea());
        
        // 🎯 PRODUCTO SIMPLIFICADO (solo código y nombre)
        if (item.getProducto() != null) {
            OrdenTablaDTO.ProductoTablaDTO productoDTO = new OrdenTablaDTO.ProductoTablaDTO(
                item.getProducto().getCodigo(),
                item.getProducto().getNombre()
            );
            itemDTO.setProducto(productoDTO);
        }
        
        return itemDTO;
    }

    // 🔄 ================================
    // 🔄 MÉTODO DE ACTUALIZACIÓN
    // 🔄 ================================

    /**
     * 🔄 ACTUALIZAR ORDEN COMPLETA desde tabla
     * Maneja actualización de orden + items (crear, actualizar, eliminar)
     */
    @Transactional
    public OrdenTablaDTO actualizarOrden(Long ordenId, OrdenActualizarDTO dto) {
        // 1️⃣ Buscar orden existente
        Orden orden = repo.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + ordenId));

        // 🔄 GUARDAR ESTADO ANTERIOR DE VENTA para detectar conversión cotización → venta
        boolean eraVentaAntes = orden.isVenta();

        // 2️⃣ Actualizar campos básicos de la orden
        orden.setFecha(dto.getFecha());
        orden.setObra(dto.getObra());
        orden.setDescripcion(dto.getDescripcion());
        orden.setVenta(dto.isVenta());
        orden.setCredito(dto.isCredito());
        orden.setTieneRetencionFuente(dto.isTieneRetencionFuente());
        // Actualizar descuentos
        Double descuentos = dto.getDescuentos() != null ? dto.getDescuentos() : (orden.getDescuentos() != null ? orden.getDescuentos() : 0.0);
        orden.setDescuentos(descuentos);
        
        // Recalcular retención de fuente con el nuevo valor de tieneRetencionFuente
        // (se calculará después cuando se actualice el subtotal)

        // 3️⃣ Actualizar referencias de entidades
        if (dto.getClienteId() != null) {
            orden.setCliente(entityManager.getReference(Cliente.class, dto.getClienteId()));
        }
        if (dto.getTrabajadorId() != null) {
            orden.setTrabajador(entityManager.getReference(Trabajador.class, dto.getTrabajadorId()));
        }
        if (dto.getSedeId() != null) {
            orden.setSede(entityManager.getReference(Sede.class, dto.getSedeId()));
        }

        // 4️⃣ Manejar items: eliminar, actualizar, crear
        if (dto.getItems() != null) {
            actualizarItemsDeOrden(orden, dto.getItems());
        }
        
        // 5️⃣ Recalcular subtotal y total después de actualizar items
        // Calcular subtotal bruto (suma de items con IVA incluido)
        double subtotalBruto = 0.0;
        if (orden.getItems() != null) {
            for (OrdenItem item : orden.getItems()) {
                if (item.getTotalLinea() != null) {
                    subtotalBruto += item.getTotalLinea();
                }
            }
        }
        subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;
        
        // Asegurar que descuentos no sea null (ya se calculó arriba)
        if (descuentos == null) {
            descuentos = 0.0;
            orden.setDescuentos(descuentos);
        }
        
        // Calcular todos los valores monetarios según la especificación
        Double[] valores = calcularValoresMonetariosOrden(subtotalBruto, descuentos, orden.isTieneRetencionFuente());
        Double subtotalSinIva = valores[0];  // Base imponible sin IVA
        Double iva = valores[1];            // IVA calculado
        Double retencionFuente = valores[2]; // Retención de fuente
        Double total = valores[3];           // Total facturado
        
        // Guardar valores en la orden
        orden.setSubtotal(subtotalSinIva);        // Base sin IVA
        orden.setIva(iva);                        // IVA
        orden.setRetencionFuente(retencionFuente); // Retención
        orden.setTotal(total);                    // Total facturado

        // 6️⃣ Guardar orden actualizada PRIMERO
        Orden ordenActualizada = repo.save(orden);
        
        // 📦 MANEJO DE INVENTARIO: Descontar stock si se confirmó una cotización
        // Si cambió de cotización (venta=false) a venta (venta=true), descontar inventario
        if (!eraVentaAntes && ordenActualizada.isVenta()) {
            System.out.println("✅ COTIZACIÓN CONFIRMADA → VENTA - Descontando inventario...");
            actualizarInventarioPorVenta(ordenActualizada);
        } else if (eraVentaAntes && !ordenActualizada.isVenta()) {
            System.out.println("⚠️ VENTA REVERTIDA → COTIZACIÓN - Restaurando inventario...");
            restaurarInventarioPorAnulacion(ordenActualizada);
        } else if (!ordenActualizada.isVenta()) {
            System.out.println("📋 Actualización de COTIZACIÓN - Inventario NO afectado");
        }
        // ...existing code...

        // 7️⃣ MANEJAR CRÉDITO SI ES NECESARIO
        // Si se actualiza a venta a crédito, crear o actualizar el crédito
        if (ordenActualizada.isVenta() && ordenActualizada.isCredito()) {
            // ...existing code...
            
            // Obtener cliente completo para actualizar si es necesario
            Cliente cliente = ordenActualizada.getCliente();
            if (cliente != null) {
                // Actualizar cliente a crédito si es necesario
                if (cliente.getCredito() == null || !cliente.getCredito()) {
                    // ...existing code...
                    cliente.setCredito(true);
                    clienteRepository.save(cliente);
                }
            }
            
            // Verificar si ya existe crédito para esta orden
            if (ordenActualizada.getCreditoDetalle() != null) {
                // Si ya existe crédito, actualizarlo con el nuevo total y retención
                // Reutilizar retencionFuente ya calculada arriba (línea 1557)
                creditoService.actualizarCreditoParaOrden(
                    ordenActualizada.getCreditoDetalle().getId(),
                    ordenActualizada.getTotal(),  // Total orden
                    retencionFuente  // ✅ Pasar también la retención (ya calculada)
                );
                Double saldoPendienteInicial = ordenActualizada.getTotal() - retencionFuente;
                // ...existing code...
            } else {
                // Si no existe crédito, crearlo
                Long clienteId = cliente != null ? cliente.getId() : null;
                if (clienteId == null) {
                    // ...existing code...
                } else {
                    // Reutilizar retencionFuente ya calculada arriba (línea 1557)
                    creditoService.crearCreditoParaOrden(
                        ordenActualizada.getId(),
                        clienteId,
                        ordenActualizada.getTotal(),  // Total orden
                        retencionFuente  // ✅ Pasar también la retención (ya calculada)
                    );
                    Double saldoPendienteInicial = ordenActualizada.getTotal() - retencionFuente;
                    // Recargar la orden para obtener el crédito recién creado
                    ordenActualizada = repo.findById(ordenActualizada.getId())
                        .orElseThrow(() -> new RuntimeException("Error al recargar orden después de crear crédito"));
                }
            }
        } else if (ordenActualizada.isVenta() && !ordenActualizada.isCredito()) {
            // Si se cambió de crédito a contado, anular el crédito existente
            if (ordenActualizada.getCreditoDetalle() != null) {
                // ...existing code...
                creditoService.anularCredito(ordenActualizada.getCreditoDetalle().getId());
                // ...existing code...
            }
        } else {
            // Si no es venta o no es crédito, verificar si hay crédito que anular
            if (ordenActualizada.getCreditoDetalle() != null) {
                // ...existing code...
                // Opcional: anular crédito si la orden ya no es venta a crédito
                // creditoService.anularCredito(ordenActualizada.getCreditoDetalle().getId());
            }
        }

        // 8️⃣ Retornar DTO optimizado para tabla (recargar para incluir crédito)
        ordenActualizada = repo.findById(ordenActualizada.getId())
            .orElseThrow(() -> new RuntimeException("Error al recargar orden final"));
        
        // Verificar que el crédito se creó correctamente
        if (ordenActualizada.isVenta() && ordenActualizada.isCredito()) {
            if (ordenActualizada.getCreditoDetalle() == null) {
                // ...existing code...
            } else {
                // ...existing code...
            }
        }
        
        return convertirAOrdenTablaDTO(ordenActualizada);
    }

    /**
     * 🔄 ACTUALIZAR ITEMS DE UNA ORDEN
     * Maneja crear, actualizar y eliminar items
     */
    private void actualizarItemsDeOrden(Orden orden, List<OrdenActualizarDTO.OrdenItemActualizarDTO> itemsDTO) {
        
        // 🗑️ Eliminar items marcados para eliminación
        orden.getItems().removeIf(item -> 
            itemsDTO.stream().anyMatch(dto -> 
                dto.getId() != null && dto.getId().equals(item.getId()) && dto.isEliminar()
            )
        );

        for (OrdenActualizarDTO.OrdenItemActualizarDTO itemDTO : itemsDTO) {
            if (itemDTO.isEliminar()) {
                continue; // Ya eliminado arriba
            }

            if (itemDTO.getId() == null) {
                // 🆕 CREAR NUEVO ITEM
                OrdenItem nuevoItem = new OrdenItem();
                nuevoItem.setOrden(orden);
                nuevoItem.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
                nuevoItem.setDescripcion(itemDTO.getDescripcion());
                nuevoItem.setCantidad(itemDTO.getCantidad());
                nuevoItem.setPrecioUnitario(itemDTO.getPrecioUnitario());
                nuevoItem.setTotalLinea(itemDTO.getTotalLinea());
                
                orden.getItems().add(nuevoItem);
                
            } else {
                // 🔄 ACTUALIZAR ITEM EXISTENTE
                OrdenItem itemExistente = orden.getItems().stream()
                    .filter(item -> item.getId().equals(itemDTO.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Item no encontrado: " + itemDTO.getId()));

                itemExistente.setProducto(productoRepository.findById(itemDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + itemDTO.getProductoId())));
                itemExistente.setDescripcion(itemDTO.getDescripcion());
                itemExistente.setCantidad(itemDTO.getCantidad());
                itemExistente.setPrecioUnitario(itemDTO.getPrecioUnitario());
                itemExistente.setTotalLinea(itemDTO.getTotalLinea());
            }
        }
    }

    /**
     * Actualiza el inventario restando los productos vendidos
     * Se ejecuta cuando se crea una nueva orden (venta)
     */
    /**
     * 📦 ACTUALIZAR INVENTARIO POR VENTA - CON MANEJO DE CONCURRENCIA
     * 
     * Mejoras implementadas:
     * - Validación de stock con lock pesimista
     * - Manejo de concurrencia con reintentos
     * - Mensajes de error específicos
     * - Transaccional para consistencia
     * - 🔪 EXCLUYE CORTES: Solo procesa productos normales
     */
    @Transactional
    /**
     * 📦 ACTUALIZAR INVENTARIO POR VENTA - Versión con DTO para excluir productos en cortes[]
     * 
     * ⚠️ IMPORTANTE: Si un producto está en cortes[], NO se decrementa su inventario aquí
     * porque procesarCortes() ya maneja el decremento cuando se vende el corte solicitado.
     * Esto evita conflictos de concurrencia al cortar un corte existente.
     */
    private void actualizarInventarioPorVenta(Orden orden, OrdenVentaDTO ventaDTO) {
        if (orden.getItems() == null || orden.getItems().isEmpty()) {
            return;
        }

        System.out.println("🔄 Actualizando inventario para orden ID: " + orden.getId());
        
        // 🔪 Obtener IDs de productos que están siendo cortados (en cortes[])
        Set<Long> productosEnCortes = new HashSet<>();
        if (ventaDTO != null && ventaDTO.getCortes() != null && !ventaDTO.getCortes().isEmpty()) {
            for (OrdenVentaDTO.CorteSolicitadoDTO corte : ventaDTO.getCortes()) {
                if (corte.getProductoId() != null) {
                    productosEnCortes.add(corte.getProductoId());
                }
            }
        }
        
        // Obtener la sede de la orden (donde se realiza la venta)
        Long sedeId = orden.getSede().getId();

        for (OrdenItem item : orden.getItems()) {
            if (item.getProducto() != null && item.getCantidad() != null && item.getCantidad() > 0) {
                Long productoId = item.getProducto().getId();
                Double cantidadVendida = item.getCantidad();
                
                // ⚠️ SKIP: Si este producto está en cortes[], procesarCortes() ya maneja su inventario
                if (productosEnCortes.contains(productoId)) {
                    continue;
                }

                if (item.getProducto() instanceof Corte) {
                    // Venta de CORTE: decrementar inventario de cortes en la sede
                    try {
                        inventarioCorteService.decrementarStock(productoId, sedeId, cantidadVendida);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("❌ Stock de corte insuficiente para corte ID " + productoId + " en sede ID " + sedeId + ": " + e.getMessage());
                    }
                } else {
                    // Producto normal: restar del inventario normal
                    actualizarInventarioConcurrente(productoId, sedeId, cantidadVendida);
                }
            }
        }
        
        System.out.println("✅ Inventario actualizado correctamente para orden ID: " + orden.getId());
    }
    
    /**
     * 📦 ACTUALIZAR INVENTARIO POR VENTA - Versión sin DTO (para compatibilidad)
     * Usado en métodos que no tienen acceso al DTO original
     */
    private void actualizarInventarioPorVenta(Orden orden) {
        actualizarInventarioPorVenta(orden, null);
    }

    /**
     * 🔒 ACTUALIZAR INVENTARIO CON MANEJO DE CONCURRENCIA
     * 
     * Implementa:
     * - Lock pesimista para evitar race conditions
     * - Permite valores negativos (ventas anticipadas)
     * - Manejo de errores específicos
     * 
     * Nota: Se permiten valores negativos en el inventario para manejar ventas
     * anticipadas (productos vendidos antes de tenerlos en tienda)
     */
    @Transactional
    private void actualizarInventarioConcurrente(Long productoId, Long sedeId, Double cantidadVendida) {
        try {
            // 🔍 BUSCAR INVENTARIO CON LOCK PESIMISTA
            Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSedeConLock(productoId, sedeId);
            
            if (!inventarioOpt.isPresent()) {
                throw new IllegalArgumentException(
                    String.format("❌ No existe inventario para producto ID %d en sede ID %d", productoId, sedeId)
                );
            }
            
            Inventario inventario = inventarioOpt.get();
            double cantidadActual = inventario.getCantidad();
            
            System.out.println("📊 Stock actual: " + cantidadActual + ", cantidad a vender: " + cantidadVendida);
            
            // ➖ ACTUALIZAR CANTIDAD (permite valores negativos para ventas anticipadas)
            double nuevaCantidad = cantidadActual - cantidadVendida;
            
            inventario.setCantidad(nuevaCantidad);
            inventarioService.actualizar(inventario.getId(), inventario);
            
            System.out.println("✅ Stock actualizado: " + cantidadActual + " → " + nuevaCantidad + 
                             (nuevaCantidad < 0 ? " (⚠️ Stock negativo - venta anticipada)" : ""));
            
        } catch (IllegalArgumentException e) {
            // Re-lanzar errores de validación
            throw e;
        } catch (jakarta.persistence.OptimisticLockException e) {
            // 🔒 Lock optimista: Otro proceso modificó el inventario (muy raro)
            System.err.println("⚠️ Conflicto de versión (lock optimista): " + e.getMessage());
            e.printStackTrace(); // Log completo para debugging
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario del producto ID %d. Por favor, intente nuevamente.", productoId)
            );
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            // 🔒 Variante de Spring para OptimisticLockException
            System.err.println("⚠️ Conflicto de versión (Spring): " + e.getMessage());
            e.printStackTrace(); // Log completo para debugging
            throw new RuntimeException(
                String.format("⚠️ Otro usuario modificó el inventario del producto ID %d. Por favor, intente nuevamente.", productoId)
            );
        } catch (org.springframework.dao.DataAccessException e) {
            // Otros errores de base de datos
            System.err.println("❌ Error de base de datos al actualizar inventario producto ID " + productoId);
            System.err.println("❌ Tipo de excepción: " + e.getClass().getName());
            System.err.println("❌ Mensaje: " + e.getMessage());
            e.printStackTrace(); // Log completo para debugging
            throw new RuntimeException(
                String.format("❌ Error de base de datos al actualizar inventario del producto ID %d. Intente nuevamente.", productoId)
            );
        } catch (Exception e) {
            // Manejar otros errores inesperados
            System.err.println("❌ Error inesperado en inventario producto ID " + productoId);
            System.err.println("❌ Tipo de excepción: " + e.getClass().getName());
            System.err.println("❌ Mensaje: " + e.getMessage());
            e.printStackTrace(); // Log completo para debugging
            throw new RuntimeException(
                String.format("❌ Error inesperado al actualizar inventario del producto ID %d. Intente nuevamente.", productoId)
            );
        }
    }

    /**
     * Restaura el inventario sumando los productos de una orden anulada
     * Se ejecuta cuando se anula una orden
     */
    private void restaurarInventarioPorAnulacion(Orden orden) {
        if (orden.getItems() == null || orden.getItems().isEmpty()) {
            return;
        }

        // Obtener la sede de la orden
        Long sedeId = orden.getSede().getId();

        for (OrdenItem item : orden.getItems()) {
            if (item.getProducto() != null && item.getCantidad() != null && item.getCantidad() > 0) {
                Long productoId = item.getProducto().getId();
                
                // Buscar inventario del producto en la sede
                Optional<Inventario> inventarioOpt = inventarioService.obtenerPorProductoYSede(productoId, sedeId);
                
                if (inventarioOpt.isPresent()) {
                    Inventario inventario = inventarioOpt.get();
                    double cantidadActual = inventario.getCantidad();
                    double cantidadARestaurar = item.getCantidad();
                    
                    // Sumar cantidad restaurada usando método seguro
                    inventarioService.actualizarInventarioVenta(productoId, sedeId, cantidadActual + cantidadARestaurar);
                } else {
                    // Si no existe inventario, crearlo con la cantidad restaurada usando método seguro
                    inventarioService.actualizarInventarioVenta(productoId, sedeId, item.getCantidad());
                }
            }
        }
    }

    /**
     * Anula una orden y restaura el inventario
     */
    @Transactional
    public Orden anularOrden(Long id) {
        Optional<Orden> ordenOpt = repo.findById(id);
        if (!ordenOpt.isPresent()) {
            throw new IllegalArgumentException("Orden no encontrada con ID: " + id);
        }

        Orden orden = ordenOpt.get();
        
        // Verificar que la orden esté activa
        if (orden.getEstado() == Orden.EstadoOrden.ANULADA) {
            throw new IllegalArgumentException("La orden ya está anulada");
        }

        // Restaurar inventario antes de anular
        restaurarInventarioPorAnulacion(orden);

        // 💳 ANULAR CRÉDITO ASOCIADO SI EXISTE
        if (orden.getCreditoDetalle() != null) {
            try {
                creditoService.anularCredito(orden.getCreditoDetalle().getId());
            } catch (Exception e) {
                // Si falla la anulación del crédito, registrar el error pero continuar con la anulación de la orden
                System.err.println("Error al anular crédito para orden " + orden.getId() + ": " + e.getMessage());
            }
        }

        // Cambiar estado a anulada
        orden.setEstado(Orden.EstadoOrden.ANULADA);
        
        return repo.save(orden);
    }
    
    /**
     * 🔪 PROCESAR CORTES DE PRODUCTOS PERFIL
     * 
     * Lógica mejorada:
     * 1. Crea o reutiliza corte solicitado (para vender)
     * 2. Crea o reutiliza corte sobrante (para inventario)
     * 3. Incrementa inventario de AMBOS cortes en +1 (simula el corte)
     * 4. Luego se decrementa el solicitado en -1 cuando se procesa la venta
     * 
     * Si los cortes ya existen, simplemente se incrementa su inventario.
     */
    @Transactional
    private void procesarCortes(Orden orden, List<OrdenVentaDTO.CorteSolicitadoDTO> cortes) {
        System.out.println("🔪 Iniciando procesamiento de " + cortes.size() + " cortes...");
        
        for (OrdenVentaDTO.CorteSolicitadoDTO corteDTO : cortes) {
            System.out.println("🔪 Procesando corte: ProductoId=" + corteDTO.getProductoId() + 
                             ", Medida solicitada=" + corteDTO.getMedidaSolicitada() + "cm" +
                             ", Cantidad=" + corteDTO.getCantidad());
            
            // Validar que tenga cantidades por sede
            if (corteDTO.getCantidadesPorSede() == null || corteDTO.getCantidadesPorSede().isEmpty()) {
                System.err.println("⚠️ Corte sin cantidades por sede, omitiendo...");
                continue;
            }
            
            // 1. Obtener producto original
            Producto productoOriginal = productoRepository.findById(corteDTO.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + corteDTO.getProductoId()));
            
            // 1.5 🔪 SI SE ESTÁ CORTANDO UN CORTE EXISTENTE, DECREMENTAR SU INVENTARIO
            // Verificar si el producto original es un Corte (instanceof)
            if (productoOriginal instanceof Corte) {
                Long sedeId = orden.getSede().getId();
                Double cantidad = corteDTO.getCantidad() != null ? corteDTO.getCantidad() : 1.0;
                try {
                    inventarioCorteService.decrementarStock(productoOriginal.getId(), sedeId, cantidad);
                } catch (Exception e) {
                    throw new RuntimeException("Error al decrementar inventario del corte que se está cortando: " + e.getMessage());
                }
            }
            
            // 2. Crear o reutilizar corte solicitado (para vender)
            Corte corteSolicitado = crearCorteIndividual(
                productoOriginal, 
                corteDTO.getMedidaSolicitada(), 
                corteDTO.getPrecioUnitarioSolicitado(),
                "SOLICITADO" // Solo para logging interno, no se incluye en el nombre
            );
            System.out.println("✅ Corte solicitado: ID=" + corteSolicitado.getId() + 
                             ", Código=" + corteSolicitado.getCodigo() + 
                             ", Largo=" + corteSolicitado.getLargoCm() + "cm");
            
            // 3. Determinar corte sobrante (reutilizar si llega ID, de lo contrario crear)
            Corte corteSobrante;
            if (corteDTO.getReutilizarCorteId() != null) {
                corteSobrante = corteRepository.findById(corteDTO.getReutilizarCorteId())
                    .orElseThrow(() -> new RuntimeException("Corte sobrante no encontrado con ID: " + corteDTO.getReutilizarCorteId()));
                System.out.println("🔁 Reutilizando corte sobrante existente: ID=" + corteSobrante.getId() + 
                                 ", Código=" + corteSobrante.getCodigo() + 
                                 ", Largo=" + corteSobrante.getLargoCm() + "cm");
            } else {
                // Usar medidaSobrante del DTO, o calcular si no viene (600cm por defecto)
                Integer medidaSobrante = corteDTO.getMedidaSobrante() != null 
                    ? corteDTO.getMedidaSobrante() 
                    : (600 - corteDTO.getMedidaSolicitada());
                corteSobrante = crearCorteIndividual(
                    productoOriginal, 
                    medidaSobrante, 
                    corteDTO.getPrecioUnitarioSobrante(),
                    "SOBRANTE" // Solo para logging interno, no se incluye en el nombre
                );
                System.out.println("🆕 Corte sobrante creado: ID=" + corteSobrante.getId() + 
                                 ", Código=" + corteSobrante.getCodigo() + 
                                 ", Largo=" + corteSobrante.getLargoCm() + "cm");
            }
            
            // 4. INCREMENTAR INVENTARIO DE AMBOS CORTES (simula el corte)
            // Cuando se hace un corte, ambos cortes se agregan al inventario
            // Luego, cuando se procesa la venta, se decrementa el solicitado
            
            Long sedeId = orden.getSede().getId();
            Double cantidad = corteDTO.getCantidad() != null ? corteDTO.getCantidad() : 1.0;
            
            // NO incrementar inventario del corte solicitado (el vendido)
            // El corte solicitado debe quedar con stock 0 tras la venta
            System.out.println("📦 Stock del corte solicitado NO incrementado (venta): Corte ID=" + corteSolicitado.getId() + 
                             ", Sede ID=" + sedeId + ", Cantidad: 0 (se vende inmediatamente)");
            
            // Si ambos cortes son el mismo (ej: corte por la mitad), solo uno debe quedar en inventario
            if (corteSolicitado.getId().equals(corteSobrante.getId())) {
                // Solo incrementar stock si la cantidad es 2.0 (caso típico de corte por la mitad)
                if (cantidad == 2.0) {
                    inventarioCorteService.incrementarStock(corteSobrante.getId(), sedeId, 1.0);
                    System.out.println("📦 Corte por la mitad: stock final: Corte ID=" + corteSobrante.getId() + 
                                     ", Sede ID=" + sedeId + ", Cantidad: 1");
                } else {
                    // Si la cantidad no es 2, no incrementar stock (ambos se venden)
                    System.out.println("📦 Ambos cortes vendidos, stock final: Corte ID=" + corteSobrante.getId() + 
                                     ", Sede ID=" + sedeId + ", Cantidad: 0");
                }
            } else {
                if (corteDTO.getCantidadesPorSede() != null && !corteDTO.getCantidadesPorSede().isEmpty()) {
                    for (OrdenVentaDTO.CorteSolicitadoDTO.CantidadPorSedeDTO cantidadSede : corteDTO.getCantidadesPorSede()) {
                        if (cantidadSede.getSedeId() == null || cantidadSede.getCantidad() == null || cantidadSede.getCantidad() <= 0) {
                            continue; // Saltar sedes con cantidad 0 o sin ID
                        }
                        Long sedeIdSobrante = cantidadSede.getSedeId();
                        Double cantidadSobrante = cantidadSede.getCantidad();
                        // Incrementar stock del corte sobrante
                        inventarioCorteService.incrementarStock(
                            corteSobrante.getId(),
                            sedeIdSobrante,
                            cantidadSobrante
                        );
                        System.out.println("📦 Stock del corte sobrante incrementado: Corte ID=" + corteSobrante.getId() + 
                                         ", Sede ID=" + sedeIdSobrante + ", Cantidad: +" + cantidadSobrante);
                    }
                } else {
                    // Si no hay cantidadesPorSede específicas, incrementar en la sede de la orden
                    inventarioCorteService.incrementarStock(corteSobrante.getId(), sedeId, cantidad);
                    System.out.println("📦 Stock del corte sobrante incrementado (sede de orden): Corte ID=" + corteSobrante.getId() + 
                                     ", Sede ID=" + sedeId + ", Cantidad: +" + cantidad);
                }
            }
            
            System.out.println("✅ Cortes procesados: Solicitado ID=" + corteSolicitado.getId() + 
                             " (" + corteSolicitado.getLargoCm() + "cm), " +
                             "Sobrante ID=" + corteSobrante.getId() + 
                             " (" + corteSobrante.getLargoCm() + "cm)");
        }
        
        System.out.println("✅ Procesamiento de cortes completado");
        System.out.println("ℹ️ NOTA: El inventario del corte solicitado se decrementará cuando se procese la venta");
    }
    
    /**
     * 🔧 CREAR CORTE INDIVIDUAL
     * 
     * Crea un corte con los datos proporcionados.
     * El código siempre es el del producto base (sin sufijo de medida).
     * El nombre incluye la medida en CMS sin indicar si es SOBRANTE o SOLICITADO.
     */
    private Corte crearCorteIndividual(Producto productoOriginal, Integer medida, Double precio, String tipo) {
        // 0) Intentar reutilizar un corte existente por código base, largo, categoría y color
        // ✅ Código siempre es el del producto base (ej: "392"), NO incluye la medida
        String codigoBase = productoOriginal.getCodigo();
        Long categoriaId = productoOriginal.getCategoria() != null ? productoOriginal.getCategoria().getId() : null;
        var color = productoOriginal.getColor();

        if (categoriaId != null && color != null) {
            var existenteOpt = corteRepository
                .findExistingByCodigoAndSpecs(codigoBase, medida.doubleValue(), categoriaId, color);
            if (existenteOpt.isPresent()) {
                Corte corteExistente = existenteOpt.get();
                // Asegurarse de que el nombre esté correcto (no concatenado)
                // Si el producto original ya es un corte, extraer el nombre base antes de 'Corte de'
                String nombreOriginal = productoOriginal.getNombre();
                String baseNombre;
                int idx = nombreOriginal.indexOf(" Corte de ");
                if (idx != -1) {
                    baseNombre = nombreOriginal.substring(0, idx);
                } else {
                    baseNombre = nombreOriginal;
                }
                String nombreCorrecto = baseNombre + " Corte de " + medida + " CMS";
                if (!nombreCorrecto.equals(corteExistente.getNombre())) {
                    corteExistente.setNombre(nombreCorrecto);
                    corteService.guardar(corteExistente); // Actualiza el nombre si estaba mal
                }
                System.out.println("🔁 Reutilizando corte existente: " + corteExistente.getCodigo() + 
                                 " (ID=" + corteExistente.getId() + ", Largo=" + medida + "cm)");
                return corteExistente;
            }
        }

        // 1) Crear nuevo corte
        Corte corte = new Corte();

        // ✅ Código siempre es el del producto base (ej: "392")
        // NO se agrega sufijo de medida al código
        corte.setCodigo(codigoBase);

        // ✅ Nombre: "[Nombre Producto Base] Corte de X CMS"
        // Si el producto original ya es un corte, extraer el nombre base antes de 'Corte de'
        String nombreOriginal = productoOriginal.getNombre();
        String baseNombre;
        int idx = nombreOriginal.indexOf(" Corte de ");
        if (idx != -1) {
            baseNombre = nombreOriginal.substring(0, idx);
        } else {
            baseNombre = nombreOriginal;
        }
        String nombreFinal = baseNombre + " Corte de " + medida + " CMS";
        corte.setNombre(nombreFinal);

        // Medida específica en centímetros
        corte.setLargoCm(medida.doubleValue());

        // Precio calculado por el frontend
        corte.setPrecio1(precio);

        // Copiar datos del producto original
        corte.setCategoria(productoOriginal.getCategoria());
        corte.setTipo(productoOriginal.getTipo());
        corte.setColor(productoOriginal.getColor());
        corte.setCantidad(0); // Se maneja por inventario
        corte.setCosto(0.0); // Por ahora sin costo específico

        // Observación descriptiva
        corte.setObservacion("Corte generado automáticamente");

        return corteService.guardar(corte);
    }
    
    /**
     * ✅ INCREMENTAR INVENTARIO DE CORTES REUTILIZADOS
     * 
     * Cuando se reutiliza un corte solicitado (reutilizarCorteSolicitadoId), se está haciendo
     * un nuevo corte del mismo tipo. Por lo tanto, el inventario debe incrementarse primero
     * (porque se está cortando) antes de decrementarlo (porque se vende).
     * 
     * Lógica:
     * - Si se reutiliza un corte solicitado → incrementar inventario en la cantidad a vender
     * - Esto simula que se está cortando el perfil nuevamente
     * - Luego, cuando se procesa la venta, se decrementa normalmente
     */
    @Transactional
    private void incrementarInventarioCortesReutilizados(Orden orden, OrdenVentaDTO ventaDTO) {
        if (ventaDTO.getItems() == null || ventaDTO.getItems().isEmpty()) {
            return;
        }
        
        Long sedeId = orden.getSede().getId();
        
        for (OrdenVentaDTO.OrdenItemVentaDTO itemDTO : ventaDTO.getItems()) {
            // Solo procesar items que reutilizan un corte solicitado
            if (itemDTO.getReutilizarCorteSolicitadoId() != null && itemDTO.getCantidad() != null && itemDTO.getCantidad() > 0) {
                Long corteId = itemDTO.getReutilizarCorteSolicitadoId();
                Double cantidad = itemDTO.getCantidad();
                
                System.out.println("🔪 Reutilizando corte solicitado ID=" + corteId + 
                                 " → Incrementando inventario en +" + cantidad + 
                                 " (se está cortando de nuevo)");
                
                // Incrementar inventario del corte reutilizado
                // Esto simula que se está haciendo el corte (inventario pasa a 1 o más)
                inventarioCorteService.incrementarStock(corteId, sedeId, cantidad);
                
                System.out.println("✅ Inventario del corte reutilizado incrementado: Corte ID=" + corteId + 
                                 ", Sede ID=" + sedeId + ", Cantidad agregada=" + cantidad);
            }
        }
    }
    
    /**
     * 🔧 GENERAR CÓDIGO PARA CORTES
     * 
     * ✅ Formato simplificado: CODIGO_ORIGINAL-MEDIDA
     * La lógica de reutilización evita duplicados verificando código + medida + categoría + color
     * 
     * @deprecated Este método ya no se usa. El código se genera directamente en crearCorteIndividual()
     */
    @Deprecated
    private String generarCodigoCorte(String codigoOriginal, Integer medida) {
        return codigoOriginal + "-" + medida;
    }

    /**
     * 💰 ACTUALIZAR RETENCIÓN DE FUENTE DE UNA ORDEN
     * 
     * Endpoint especializado para actualizar SOLO los campos de retención de fuente
     * sin necesidad de enviar todos los datos de la orden (items, cliente, sede, etc.)
     * 
     * Características:
     * - Actualiza tieneRetencionFuente, retencionFuente, e IVA
     * - Recalcula el total de la orden
     * - Si la orden tiene crédito, actualiza también el saldo del crédito
     * - Validaciones de seguridad (orden debe existir y estar ACTIVA)
     * 
     * @param ordenId ID de la orden a actualizar
     * @param dto DTO con los nuevos valores de retención
     * @return Orden actualizada con todos sus campos
     * @throws IllegalArgumentException si la orden no existe o está anulada
     */
    @Transactional
    public Orden actualizarRetencionFuente(Long ordenId, com.casaglass.casaglass_backend.dto.RetencionFuenteDTO dto) {
        System.out.println("💰 DEBUG: Actualizando retención de fuente para orden ID: " + ordenId);
        System.out.println("💰 DEBUG: Datos recibidos: " + dto);
        
        // 1️⃣ BUSCAR ORDEN EXISTENTE
        Orden orden = repo.findById(ordenId)
            .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada con ID: " + ordenId));
        
        // 2️⃣ VALIDAR QUE LA ORDEN ESTÉ ACTIVA
        if (orden.getEstado() == Orden.EstadoOrden.ANULADA) {
            throw new IllegalArgumentException("No se puede actualizar la retención de una orden anulada");
        }
        
        // 3️⃣ VALIDAR DATOS DEL DTO
        if (dto.getTieneRetencionFuente() == null) {
            throw new IllegalArgumentException("El campo tieneRetencionFuente es obligatorio");
        }
        if (dto.getRetencionFuente() == null) {
            throw new IllegalArgumentException("El valor de retencionFuente es obligatorio");
        }
        
        // Si no tiene retención, el valor debe ser 0
        if (!dto.getTieneRetencionFuente() && dto.getRetencionFuente() != 0.0) {
            throw new IllegalArgumentException("Si tieneRetencionFuente es false, retencionFuente debe ser 0.0");
        }
        
        // 4️⃣ ACTUALIZAR CAMPOS DE RETENCIÓN
        orden.setTieneRetencionFuente(dto.getTieneRetencionFuente());
        orden.setRetencionFuente(dto.getRetencionFuente());
        
        // 5️⃣ ACTUALIZAR IVA SI SE PROPORCIONÓ (OPCIONAL)
        if (dto.getIva() != null) {
            orden.setIva(dto.getIva());
            System.out.println("💰 DEBUG: IVA actualizado a: " + dto.getIva());
        }
        
        // 6️⃣ RECALCULAR TOTAL (suma de items - descuentos, SIN restar retención)
        // El total facturado NO incluye la retención restada
        // La retención se resta solo para el saldo del crédito
        double subtotalBruto = 0.0;
        if (orden.getItems() != null && !orden.getItems().isEmpty()) {
            for (OrdenItem item : orden.getItems()) {
                subtotalBruto += item.getTotalLinea() != null ? item.getTotalLinea() : 0.0;
            }
        }
        subtotalBruto = Math.round(subtotalBruto * 100.0) / 100.0;
        
        Double descuentos = orden.getDescuentos() != null ? orden.getDescuentos() : 0.0;
        Double totalFacturado = subtotalBruto - descuentos;
        totalFacturado = Math.round(totalFacturado * 100.0) / 100.0;
        
        orden.setTotal(totalFacturado);
        System.out.println("💰 DEBUG: Total facturado recalculado: " + totalFacturado);
        
        // 7️⃣ GUARDAR ORDEN
        Orden ordenActualizada = repo.save(orden);
        System.out.println("✅ DEBUG: Orden actualizada exitosamente");
        
        // 8️⃣ ACTUALIZAR CRÉDITO SI EXISTE
        if (orden.isCredito() && orden.getCreditoDetalle() != null) {
            System.out.println("💳 DEBUG: Actualizando crédito asociado...");
            creditoService.recalcularTotales(orden.getCreditoDetalle().getId());
            System.out.println("✅ DEBUG: Crédito recalculado exitosamente");
        }
        
        return ordenActualizada;
    }
}