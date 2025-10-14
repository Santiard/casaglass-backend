# 🔄 Guía de Migración Frontend - Endpoints de Inventario

## 📋 **Resumen de Cambios**

Esta documentación explica cómo migrar tu frontend para usar los nuevos endpoints optimizados de inventario completo.

---

## 🎯 **Cambios Principales**

### 1️⃣ **Nuevos Campos Enum**
- **`tipo`**: Enum con valores `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NATURAL`, `NA`
- **`color`**: Enum con valores `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NATURAL`, `NA`

### 2️⃣ **Endpoints Consolidados**
- **Productos:** `/api/inventario-completo`
- **Cortes:** `/api/corte-inventario-completo`

### 3️⃣ **Información Consolidada**
- Cantidad total calculada automáticamente
- Inventarios agrupados por sede
- Información de categorías incluida

---

## 🔄 **Migración Paso a Paso**

### **PASO 1: Actualizar Llamadas HTTP**

#### ❌ **ANTES**
```javascript
// Múltiples llamadas separadas
const fetchInventarioAnterior = async () => {
  const productos = await fetch('/api/productos');
  const inventarios = await fetch('/api/inventarios');
  const vidrios = await fetch('/api/producto-vidrios');
  
  // Lógica compleja de combinación
  return combinarDatos(productos, inventarios, vidrios);
};
```

#### ✅ **AHORA**
```javascript
// Una sola llamada optimizada
const fetchInventarioNuevo = async () => {
  return await fetch('/api/inventario-completo');
};
```

### **PASO 2: Actualizar Estructura de Datos**

#### ❌ **ESTRUCTURA ANTERIOR**
```javascript
const producto = {
  id: 1,
  codigo: "VID001",
  nombre: "Vidrio Templado",
  tipo: "mate",           // String libre
  color: "blanco mate",   // String libre
  // ... inventarios separados
};
```

#### ✅ **NUEVA ESTRUCTURA**
```javascript
const productoCompleto = {
  productoId: 1,
  codigo: "VID001",
  nombre: "Vidrio Templado",
  tipo: "MATE",          // Enum validado
  color: "BLANCO",       // Enum validado
  categoria: "Vidrios",  // Información de categoría incluida
  cantidadTotal: 25,     // Calculado automáticamente
  inventarios: [         // Agrupado por sede
    {
      sedeId: 1,
      sedeName: "Sede Central",
      cantidad: 15
    },
    {
      sedeId: 2, 
      sedeName: "Sucursal Norte",
      cantidad: 10
    }
  ]
  // ... más campos consolidados
};
```

### **PASO 3: Implementar Filtros**

```javascript
// Servicio de inventario actualizado
class InventarioService {
  
  // Obtener todo el inventario
  async obtenerTodo() {
    return fetch('/api/inventario-completo');
  }
  
  // Filtrar por sede
  async filtrarPorSede(sedeId) {
    return fetch(`/api/inventario-completo/sede/${sedeId}`);
  }
  
  // Filtrar por tipo
  async filtrarPorTipo(tipo) {
    // tipo debe ser: MATE, BLANCO, NEGRO, BRONCE, NATURAL, NA
    return fetch(`/api/inventario-completo/tipo/${tipo}`);
  }
  
  // Filtrar por color
  async filtrarPorColor(color) {
    // color debe ser: MATE, BLANCO, NEGRO, BRONCE, NATURAL, NA
    return fetch(`/api/inventario-completo/color/${color}`);
  }
  
  // Obtener cortes específicos
  async obtenerCortes() {
    return fetch('/api/corte-inventario-completo');
  }
}
```

### **PASO 4: Actualizar Componentes**

#### **Componente de Lista de Inventario**

```javascript
// React Hook personalizado
const useInventarioCompleto = () => {
  const [inventario, setInventario] = useState([]);
  const [loading, setLoading] = useState(false);
  
  const cargarInventario = async (filtros = {}) => {
    setLoading(true);
    try {
      let url = '/api/inventario-completo';
      
      // Aplicar filtros dinámicamente
      if (filtros.sede) url += `/sede/${filtros.sede}`;
      else if (filtros.tipo) url += `/tipo/${filtros.tipo}`;
      else if (filtros.color) url += `/color/${filtros.color}`;
      
      const response = await fetch(url);
      const data = await response.json();
      setInventario(data);
    } catch (error) {
      console.error('Error cargando inventario:', error);
    } finally {
      setLoading(false);
    }
  };
  
  return { inventario, loading, cargarInventario };
};

// Componente de filtros
const FiltrosInventario = ({ onFiltroChange }) => {
  const TIPOS = ['MATE', 'BLANCO', 'NEGRO', 'BRONCE', 'NATURAL', 'NA'];
  const COLORES = ['MATE', 'BLANCO', 'NEGRO', 'BRONCE', 'NATURAL', 'NA'];
  
  return (
    <div className="filtros">
      <select onChange={(e) => onFiltroChange({tipo: e.target.value})}>
        <option value="">Todos los tipos</option>
        {TIPOS.map(tipo => (
          <option key={tipo} value={tipo}>{tipo}</option>
        ))}
      </select>
      
      <select onChange={(e) => onFiltroChange({color: e.target.value})}>
        <option value="">Todos los colores</option>
        {COLORES.map(color => (
          <option key={color} value={color}>{color}</option>
        ))}
      </select>
    </div>
  );
};
```

