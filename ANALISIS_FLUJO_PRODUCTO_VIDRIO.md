# 🔍 ANÁLISIS DEL FLUJO: CREACIÓN DE PRODUCTO VIDRIO

## 📋 PROBLEMA IDENTIFICADO

Al crear un `ProductoVidrio` desde el frontend:
- ✅ Se crea el registro en `productos` (tabla base)
- ❌ NO se crea el registro en `productos_vidrio` (tabla extendida)
- ✅ Se crea el inventario (lo que confirma que el producto existe en `productos`)

## 🔄 FLUJO ACTUAL DEL SISTEMA

### 1. FRONTEND → BACKEND
```
Frontend envía POST /api/productos-vidrio
Body: {
  "codigo": "VIDRIO2",
  "nombre": "VIDRIO 2 PA PRUEBA",
  "mm": 1,
  "m1": 1,
  "m2": 3,
  ...
}
```

### 2. CONTROLADOR (`ProductoVidrioController.java`)
```java
@PostMapping
public ResponseEntity<?> crear(@RequestBody ProductoVidrio producto) {
    return ResponseEntity.ok(service.guardar(producto));
}
```

**PUNTO CRÍTICO #1**: Jackson deserializa el JSON a un objeto Java.
- Si Jackson no puede determinar que debe crear un `ProductoVidrio`, puede crear un `Producto` base.
- Esto depende de cómo Jackson maneja la herencia.

### 3. SERVICIO (`ProductoVidrioService.java`)
```java
public ProductoVidrio guardar(ProductoVidrio p) {
    // ... validaciones ...
    entityManager.persist(p);
    entityManager.flush();
    // ...
}
```

**PUNTO CRÍTICO #2**: Hibernate necesita saber que `p` es una instancia de `ProductoVidrio`.
- Si `p` es realmente un `Producto` (no `ProductoVidrio`), Hibernate solo insertará en `productos`.
- Si `p` es un `ProductoVidrio`, Hibernate debería insertar en ambas tablas.

### 4. HIBERNATE CON JOINED INHERITANCE

**Comportamiento esperado con `@Inheritance(strategy = InheritanceType.JOINED)`**:

Cuando Hibernate persiste un `ProductoVidrio`:
1. **Primero**: INSERT en `productos` (tabla padre)
   ```sql
   INSERT INTO productos (codigo, nombre, ...) VALUES (...)
   ```
2. **Segundo**: INSERT en `productos_vidrio` (tabla hija)
   ```sql
   INSERT INTO productos_vidrio (id, mm, m1, m2, m1m2) 
   VALUES (LAST_INSERT_ID(), ...)
   ```

**Comportamiento actual**:
- Solo se ejecuta el paso 1
- El paso 2 NO se ejecuta

## 🔎 ANÁLISIS DE LA CAUSA RAÍZ

### HIPÓTESIS 1: Jackson deserializa como `Producto` en lugar de `ProductoVidrio`

**Evidencia**:
- Los logs de debug en `ProductoVidrioService.guardar()` NO aparecen en la consola del servidor
- Esto sugiere que el método `guardar()` de `ProductoVidrioService` NO se está ejecutando
- O el objeto recibido NO es realmente un `ProductoVidrio`

**Verificación necesaria**:
- Agregar logs en el controlador para verificar el tipo de objeto recibido
- Verificar si Jackson está usando `@JsonTypeInfo` o similar para manejar la herencia

### HIPÓTESIS 2: El frontend está llamando al endpoint incorrecto

**Evidencia**:
- El frontend muestra: `🔍 DEBUG: Creando producto VIDRIO`
- Pero no vemos logs de `ProductoVidrioController.crear()`

**Verificación necesaria**:
- Confirmar que el frontend está enviando a `/api/productos-vidrio` y no a `/api/productos`
- Si está enviando a `/api/productos`, entonces se está usando `ProductoService.guardar()` que solo guarda en `productos`

### HIPÓTESIS 3: Hibernate no detecta el tipo correcto al persistir

**Evidencia**:
- Aunque usamos `entityManager.persist(p)`, Hibernate podría no estar detectando que es un `ProductoVidrio`
- Esto puede pasar si el objeto fue deserializado como `Producto` y luego se intenta persistir

**Verificación necesaria**:
- Verificar el tipo real del objeto antes de persistir
- Verificar si Hibernate está usando un `DiscriminatorColumn` (no debería con JOINED, pero verificar)

## 🎯 PUNTOS DE FALLA IDENTIFICADOS

### FALLA #1: Deserialización de Jackson
**Ubicación**: `ProductoVidrioController.crear(@RequestBody ProductoVidrio producto)`

