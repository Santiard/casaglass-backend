# 🔧 CONFIGURACIÓN DE PRODUCCIÓN - URL DE BASE DE DATOS

## 📋 RESUMEN

**✅ NO hay URLs hardcodeadas en el repositorio para producción**

La aplicación está configurada para tomar la URL de base de datos desde **variables de entorno** en el servidor de producción.

---

## 🔍 ANÁLISIS DE CONFIGURACIÓN

### 1. `application.properties` (Archivo principal)

```properties
# Perfil activo (por defecto: dev, pero puede ser sobrescrito)
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}

# URL de base de datos desde variable de entorno
spring.datasource.url=${SPRING_DATASOURCE_URL:}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

**✅ La URL viene de la variable de entorno `SPRING_DATASOURCE_URL`**

---

### 2. `application-prod.properties` (Configuración de producción)

```properties
# Las credenciales de base de datos vienen de variables de entorno
# inyectadas por Docker Compose:
# - SPRING_DATASOURCE_URL
# - SPRING_DATASOURCE_USERNAME
# - SPRING_DATASOURCE_PASSWORD
```

**✅ NO hay URL hardcodeada, solo comentarios explicativos**

---

### 3. `docker-compose.example.yml` (Ejemplo de configuración)

```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=prod
    - SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/casaglassDB?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
    - SPRING_DATASOURCE_USERNAME=casaglassuser
    - SPRING_DATASOURCE_PASSWORD=casaglassclave
```

**⚠️ Este es solo un EJEMPLO, no se usa en producción real**

---

### 4. `Dockerfile`

```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
```

**✅ Solo establece el perfil, NO la URL**

---

## 🎯 ¿QUÉ URL ESTÁ TOMANDO PRODUCCIÓN?

### Si NO hay variables de entorno configuradas:

❌ **ERROR**: La aplicación NO podrá conectarse porque:
- `spring.datasource.url=${SPRING_DATASOURCE_URL:}` → Si no existe la variable, será `""` (vacío)
- La aplicación fallará al iniciar

### Si SÍ hay variables de entorno configuradas:

✅ **FUNCIONA**: La aplicación tomará la URL desde:
- Variable de entorno `SPRING_DATASOURCE_URL` en el servidor
- Variable de entorno `SPRING_DATASOURCE_USERNAME`
- Variable de entorno `SPRING_DATASOURCE_PASSWORD`

---

## 🔧 CÓMO CONFIGURAR EN PRODUCCIÓN

### Opción 1: Variables de entorno del sistema

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:mariadb://tu-servidor-db:3306/tu-base-datos?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
export SPRING_DATASOURCE_USERNAME=tu-usuario
export SPRING_DATASOURCE_PASSWORD=tu-password
```

### Opción 2: Docker Compose (producción)

Crear `docker-compose.yml` (NO subir al repositorio):

```yaml
services:
  backend:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mariadb://tu-servidor-db:3306/tu-base-datos?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=tu-usuario
      - SPRING_DATASOURCE_PASSWORD=tu-password
```

### Opción 3: Variables de entorno en plataforma de despliegue

Si usas:
- **Heroku**: Configurar en Settings → Config Vars
- **AWS Elastic Beanstalk**: Configurar en Environment Properties
- **Google Cloud Run**: Configurar en Environment Variables
- **Azure App Service**: Configurar en Application Settings
- **DigitalOcean App Platform**: Configurar en App Spec → env

---

## ⚠️ IMPORTANTE: SEGURIDAD

### ✅ CORRECTO (Actual):
- ❌ NO hay URLs hardcodeadas en el código
- ❌ NO hay credenciales en el repositorio
- ✅ Las credenciales vienen de variables de entorno

### ❌ INCORRECTO (Nunca hacer):
- ❌ NO hardcodear URLs en `application-prod.properties`
- ❌ NO hardcodear credenciales en ningún archivo
- ❌ NO subir `docker-compose.yml` con credenciales reales al repositorio

---

## 🔍 VERIFICACIÓN

Para verificar qué URL está usando producción:

1. **Revisar logs de inicio de la aplicación**
   - Buscar: `HikariPool` o `DataSource`
   - Debería mostrar la URL (sin password)

2. **Revisar variables de entorno del servidor**
   ```bash
   # En el servidor de producción
   echo $SPRING_DATASOURCE_URL
   ```

3. **Revisar configuración de la plataforma de despliegue**
   - Verificar que las variables de entorno estén configuradas

---

## 📝 RECOMENDACIONES

1. **✅ Mantener la configuración actual** (variables de entorno)
2. **✅ Documentar** dónde están configuradas las variables en producción
3. **✅ Usar secretos** de la plataforma de despliegue (no variables de entorno simples)
4. **✅ Verificar** que `docker-compose.yml` real NO esté en el repositorio (solo `docker-compose.example.yml`)

---

## 🎯 RESPUESTA DIRECTA

**PREGUNTA**: ¿El entorno de producción está tomando la URL del repositorio?

**RESPUESTA**: 
- ❌ **NO**, la URL NO está en el repositorio
- ✅ La URL viene de **variables de entorno** configuradas en el servidor de producción
- ✅ Esto es **CORRECTO** y **SEGURO**

**¿Qué URL está tomando?**
- Depende de las variables de entorno configuradas en tu servidor de producción
- Si no están configuradas, la aplicación **NO funcionará**
- Debes configurar `SPRING_DATASOURCE_URL` en tu plataforma de despliegue

---

## 🔗 ARCHIVOS RELEVANTES

- `src/main/resources/application.properties` → Configuración base
- `src/main/resources/application-prod.properties` → Configuración de producción (sin URLs)
- `docker-compose.example.yml` → Ejemplo (NO se usa en producción)
- `Dockerfile` → Solo establece perfil, no URL

---

## 📞 PRÓXIMOS PASOS

1. Verificar que las variables de entorno estén configuradas en producción
2. Si no están configuradas, configurarlas según tu plataforma de despliegue
3. Verificar que la aplicación pueda conectarse a la base de datos



