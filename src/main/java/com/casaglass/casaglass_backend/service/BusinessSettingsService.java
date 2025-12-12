package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.BusinessSettings;
import com.casaglass.casaglass_backend.repository.BusinessSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessSettingsService {

    private final BusinessSettingsRepository repository;

    public BusinessSettingsService(BusinessSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * 📋 OBTENER CONFIGURACIÓN ACTUAL
     * Retorna la primera configuración encontrada (debería haber solo una)
     * Si no existe, retorna una configuración con valores por defecto
     */
    @Transactional(readOnly = true)
    public BusinessSettings obtenerConfiguracion() {
        List<BusinessSettings> settings = repository.findAll();
        if (!settings.isEmpty()) {
            return settings.get(0);
        }
        // Si no existe, retornar configuración por defecto (sin guardar)
        BusinessSettings defaultSettings = new BusinessSettings();
        defaultSettings.setIvaRate(19.0);
        defaultSettings.setReteRate(2.5);
        defaultSettings.setReteThreshold(1_000_000L);
        defaultSettings.setUpdatedAt(LocalDate.now());
        return defaultSettings;
    }

    /**
     * 📋 OBTENER CONFIGURACIÓN POR ID
     */
    @Transactional(readOnly = true)
    public Optional<BusinessSettings> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    /**
     * 💾 CREAR CONFIGURACIÓN
     * Crea una nueva configuración de negocio
     * Nota: Normalmente solo debería haber una configuración
     */
    public BusinessSettings crear(BusinessSettings settings) {
        // Validar valores
        validarConfiguracion(settings);
        
        // Establecer fecha de actualización
        settings.setUpdatedAt(LocalDate.now());
        
        return repository.save(settings);
    }

    /**
     * 🔄 ACTUALIZAR CONFIGURACIÓN
     * Actualiza la configuración existente
     */
    public BusinessSettings actualizar(Long id, BusinessSettings settings) {
        BusinessSettings existente = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada con ID: " + id));
        
        // Validar valores
        validarConfiguracion(settings);
        
        // Actualizar campos
        existente.setIvaRate(settings.getIvaRate());
        existente.setReteRate(settings.getReteRate());
        existente.setReteThreshold(settings.getReteThreshold());
        existente.setUpdatedAt(LocalDate.now());
        
        return repository.save(existente);
    }

    /**
     * 🔄 ACTUALIZAR CONFIGURACIÓN ACTUAL (sin ID)
     * Actualiza la primera configuración encontrada o crea una nueva si no existe
     */
    public BusinessSettings actualizarConfiguracion(BusinessSettings settings) {
        List<BusinessSettings> existentes = repository.findAll();
        
        // Validar valores
        validarConfiguracion(settings);
        
        if (!existentes.isEmpty()) {
            // Actualizar la primera configuración existente
            BusinessSettings existente = existentes.get(0);
            existente.setIvaRate(settings.getIvaRate());
            existente.setReteRate(settings.getReteRate());
            existente.setReteThreshold(settings.getReteThreshold());
            existente.setUpdatedAt(LocalDate.now());
            return repository.save(existente);
        } else {
            // Crear nueva configuración
            settings.setUpdatedAt(LocalDate.now());
            return repository.save(settings);
        }
    }

    /**
     * ✅ VALIDAR CONFIGURACIÓN
     * Valida que los valores estén en rangos válidos
     */
    private void validarConfiguracion(BusinessSettings settings) {
        if (settings.getIvaRate() == null || settings.getIvaRate() < 0 || settings.getIvaRate() > 100) {
            throw new IllegalArgumentException("El IVA rate debe estar entre 0 y 100");
        }
        if (settings.getReteRate() == null || settings.getReteRate() < 0 || settings.getReteRate() > 100) {
            throw new IllegalArgumentException("El rete rate debe estar entre 0 y 100");
        }
        if (settings.getReteThreshold() == null || settings.getReteThreshold() < 0) {
            throw new IllegalArgumentException("El rete threshold debe ser mayor o igual a 0");
        }
    }

    /**
     * 🗑️ ELIMINAR CONFIGURACIÓN
     */
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Configuración no encontrada con ID: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * 📋 LISTAR TODAS LAS CONFIGURACIONES
     * Normalmente solo debería haber una
     */
    @Transactional(readOnly = true)
    public List<BusinessSettings> listar() {
        return repository.findAll();
    }
}