#### **Componente de Producto Mejorado**

```javascript
const ProductoCard = ({ producto }) => {
  return (
    <div className="producto-card">
      <h3>{producto.nombre}</h3>
      <p><strong>Código:</strong> {producto.codigo}</p>
      <p><strong>Tipo:</strong> {producto.tipo}</p>
      <p><strong>Color:</strong> {producto.color}</p>
      <p><strong>Categoría:</strong> {producto.categoria}</p>
      
      {/* Cantidad total ya calculada */}
      <div className="cantidad-total">
        <strong>Stock Total: {producto.cantidadTotal}</strong>
      </div>
      
      {/* Desglose por sede */}
      <div className="inventarios">
        <h4>Por Sede:</h4>
        {producto.inventarios.map(inv => (
          <div key={inv.sedeId}>
            {inv.sedeName}: {inv.cantidad} unidades
          </div>
        ))}
      </div>
      
      {/* Precios */}
      <div className="precios">
        <p>Precio 1: ${producto.precio1}</p>
        <p>Precio 2: ${producto.precio2}</p>
        <p>Precio 3: ${producto.precio3}</p>
        {producto.precioEspecial && (
          <p>Especial: ${producto.precioEspecial}</p>
        )}
      </div>
    </div>
  );
};
```

---

## 🎨 **Componente de Gestión de Cortes**

```javascript
const useCorteInventario = () => {
  const [cortes, setCortes] = useState([]);
  
  const cargarCortes = async (filtros = {}) => {
    let url = '/api/corte-inventario-completo';
    
    if (filtros.sede) url += `/sede/${filtros.sede}`;
    else if (filtros.tipo) url += `/tipo/${filtros.tipo}`;
    else if (filtros.color) url += `/color/${filtros.color}`;
    
    const response = await fetch(url);
    const data = await response.json();
    setCortes(data);
  };
  
  return { cortes, cargarCortes };
};

const CorteCard = ({ corte }) => {
  return (
    <div className="corte-card">
      <h3>{corte.nombre}</h3>
      <p><strong>Código:</strong> {corte.codigo}</p>
      <p><strong>Tipo:</strong> {corte.tipo}</p>
      <p><strong>Color:</strong> {corte.color}</p>
      
      {/* Campos específicos de corte */}
      <div className="dimensiones-corte">
        <p><strong>Largo:</strong> {corte.largoCm} cm</p>
        <p><strong>Precio:</strong> ${corte.precio}</p>
        {corte.observacion && (
          <p><strong>Observación:</strong> {corte.observacion}</p>
        )}
      </div>
      
      <div className="stock-corte">
        <strong>Stock Total: {corte.cantidadTotal}</strong>
      </div>
    </div>
  );
};
```

---

## ⚡ **Optimizaciones Implementadas**

### 1️⃣ **Rendimiento**
- **Menos llamadas HTTP:** De 3-4 llamadas a 1 sola
- **Carga eager:** Relaciones precargadas para evitar N+1 queries
- **Datos consolidados:** Frontend recibe información lista para usar

### 2️⃣ **Validación**
- **Enums validados:** Tipos y colores con valores predefinidos
- **Consistencia:** Mismos valores para tipo y color garantizan coherencia

### 3️⃣ **Funcionalidad**
- **Filtrado eficiente:** Por sede, tipo o color desde el backend
- **Cálculos automáticos:** Cantidad total precalculada
- **Agrupación inteligente:** Inventarios agrupados por sede

---

## 🚀 **Pasos para Implementar**

### **Checklist de Migración**

- [ ] **Actualizar servicios de API** para usar nuevos endpoints
- [ ] **Modificar componentes** para usar nueva estructura de datos  
- [ ] **Implementar filtros** con valores enum válidos
- [ ] **Actualizar tests** para nuevos formatos de respuesta
- [ ] **Verificar tipos TypeScript** si usas TypeScript
- [ ] **Probar funcionalidad** de filtros y carga de datos

### **Validación Final**

```javascript
// Función de prueba para validar migración
const validarMigracion = async () => {
  console.log('🧪 Probando nuevos endpoints...');
  
  // Test 1: Inventario completo
  const inventario = await fetch('/api/inventario-completo');
  console.log('✅ Inventario completo:', inventario.length, 'productos');
  
  // Test 2: Filtro por tipo
  const porTipo = await fetch('/api/inventario-completo/tipo/MATE');
  console.log('✅ Filtro por tipo MATE:', porTipo.length, 'productos');
  
  // Test 3: Cortes
  const cortes = await fetch('/api/corte-inventario-completo');
  console.log('✅ Inventario de cortes:', cortes.length, 'cortes');
  
  console.log('🎉 Migración completada exitosamente!');
};
```

---

## 📞 **Soporte de Migración**

Si encuentras algún problema durante la migración:

1. **Verifica los valores enum** sean exactamente: `MATE`, `BLANCO`, `NEGRO`, `BRONCE`, `NATURAL`, `NA`
2. **Confirma las URLs** de los endpoints según esta documentación
3. **Revisa la estructura** de respuesta contra los ejemplos proporcionados

¡Tu frontend ahora está optimizado para un mejor rendimiento! 🚀