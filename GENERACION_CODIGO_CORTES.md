# 🔍 Generación de Código de Cortes - Documentación Técnica

## 📋 Resumen Ejecutivo

Cuando se crea un corte desde una orden de venta, el código se genera automáticamente en el backend con el siguiente formato:

**Formato del código (simplificado):**
```
CODIGO_ORIGINAL-MEDIDA
```

Donde:
- `CODIGO_ORIGINAL`: Código del producto perfil original (ej: "192")
- `MEDIDA`: Medida del corte en centímetros (ej: "150" o "450")

**Ejemplo:**
- Perfil original: código "192", medida 150cm → Corte: `"192-150"`
- Perfil original: código "192", medida 450cm → Corte: `"192-450"`

> ✅ **Actualización:** Se eliminó el sufijo de timestamp (`-XXXX`) para simplificar los códigos. La lógica de reutilización evita duplicados verificando código + medida + categoría + color.

---

## 🔧 Ubicación del Código

**Archivo:** `src/main/java/com/casaglass/casaglass_backend/service/OrdenService.java`

**Método:** `crearCorteIndividual()` (líneas 1290-1332)

**Llamado desde:** `procesarCortes()` (líneas 1185-1283)

---

## 📝 Flujo Detallado de Generación

### Paso 1: Construir Prefijo del Código

```java
String codigoPrefix = productoOriginal.getCodigo() + "-" + medida;
```

**Ejemplo:**
- Producto original: código "192"
- Medida: 150cm
- `codigoPrefix = "192-150"`

---

### Paso 2: Intentar Reutilizar Corte Existente

**Antes de crear un nuevo corte, el sistema intenta reutilizar uno existente** si cumple con:

1. **Código exacto:** El código debe ser exactamente `CODIGO_ORIGINAL-MEDIDA` (ej: "192-150")
2. **Largo exacto:** Debe tener exactamente la misma medida (en cm)
3. **Categoría:** Debe pertenecer a la misma categoría
4. **Color:** Debe tener el mismo color

**Query utilizada:**
```java
corteRepository.findExistingByCodigoAndSpecs(
    codigo,                 // "192-150" (código exacto)
    medida.doubleValue(),   // 150.0
    categoriaId,            // ID de la categoría
    color                   // Enum ColorProducto
)
```

**Si encuentra un corte existente:**
- ✅ **Retorna ese corte** (no crea uno nuevo)
- ✅ **No genera nuevo código**
- ✅ **Reutiliza el inventario existente**

**Si NO encuentra un corte existente:**
- ➡️ Continúa al Paso 3 para crear uno nuevo

---

### Paso 3: Generar Código Simplificado

**Solo se ejecuta si NO se encontró un corte existente para reutilizar.**

```java
String codigo = productoOriginal.getCodigo() + "-" + medida;  // "192-150"
corte.setCodigo(codigo);
```

**Explicación:**
- ✅ **Código simplificado:** Solo `CODIGO_ORIGINAL-MEDIDA`
- ✅ **Sin sufijo de timestamp:** Se eliminó para simplificar y mejorar legibilidad
- ✅ **Reutilización automática:** La lógica del Paso 2 evita duplicados
- **Código final:** `"192-150"`

**Ejemplo real:**
- Producto original: código "192"
- Medida: 150cm
- **Código:** `"192-150"`

---

## 🎯 Casos de Uso Específicos

### Caso 1: Corte Solicitado (para vender)

**Input:**
- Producto original: código "192", categoría "PERFIL", color "MATE"
- Medida solicitada: 150cm
- Precio: 5000

**Proceso:**
1. Prefijo: `"192-150"`
2. Busca corte existente con:
   - Código que empiece con "192-150"
   - Largo = 150cm
   - Categoría = "PERFIL"
   - Color = "MATE"
3. **Si existe:** Retorna ese corte (reutiliza)
4. **Si NO existe:** Crea nuevo con código `"192-150-XXXX"`

**Resultado:**
- Código: `"192-150"` (o reutiliza existente)
- Nombre: `"PERFIL ESTRUCTURAL 744 MATE - 150cm (SOLICITADO)"`
- Tipo: `"SOLICITADO"`

---

### Caso 2: Corte Sobrante (para inventario)

**Input:**
- Producto original: código "192", categoría "PERFIL", color "MATE"
- Medida sobrante: 450cm (600 - 150)
- Precio: 3000

**Proceso:**
1. Prefijo: `"192-450"`
2. Busca corte existente con:
   - Código que empiece con "192-450"
   - Largo = 450cm
   - Categoría = "PERFIL"
   - Color = "MATE"
