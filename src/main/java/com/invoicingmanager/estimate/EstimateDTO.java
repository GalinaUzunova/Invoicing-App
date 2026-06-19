package com.invoicingmanager.estimate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimateDTO {

    private Long id;

    @NotNull
    private Long customerId;

    @NotBlank
    @Size(max = 50)
    private String quotationNumber;

    private EstimateStatus status = EstimateStatus.DRAFT;

    @NotNull
    private LocalDate issueDate = LocalDate.now();

    @FutureOrPresent(message = "Expiry date cannot be in the past")
    private LocalDate expiryDate;

    @Size(max = 1_000)
    private String notes;

    @Valid
    @Size(min = 1, message = "At least one line item is required")
    private List<EstimateLineItemDTO> lineItems = new ArrayList<>();

    @AssertTrue(message = "Expiry date cannot be earlier than issue date")
    public boolean isExpiryDateOnOrAfterIssueDate() {
        return issueDate == null || expiryDate == null || !expiryDate.isBefore(issueDate);
    }
}
