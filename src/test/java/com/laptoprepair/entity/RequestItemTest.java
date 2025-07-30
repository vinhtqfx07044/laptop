package com.laptoprepair.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RequestItemTest {

    @Test
    void calculateSubtotal_WithNullPrice_ShouldReturnZero() {
        RequestItem item = new RequestItem();
        item.setPrice(null);
        item.setQuantity(2);
        item.setDiscount(BigDecimal.valueOf(10));
        item.setVatRate(BigDecimal.valueOf(0.1));

        BigDecimal result = item.getLineTotal();

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void calculateSubtotal_WithDiscountAndVat_ShouldComputeCorrectly() {
        RequestItem item = new RequestItem();
        item.setPrice(BigDecimal.valueOf(100));
        item.setQuantity(2);
        item.setDiscount(BigDecimal.valueOf(10));
        item.setVatRate(BigDecimal.valueOf(0.1));

        BigDecimal result = item.getLineTotal();

        // Net = (100 - 10) * 2 = 180
        // Total = 180 + (180 * 0.1) = 198
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(198));
    }

    @Test
    void calculateSubtotal_WithRounding_ShouldRoundCorrectly() {
        RequestItem item = new RequestItem();
        item.setPrice(new BigDecimal("100000.50"));
        item.setQuantity(3);
        item.setDiscount(new BigDecimal("5000.75"));
        item.setVatRate(new BigDecimal("0.1"));

        BigDecimal result = item.getLineTotal();

        // Net = (100000.50 - 5000.75) * 3 = 94999.75 * 3 = 284999.25
        // With VAT = 284999.25 + (284999.25 * 0.1) = 284999.25 + 28499.925 = 313499.175
        // Rounded (HALF_UP, 0 decimal places) = 313499
        assertThat(result).isEqualByComparingTo(new BigDecimal("313499"));
    }

    @Test
    void calculateSubtotal_WithNullDiscount_ShouldTreatAsZero() {
        RequestItem item = new RequestItem();
        item.setPrice(BigDecimal.valueOf(100000));
        item.setQuantity(1);
        item.setDiscount(null);
        item.setVatRate(BigDecimal.valueOf(0.1));

        BigDecimal result = item.getLineTotal();

        // Net = 100000 * 1 = 100000
        // Total = 100000 + (100000 * 0.1) = 110000
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(110000));
    }

    @Test
    void calculateSubtotal_WithNullVatRate_ShouldTreatAsZero() {
        RequestItem item = new RequestItem();
        item.setPrice(BigDecimal.valueOf(100000));
        item.setQuantity(1);
        item.setDiscount(BigDecimal.valueOf(10000));
        item.setVatRate(null);

        BigDecimal result = item.getLineTotal();

        // Net = (100000 - 10000) * 1 = 90000
        // Total = 90000 (no VAT)
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(90000));
    }
}