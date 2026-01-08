# 🗺️ Plan de Implementación: Integer → Double

**Objetivo:** Corregir 49 errores de compilación en 14 archivos  
**Estrategia:** Corrección ordenada, de lo más básico a lo más complejo  
**Fecha inicio:** 8 de Enero, 2026

---

## 📋 Orden de Corrección

### FASE 1: DTOs y Conversiones Básicas ✅ (COMPLETADO)
- [x] `Inventario.java` (entidad)
- [x] `InventarioProductoDTO.java`
- [x] `ProductoInventarioCompletoDTO.java`
- [x] `InventarioActualizarDTO.java`
- [x] `InventarioCorteDTO.java`
- [x] `OrdenVentaDTO.java`
- [x] `IngresoCreateDTO.java`
- [x] `TrasladoDetalleResponseDTO.java`
- [x] `TrasladoMovimientoDTO.java`
- [x] `ReembolsoVentaCreateDTO.java`
- [x] `ReembolsoVentaResponseDTO.java`
- [x] `ReembolsoIngresoCreateDTO.java`

### FASE 2: Servicios de Inventario Base (6 archivos, 17 errores)
Estos son fundamentales porque todos los demás servicios los usan.

#### 2.1. InventarioService.java ⏳ (PRIORIDAD ALTA)
**Errores:** 8  
**Por qué primero:** Es el servicio base que todos usan para actualizar inventario

**Cambios necesarios:**
- Línea 132: `inventario.setCantidad(0);` → `inventario.setCantidad(0.0);`
- Línea 142: `inventario.setCantidad(0);` → `inventario.setCantidad(0.0);`
- Línea 200: `inventario.setCantidad(0);` → `inventario.setCantidad(0.0);`
- Líneas 229-231: Conversiones `Integer` → `Double` en actualizaciones de cantidad
- Líneas 249, 259: `Integer` → `Double` en variables locales

#### 2.2. InventarioCompletoService.java ⏳
**Errores:** 4  
**Por qué segundo:** Maneja listados completos de inventario

**Cambios necesarios:**
- Líneas 80-81: Corregir tipos de `Map<Long, Integer>` → `Map<Long, Double>`
- Línea 103: Cambiar `Integer::sum` → `Double::sum`
- Líneas 111-112: Corregir inferencia de tipos en streams

#### 2.3. InventarioCorteService.java ⏳
**Errores:** 4  
**Por qué tercero:** Específico para cortes, menos usado

**Cambios necesarios:**
- Línea 203: `int` → `Double` en variable
- Líneas 206-208: `Integer` → `Double` en cantidades por sede

#### 2.4. TrasladoDetalleResponseDTO.java ⏳
**Errores:** 1  
**Por qué cuarto:** DTO usado en respuestas de traslados

**Cambios necesarios:**
- Línea 27: Constructor que recibe `Integer` → cambiar a `Double`

### FASE 3: Servicios de Operaciones (5 archivos, 22 errores)
Dependen de los servicios de inventario.

#### 3.1. TrasladoService.java ⏳
**Errores:** 6  
**Por qué primero:** Maneja movimientos entre sedes

**Cambios necesarios:**
- Líneas 125, 315: Conversión `double` → `int` (cambiar a `Double`)
- Líneas 132, 322: `int` → `Double` en variables
- Líneas 151, 332: `Integer` → `Double` en operaciones

#### 3.2. IngresoService.java ⏳
**Errores:** 2  
**Por qué segundo:** Registra ingresos de productos

**Cambios necesarios:**
- Línea 315: `Double` → `Integer` (eliminar conversión)
- Línea 467: `Integer` → `Double` en actualización

#### 3.3. OrdenService.java ⏳ (CRÍTICO - MÁS ERRORES)
**Errores:** 11  
**Por qué tercero:** Servicio más complejo, maneja ventas

**Cambios necesarios:**
- Líneas 203, 348, 482, 591: `Double` → `Integer` en cantidades de items
- Líneas 1843, 1915: `Double` → `int` en operaciones de cortes
- Línea 1850: `int` → `Double` en asignación
- Líneas 1998, 2046, 2072, 2203: Conversiones de tipos en validaciones

#### 3.4. ReembolsoIngresoService.java ⏳
**Errores:** 2