3. **Si existe:** Retorna ese corte (reutiliza)
4. **Si NO existe:** Crea nuevo con código `"192-450-XXXX"`

**Resultado:**
- Código: `"192-450"` (o reutiliza existente)
- Nombre: `"PERFIL ESTRUCTURAL 744 MATE - 450cm (SOBRANTE)"`
- Tipo: `"SOBRANTE"`

---

## 🔄 Lógica de Reutilización

### ¿Por qué se reutiliza?

**Ventajas:**
1. ✅ **Evita duplicados:** No crea múltiples cortes idénticos
2. ✅ **Consolida inventario:** Todos los cortes del mismo tipo comparten stock
3. ✅ **Optimiza búsquedas:** Menos registros en la base de datos
4. ✅ **Mantiene consistencia:** Mismo código para el mismo corte

### ¿Cuándo se reutiliza?

**Se reutiliza si:**
- ✅ Existe un corte con código que empiece con `CODIGO_ORIGINAL-MEDIDA`
- ✅ Tiene exactamente la misma medida (largo)
- ✅ Pertenece a la misma categoría
- ✅ Tiene el mismo color

**NO se reutiliza si:**
- ❌ No existe ningún corte con esas características
- ❌ Existe pero tiene diferente medida
- ❌ Existe pero tiene diferente categoría
- ❌ Existe pero tiene diferente color

---

## 📊 Ejemplos Reales

### Ejemplo 1: Primer Corte (no existe)

**Input:**
- Producto: código "192", categoría ID=5, color "MATE"
- Medida: 150cm

**Proceso:**
1. Prefijo: `"192-150"`
2. Busca existente: ❌ No encuentra
3. Genera código: `"192-150-0123"` (timestamp: ...0123)
4. Crea nuevo corte

**Resultado:**
```json
{
  "id": 100,
  "codigo": "192-150",
  "nombre": "PERFIL ESTRUCTURAL 744 MATE - 150cm (SOLICITADO)",
  "largoCm": 150.0,
  "categoria": { "id": 5, "nombre": "PERFIL" },
  "color": "MATE"
}
```

---

### Ejemplo 2: Segundo Corte (mismo tipo, reutiliza)

**Input:**
- Producto: código "192", categoría ID=5, color "MATE"
- Medida: 150cm

**Proceso:**
1. Prefijo: `"192-150"`
2. Busca existente: ✅ Encuentra el corte del Ejemplo 1
3. **Reutiliza:** Retorna corte ID=100

**Resultado:**
- ✅ **NO crea nuevo corte**
- ✅ **Retorna el existente** (ID=100, código "192-150")
- ✅ **Incrementa inventario** del corte existente

---

### Ejemplo 3: Corte Diferente (misma medida, diferente color)

**Input:**
- Producto: código "192", categoría ID=5, color "BLANCO" (diferente)
- Medida: 150cm

**Proceso:**
1. Prefijo: `"192-150"`
2. Busca existente: ❌ No encuentra (color diferente)
3. Genera código: `"192-150-4567"` (nuevo timestamp)
4. Crea nuevo corte

**Resultado:**
```json
{
  "id": 101,
  "codigo": "192-150",  // ✅ Mismo código, pero color diferente
  "nombre": "PERFIL ESTRUCTURAL 744 BLANCO - 150cm (SOLICITADO)",
  "largoCm": 150.0,
  "categoria": { "id": 5, "nombre": "PERFIL" },
  "color": "BLANCO"  // ✅ Color diferente (permite mismo código)
}
```

---

## 🔍 Método de Búsqueda de Reutilización

**Archivo:** `src/main/java/com/casaglass/casaglass_backend/repository/CorteRepository.java`

**Query JPQL:**
```java
@Query("SELECT c FROM Corte c WHERE c.codigo = :codigo AND c.largoCm = :largo AND c.categoria.id = :categoriaId AND c.color = :color")
Optional<Corte> findExistingByCodigoAndSpecs(
    @Param("codigo") String codigo,                 // "192-150" (código exacto)
    @Param("largo") Double largo,                   // 150.0
    @Param("categoriaId") Long categoriaId,         // 5
    @Param("color") ColorProducto color             // MATE
);
```

**Explicación:**
- `c.codigo = :codigo`: Busca por código exacto (más eficiente que LIKE)
- `c.largoCm = :largo`: Debe tener exactamente la misma medida
- `c.categoria.id = :categoriaId`: Debe ser de la misma categoría
- `c.color = :color`: Debe tener el mismo color

