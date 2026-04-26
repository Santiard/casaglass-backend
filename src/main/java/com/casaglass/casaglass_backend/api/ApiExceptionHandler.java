package com.casaglass.casaglass_backend.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolationException;
import com.casaglass.casaglass_backend.exception.InventarioInsuficienteException;
import org.hibernate.LazyInitializationException;
import java.sql.SQLIntegrityConstraintViolationException;
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
     * Útil para: NIT duplicado, correo duplicado, eliminación con registros relacionados, etc.
     */
    /**
     * Traslado / venta: stock no alcanza para la operación.
     */
    @ExceptionHandler(InventarioInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleInventarioInsuficiente(InventarioInsuficienteException ex) {
        Map<String, Object> b = new java.util.HashMap<>();
        b.put("timestamp", Instant.now().toString());
        b.put("status", HttpStatus.CONFLICT.value());
        b.put("error", "INVENTARIO_INSUFICIENTE");
        b.put("message", ex.getMessage());
        if (ex.getProductoId() != null) {
            b.put("productoId", ex.getProductoId());
        }
        if (ex.getSedeId() != null) {
            b.put("sedeId", ex.getSedeId());
        }
        if (ex.getCantidadDisponible() != null) {
            b.put("cantidadDisponible", ex.getCantidadDisponible());
        }
        if (ex.getCantidadRequerida() != null) {
            b.put("cantidadRequerida", ex.getCantidadRequerida());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(b);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Mensaje por defecto
        String message = "Violación de integridad de datos.";

        // Heurística simple para dar mensajes más claros sin casarnos con un motor específico
        String lower = (ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        lower = lower != null ? lower.toLowerCase() : "";

        if (lower.contains("unique") || lower.contains("duplicate") || lower.contains("unicidad")) {
            // Misma entrega + misma orden dos veces (p. ej. abono y reembolso) si existe UNIQUE(entrega_id, orden_id) en BD
            if (lower.contains("entrega_detalle")
                    || (lower.contains("entrega_id") && lower.contains("orden_id"))) {
                message = "No se puede duplicar la misma orden en la misma entrega de dinero. Revise abonos y reembolsos o el índice único en la base de datos.";
            } else
            if (lower.contains("ukgs8pntaqxksfh6jp4asuokx7a") || lower.contains("nit")) {
                message = "El NIT ya está registrado.";
            } else if (lower.contains("uk8duxx4vm6d736wokeq3u5skw7") || lower.contains("correo") || lower.contains("email")) {
                message = "El correo electrónico ya está registrado.";
            } else {
                // Mensaje genérico para otros casos de unique
                message = "Ya existe un registro con estos datos (violación de restricción única).";
            }
        } else if (lower.contains("foreign key") || lower.contains("foreign-key") || lower.contains("fk_")
                || lower.contains("cannot delete") || lower.contains("cannot add") || lower.contains("a foreign key constraint fails")) {
            // Caso: no se puede eliminar por relaciones, o referencia a fila inexistente
            message = "No se puede completar operación: restricción de clave foránea o dato referenciado inválido.";
        }

        Map<String, Object> payload = new java.util.HashMap<>(body(HttpStatus.CONFLICT, message, "CONFLICT"));
        String rootMsg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (rootMsg != null && !rootMsg.isBlank()) {
            payload.put("detail", rootMsg);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(payload);
    }

    private ResponseEntity<Map<String, Object>> unwrapDataIntegrityOrConflict(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof DataIntegrityViolationException) {
                return handleDataIntegrity((DataIntegrityViolationException) t);
            }
            if (t instanceof SQLIntegrityConstraintViolationException) {
                String m = t.getMessage();
                if (m != null && isEntregaDetalleDuplicate(m)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(body(HttpStatus.CONFLICT,
                                    "No se puede duplicar la misma orden en la misma entrega de dinero. Revise abonos y reembolsos o el índice único en la base de datos.",
                                    "CONFLICT"));
                }
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(body(HttpStatus.CONFLICT,
                                m != null ? m : "Violación de integridad de datos.",
                                "CONFLICT"));
            }
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root != null && root.getMessage() != null ? root.getMessage() : ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = "No se pudo completar la operación (error de transacción).";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, msg, "TRANSACTION_ERROR"));
    }

    private static boolean isEntregaDetalleDuplicate(String raw) {
        String lower = raw.toLowerCase();
        return lower.contains("entrega_detalle") || (lower.contains("entrega_id") && lower.contains("orden_id"));
    }

    /**
     * "Could not commit JPA transaction" y similares: el fallo real suele ser {@link DataIntegrityViolationException} anidada.
     */
    @ExceptionHandler({ TransactionSystemException.class, RollbackException.class, UnexpectedRollbackException.class })
    public ResponseEntity<Map<String, Object>> handleTransactionOrRollback(Exception ex) {
        return unwrapDataIntegrityOrConflict(ex);
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
     * Validaciones de negocio / parámetros manuales -> 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Parámetros inválidos.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST"));
    }

    /**
     * Estados HTTP explícitos lanzados desde servicios/controladores (404, 409, etc.)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status)
                .body(body(status, message, status.name()));
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
     * Entidad no encontrada (referencias huérfanas o lazy loading fallido) -> 500
     * Maneja casos donde se intenta acceder a una entidad relacionada que no existe
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        String message = "Error de integridad de datos: Hay referencias a registros que ya no existen. Contacte al administrador para corregir los datos.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, message, "DATA_INTEGRITY_ERROR"));
    }

    /**
     * Error de inicialización perezosa (lazy loading fuera de sesión) -> 500
     */
    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<Map<String, Object>> handleLazyInitialization(LazyInitializationException ex) {
        String message = "Error al cargar datos relacionados. Por favor, intente nuevamente.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, message, "LAZY_LOADING_ERROR"));
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