**Cambios necesarios:**
- Línea 251: `Double` → `Integer` (eliminar conversión)
- Línea 304: `int` → `Double` en operación

#### 3.5. ReembolsoVentaService.java ⏳
**Errores:** 2

**Cambios necesarios:**
- Línea 263: `Double` → `Integer` (eliminar conversión)
- Línea 315: `Integer` → `Double` en operación

### FASE 4: Servicios de Productos y Dashboards (4 archivos, 5 errores)

#### 4.1. ProductoService.java ⏳
**Errores:** 3

**Cambios necesarios:**
- Línea 66: `int` → `Double` en inicialización
- Líneas 364, 371: `Integer` → `Double` en operaciones

#### 4.2. ProductoVidrioService.java ⏳
**Errores:** 1

**Cambios necesarios:**
- Línea 149: `int` → `Double` en inicialización

#### 4.3. SedeDashboardService.java ⏳
**Errores:** 1

**Cambios necesarios:**
- Línea 173: `Double` → `Integer` (eliminar conversión)

#### 4.4. TrasladoMovimientoService.java ⏳
**Errores:** 2

**Cambios necesarios:**
- Línea 268: `Integer` → `Double` en conversión
- Línea 272: Corregir inferencia de tipos

### FASE 5: DTOs Pendientes (1 archivo, 1 error)

#### 5.1. ReembolsoVentaResponseDTO.java ⏳
**Errores:** 1

**Cambios necesarios:**
- Línea 75: Constructor - `Integer` → `Double`

---

## 🎯 Estrategia de Corrección

### Para cada archivo:
1. **Leer** el contexto completo (50-100 líneas alrededor del error)
2. **Identificar** el patrón del error
3. **Aplicar** el cambio correcto:
   - `Integer variable` → `Double variable`
   - `int value` → `double value` o `Double value`
   - `.setCantidad(0)` → `.setCantidad(0.0)`
   - `.setCantidad(cantidad.intValue())` → `.setCantidad(cantidad)`
   - `Integer::sum` → `Double::sum`
   - `Map<Long, Integer>` → `Map<Long, Double>`
4. **Compilar** para verificar que se corrigió
5. **Marcar** como completado en este documento

### Patrones Comunes de Corrección:

```java
// ❌ ANTES → ✅ DESPUÉS

// Patrón 1: Variables Integer
Integer cantidad;              → Double cantidad;

// Patrón 2: Literales enteros
.setCantidad(0);              → .setCantidad(0.0);
.setCantidad(cantidad);       → .setCantidad(cantidad); // ya es Double

// Patrón 3: Conversiones explícitas
cantidad.intValue();          → cantidad; // ya no necesita conversión

// Patrón 4: Variables primitivas int
int total = 0;                → double total = 0.0;
for(int i...) // NO CAMBIAR  → for(int i...) // índices siguen siendo int

// Patrón 5: Operaciones matemáticas
int suma = a + b;             → double suma = a + b;

// Patrón 6: Streams y colecciones
Integer::sum                  → Double::sum
Map<Long, Integer>            → Map<Long, Double>

// Patrón 7: Comparaciones
if (cantidad == 0)            → if (cantidad == 0.0)
```

---

## 📊 Progreso

- **Fase 1:** ✅ 12/12 archivos (100%)
- **Fase 2:** ⏳ 0/4 archivos (0%)
- **Fase 3:** ⏳ 0/5 archivos (0%)
- **Fase 4:** ⏳ 0/4 archivos (0%)
- **Fase 5:** ⏳ 0/1 archivos (0%)

**Total:** ✅ 12/26 archivos (46%)  
**Errores corregidos:** 0/49 (0%)

---

## 🚀 Próximo Archivo a Corregir

### → InventarioService.java (8 errores)

**Comando para empezar:**
```
Revisar líneas 120-270 de InventarioService.java
```

---

## ✅ Checklist de Verificación por Archivo

Antes de marcar un archivo como completado:
- [ ] Todos los errores del archivo están corregidos
- [ ] El archivo compila sin errores
- [ ] Se revisaron patrones similares en el archivo
- [ ] Se actualizó el progreso en este documento

---

**Última actualización:** 8 de Enero, 2026 15:35
