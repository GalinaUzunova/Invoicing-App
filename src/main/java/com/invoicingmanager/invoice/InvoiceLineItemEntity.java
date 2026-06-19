package com.invoicingmanager.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @NotBlank
    @Size(max = 150)
    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @NotNull
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
