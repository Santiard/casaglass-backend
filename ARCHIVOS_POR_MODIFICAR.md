# ⚠️ Archivos que Necesitan Modificaciones para el Cambio Integer → Double

## Resumen

El cambio de `Integer` a `Double` para las cantidades de inventario requiere modificaciones en **múltiples archivos de servicios**.

Se detectaron **49 errores de compilación** en los siguientes archivos:

## 📁 Archivos Afectados

### Services (10 archivos)
1. ✅ `IngresoService.java` - 2 errores
2. ✅ `InventarioService.java` - 8 errores
3. ✅ `InventarioCompletoService.java` - 4 errores  
4. ✅ `InventarioCorteService.java` - 4 errores
5. ✅ `OrdenService.java` - 11 errores
6. ✅ `ProductoService.java` - 3 errores
7. ✅ `ProductoVidrioService.java` - 1 error
8. ✅ `ReembolsoIngresoService.java` - 2 errores
9. ✅ `ReembolsoVentaService.java` - 2 errores
10. ✅ `SedeDashboardService.java` - 1 error
11. ✅ `TrasladoService.java` - 6 errores
12. ✅ `TrasladoMovimientoService.java` - 2 errores

### DTOs (2 archivos)
1. ✅ `ReembolsoVentaResponseDTO.java` - 1 error
2. ✅ `TrasladoDetalleResponseDTO.java` - 1 error

## 🔧 Tipos de Cambios Requeridos

### 1. Conversiones Integer ↔ Double
```java
// ❌ ANTES
Integer cantidad = 5;
inventario.setCantidad(cantidad);

// ✅ DESPUÉS
Double cantidad = 5.0;
inventario.setCantidad(cantidad);
```

### 2. Conversiones int ↔ Double
```java
// ❌ ANTES
inventario.setCantidad(0);

// ✅ DESPUÉS
inventario.setCantidad(0.0);
```

### 3. Conversiones Double ↔ int
```java
// ❌ ANTES
int total = inventario.getCantidad();

// ✅ DESPUÉS
double total = inventario.getCantidad();
// O si necesitas int:
int total = inventario.getCantidad().intValue();
```

### 4. Cambios en Mapas
```java
// ❌ ANTES
Map<Long, Integer> cantidades = ...;

// ✅ DESPUÉS
Map<Long, Double> cantidades = ...;
```

### 5. Cambios en Integer::sum → Double::sum
```java
// ❌ ANTES
.collect(Collectors.toMap(..., Integer::sum))

// ✅ DESPUÉS
.collect(Collectors.toMap(..., Double::sum))
```

## 📝 Recomendación

Dado el **alto número de archivos afectados** (14 archivos, 49 errores), se recomienda:

1. **Opción A - Revertir cambios:**
   - Revertir los cambios en la entidad `Inventario.java` y DTOs
   - Mantener `Integer` en toda la aplicación
   - Solo cambiar la base de datos a `DECIMAL(10,2)`
   - Java maneja la conversión automática

2. **Opción B - Completar cambios:**  
   - Modificar todos los 14 archivos de servicio
   - Probar exhaustivamente cada funcionalidad
   - Riesgo de introducir bugs

3. **Opción C - Cambio gradual:**
   - Crear una rama específica para este cambio
   - Modificar archivo por archivo
   - Probar cada cambio individualmente

## ⚠️ Impacto en Producción

Si se despliega código parcialmente modificado:
- ❌ La aplicación **NO iniciará**
- ❌ Errores de compilación bloquearán el startup
- ❌ Funcionalidades críticas afectadas: ventas, ingresos, traslados, reembolsos

## 🚀 Siguiente Paso Recomendado

**Esperar instrucciones del usuario sobre cómo proceder:**
- ¿Completar todos los cambios ahora?
- ¿Revertir y mantener Integer?
- ¿Crear documentación para que el equipo lo haga manualmente?
