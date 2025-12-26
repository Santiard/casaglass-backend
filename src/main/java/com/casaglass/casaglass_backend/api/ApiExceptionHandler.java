package com.casaglass.casaglass_backend.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;

/**
 * Maneja errores a nivel global y devuelve JSON consistente.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private Map<String, Object> body(HttpStatus status, String message, String error) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return response;
    }

    /**
     * Violaciones de integridad (FK/UNIQUE, etc.) -> 409 Conflict
     * Útil para: NIT duplicado, eliminación con registros relacionados, etc.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Mensaje por defecto
        String message = "Violación de integridad de datos.";

        // Heurística simple para dar mensajes más claros sin casarnos con un motor específico
        String lower = (ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        lower = lower != null ? lower.toLowerCase() : "";

        if (lower.contains("unique") || lower.contains("duplicate") || lower.contains("unicidad")) {
            // Caso típico: NIT único en proveedores o clientes
            message = "El NIT ya está registrado.";
        } else if (lower.contains("foreign key") || lower.contains("foreign-key") || lower.contains("fk_")) {
            // Caso: no se puede eliminar por relaciones
            message = "No se puede eliminar el registro porque tiene datos relacionados.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, message, "CONFLICT"));
    }

    /**
     * Validaciones bean validation fallidas en @RequestBody (@Valid) -> 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Datos inválidos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, firstError, "BAD_REQUEST"));
    }

    /**
     * Validaciones en parámetros (@Validated en @PathVariable/@RequestParam) -> 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("Parámetros inválidos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, msg, "BAD_REQUEST"));
    }

    /**
     * Borrado de recurso inexistente -> 404
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyDelete(EmptyResultDataAccessException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(HttpStatus.NOT_FOUND, "El recurso no existe.", "NOT_FOUND"));
    }

    /**
     * Fallback genérico -> 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        // 🐛 DEBUG: Log removido para producción
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno: " + ex.getMessage(), "INTERNAL_SERVER_ERROR"));
    }
}
