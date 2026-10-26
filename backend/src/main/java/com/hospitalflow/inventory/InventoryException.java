package com.hospitalflow.inventory;

import org.springframework.http.HttpStatus;

/** Inventory/dispensing failure carrying the HTTP status to return. */
public class InventoryException extends RuntimeException {

    private final HttpStatus status;

    public InventoryException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    static InventoryException badRequest(String message) { return new InventoryException(HttpStatus.BAD_REQUEST, message); }
    static InventoryException notFound(String message) { return new InventoryException(HttpStatus.NOT_FOUND, message); }
    static InventoryException conflict(String message) { return new InventoryException(HttpStatus.CONFLICT, message); }
}
