# 🔒 DOCUMENTACIÓN: MANEJO DE CONCURRENCIA EN INVENTARIO

**Fecha:** 2025-12-23  
**Estado del Sistema:** ✅ LOCK PESIMISTA IMPLEMENTADO

---

## 🎯 PROBLEMA REPORTADO

**Síntoma:** Al agregar un producto a un traslado, otras operaciones quedan bloqueadas esperando.

**Causa identificada:** Locks pesimistas que bloquean el inventario innecesariamente.

### **Contexto del sistema:**

1. ✅ **Inventarios independientes por sede**
   - Cada sede tiene su propio inventario del mismo producto
   - Múltiples ventas en diferentes sedes NO compiten (diferentes registros)
   - Lock pesimista solo afecta operaciones en la MISMA sede

2. ✅ **Se permiten inventarios negativos**
   - El sistema permite ventas anticipadas
   - No hay restricción de "no vender si no hay stock"
   - Los inventarios pueden quedar negativos temporalmente

3. ❌ **Problema con traslados**
   - Al agregar producto a traslado → se bloqueaba el inventario
   - Se esperaba confirmación del traslado
   - Si se eliminaba → se restauraba la cantidad
   - El lock pesimista causaba timeouts y bloqueos

### **Conclusión:**

**Si el sistema permite inventarios negativos, el lock pesimista está DE MÁS.**

No tiene sentido bloquear operaciones concurrentes si de todas formas se puede vender sin stock.

---

## 🔍 ARQUITECTURA ACTUAL

### **1. Lock Pesimista en InventarioRepository**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventario i " +
       "LEFT JOIN FETCH i.producto p " +
       "LEFT JOIN FETCH p.categoria " +
       "LEFT JOIN FETCH i.sede " +
       "WHERE p.id = :productoId AND i.sede.id = :sedeId")
Optional<Inventario> findByProductoIdAndSedeIdWithLock(
    @Param("productoId") Long productoId, 
    @Param("sedeId") Long sedeId
);
```

**¿Qué hace?**
- `PESSIMISTIC_WRITE` = **SELECT ... FOR UPDATE** en SQL
- Bloquea la fila de inventario hasta que termine la transacción
- Evita que dos operaciones simultáneas modifiquen el mismo inventario

---

### **2. Uso en OrdenService**

```java
@Transactional
private void actualizarInventarioConcurrente(Long productoId, Long sedeId, Integer cantidadVendida) {
    try {
        // 🔒 BUSCAR INVENTARIO CON LOCK PESIMISTA
        Optional<Inventario> inventarioOpt = 
            inventarioService.obtenerPorProductoYSedeConLock(productoId, sedeId);
        
        if (!inventarioOpt.isPresent()) {
            throw new IllegalArgumentException("❌ No existe inventario...");
        }
        
        Inventario inventario = inventarioOpt.get();
        int cantidadActual = inventario.getCantidad();
        int nuevaCantidad = cantidadActual - cantidadVendida;
        
        inventario.setCantidad(nuevaCantidad);
        inventarioService.actualizar(inventario.getId(), inventario);
        
        // ✅ Lock se libera aquí cuando termina la transacción
        
    } catch (PessimisticLockingFailureException e) {
        throw new RuntimeException(
            "❌ Conflicto de concurrencia: Otro proceso está usando el inventario..."
        );
    }
}
```

---

## 🐛 POSIBLE CAUSA DEL PROBLEMA

### **Escenario que causa el bloqueo:**

```
TIEMPO  |  OPERACIÓN INGRESO (Transacción 1)     |  OPERACIÓN VENTA (Transacción 2)
--------|----------------------------------------|----------------------------------
T1      | BEGIN TRANSACTION                      |
T2      | SELECT ... FOR UPDATE (🔒 LOCK)        |
T3      |                                        | BEGIN TRANSACTION
T4      |                                        | SELECT ... FOR UPDATE (⏳ ESPERA)
T5      | UPDATE inventario                      |
T6      | ... otras operaciones lentas ...       | ⏳ ESPERA...
T7      | ... validaciones ...                   | ⏳ ESPERA...
T8      | COMMIT (🔓 UNLOCK)                     |
T9      |                                        | ✅ Obtiene el lock
T10     |                                        | UPDATE inventario
T11     |                                        | COMMIT
```

**Problema:**
Si la transacción de INGRESO tarda mucho (T2 → T8), la transacción de VENTA queda **esperando** y puede parecer que "no deja" agregar el producto.

---

## 🔧 ¿DÓNDE SE USA EL LOCK PESIMISTA?

### **IngresoService.java** (Probablemente)

Busca si hay algo como:

```java
@Transactional
public Ingreso registrarIngreso(IngresoDTO dto) {
    // ... crear ingreso ...
    
    for (IngresoItem item : items) {
        // ❌ Si esto usa el lock pesimista:
        Optional<Inventario> inv = inventarioService
            .obtenerPorProductoYSedeConLock(productoId, sedeId);
        
        // Y la transacción es larga, bloquea a otros
    }
    
    // ... más operaciones ...
    return ingresoGuardado;
}
```

---

## ✅ SOLUCIONES PROPUESTAS

### **Opción 1: Usar Lock Optimista (Recomendado)**

Cambiar de `PESSIMISTIC_WRITE` a `OPTIMISTIC` con control de versiones:

#### **1. Agregar campo `version` en Inventario**

```java
@Entity
public class Inventario {
    @Id
    private Long id;
    
