package com.laptoprepair.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "request_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
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

    @Override
    public String toString() {
        return String.format("RequestItem(id=%s,serviceItemId=%s,name=%s,price=%s,vat=%s,warranty=%d,qty=%d,discount=%s)",
                getId(),
                serviceItemId,
                name,
                normalizeBigDecimal(price),
                normalizeBigDecimal(vatRate),
                warrantyDays,
                quantity,
                normalizeBigDecimal(discount));
    }

    private String normalizeBigDecimal(BigDecimal value) {
        if (value == null) return "null";
        return value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal calculateSubtotal(){
        if (price == null) return BigDecimal.ZERO;
        
        BigDecimal safeDiscount = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal safeVatRate = vatRate != null ? vatRate : BigDecimal.ZERO;
        
        BigDecimal net = price.subtract(safeDiscount)
                              .multiply(BigDecimal.valueOf(quantity));
        return net.add(net.multiply(safeVatRate));
    }
}