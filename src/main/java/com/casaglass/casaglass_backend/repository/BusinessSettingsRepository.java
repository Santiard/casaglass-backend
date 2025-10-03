package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {
}