---

## ⚠️ Notas Importantes

### 1. ✅ Código Simplificado (Sin Sufijo de Timestamp)

El código ahora es simplemente `CODIGO_ORIGINAL-MEDIDA`, sin sufijo adicional.

**Ventajas:**
- ✅ Códigos más cortos y legibles: `"192-150"` vs `"192-150-0123"`
- ✅ Más fácil de recordar y usar para los usuarios
- ✅ La lógica de reutilización evita duplicados automáticamente

**Seguridad:**
- ✅ La verificación por código + medida + categoría + color evita duplicados
- ✅ Con ~120 combinaciones máximo (30 perfiles × 3 colores × medidas), el riesgo de colisión es mínimo
- ✅ Si dos cortes tienen el mismo código pero diferente color, son productos diferentes válidos

### 2. El método `generarCodigoCorte()` está deprecado

Existe un método `generarCodigoCorte()` en la línea 1341 que está marcado como `@Deprecated`.

**Este método NO se está utilizando actualmente.** El código se genera directamente en `crearCorteIndividual()`.

### 3. ✅ Reutilización Automática Previene Duplicados

El código generado **SÍ se verifica** contra la base de datos antes de guardar mediante la lógica de reutilización (Paso 2). Si existe un corte con el mismo código, medida, categoría y color, se reutiliza en lugar de crear uno nuevo.

### 4. El tipo (SOLICITADO/SOBRANTE) NO afecta el código

El código se genera igual para cortes "SOLICITADO" y "SOBRANTE". La diferencia está en:
- El **nombre** del corte (incluye el tipo entre paréntesis)
- La **observación** del corte
- El **precio** asignado

**Ejemplo:**
- Solicitado: `"192-150"` → Nombre: `"... (SOLICITADO)"`
- Sobrante: `"192-450"` → Nombre: `"... (SOBRANTE)"`

---

## 📝 Resumen de Formato

| Componente | Ejemplo | Descripción |
|------------|---------|-------------|
| **Código Original** | `"192"` | Código del producto perfil |
| **Separador** | `"-"` | Separador entre código y medida |
| **Medida** | `"150"` | Medida del corte en centímetros |
| **Código Final** | `"192-150"` | Código completo del corte (simplificado) |

---

## 🎯 Respuestas a Preguntas Específicas

### ¿Cómo se genera el código del corte?
✅ Se genera automáticamente en el backend con formato: `CODIGO_ORIGINAL-MEDIDA`

### ¿Se usa el código del producto original como prefijo?
✅ Sí, exactamente: `productoOriginal.getCodigo() + "-" + medida`

### ¿Se incluye la medida?
✅ Sí, la medida se incluye después del código original: `"192-150"`

### ¿Hay algún separador o formato específico?
✅ Sí, se usa un guion (`-`) como separador entre código y medida: `"192-150"`

### ¿Cuál es el formato exacto?
✅ `CODIGO_ORIGINAL-MEDIDA` (sin sufijo adicional)

### ¿Hay alguna lógica especial?
✅ Sí, **reutilización automática**: Antes de crear un nuevo corte, busca si ya existe uno con las mismas características (código exacto, medida, categoría, color) y lo reutiliza.

### ¿Se genera diferente para el corte solicitado vs el sobrante?
❌ **NO**, el código se genera igual. La diferencia está en el nombre y la observación.

### ¿Se verifica si ya existe un corte con ese código antes de crear uno nuevo?
✅ Sí, se verifica por:
- Código exacto (`CODIGO_ORIGINAL-MEDIDA`)
- Medida exacta
- Categoría
- Color

### ¿Hay algún contador o secuencia?
❌ **NO**, el código es simplemente `CODIGO_ORIGINAL-MEDIDA`. La reutilización automática evita duplicados.

### ¿Dónde se genera el código?
✅ En `OrdenService.crearCorteIndividual()` (línea 1304)

---

## 🔗 Referencias de Código

- **Método principal:** `OrdenService.crearCorteIndividual()` (línea 1290)
- **Método de búsqueda:** `CorteRepository.findExistingByPrefixAndSpecs()` (línea 56)
- **Llamado desde:** `OrdenService.procesarCortes()` (línea 1195)
- **Modelo:** `Corte.java` (extiende `Producto`)

---

**Última actualización:** 2025-01-XX
**Versión del código analizado:** Actual (post-fix ProductoVidrio)

