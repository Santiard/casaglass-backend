# 🔒 Endpoint de Confirmación de Traslados

## 🎯 **Nuevo Endpoint Implementado**

### `PUT /api/traslados-movimientos/{id}/confirmar`

**Propósito:** Confirmar un traslado estableciendo el trabajador responsable y la fecha de confirmación automática.

---

## 📋 **Especificación Completa**

### **URL:** 
```
PUT /api/traslados-movimientos/{id}/confirmar
```

### **Request Body:**
```json
{
  "trabajadorId": 25
}
```

### **Response Exitoso (200):**
```json
{
  "message": "Traslado confirmado exitosamente",
  "traslado": {
    "id": 1,
    "fecha": "2025-01-15",
    "sedeOrigen": {
      "id": 1,
      "nombre": "Centro"
    },
    "sedeDestino": {
      "id": 2,
      "nombre": "Ínsula"
    },
    "trabajadorConfirmacion": {
      "id": 25,
      "nombre": "Juan Pérez Rodríguez"
    },
    "fechaConfirmacion": "2025-10-13",
    "detalles": [
      {
        "id": 10,
        "cantidad": 5,
        "producto": {
          "id": 100,
          "nombre": "Vidrio Templado 6mm",
          "codigo": "VT-001",
          "categoria": "Vidrios"
        }
      }
    ]
  }
}
```

### **Response Error (400 - Bad Request):**
```json
{
  "timestamp": "2025-10-13T22:20:00",
  "status": 400,
  "error": "Bad Request"
}
```

### **Response Error (404 - Not Found):**
```json
{
  "timestamp": "2025-10-13T22:20:00", 
  "status": 404,
  "error": "Not Found"
}
```

---

## 🚀 **Ejemplos de Uso**

### **JavaScript/Fetch API**

```javascript
const confirmarTraslado = async (trasladoId, trabajadorId) => {
  try {
    const response = await fetch(`/api/traslados-movimientos/${trasladoId}/confirmar`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        trabajadorId: trabajadorId
      })
    });

    if (!response.ok) {
      throw new Error(`Error ${response.status}: ${response.statusText}`);
    }

    const result = await response.json();
    
    console.log(result.message); // "Traslado confirmado exitosamente"
    console.log('Traslado confirmado:', result.traslado);
    
    return result;
  } catch (error) {
    console.error('Error al confirmar traslado:', error);
    throw error;
  }
};

// Uso del función
confirmarTraslado(1, 25)
  .then(result => {
    alert(result.message);
    // Actualizar UI con result.traslado
  })
  .catch(error => {
    alert('Error al confirmar traslado');
  });
```

### **Axios**

```javascript
const confirmarTrasladoAxios = async (trasladoId, trabajadorId) => {
  try {
    const response = await axios.put(
      `/api/traslados-movimientos/${trasladoId}/confirmar`,
      { trabajadorId }
    );
    
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      throw new Error('Traslado no encontrado');
    } else if (error.response?.status === 400) {
      throw new Error('Datos inválidos');
    }
    throw error;
  }
};
```

### **React Hook**

```javascript
const useConfirmarTraslado = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const confirmar = async (trasladoId, trabajadorId) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`/api/traslados-movimientos/${trasladoId}/confirmar`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ trabajadorId })
      });

      if (!response.ok) {
        throw new Error(`Error ${response.status}`);
      }

      const result = await response.json();
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { confirmar, loading, error };
};

// Uso en componente
const ConfirmarTrasladoButton = ({ trasladoId, trabajadorId, onConfirmado }) => {
  const { confirmar, loading, error } = useConfirmarTraslado();

  const handleConfirmar = async () => {
    try {
      const result = await confirmar(trasladoId, trabajadorId);
      alert(result.message);
      onConfirmado?.(result.traslado);
    } catch (error) {
      alert('Error al confirmar traslado');
    }
  };

  return (
    <div>
      <button onClick={handleConfirmar} disabled={loading}>
        {loading ? 'Confirmando...' : 'Confirmar Traslado'}
      </button>
      {error && <div className="error">Error: {error}</div>}
    </div>
  );
};
```

---

## 🔧 **Detalles Técnicos**

### **Funcionalidad Interna:**

