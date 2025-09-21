package com.laptoprepair.entity;

import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.utils.CurrencyUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a repair request made by a customer.
 * Contains details about the customer, the device, the requested service, and
 * the status of the request.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Request extends BaseEntity {

    @NotBlank(message = "Tên khách hàng là bắt buộc")
    @Size(min = 3, max = 100, message = "Tên khách hàng phải có từ 3-100 ký tự")
    private String name;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "0\\d{9}", message = "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String address;
    private String brandModel;

    @Column(name = "serial_number")
    private String serialNumber;

    @NotNull(message = "Ngày hẹn là bắt buộc")
    private LocalDateTime appointmentDate;

    @NotBlank(message = "Mô tả tình trạng thiết bị là bắt buộc")
    @Size(min = 10, max = 1000, message = "Mô tả phải có từ 10-1000 ký tự")
    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.SCHEDULED;

    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestHistory> history = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestImage> images = new ArrayList<>();

    public BigDecimal getTotal() {
        return CurrencyUtils.calculateRequestTotal(items);
    }
}