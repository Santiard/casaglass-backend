# 📋 Configuración de Perfiles y Variables de Entorno

Este proyecto está configurado para usar perfiles de Spring Boot y variables de entorno para diferentes entornos.

## 🏗️ Estructura de Configuración

### `application.properties` (Base)
- Contiene placeholders para variables de entorno
- No tiene credenciales hardcodeadas
- Configuración común para todos los entornos

### `application-dev.properties` (Desarrollo Local)
- Conexión a MariaDB local (`localhost:3306/Proyecto1`)
- Credenciales: `root` / `admin123`
- Muestra SQL en consola para debugging
- Activa automáticamente con perfil `dev`

### `application-prod.properties` (Producción)
- **Sin credenciales** (vienen de variables de entorno)
- No muestra SQL en consola
- Usa `ddl-auto=validate` (más seguro)
- Activa automáticamente con perfil `prod`

## 🚀 Uso en Desarrollo Local

### Opción 1: Activar perfil en la línea de comandos
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Opción 2: Variable de entorno
```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run

# Windows CMD
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run

# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### Opción 3: En tu IDE (IntelliJ IDEA / Eclipse)
1. **IntelliJ IDEA:**
   - Run Configuration → Environment variables → `SPRING_PROFILES_ACTIVE=dev`
   - O en "Program arguments": `--spring.profiles.active=dev`

2. **Eclipse/STS:**
   - Run Configuration → Arguments → "Program arguments": `--spring.profiles.active=dev`

## 🐳 Uso en Producción (Docker)

### Variables de Entorno Requeridas

En tu `docker-compose.yml` o al ejecutar el contenedor:

```yaml
services:
  backend:
    build: .
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/casaglassDB?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=casaglassuser
      - SPRING_DATASOURCE_PASSWORD=casaglassclave
    ports:
      - "8080:8080"
    depends_on:
      - mariadb
```

### Build y Ejecución

```bash
# Construir la imagen
docker build -t casaglass-backend .

# Ejecutar con variables de entorno
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/casaglassDB?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true \
  -e SPRING_DATASOURCE_USERNAME=casaglassuser \
  -e SPRING_DATASOURCE_PASSWORD=casaglassclave \
  casaglass-backend
```

## 🌐 Configuración CORS

El proyecto incluye una configuración CORS global en `CorsConfig.java` que permite solicitudes desde:

- `http://localhost:3000` (desarrollo local)
- `http://148.230.87.167:3000` (pruebas por IP)
- `https://app.midominio.com` (futuro subdominio)
- `https://midominio.com` (dominio principal)

**Nota:** Los controladores con `@CrossOrigin(origins = "*")` seguirán funcionando, pero la configuración global tiene prioridad y es más segura.

## ✅ Verificación

### Verificar que el perfil está activo:
```bash
# Ver logs de inicio de Spring Boot
# Deberías ver algo como: "The following profiles are active: dev"
```

### Verificar conexión a BD:
```bash
# Si todo está bien, verás en los logs:
# "HikariPool-1 - Starting..."
# "HikariPool-1 - Start completed."
```

### Health Check:
```bash
curl http://localhost:8080/actuator/health
```

## 📝 Notas Importantes

1. **Nunca commits credenciales de producción** en `application-prod.properties`
2. **Siempre usa variables de entorno** en producción
3. **Verifica** que `SPRING_PROFILES_ACTIVE` esté configurado correctamente
4. **El perfil `dev`** se usa automáticamente si no se especifica otro en desarrollo local
5. **El Dockerfile** ya tiene `ENV SPRING_PROFILES_ACTIVE=prod` por defecto

## 🔧 Troubleshooting

### Error: "Could not resolve placeholder 'SPRING_DATASOURCE_URL'"
- **Solución:** Asegúrate de activar el perfil `dev` en desarrollo o inyectar las variables de entorno en producción.

### Error: "Access denied for user"
- **Solución:** Verifica las credenciales en `application-dev.properties` (desarrollo) o en las variables de entorno (producción).

### Error: "Unknown database"
- **Solución:** En desarrollo, asegúrate de que la base de datos `Proyecto1` existe o usa `createDatabaseIfNotExist=true` en la URL.

