package com.laptoprepair.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Represents a service item offered by the repair shop.
 * This entity stores details about the service, its price, VAT rate, warranty
 * period, and active status.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceItem extends BaseEntity {
    private String name;
    private BigDecimal price;
    private BigDecimal vatRate;
    private int warrantyDays;
    private boolean active = true;
}