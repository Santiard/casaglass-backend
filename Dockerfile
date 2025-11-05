# ============================================================
# 🏗️ ETAPA DE CONSTRUCCIÓN (Builder)
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar archivos de Maven primero (cache de dependencias)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn -q -DskipTests=true package

# ============================================================
# 🚀 ETAPA DE EJECUCIÓN (Runtime)
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR desde la etapa de construcción
COPY --from=builder /app/target/*.jar app.jar

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

# Puerto de la aplicación
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

