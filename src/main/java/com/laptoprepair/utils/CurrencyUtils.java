package com.laptoprepair.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for currency calculations and rounding operations.
 * Ensures consistent currency handling across the application.
 */
public final class CurrencyUtils {

    private static final int DEFAULT_DECIMAL_PLACES = 0;
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    private CurrencyUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Calculates line total for a request item with proper rounding.
     * Formula: ((price - discount) * quantity) * (1 + vatRate)
     * 
     * @param price    the unit price
     * @param discount the discount amount
     * @param quantity the quantity
     * @param vatRate  the VAT rate (e.g., 0.1 for 10%)
     * @return rounded line total
     */
    public static BigDecimal calculateLineTotal(BigDecimal price, BigDecimal discount, int quantity,
            BigDecimal vatRate) {
        if (price == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal safeDiscount = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal safeVatRate = vatRate != null ? vatRate : BigDecimal.ZERO;

        BigDecimal net = price.subtract(safeDiscount)
                .multiply(BigDecimal.valueOf(quantity));
        BigDecimal withVat = net.add(net.multiply(safeVatRate));

        return withVat.setScale(DEFAULT_DECIMAL_PLACES, DEFAULT_ROUNDING_MODE);
    }

    /**
     * Calculates request total with proper rounding.
     * 
     * @param items the list of request items
     * @return rounded total amount
     */
    public static BigDecimal calculateRequestTotal(java.util.List<com.laptoprepair.entity.RequestItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = items.stream()
                .map(com.laptoprepair.entity.RequestItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.setScale(DEFAULT_DECIMAL_PLACES, DEFAULT_ROUNDING_MODE);
    }
}