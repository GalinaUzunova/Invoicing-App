package com.invoicingmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoicingmanager.company.CompanyDetailsDTO;
import com.invoicingmanager.company.CompanyDetailsEntity;
import com.invoicingmanager.customer.CustomerDTO;
import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.estimate.EstimateDTO;
import com.invoicingmanager.estimate.EstimateLineItemDTO;
import com.invoicingmanager.estimate.EstimateLineItemEntity;
import com.invoicingmanager.invoice.InvoiceDTO;
import com.invoicingmanager.invoice.InvoiceLineItemDTO;
import com.invoicingmanager.invoice.InvoiceLineItemEntity;
import com.invoicingmanager.user.RegisterUserDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RequiredFieldsValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void customerDTORequiresNameEmailAndPhone() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName(" ");
        dto.setEmail(" ");
        dto.setPhone(" ");

        Set<ConstraintViolation<CustomerDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "name"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "phone"));
    }

    @Test
    void customerDTORejectsNullRequiredFields() {
        CustomerDTO dto = validCustomerDTO();
        dto.setName(null);
        dto.setEmail(null);
        dto.setPhone(null);

        Set<ConstraintViolation<CustomerDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "name"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "phone"));
    }

    @Test
    void customerDTOAllowsNullOptionalFields() {
        CustomerDTO dto = validCustomerDTO();
        dto.setId(null);
        dto.setBillingAddress(null);
        dto.setCity(null);
        dto.setCountry(null);
        dto.setTaxNumber(null);
        dto.setNotes(null);

        Set<ConstraintViolation<CustomerDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void customerEntityRequiresNameEmailAndPhone() {
        CustomerEntity entity = new CustomerEntity();
        entity.setName(" ");
        entity.setEmail(" ");
        entity.setPhone(" ");

        Set<ConstraintViolation<CustomerEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "name"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "phone"));
    }

    @Test
    void companyDetailsDTORequiresCompanyNameEmailAndPhone() {
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setCompanyName(" ");
        dto.setEmail(" ");
        dto.setPhone(" ");

        Set<ConstraintViolation<CompanyDetailsDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "companyName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "phone"));
    }

    @Test
    void companyDetailsDTORejectsNullRequiredFields() {
        CompanyDetailsDTO dto = validCompanyDetailsDTO();
        dto.setCompanyName(null);
        dto.setEmail(null);
        dto.setPhone(null);

        Set<ConstraintViolation<CompanyDetailsDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "companyName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "phone"));
    }

    @Test
    void companyDetailsDTOAllowsNullOptionalFields() {
        CompanyDetailsDTO dto = validCompanyDetailsDTO();
        dto.setVatNumber(null);
        dto.setAddress(null);

        Set<ConstraintViolation<CompanyDetailsDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void companyDetailsEntityRequiresCompanyNameEmailAndPhone() {
        CompanyDetailsEntity entity = new CompanyDetailsEntity();
        entity.setCompanyName(" ");
        entity.setEmail(" ");
        entity.setPhone(" ");

        Set<ConstraintViolation<CompanyDetailsEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "companyName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "phone"));
    }

    @Test
    void invoiceLineItemDTORequiresItemQuantityAndPrice() {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemName(" ");
        dto.setQuantity(null);
        dto.setUnitPrice(null);
        dto.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<InvoiceLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "itemName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quantity"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void invoiceLineItemDTORejectsNullRequiredFields() {
        InvoiceLineItemDTO dto = validInvoiceLineItemDTO();
        dto.setItemName(null);
        dto.setQuantity(null);
        dto.setUnitPrice(null);
        dto.setTaxRate(null);

        Set<ConstraintViolation<InvoiceLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "itemName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quantity"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "taxRate"));
    }

    @Test
    void invoiceLineItemDTOAllowsNullOptionalDescription() {
        InvoiceLineItemDTO dto = validInvoiceLineItemDTO();
        dto.setDescription(null);

        Set<ConstraintViolation<InvoiceLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void invoiceLineItemDTOAllowsZeroUnitPrice() {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemName("Widget");
        dto.setQuantity(BigDecimal.ONE);
        dto.setUnitPrice(BigDecimal.ZERO);
        dto.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<InvoiceLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void invoiceLineItemDTORejectsNegativeUnitPrice() {
        InvoiceLineItemDTO dto = validInvoiceLineItemDTO();
        dto.setUnitPrice(new BigDecimal("-0.01"));

        Set<ConstraintViolation<InvoiceLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void invoiceLineItemEntityRequiresItemQuantityAndPrice() {
        InvoiceLineItemEntity entity = new InvoiceLineItemEntity();
        entity.setItemName(" ");
        entity.setQuantity(null);
        entity.setUnitPrice(null);
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<InvoiceLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "itemName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quantity"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void invoiceLineItemEntityAllowsZeroUnitPrice() {
        InvoiceLineItemEntity entity = new InvoiceLineItemEntity();
        entity.setItemName("Widget");
        entity.setQuantity(BigDecimal.ONE);
        entity.setUnitPrice(BigDecimal.ZERO);
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<InvoiceLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).noneMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void invoiceLineItemEntityRejectsNegativeUnitPrice() {
        InvoiceLineItemEntity entity = new InvoiceLineItemEntity();
        entity.setItemName("Widget");
        entity.setQuantity(BigDecimal.ONE);
        entity.setUnitPrice(new BigDecimal("-0.01"));
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<InvoiceLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void estimateLineItemDTORequiresItemQuantityAndPrice() {
        EstimateLineItemDTO dto = new EstimateLineItemDTO();
        dto.setItemName(" ");
        dto.setQuantity(null);
        dto.setUnitPrice(null);
        dto.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<EstimateLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "itemName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quantity"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void estimateLineItemDTORejectsNullRequiredFields() {
        EstimateLineItemDTO dto = validEstimateLineItemDTO();
        dto.setItemName(null);
        dto.setQuantity(null);
        dto.setUnitPrice(null);
        dto.setTaxRate(null);

        Set<ConstraintViolation<EstimateLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "itemName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quantity"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "taxRate"));
    }

    @Test
    void estimateLineItemDTOAllowsNullOptionalDescription() {
        EstimateLineItemDTO dto = validEstimateLineItemDTO();
        dto.setDescription(null);

        Set<ConstraintViolation<EstimateLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void invoiceDTORejectsNullRequiredFields() {
        InvoiceDTO dto = validInvoiceDTO();
        dto.setCustomerId(null);
        dto.setInvoiceNumber(null);
        dto.setIssueDate(null);
        dto.setLineItems(null);

        Set<ConstraintViolation<InvoiceDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "customerId"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "invoiceNumber"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "issueDate"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "lineItems"));
    }

    @Test
    void invoiceDTOAllowsNullOptionalFields() {
        InvoiceDTO dto = validInvoiceDTO();
        dto.setId(null);
        dto.setStatus(null);
        dto.setDueDate(null);
        dto.setNotes(null);

        Set<ConstraintViolation<InvoiceDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void estimateDTORejectsNullRequiredFields() {
        EstimateDTO dto = validEstimateDTO();
        dto.setCustomerId(null);
        dto.setQuotationNumber(null);
        dto.setIssueDate(null);
        dto.setLineItems(null);

        Set<ConstraintViolation<EstimateDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "customerId"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quotationNumber"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "issueDate"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "lineItems"));
    }

    @Test
    void estimateDTOAllowsNullOptionalFields() {
        EstimateDTO dto = validEstimateDTO();
        dto.setId(null);
        dto.setStatus(null);
        dto.setExpiryDate(null);
        dto.setNotes(null);

        Set<ConstraintViolation<EstimateDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void registerUserDTORejectsNullRequiredFields() {
        RegisterUserDTO dto = validRegisterUserDTO();
        dto.setFirstName(null);
        dto.setLastName(null);
        dto.setEmail(null);
        dto.setPassword(null);
        dto.setConfirmPassword(null);

        Set<ConstraintViolation<RegisterUserDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "firstName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "lastName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "email"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "password"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "confirmPassword"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "passwordConfirmed"));
    }

    @Test
    void estimateLineItemDTOAllowsZeroUnitPrice() {
        EstimateLineItemDTO dto = new EstimateLineItemDTO();
        dto.setItemName("Widget");
        dto.setQuantity(BigDecimal.ONE);
        dto.setUnitPrice(BigDecimal.ZERO);
        dto.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<EstimateLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void estimateLineItemDTORejectsNegativeUnitPrice() {
        EstimateLineItemDTO dto = validEstimateLineItemDTO();
        dto.setUnitPrice(new BigDecimal("-0.01"));

        Set<ConstraintViolation<EstimateLineItemDTO>> violations = validator.validate(dto);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void estimateLineItemEntityRequiresItemQuantityAndPrice() {
        EstimateLineItemEntity entity = new EstimateLineItemEntity();
        entity.setItemName(" ");
        entity.setQuantity(null);
        entity.setUnitPrice(null);
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<EstimateLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "itemName"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "quantity"));
        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void estimateLineItemEntityAllowsZeroUnitPrice() {
        EstimateLineItemEntity entity = new EstimateLineItemEntity();
        entity.setItemName("Widget");
        entity.setQuantity(BigDecimal.ONE);
        entity.setUnitPrice(BigDecimal.ZERO);
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<EstimateLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).noneMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    @Test
    void estimateLineItemEntityRejectsNegativeUnitPrice() {
        EstimateLineItemEntity entity = new EstimateLineItemEntity();
        entity.setItemName("Widget");
        entity.setQuantity(BigDecimal.ONE);
        entity.setUnitPrice(new BigDecimal("-0.01"));
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<EstimateLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    private boolean propertyIs(ConstraintViolation<?> violation, String propertyName) {
        return violation.getPropertyPath().toString().equals(propertyName);
    }

    private CustomerDTO validCustomerDTO() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName("Acme Ltd");
        dto.setEmail("billing@acme.test");
        dto.setPhone("+441234");
        return dto;
    }

    private CompanyDetailsDTO validCompanyDetailsDTO() {
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setCompanyName("Acme Ltd");
        dto.setEmail("info@acme.test");
        dto.setPhone("+441234");
        return dto;
    }

    private InvoiceDTO validInvoiceDTO() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(1L);
        dto.setInvoiceNumber("INV-001");
        dto.setIssueDate(LocalDate.now());
        dto.getLineItems().add(validInvoiceLineItemDTO());
        return dto;
    }

    private EstimateDTO validEstimateDTO() {
        EstimateDTO dto = new EstimateDTO();
        dto.setCustomerId(1L);
        dto.setQuotationNumber("QUO-001");
        dto.setIssueDate(LocalDate.now());
        dto.getLineItems().add(validEstimateLineItemDTO());
        return dto;
    }

    private InvoiceLineItemDTO validInvoiceLineItemDTO() {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemName("Service");
        dto.setQuantity(BigDecimal.ONE);
        dto.setUnitPrice(BigDecimal.ONE);
        dto.setTaxRate(BigDecimal.ZERO);
        return dto;
    }

    private EstimateLineItemDTO validEstimateLineItemDTO() {
        EstimateLineItemDTO dto = new EstimateLineItemDTO();
        dto.setItemName("Service");
        dto.setQuantity(BigDecimal.ONE);
        dto.setUnitPrice(BigDecimal.ONE);
        dto.setTaxRate(BigDecimal.ZERO);
        return dto;
    }

    private RegisterUserDTO validRegisterUserDTO() {
        RegisterUserDTO dto = new RegisterUserDTO();
        dto.setFirstName("Grace");
        dto.setLastName("Owner");
        dto.setEmail("owner@example.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        return dto;
    }
}