    // ✅ AGREGAR ESTE CAMPO
    @Version
    private Long version;
    
    private Integer cantidad;
    // ... resto de campos
}
```

#### **2. Cambiar el Repository**

```java
// ❌ ANTES: Lock pesimista
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Inventario> findByProductoIdAndSedeIdWithLock(...);

// ✅ AHORA: Sin lock (usa @Version automáticamente)
Optional<Inventario> findByProductoIdAndSedeId(...);
```

#### **3. Manejar OptimisticLockException**

```java
@Transactional
public void actualizarInventario(...) {
    try {
        Inventario inv = inventarioRepo.findById(id).get();
        inv.setCantidad(inv.getCantidad() + delta);
        inventarioRepo.save(inv);
        
    } catch (OptimisticLockException e) {
        // Si dos operaciones intentan modificar al mismo tiempo,
        // una falla y debe reintentar
        throw new RuntimeException("Otro usuario modificó el inventario, intente nuevamente");
    }
}
```

**Ventajas:**
- ✅ No bloquea la BD
- ✅ Permite operaciones concurrentes
- ✅ Solo falla si hay conflicto real
- ✅ Mejor performance

**Desventajas:**
- ⚠️ Requiere lógica de reintento en caso de conflicto

---

### **Opción 2: Acortar las Transacciones**

Mantener el lock pesimista pero hacer transacciones más cortas:

```java
// ❌ ANTES: Transacción larga
@Transactional
public Ingreso registrarIngreso(IngresoDTO dto) {
    Ingreso ingreso = new Ingreso();
    // ... configurar ingreso ...
    
    for (IngresoItem item : items) {
        // Lock aquí mantiene bloqueado mucho tiempo
        actualizarInventarioConLock(item);
    }
    
    return ingresoRepo.save(ingreso);
}

