package com.invoicingmanager.estimate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimateLineItemDTO {

    @NotBlank
    @Size(max = 150)
    private String itemName;

    @Size(max = 500)
    private String description;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @NotNull
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal taxRate = BigDecimal.ZERO;
}
