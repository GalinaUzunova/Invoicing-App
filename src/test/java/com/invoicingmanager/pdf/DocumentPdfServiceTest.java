package com.invoicingmanager.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.invoicingmanager.company.CompanyDetailsEntity;
import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.estimate.EstimateEntity;
import com.invoicingmanager.estimate.EstimateLineItemEntity;
import com.invoicingmanager.estimate.EstimateStatus;
import com.invoicingmanager.invoice.InvoiceEntity;
import com.invoicingmanager.invoice.InvoiceLineItemEntity;
import com.invoicingmanager.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

@SpringBootTest
class DocumentPdfServiceTest {

    @Autowired
    private DocumentPdfService documentPdfService;

    @Test
    void generatesInvoicePdfFromTemplate() {
        byte[] pdf = documentPdfService.generateInvoicePdf(invoice(), company(), null);

        assertThat(pdf).startsWith("%PDF".getBytes());
    }

    @Test
    void generatesEstimatePdfFromTemplate() {
        byte[] pdf = documentPdfService.generateEstimatePdf(estimate(), company(), null);

        assertThat(pdf).startsWith("%PDF".getBytes());
    }

    @Test
    void generatePdfMethodsRejectNullRequiredParameters() {
        assertThatThrownBy(() -> documentPdfService.generateInvoicePdf(null, company(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invoice is required");
        assertThatThrownBy(() -> documentPdfService.generateInvoicePdf(invoice(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company details is required");
        assertThatThrownBy(() -> documentPdfService.generateEstimatePdf(null, company(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate is required");
        assertThatThrownBy(() -> documentPdfService.generateEstimatePdf(estimate(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company details is required");
    }

    @Test
    void skipsUnsupportedPdfLogoTypes() {
        assertThat(documentPdfService.toPdfLogoDataUri(testLogoResource("webp"), "image/webp"))
                .isEmpty();
    }

    @Test
    void skipsNullPdfLogoContentTypeBeforeReadingResource() {
        assertThat(documentPdfService.toPdfLogoDataUri(null, null))
                .isEmpty();
    }

    @Test
    void rejectsNullPdfLogoResourceForSupportedContentType() {
        assertThatThrownBy(() -> documentPdfService.toPdfLogoDataUri(null, MediaType.IMAGE_PNG_VALUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("logo resource is required");
    }

    @Test
    void convertsSupportedLogoToDataUri() {
        assertThat(documentPdfService.toPdfLogoDataUri(
                testLogoResource("png"),
                Objects.requireNonNull(MediaType.IMAGE_PNG_VALUE, "PNG media type must not be null")
        ))
                .hasValueSatisfying(value -> assertThat(value).startsWith("data:image/png;base64,"));
    }

    private ByteArrayResource testLogoResource(String content) {
        return new ByteArrayResource(Objects.requireNonNull(content.getBytes(), "logo bytes must not be null"));
    }

    private InvoiceEntity invoice() {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Acme Ltd");
        customer.setEmail("billing@acme.test");
        customer.setPhone("+441234");
        customer.setBillingAddress("1 Main Street");

        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
        lineItem.setItemName("Service");
        lineItem.setDescription("Monthly service");
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(new BigDecimal("100.00"));
        lineItem.setTaxRate(new BigDecimal("20.00"));
        lineItem.setLineTotal(new BigDecimal("120.00"));

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber("INV-TEST");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(LocalDate.of(2026, 1, 1));
        invoice.setDueDate(LocalDate.of(2026, 1, 15));
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxTotal(new BigDecimal("20.00"));
        invoice.setGrandTotal(new BigDecimal("120.00"));
        invoice.addLineItem(lineItem);
        return invoice;
    }

    private EstimateEntity estimate() {
        CustomerEntity customer = new CustomerEntity();
        customer.setName("Acme Ltd");
        customer.setEmail("billing@acme.test");
        customer.setPhone("+441234");
        customer.setBillingAddress("1 Main Street");

        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName("Service");
        lineItem.setDescription("Monthly service");
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(new BigDecimal("100.00"));
        lineItem.setTaxRate(new BigDecimal("20.00"));
        lineItem.setLineTotal(new BigDecimal("120.00"));

        EstimateEntity estimate = new EstimateEntity();
        estimate.setCustomer(customer);
        estimate.setQuotationNumber("QUO-TEST");
        estimate.setStatus(EstimateStatus.DRAFT);
        estimate.setIssueDate(LocalDate.of(2026, 1, 1));
        estimate.setExpiryDate(LocalDate.of(2026, 1, 15));
        estimate.setSubtotal(new BigDecimal("100.00"));
        estimate.setTaxTotal(new BigDecimal("20.00"));
        estimate.setGrandTotal(new BigDecimal("120.00"));
        estimate.addLineItem(lineItem);
        return estimate;
    }

    private CompanyDetailsEntity company() {
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setCompanyName("Example Company");
        company.setEmail("hello@example.test");
        company.setPhone("+441234");
        company.setAddress("2 Business Street");
        return company;
    }
}
