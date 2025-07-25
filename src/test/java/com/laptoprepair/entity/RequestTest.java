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
}