**Problema**: Jackson puede estar deserializando el JSON como `Producto` en lugar de `ProductoVidrio` porque:
- No hay información de tipo en el JSON (no hay campo `@class` o similar)
- Jackson no sabe que debe crear un `ProductoVidrio` basándose solo en los campos `mm`, `m1`, `m2`

**Solución requerida**: 
- Configurar Jackson para que detecte el tipo basándose en los campos presentes
- O usar `@JsonTypeInfo` y `@JsonSubTypes` en la clase base `Producto`

### FALLA #2: Falta de verificación del tipo antes de persistir
**Ubicación**: `ProductoVidrioService.guardar(ProductoVidrio p)`

**Problema**: No hay verificación explícita de que el objeto sea realmente un `ProductoVidrio` antes de persistir.

**Solución requerida**:
- Verificar el tipo del objeto antes de persistir
- Si no es `ProductoVidrio`, lanzar error o convertir

### FALLA #3: Posible uso del endpoint incorrecto
**Ubicación**: Frontend podría estar llamando a `/api/productos` en lugar de `/api/productos-vidrio`

**Problema**: Si el frontend llama a `/api/productos`, se usa `ProductoService.guardar()` que solo guarda en `productos`.

**Solución requerida**:
- Verificar en el frontend qué endpoint se está usando
- Asegurar que se use `/api/productos-vidrio` para productos vidrio

## 📊 CONFIGURACIÓN ACTUAL DE HERENCIA

### Clase Base: `Producto`
```java
@Entity
@Table(name = "productos")
@Inheritance(strategy = InheritanceType.JOINED)  // ✅ Correcto
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}
```

### Clase Hija: `ProductoVidrio`
```java
@Entity
@Table(name = "productos_vidrio")
@PrimaryKeyJoinColumn(name = "id")  // ✅ Correcto
public class ProductoVidrio extends Producto {
    @Column(nullable = false)
    private Double mm;
    // ...
}
```

**Análisis**: La configuración de JPA es correcta. El problema NO está en la configuración de herencia.

## 🔧 ANÁLISIS DE LA SOLUCIÓN CORRECTA

### SOLUCIÓN 1: Configurar Jackson para manejar herencia (RECOMENDADA)

**Problema**: Jackson necesita saber qué tipo crear cuando deserializa JSON a una jerarquía de herencia.

**Solución**: Agregar `@JsonTypeInfo` y `@JsonSubTypes` en `Producto`:

```java
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "tipoProducto"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProductoVidrio.class, name = "VIDRIO"),
    @JsonSubTypes.Type(value = Corte.class, name = "CORTE"),
    @JsonSubTypes.Type(value = Producto.class, name = "PRODUCTO")
})
@Entity
@Table(name = "productos")
@Inheritance(strategy = InheritanceType.JOINED)
public class Producto {
    // ...
}
```

**Problema con esta solución**: Requiere que el frontend envíe el campo `tipoProducto` en el JSON.

### SOLUCIÓN 2: Detectar tipo basándose en campos presentes (ALTERNATIVA)

**Problema**: No queremos cambiar el JSON del frontend.

**Solución**: Crear un deserializador personalizado que detecte el tipo basándose en los campos presentes:
- Si tiene `mm`, `m1`, `m2` → `ProductoVidrio`
- Si tiene `largoCm` → `Corte`
- Si no → `Producto`

### SOLUCIÓN 3: Verificar y convertir en el controlador (TEMPORAL)

**Problema**: El objeto llega como `Producto` pero debería ser `ProductoVidrio`.

**Solución**: En el controlador, verificar si tiene los campos de vidrio y crear un nuevo `ProductoVidrio`:

```java
@PostMapping
public ResponseEntity<?> crear(@RequestBody Producto producto) {
    // Si tiene campos de vidrio, convertir a ProductoVidrio
    if (producto instanceof ProductoVidrio) {
        return ResponseEntity.ok(service.guardar((ProductoVidrio) producto));
    } else if (tieneCamposVidrio(producto)) {
        ProductoVidrio pv = convertirAVidrio(producto);
        return ResponseEntity.ok(service.guardar(pv));
    }
    // ...
}
```

## 🎯 CONCLUSIÓN

**Causa raíz más probable**: Jackson está deserializando el JSON como `Producto` en lugar de `ProductoVidrio` porque no hay información de tipo en el JSON y Jackson no puede inferir el tipo basándose solo en los campos.

**Solución recomendada**: Configurar Jackson para que detecte el tipo basándose en los campos presentes, o usar un deserializador personalizado.

**Verificación necesaria**: 
1. Agregar logs en el controlador para verificar el tipo de objeto recibido
2. Verificar qué endpoint está usando el frontend
3. Verificar si los logs de `ProductoVidrioService.guardar()` se están ejecutando

