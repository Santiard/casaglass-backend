# ✅ VERIFICACIÓN: Variables de Entorno en Producción

## 🎯 RESPUESTA DIRECTA

**SÍ, las variables de entorno se setean y son bien recibidas por Spring Boot**, PERO necesitas verificar que:

1. ✅ Las variables estén configuradas en tu plataforma de despliegue
2. ✅ Spring Boot las lea correctamente al iniciar
3. ✅ La conexión a la base de datos funcione

---

## 🔍 CÓMO SPRING BOOT LEE LAS VARIABLES DE ENTORNO

### Configuración Actual

```properties
# application.properties
spring.datasource.url=${SPRING_DATASOURCE_URL:}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

**Sintaxis `${VARIABLE:default}`:**
- Si `SPRING_DATASOURCE_URL` existe → usa su valor
- Si NO existe → usa `""` (cadena vacía) → **ERROR**

---

## ✅ VERIFICACIÓN PASO A PASO

### 1. Verificar que las Variables Estén Configuradas

#### En Docker/Docker Compose:
```bash
# Verificar variables del contenedor
docker exec casaglass-backend env | grep SPRING_DATASOURCE
```

#### En plataformas cloud:
- **Heroku**: `heroku config` o en Settings → Config Vars
- **AWS**: Verificar en Environment Properties
- **Azure**: Verificar en Application Settings
- **Google Cloud**: Verificar en Environment Variables

### 2. Verificar Logs de Inicio de la Aplicación

Spring Boot muestra información sobre la conexión a la base de datos al iniciar:

#### ✅ LOGS CORRECTOS (Variables bien recibidas):
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

O si hay problemas de conexión:
```
HikariPool-1 - Exception during pool initialization.
java.sql.SQLException: Access denied for user...
```

#### ❌ LOGS DE ERROR (Variables NO recibidas):
```
Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.
```

O:
```
Failed to bind properties under 'spring.datasource.url' to java.lang.String
```

### 3. Verificar que el Perfil de Producción Esté Activo

En los logs de inicio, buscar:
```
The following profiles are active: prod
```

Si no aparece, la aplicación está usando el perfil `dev` (por defecto).

---

## 🔧 CÓMO DIAGNOSTICAR PROBLEMAS

### Problema 1: Variables NO están configuradas

**Síntoma:**
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**Solución:**
1. Configurar las variables en tu plataforma de despliegue
2. Reiniciar la aplicación

### Problema 2: Variables están configuradas pero con valores incorrectos

**Síntoma:**
```
HikariPool-1 - Exception during pool initialization.
java.sql.SQLException: Access denied for user...
```

**Solución:**
1. Verificar que `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` sean correctos
2. Verificar que el usuario tenga permisos en la base de datos

### Problema 3: URL incorrecta

**Síntoma:**
```
java.sql.SQLException: Could not connect to address=(host=...)(port=...)(type=master)
```

**Solución:**
1. Verificar que `SPRING_DATASOURCE_URL` tenga el formato correcto:
   ```
   jdbc:mariadb://host:puerto/base-datos?parametros
   ```
2. Verificar que el servidor de base de datos sea accesible desde el contenedor/servidor

---

## 📝 EJEMPLO DE CONFIGURACIÓN CORRECTA

### Docker Compose (Producción)

```yaml
services:
  backend:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mariadb://tu-servidor:3306/tu-base?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=tu-usuario
      - SPRING_DATASOURCE_PASSWORD=tu-password-seguro
```

### Variables de Entorno del Sistema

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:mariadb://tu-servidor:3306/tu-base?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
export SPRING_DATASOURCE_USERNAME=tu-usuario
export SPRING_DATASOURCE_PASSWORD=tu-password-seguro
```

---

## 🧪 TEST RÁPIDO: Verificar que Funciona

### Opción 1: Endpoint de Health Check

Si tienes Actuator configurado:
```bash
curl http://tu-servidor:8080/actuator/health
```

Respuesta esperada:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

### Opción 2: Verificar Logs de Inicio

Buscar en los logs:
1. ✅ `The following profiles are active: prod`
2. ✅ `HikariPool-1 - Starting...`
3. ✅ `HikariPool-1 - Start completed.`
4. ✅ `Started CasaglassBackendApplication`

Si ves estos mensajes, **las variables están siendo recibidas correctamente**.

---

## ⚠️ NOTAS IMPORTANTES

### 1. Orden de Precedencia de Spring Boot

Spring Boot lee las variables en este orden (mayor prioridad primero):
1. Variables de entorno del sistema
2. Variables de entorno del contenedor (Docker)
3. `application-{profile}.properties`
4. `application.properties`

**Tu configuración actual:**
- Variables de entorno → ✅ Mayor prioridad
- `application-prod.properties` → No tiene URLs (correcto)
- `application.properties` → Usa variables de entorno (correcto)

### 2. El Dockerfile NO establece las variables de datasource

```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
# ❌ NO establece SPRING_DATASOURCE_URL
```

**Esto es CORRECTO** porque:
- Las credenciales NO deben estar en el Dockerfile
- Deben venir de fuera (docker-compose, plataforma cloud, etc.)

### 3. Formato de la URL

La URL debe tener este formato:
```
jdbc:mariadb://host:puerto/nombre-base-datos?parametros
```

Parámetros recomendados:
- `useUnicode=true`
- `characterEncoding=utf8` o `characterEncoding=UTF-8`
- `useSSL=false` (si no usas SSL)
- `allowPublicKeyRetrieval=true` (si es necesario)

---

## 🎯 CHECKLIST DE VERIFICACIÓN

- [ ] Variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` configuradas
- [ ] Variable `SPRING_PROFILES_ACTIVE=prod` configurada
- [ ] Logs muestran `The following profiles are active: prod`
- [ ] Logs muestran `HikariPool-1 - Start completed.`
- [ ] No hay errores de conexión a base de datos
- [ ] Endpoint `/actuator/health` responde con `"status": "UP"`
- [ ] La aplicación puede realizar operaciones de base de datos

---

## 📞 SI ALGO NO FUNCIONA

1. **Revisar logs completos** de inicio de la aplicación
2. **Verificar variables de entorno** en tu plataforma de despliegue
3. **Probar conexión manual** a la base de datos desde el servidor
4. **Verificar permisos** del usuario de base de datos
5. **Verificar firewall/red** que permita conexión al servidor de base de datos

---

## ✅ CONCLUSIÓN

**SÍ, las variables de entorno se setean y son bien recibidas por Spring Boot**, siempre y cuando:

1. ✅ Estén configuradas en tu plataforma de despliegue
2. ✅ Tengan los valores correctos
3. ✅ El servidor de base de datos sea accesible

**Para verificar que todo funciona:**
- Revisar logs de inicio
- Probar endpoint de health check
- Verificar que la aplicación pueda conectarse a la base de datos



