# 📋 Documentación de Endpoints de Inventario Completo

## 🎯 Objetivo
Los endpoints de inventario completo fueron creados para **optimizar las llamadas HTTP del frontend**, consolidando múltiples entidades en una sola respuesta para evitar llamadas repetidas.

---

## 📦 Estructura de DTOs

### 🔹 ProductoInventarioCompletoDTO

**Propósito:** Consolidar información de productos, inventarios y vidrios en una sola respuesta.

```java
public class ProductoInventarioCompletoDTO {
    // Datos del producto
    private Long productoId;
    private String codigo;
    private String nombre;
    private String posicion;
    private String tipo;          // 🆕 ENUM: MATE, BLANCO, NEGRO, BRONCE, NATURAL, NA
    private String color;         // 🆕 ENUM: MATE, BLANCO, NEGRO, BRONCE, NATURAL, NA
    private String categoria;
    private String descripcion;
    private Double costo;
    private Double precio1;
    private Double precio2;
    private Double precio3;
    private Double precioEspecial;

    // Datos del inventario consolidado
    private Integer cantidadTotal;  // ⚡ Cálculo automático
    private List<InventarioProductoDTO> inventarios;

    // Datos específicos de vidrio (si aplica)
    private Double largoCm;
    private Double anchoCm;
    private Double grosorMm;
}
```

### 🔹 CorteInventarioCompletoDTO

**Propósito:** Información específica para productos tipo "corte" con campos adicionales.

```java
public class CorteInventarioCompletoDTO {
    // Hereda todos los campos de ProductoInventarioCompletoDTO
    // Campos específicos de corte:
    private Double largoCm;      // Dimensión específica del corte
    private Double precio;       // Precio específico del corte
    private String observacion;  // Observaciones del corte
}
```

---

## 🌐 Endpoints Disponibles

### 📋 **Inventario Completo de Productos**

#### `GET /api/inventario-completo`
Obtiene **todos los productos** con su información consolidada.

**Respuesta:**
```json
[
  {
    "productoId": 1,
    "codigo": "VID001",
    "nombre": "Vidrio Templado",
    "posicion": "A1",
    "tipo": "MATE",
    "color": "BLANCO",
    "categoria": "Vidrios",
    "descripcion": "Vidrio templado de alta calidad",
    "costo": 150.00,
    "precio1": 200.00,
    "precio2": 180.00,
    "precio3": 160.00,
    "precioEspecial": 140.00,
    "cantidadTotal": 25,
    "inventarios": [
      {
        "sedeId": 1,
        "sedeName": "Sede Central",
        "cantidad": 15
      },
      {
        "sedeId": 2,
        "sedeName": "Sucursal Norte",
        "cantidad": 10
      }
    ],
    "largoCm": 100.0,
    "anchoCm": 80.0,
    "grosorMm": 6.0
  }
]
```

#### `GET /api/inventario-completo/sede/{sedeId}`
Obtiene productos **filtrados por sede específica**.

#### `GET /api/inventario-completo/tipo/{tipo}`
Obtiene productos **filtrados por tipo**. 
**Valores permitidos:** `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NATURAL`, `NA`

#### `GET /api/inventario-completo/color/{color}`
Obtiene productos **filtrados por color**.
**Valores permitidos:** `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NATURAL`, `NA`

---

### ✂️ **Inventario Completo de Cortes**

#### `GET /api/cortes-inventario-completo`
Obtiene **todos los cortes** con información consolidada específica.

**Respuesta:**
```json
[
  {
    "productoId": 5,
    "codigo": "COR001",
    "nombre": "Corte Especial",
    "posicion": "C2",
    "tipo": "NEGRO",
    "color": "NEGRO",
    "categoria": "Cortes",
    "descripcion": "Corte personalizado",
    "costo": 80.00,
    "precio1": 120.00,
    "precio2": 110.00,
    "precio3": 100.00,
    "precioEspecial": 90.00,
    "cantidadTotal": 8,
    "inventarios": [
      {
        "sedeId": 1,
        "sedeName": "Sede Central",
        "cantidad": 5
      },
      {
        "sedeId": 3,
        "sedeName": "Taller",
        "cantidad": 3
      }
    ],
    "largoCm": 50.0,
    "precio": 120.00,
    "observacion": "Corte con acabado especial"
  }
]
```

