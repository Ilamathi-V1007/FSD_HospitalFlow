package com.hospitalflow.inventory;

public enum StockStatus {
    IN_STOCK, LOW_STOCK, OUT_OF_STOCK;

    /** OUT_OF_STOCK at zero, LOW_STOCK at or below the reorder threshold, otherwise IN_STOCK. */
    public static StockStatus of(int quantity, int reorderThreshold) {
        if (quantity <= 0) return OUT_OF_STOCK;
        if (quantity <= reorderThreshold) return LOW_STOCK;
        return IN_STOCK;
    }
}