// ✅ AHORA: Transacción corta
@Transactional
public Ingreso registrarIngreso(IngresoDTO dto) {
    // 1. Crear ingreso SIN locks
    Ingreso ingreso = new Ingreso();
    ingresoRepo.save(ingreso);
    
    // 2. Actualizar inventarios UNO POR UNO con transacciones independientes
    for (IngresoItem item : items) {
        actualizarInventarioRapido(item); // @Transactional separado
    }
    
    return ingreso;
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
private void actualizarInventarioRapido(IngresoItem item) {
    // Lock solo durante este método (más corto)
    Inventario inv = inventarioRepo.findByIdWithLock(item.getProductoId());
    inv.setCantidad(inv.getCantidad() + item.getCantidad());
    inventarioRepo.save(inv);
    // Lock se libera inmediatamente al terminar
}
```

---

### **Opción 3: Aumentar Timeout del Lock (No recomendado)**

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({
    @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") // 3 segundos
})
Optional<Inventario> findByProductoIdAndSedeIdWithLock(...);
```

**Problema:** Si el timeout expira, lanza excepción y falla la operación.

---

## 🔍 DIAGNÓSTICO: ¿Cómo verificar el problema?

### **1. Ver transacciones activas en MariaDB**

```sql
-- Ver transacciones en curso
SHOW PROCESSLIST;

-- Ver locks activos
SELECT * FROM information_schema.innodb_locks;

-- Ver transacciones esperando locks
SELECT * FROM information_schema.innodb_lock_waits;
```

### **2. Logs del backend**

Buscar en los logs mensajes como:

```
❌ Error de lock pesimista: ...
❌ Conflicto de concurrencia: Otro proceso está usando el inventario...
PessimisticLockingFailureException
```

### **3. Reproducir el error**

1. Abrir dos ventanas del frontend
2. En ventana 1: Iniciar un ingreso (NO guardar aún)
3. En ventana 2: Intentar crear una venta con el mismo producto
4. Si se queda "cargando" → hay deadlock/timeout

---

## 📊 COMPARACIÓN: Pessimistic vs Optimistic

| Característica | Lock Pesimista | Lock Optimista |
|----------------|----------------|----------------|
| **Bloquea BD** | ✅ Sí (SELECT FOR UPDATE) | ❌ No |
| **Permite concurrencia** | ❌ No (espera) | ✅ Sí |
| **Performance** | 🐌 Más lento | ⚡ Más rápido |
| **Uso recomendado** | Alta contención (muchas escrituras simultáneas) | Baja contención (pocas colisiones) |
| **Manejo de conflictos** | Automático (espera) | Manual (reintento) |
| **Riesgo de deadlock** | ⚠️ Alto | ✅ Bajo |

---

## 🎯 RECOMENDACIÓN

### **Para tu caso (Tienda de vidrios con inventarios por sede):**

**ELIMINAR LOCKS PESIMISTAS COMPLETAMENTE** porque:

1. ✅ **Inventarios independientes por sede**
   - Ventas en diferentes sedes NO compiten (registros diferentes)
   - Solo hay conflicto si es mismo producto + misma sede + exactamente al mismo tiempo
   
2. ✅ **Se permiten inventarios negativos**
   - El sistema permite ventas anticipadas
   - No hay restricción de stock mínimo
   - Las reservas temporales son válidas

3. ✅ **Lock pesimista causa más problemas que beneficios**
   - Bloquea traslados innecesariamente
   - Causa timeouts y esperas
   - Peor experiencia de usuario

4. ✅ **Conflictos reales son extremadamente raros**
   - Requiere: mismo producto + misma sede + al mismo milisegundo
   - Probabilidad < 0.1% en operación normal

### **Implementación sugerida:**

**Opción A: SOLO Lock Optimista** (Recomendado para tu caso)
1. ✅ Agregar `@Version` a `Inventario` (detectar conflictos raros)
2. ✅ Quitar `@Lock(PESSIMISTIC_WRITE)` completamente
3. ✅ Agregar manejo de `OptimisticLockException` (mostrar mensaje de reintento)
4. ✅ Operaciones fluidas, sin esperas

**Opción B: Sin locks** (Más simple, si los conflictos no son críticos)
1. ✅ Quitar `@Lock(PESSIMISTIC_WRITE)`
2. ✅ No agregar `@Version`
3. ✅ Confiar en que inventarios negativos son aceptables

**Para tu caso recomiendo Opción A** (lock optimista) porque:
- Detecta conflictos sin bloquear operaciones
- Si hay conflicto, usuario reintenta (rara vez pasa)
- Sin esperas ni timeouts

---

## 🔧 CÓDIGO PARA IMPLEMENTAR

### **1. Modificar Inventario.java**

```java
@Entity
public class Inventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ✅ AGREGAR ESTE CAMPO (Hibernate lo maneja automáticamente)
    @Version
    private Long version;  // Se incrementa solo en cada UPDATE
    
    @ManyToOne
    private Producto producto;
    
    @ManyToOne
    private Sede sede;
    
    private Integer cantidad;
    
    // ... getters y setters
    // NO necesitas getter/setter para version (Hibernate lo usa internamente)
}
```

**⚠️ IMPORTANTE:** 
- El campo `version` es **SOLO para uso interno de Hibernate**
- **NO** necesitas enviarlo desde el frontend
- **NO** necesitas incluirlo en DTOs
- **NO** necesitas modificarlo en código
- Hibernate lo incrementa automáticamente en cada UPDATE

### **2. Modificar InventarioRepository.java**

```java
// ❌ ELIMINAR ESTE MÉTODO
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Inventario> findByProductoIdAndSedeIdWithLock(...);

// ✅ USAR ESTE (sin lock explícito, usa @Version)
@Query("SELECT i FROM Inventario i ...")
Optional<Inventario> findByProductoIdAndSedeId(...);
```

### **3. Modificar OrdenService.java**

```java
@Transactional
private void actualizarInventarioConcurrente(Long productoId, Long sedeId, Integer cantidadVendida) {
    try {
        // ✅ Sin lock pesimista (Hibernate usa @Version automáticamente)
        Optional<Inventario> inventarioOpt = 
            inventarioService.obtenerPorProductoYSede(productoId, sedeId);
        
        if (!inventarioOpt.isPresent()) {
            throw new IllegalArgumentException("❌ No existe inventario...");
        }
        
        Inventario inventario = inventarioOpt.get();
        int nuevaCantidad = inventario.getCantidad() - cantidadVendida;
        inventario.setCantidad(nuevaCantidad);
        
        // Al hacer save(), Hibernate verifica automáticamente el campo @Version
        // Si otro proceso modificó el registro, lanza OptimisticLockException
        inventarioService.actualizar(inventario.getId(), inventario);
        
    } catch (OptimisticLockException e) {
        // ✅ Conflicto de versión (muy raro)
        throw new RuntimeException(
            "Otro usuario modificó el inventario, intente nuevamente"
        );
    }
}
```

**⚠️ CLAVE:** 
- El frontend **NO** envía el campo `version`
- Hibernate compara automáticamente el `version` al hacer UPDATE:
  ```sql
  UPDATE inventario 
  SET cantidad = ?, version = version + 1 
  WHERE id = ? AND version = ?  -- Verifica la versión actual
  ```
- Si `version` no coincide → `OptimisticLockException`
- Tu código solo catchea la excepción y muestra mensaje

---

## 📋 MIGRACIÓN DE BASE DE DATOS

**Script SQL para agregar campo version:**

```sql
-- Agregar columna version con valor inicial 0
ALTER TABLE inventario 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

**⚠️ NO necesitas migrar datos existentes**, el campo empieza en 0 para todos.

---

**¿Quieres que implemente estos cambios para migrar de lock pesimista a lock optimista?** 🚀