#### `GET /api/cortes-inventario-completo/sede/{sedeId}`
Obtiene cortes **filtrados por sede específica**.

#### `GET /api/cortes-inventario-completo/tipo/{tipo}`
Obtiene cortes **filtrados por tipo**.

#### `GET /api/cortes-inventario-completo/color/{color}`
Obtiene cortes **filtrados por color**.

---

## 🎨 **Gestión de Enums**

### Endpoints para Tipos de Producto

#### `GET /api/tipo-producto/tipos`
```json
["MATE", "BLANCO", "NEGRO", "BRONCE", "NATURAL", "NA"]
```

#### `GET /api/tipo-producto/colores`
```json
["MATE", "BLANCO", "NEGRO", "BRONCE", "NATURAL", "NA"]
```

---

## 🚀 **Ventajas para el Frontend**

### ✅ **Antes vs Ahora**

**❌ ANTES (Múltiples llamadas):**
```javascript
// 3+ llamadas HTTP separadas
const productos = await fetch('/api/productos');
const inventarios = await fetch('/api/inventarios');
const vidrios = await fetch('/api/producto-vidrios');
const sedes = await fetch('/api/sedes');

// Lógica de combinación en frontend
const resultado = combinarDatos(productos, inventarios, vidrios, sedes);
```

**✅ AHORA (Una sola llamada):**
```javascript
// 1 sola llamada HTTP
const inventarioCompleto = await fetch('/api/inventario-completo');
// Datos ya consolidados y listos para usar
```

### 🎯 **Casos de Uso Recomendados**

1. **Pantalla Principal de Inventario:** `GET /api/inventario-completo`
2. **Inventario por Sucursal:** `GET /api/inventario-completo/sede/1`
3. **Filtro por Tipo de Material:** `GET /api/inventario-completo/tipo/MATE`
4. **Filtro por Color:** `GET /api/inventario-completo/color/BLANCO`
5. **Gestión de Cortes:** `GET /api/cortes-inventario-completo`

### 📊 **Optimizaciones Incluidas**

- **Cálculo automático** de `cantidadTotal` (suma de todas las sedes)
- **Carga eager** de relaciones para evitar N+1 queries
- **Agrupación por sede** en el campo `inventarios`
- **Enums validados** para tipos y colores
- **Información consolidada** de productos y sus dimensiones

---

## 🔧 **Configuración de Frontend**

### Ejemplo de Integración (React/Vue)

```javascript
// Hook para inventario completo
const useInventarioCompleto = () => {
  const [inventario, setInventario] = useState([]);
  
  const cargarInventario = async (filtros = {}) => {
    let url = '/api/inventario-completo';
    
    if (filtros.sede) url += `/sede/${filtros.sede}`;
    if (filtros.tipo) url += `/tipo/${filtros.tipo}`;
    if (filtros.color) url += `/color/${filtros.color}`;
    
    const response = await fetch(url);
    const data = await response.json();
    setInventario(data);
  };
  
  return { inventario, cargarInventario };
};

// Hook para cortes
const useCorteInventario = () => {
  const [cortes, setCortes] = useState([]);
  
  const cargarCortes = async (filtros = {}) => {
    let url = '/api/cortes-inventario-completo';
    
    if (filtros.sede) url += `/sede/${filtros.sede}`;
    if (filtros.tipo) url += `/tipo/${filtros.tipo}`;
    if (filtros.color) url += `/color/${filtros.color}`;
    
    const response = await fetch(url);
    const data = await response.json();
    setCortes(data);
  };
  
  return { cortes, cargarCortes };
};
```

### Filtros Disponibles
```javascript
const TIPOS_DISPONIBLES = ['MATE', 'BLANCO', 'NEGRO', 'BRONCE', 'NATURAL', 'NA'];
const COLORES_DISPONIBLES = ['MATE', 'BLANCO', 'NEGRO', 'BRONCE', 'NATURAL', 'NA'];
```

---

## 🎉 **Resultado Final**

Con estos cambios, tu frontend puede:

1. **Reducir llamadas HTTP** de 3-4 a 1 sola llamada
2. **Filtrar eficientemente** por sede, tipo o color
3. **Obtener datos consolidados** listos para mostrar
4. **Usar enums validados** para tipos y colores
5. **Diferenciar productos y cortes** con endpoints específicos

¡Los endpoints están listos para ser consumidos por tu aplicación frontend! 🚀