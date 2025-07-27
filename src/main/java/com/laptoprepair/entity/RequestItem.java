package com.laptoprepair.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "request_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @JsonIgnore
    private Request request;

    @Column(name = "service_item_id", nullable = false)
    private UUID serviceItemId;

    private String name;

    private BigDecimal price;

    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate;

    @Column(name = "warranty_days", nullable = false)
    private Integer warrantyDays = 0;

    private int quantity = 1;

    private BigDecimal discount = BigDecimal.ZERO;

    public BigDecimal getLineTotal() {
        return calculateSubtotal();
    }

    private BigDecimal calculateSubtotal() {
        if (price == null)
            return BigDecimal.ZERO;

        BigDecimal safeDiscount = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal safeVatRate = vatRate != null ? vatRate : BigDecimal.ZERO;

        BigDecimal net = price.subtract(safeDiscount)
                .multiply(BigDecimal.valueOf(quantity));
        return net.add(net.multiply(safeVatRate));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        RequestItem that = (RequestItem) obj;

        return quantity == that.quantity &&
                Objects.equals(warrantyDays, that.warrantyDays) &&
                Objects.equals(serviceItemId, that.serviceItemId) &&
                Objects.equals(name, that.name) &&
                compareBigDecimal(price, that.price) &&
                compareBigDecimal(vatRate, that.vatRate) &&
                compareBigDecimal(discount, that.discount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                serviceItemId,
                name,
                normalizeForHash(price),
                normalizeForHash(vatRate),
                warrantyDays,
                quantity,
                normalizeForHash(discount));
    }

    private boolean compareBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.stripTrailingZeros().equals(b.stripTrailingZeros());
    }

    private BigDecimal normalizeForHash(BigDecimal value) {
        if (value == null)
            return null;
        return value.stripTrailingZeros();
    }
}