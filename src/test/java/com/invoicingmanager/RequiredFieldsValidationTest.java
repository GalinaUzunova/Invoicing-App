package com.invoicingmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoicingmanager.company.CompanyDetailsDTO;
import com.invoicingmanager.company.CompanyDetailsEntity;
import com.invoicingmanager.customer.CustomerDTO;
import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.estimate.EstimateLineItemDTO;
import com.invoicingmanager.estimate.EstimateLineItemEntity;
import com.invoicingmanager.invoice.InvoiceLineItemDTO;
import com.invoicingmanager.invoice.InvoiceLineItemEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
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
    void invoiceLineItemDTORejectsZeroUnitPrice() {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemName("Widget");
        dto.setQuantity(BigDecimal.ONE);
        dto.setUnitPrice(BigDecimal.ZERO);
        dto.setTaxRate(BigDecimal.ZERO);

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
    void invoiceLineItemEntityRejectsZeroUnitPrice() {
        InvoiceLineItemEntity entity = new InvoiceLineItemEntity();
        entity.setItemName("Widget");
        entity.setQuantity(BigDecimal.ONE);
        entity.setUnitPrice(BigDecimal.ZERO);
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
    void estimateLineItemDTORejectsZeroUnitPrice() {
        EstimateLineItemDTO dto = new EstimateLineItemDTO();
        dto.setItemName("Widget");
        dto.setQuantity(BigDecimal.ONE);
        dto.setUnitPrice(BigDecimal.ZERO);
        dto.setTaxRate(BigDecimal.ZERO);

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
    void estimateLineItemEntityRejectsZeroUnitPrice() {
        EstimateLineItemEntity entity = new EstimateLineItemEntity();
        entity.setItemName("Widget");
        entity.setQuantity(BigDecimal.ONE);
        entity.setUnitPrice(BigDecimal.ZERO);
        entity.setTaxRate(BigDecimal.ZERO);

        Set<ConstraintViolation<EstimateLineItemEntity>> violations = validator.validate(entity);

        assertThat(violations).anyMatch(violation -> propertyIs(violation, "unitPrice"));
    }

    private boolean propertyIs(ConstraintViolation<?> violation, String propertyName) {
        return violation.getPropertyPath().toString().equals(propertyName);
    }
}
