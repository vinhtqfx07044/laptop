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
}