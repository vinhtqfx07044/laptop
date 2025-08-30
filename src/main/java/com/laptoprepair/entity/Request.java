package com.laptoprepair.entity;

import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.utils.CurrencyUtils;
import com.laptoprepair.validation.ValidationConstants;
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

    @NotBlank(message = ValidationConstants.REQUEST_NAME_BLANK_MSG)
    @Size(min = ValidationConstants.REQUEST_NAME_MIN_LENGTH, max = ValidationConstants.REQUEST_NAME_MAX_LENGTH, message = ValidationConstants.REQUEST_NAME_SIZE_MSG)
    private String name;

    @NotBlank(message = ValidationConstants.REQUEST_PHONE_BLANK_MSG)
    @Pattern(regexp = ValidationConstants.REQUEST_PHONE_PATTERN, message = ValidationConstants.REQUEST_PHONE_PATTERN_MSG)
    private String phone;

    @Email(message = ValidationConstants.REQUEST_EMAIL_FORMAT_MSG)
    private String email;

    private String address;
    private String brandModel;

    @Column(name = "serial_number")
    private String serialNumber;

    @NotNull(message = ValidationConstants.REQUEST_APPOINTMENT_DATE_NULL_MSG)
    private LocalDateTime appointmentDate;

    @NotBlank(message = ValidationConstants.REQUEST_DESCRIPTION_BLANK_MSG)
    @Size(min = ValidationConstants.REQUEST_DESCRIPTION_MIN_LENGTH, max = ValidationConstants.REQUEST_DESCRIPTION_MAX_LENGTH, message = ValidationConstants.REQUEST_DESCRIPTION_SIZE_MSG)
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