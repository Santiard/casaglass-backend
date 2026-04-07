-- Plan de cortes para cotizaciones (sin impacto en inventario real hasta confirmar venta)

CREATE TABLE IF NOT EXISTS `orden_cortes_plan` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `orden_id` bigint(20) NOT NULL,
  `producto_origen_id` bigint(20) NOT NULL,
  `origen_tipo` varchar(20) NOT NULL,
  `origen_corte_id` bigint(20) DEFAULT NULL,
  `plan_orden` int(11) NOT NULL,
  `medida_solicitada` int(11) NOT NULL,
  `medida_sobrante` int(11) DEFAULT NULL,
  `cantidad` double NOT NULL,
  `precio_unitario_solicitado` double DEFAULT NULL,
  `precio_unitario_sobrante` double DEFAULT NULL,
  `reutilizar_corte_id` bigint(20) DEFAULT NULL,
  `corte_solicitado_id` bigint(20) DEFAULT NULL,
  `estado` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_orden_corte_plan_orden` (`orden_id`),
  KEY `idx_orden_corte_plan_estado` (`estado`),
  KEY `idx_orden_corte_plan_producto_origen` (`producto_origen_id`),
  KEY `idx_orden_corte_plan_origen_corte` (`origen_corte_id`),
  CONSTRAINT `fk_orden_corte_plan_orden` FOREIGN KEY (`orden_id`) REFERENCES `ordenes` (`id`),
  CONSTRAINT `fk_orden_corte_plan_producto_origen` FOREIGN KEY (`producto_origen_id`) REFERENCES `productos` (`id`),
  CONSTRAINT `fk_orden_corte_plan_origen_corte` FOREIGN KEY (`origen_corte_id`) REFERENCES `cortes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `orden_cortes_plan_sede` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plan_id` bigint(20) NOT NULL,
  `sede_id` bigint(20) NOT NULL,
  `cantidad` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_orden_corte_plan_sede_plan` (`plan_id`),
  KEY `idx_orden_corte_plan_sede_sede` (`sede_id`),
  CONSTRAINT `fk_orden_corte_plan_sede_plan` FOREIGN KEY (`plan_id`) REFERENCES `orden_cortes_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orden_corte_plan_sede_sede` FOREIGN KEY (`sede_id`) REFERENCES `sedes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