1. **Validación:** Verifica que `trabajadorId` esté presente en el request
2. **Confirmación:** Establece el trabajador de confirmación en el traslado
3. **Fecha automática:** Asigna `fechaConfirmacion` con la fecha actual (LocalDate.now())
4. **Respuesta consolidada:** Retorna el traslado completo con toda la información actualizada

### **Campos Actualizados Automáticamente:**

- ✅ **`trabajadorConfirmacion`** - Se establece con el ID proporcionado
- ✅ **`fechaConfirmacion`** - Se establece con la fecha actual
- ✅ **Response completa** - Incluye toda la información consolidada del traslado

### **Validaciones:**

- ❌ **trabajadorId nulo** → 400 Bad Request
- ❌ **Traslado no existe** → 404 Not Found
- ❌ **Trabajador no existe** → 404 Not Found

---

## 🎨 **Componente Completo de Confirmación**

```jsx
const TrasladoCard = ({ traslado, onTrasladoActualizado }) => {
  const { confirmar, loading } = useConfirmarTraslado();
  const [trabajadorId, setTrabajadorId] = useState('');

  const handleConfirmar = async () => {
    if (!trabajadorId) {
      alert('Seleccione un trabajador');
      return;
    }

    try {
      const result = await confirmar(traslado.id, parseInt(trabajadorId));
      
      // Mostrar mensaje de éxito
      toast.success(result.message);
      
      // Actualizar la lista de traslados en el componente padre
      onTrasladoActualizado?.(result.traslado);
      
    } catch (error) {
      toast.error('Error al confirmar traslado');
    }
  };

  return (
    <div className="traslado-card">
      <h3>Traslado #{traslado.id}</h3>
      
      <div className="info">
        <p>📅 Fecha: {traslado.fecha}</p>
        <p>📍 De: {traslado.sedeOrigen.nombre}</p>
        <p>🎯 Para: {traslado.sedeDestino.nombre}</p>
      </div>

      {traslado.trabajadorConfirmacion ? (
        <div className="confirmado">
          ✅ Confirmado por: {traslado.trabajadorConfirmacion.nombre}
          <br />
          📅 Fecha: {traslado.fechaConfirmacion}
        </div>
      ) : (
        <div className="pendiente">
          <p>⏳ Pendiente de confirmación</p>
          
          <div className="confirmar-section">
            <select 
              value={trabajadorId} 
              onChange={(e) => setTrabajadorId(e.target.value)}
              disabled={loading}
            >
              <option value="">Seleccionar trabajador...</option>
              <option value="25">Juan Pérez Rodríguez</option>
              <option value="26">María García López</option>
              <option value="27">Carlos Ruiz Mendoza</option>
            </select>
            
            <button 
              onClick={handleConfirmar}
              disabled={loading || !trabajadorId}
              className="btn-confirmar"
            >
              {loading ? 'Confirmando...' : 'Confirmar Traslado'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
```

---

## 📊 **Diferencias con Endpoint Anterior**

### **Endpoint Anterior** (aún funciona):
```http
POST /api/traslados/{id}/confirmar?trabajadorId=25
```

### **Nuevo Endpoint Mejorado:**
```http
PUT /api/traslados-movimientos/{id}/confirmar
Content-Type: application/json

{ "trabajadorId": 25 }
```

### **Ventajas del Nuevo Endpoint:**

1. ✅ **Método semántico correcto** (PUT para actualización)
2. ✅ **Body JSON estructurado** (mejor que query params)
3. ✅ **Respuesta completa** con mensaje y objeto actualizado
4. ✅ **DTO optimizado** para frontend
5. ✅ **Manejo de errores mejorado**

---

## ✅ **Estado de Implementación**

- ✅ **Endpoint implementado:** `PUT /api/traslados-movimientos/{id}/confirmar`
- ✅ **DTOs creados:** `ConfirmarTrasladoRequest` y `ConfirmarTrasladoResponse`
- ✅ **Servicio actualizado** con método de confirmación
- ✅ **Compilación exitosa** - 105 archivos
- ✅ **Validaciones incluidas** para casos de error
- ✅ **Respuesta estructurada** según especificación

### **Listo para usar:**
```bash
curl -X PUT "http://localhost:8080/api/traslados-movimientos/1/confirmar" \
  -H "Content-Type: application/json" \
  -d '{"trabajadorId": 25}'
```

¡El endpoint de confirmación está listo y funcionando! 🚀