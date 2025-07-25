package com.laptoprepair.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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