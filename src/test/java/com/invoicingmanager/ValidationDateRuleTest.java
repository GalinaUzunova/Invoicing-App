package com.invoicingmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoicingmanager.estimate.EstimateDTO;
import com.invoicingmanager.estimate.EstimateEntity;
import com.invoicingmanager.invoice.InvoiceDTO;
import com.invoicingmanager.invoice.InvoiceEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValidationDateRuleTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void invoiceDTORejectsIssueDateBeforeToday() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L);
        dto.setInvoiceNumber("INV-001");
        dto.setIssueDate(LocalDate.now().minusDays(1));

        Set<ConstraintViolation<InvoiceDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("issueDate");
                    assertThat(violation.getMessage()).isEqualTo("Issue date cannot be in the past");
                });
    }

    @Test
    void invoiceDTORejectsDueDateBeforeIssueDate() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L);
        dto.setInvoiceNumber("INV-001");
        dto.setIssueDate(LocalDate.now().plusDays(5));
        dto.setDueDate(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<InvoiceDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("dueDateOnOrAfterIssueDate");
                    assertThat(violation.getMessage()).isEqualTo("Due date cannot be earlier than issue date");
                });
    }

    @Test
    void estimateDTORejectsIssueDateBeforeToday() {
        EstimateDTO dto = new EstimateDTO();
        dto.setCustomerId(1L);
        dto.setQuotationNumber("QUO-001");
        dto.setIssueDate(LocalDate.now().minusDays(1));

        Set<ConstraintViolation<EstimateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("issueDate");
                    assertThat(violation.getMessage()).isEqualTo("Issue date cannot be in the past");
                });
    }

    @Test
    void estimateDTORejectsExpiryDateBeforeIssueDate() {
        EstimateDTO dto = new EstimateDTO();
        dto.setCustomerId(1L);
        dto.setQuotationNumber("QUO-001");
        dto.setIssueDate(LocalDate.now().plusDays(5));
        dto.setExpiryDate(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<EstimateDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("expiryDateOnOrAfterIssueDate");
                    assertThat(violation.getMessage()).isEqualTo("Expiry date cannot be earlier than issue date");
                });
    }

    @Test
    void invoiceEntityRejectsDueDateBeforeIssueDate() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setIssueDate(LocalDate.now().plusDays(5));
        invoice.setDueDate(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<InvoiceEntity>> violations = validator.validate(invoice);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("dueDateOnOrAfterIssueDate"));
    }

    @Test
    void estimateEntityRejectsExpiryDateBeforeIssueDate() {
        EstimateEntity estimate = new EstimateEntity();
        estimate.setQuotationNumber("QUO-001");
        estimate.setIssueDate(LocalDate.now().plusDays(5));
        estimate.setExpiryDate(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<EstimateEntity>> violations = validator.validate(estimate);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("expiryDateOnOrAfterIssueDate"));
    }
}
