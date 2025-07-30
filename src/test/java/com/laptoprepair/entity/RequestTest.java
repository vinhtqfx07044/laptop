package com.laptoprepair.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTest {

    @Test
    void getTotal_WithNoItems_ShouldReturnZero() {
        Request request = new Request();
        request.setItems(new ArrayList<>());

        BigDecimal result = request.getTotal();

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTotal_WithMultipleItems_ShouldSumLineTotals() {
        Request request = new Request();

        RequestItem item1 = new RequestItem();
        item1.setPrice(BigDecimal.valueOf(100));
        item1.setQuantity(1);
        item1.setDiscount(BigDecimal.ZERO);
        item1.setVatRate(BigDecimal.valueOf(0.1));

        RequestItem item2 = new RequestItem();
        item2.setPrice(BigDecimal.valueOf(200));
        item2.setQuantity(2);
        item2.setDiscount(BigDecimal.ZERO);
        item2.setVatRate(BigDecimal.valueOf(0.1));

        List<RequestItem> items = List.of(item1, item2);
        request.setItems(items);

        BigDecimal result = request.getTotal();

        // Item1: 100 * 1 * 1.1 = 110
        // Item2: 200 * 2 * 1.1 = 440
        // Total: 110 + 440 = 550
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(550));
    }

    @Test
    void getTotal_WithRoundingRequired_ShouldApplyRounding() {
        Request request = new Request();

        RequestItem item1 = new RequestItem();
        item1.setPrice(new BigDecimal("100.25"));
        item1.setQuantity(1);
        item1.setDiscount(new BigDecimal("5.25"));
        item1.setVatRate(new BigDecimal("0.1"));

        RequestItem item2 = new RequestItem();
        item2.setPrice(new BigDecimal("200.33"));
        item2.setQuantity(1);
        item2.setDiscount(new BigDecimal("10.33"));
        item2.setVatRate(new BigDecimal("0.1"));

        List<RequestItem> items = List.of(item1, item2);
        request.setItems(items);

        BigDecimal result = request.getTotal();

        // Item1: (100.25 - 5.25) * 1 * 1.1 = 95 * 1.1 = 104.5 -> 105 (rounded)
        // Item2: (200.33 - 10.33) * 1 * 1.1 = 190 * 1.1 = 209
        // Total: 105 + 209 = 314
        assertThat(result).isEqualByComparingTo(new BigDecimal("314"));
    }
}