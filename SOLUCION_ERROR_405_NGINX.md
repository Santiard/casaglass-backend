# 🔧 SOLUCIÓN: Error 405 Not Allowed en Producción

## 📋 ANÁLISIS DEL PROBLEMA

### Errores Identificados:

1. **Error 405 Not Allowed de nginx**
   ```
   <html>
   <head><title>405 Not Allowed</title></head>
   <body>
   <center><h1>405 Not Allowed</h1></center>
   <hr><center>nginx/1.29.3</center>
   </body>
   </html>
   ```

2. **VITE_API_URL no definida** (problema del frontend, pero relacionado)

3. **Página insegura (http://)** - advertencia de seguridad

---

## 🎯 CAUSA RAÍZ

El error **405 Not Allowed** de nginx indica que:
- ❌ nginx está bloqueando las peticiones POST antes de que lleguen al backend
- ❌ La configuración de nginx no está permitiendo métodos HTTP correctos
- ❌ O nginx no está redirigiendo correctamente las peticiones al backend

**El backend Spring Boot está funcionando correctamente**, el problema es la configuración de nginx.

---

## ✅ SOLUCIÓN: Configuración de nginx

### Configuración Correcta de nginx

Crea o actualiza tu archivo de configuración de nginx (normalmente en `/etc/nginx/sites-available/tu-sitio` o `/etc/nginx/nginx.conf`):

```nginx
server {
    listen 80;
    server_name 148.230.87.167;

    # Frontend (Vite)
    location / {
        root /ruta/a/tu/frontend/dist;
        try_files $uri $uri/ /index.html;
        index index.html;
    }

    # Backend API (Spring Boot)
    location /api {
        # Permitir todos los métodos HTTP necesarios
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' 'http://148.230.87.167:3000';
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS, PATCH';
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
            add_header 'Access-Control-Allow-Credentials' 'true';
            add_header 'Access-Control-Max-Age' 1728000;
            add_header 'Content-Type' 'text/plain; charset=utf-8';
            add_header 'Content-Length' 0;
            return 204;
        }

        # Proxy al backend Spring Boot
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        
        # Headers necesarios
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Buffer settings
        proxy_buffering off;
        proxy_request_buffering off;
    }

    # Health check del backend
    location /actuator {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 🔍 PUNTOS CLAVE DE LA CONFIGURACIÓN

### 1. Permitir Métodos HTTP

```nginx
# ✅ CORRECTO: Permitir todos los métodos necesarios
location /api {
    # No usar limit_except aquí, permite todos los métodos
    proxy_pass http://localhost:8080;
}
```

**❌ INCORRECTO** (causa error 405):
```nginx
location /api {
    limit_except GET {
        deny all;  # ❌ Esto bloquea POST, PUT, DELETE
    }
    proxy_pass http://localhost:8080;
}
```

### 2. Manejar OPTIONS (CORS Preflight)

```nginx
if ($request_method = 'OPTIONS') {
    add_header 'Access-Control-Allow-Origin' 'http://148.230.87.167:3000';
    add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS, PATCH';
    add_header 'Access-Control-Allow-Headers' '*';
    add_header 'Access-Control-Allow-Credentials' 'true';
    return 204;
}
```

### 3. Proxy Correcto al Backend

```nginx
proxy_pass http://localhost:8080;  # ✅ Puerto donde corre Spring Boot
```

**Verificar que Spring Boot esté corriendo en el puerto 8080:**
```bash
netstat -tulpn | grep 8080
# o
ss -tulpn | grep 8080
```

---

## 🚀 PASOS PARA APLICAR LA SOLUCIÓN

### 1. Crear/Editar Configuración de nginx

```bash
sudo nano /etc/nginx/sites-available/casaglass
```

Pegar la configuración de arriba y ajustar:
- `server_name`: Tu dominio o IP
- `root`: Ruta a tu frontend compilado
- `proxy_pass`: Puerto donde corre Spring Boot (probablemente 8080)

### 2. Crear Enlace Simbólico (si no existe)

```bash
sudo ln -s /etc/nginx/sites-available/casaglass /etc/nginx/sites-enabled/
```

### 3. Verificar Configuración

```bash
sudo nginx -t
```

Debería mostrar:
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### 4. Recargar nginx

```bash
sudo systemctl reload nginx
# o
sudo service nginx reload
```

### 5. Verificar que Funciona

```bash
# Probar endpoint de login
curl -X POST http://148.230.87.167/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

---

## 🔒 SEGURIDAD: HTTPS (Recomendado)

### Advertencia Actual:
```
Campos de contraseña presentes en una página insegura (http://)
```

### Solución: Configurar HTTPS

1. **Obtener certificado SSL** (Let's Encrypt):
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d tu-dominio.com
```

2. **Configuración nginx con HTTPS:**
```nginx
server {
    listen 80;
    server_name 148.230.87.167;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name 148.230.87.167;

    ssl_certificate /etc/letsencrypt/live/tu-dominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tu-dominio.com/privkey.pem;

    # ... resto de la configuración igual ...
}
```

---

## 🐛 VERIFICACIÓN DEL BACKEND

### El Backend Está Correctamente Configurado:

✅ **Endpoint existe**: `POST /api/auth/login` en `AuthController.java`
✅ **CORS configurado**: Permite `http://148.230.87.167:*` en `CorsConfig.java`
✅ **Métodos permitidos**: GET, POST, PUT, DELETE, OPTIONS, PATCH

### Verificar que el Backend Esté Corriendo:

```bash
# Verificar proceso de Spring Boot
ps aux | grep java

# Verificar puerto 8080
netstat -tulpn | grep 8080

# Probar directamente (sin nginx)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

Si esto funciona directamente, **el problema es 100% nginx**.

---

## 📝 NOTA SOBRE VITE_API_URL

El error `VITE_API_URL no está definida` es del frontend. Debes crear `.env.production`:

```bash
# .env.production (en el proyecto frontend)
VITE_API_URL=http://148.230.87.167:8080/api
```

O mejor aún, usar la misma URL que nginx:
```bash
VITE_API_URL=http://148.230.87.167/api
```

**Importante**: Después de cambiar `.env.production`, debes **recompilar** el frontend:
```bash
npm run build
```

---

## ✅ CHECKLIST DE VERIFICACIÓN

- [ ] nginx configurado para permitir POST, PUT, DELETE en `/api`
- [ ] nginx redirige `/api` a `http://localhost:8080`
- [ ] Spring Boot corriendo en puerto 8080
- [ ] Configuración de nginx verificada con `nginx -t`
- [ ] nginx recargado con `systemctl reload nginx`
- [ ] Frontend tiene `.env.production` con `VITE_API_URL`
- [ ] Frontend recompilado después de cambiar `.env.production`
- [ ] Endpoint `/api/auth/login` responde correctamente

---

## 🎯 RESUMEN

**Problema**: nginx está bloqueando peticiones POST (error 405)

**Solución**: 
1. Configurar nginx para permitir todos los métodos HTTP en `/api`
2. Redirigir `/api` a `http://localhost:8080` (backend Spring Boot)
3. Configurar CORS en nginx para OPTIONS requests
4. Crear `.env.production` en el frontend con `VITE_API_URL`

**El backend NO necesita cambios**, solo la configuración de nginx.



