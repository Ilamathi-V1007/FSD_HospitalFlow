package com.hospitalflow.inventory;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Runs before GlobalExceptionHandler so InventoryException keeps its status (404/409) instead of
 * falling into the generic 500 handler. It handles nothing else, so other modules are unaffected.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InventoryExceptionHandler {

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<Map<String, String>> handleInventory(InventoryException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getMessage()));
    }
}
