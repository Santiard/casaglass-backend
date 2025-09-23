package com.casaglass.casaglass_backend.repository;

import com.casaglass.casaglass_backend.model.OrdenItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenItemRepository extends JpaRepository<OrdenItem, Long> {
    List<OrdenItem> findByOrdenId(Long ordenId);
}