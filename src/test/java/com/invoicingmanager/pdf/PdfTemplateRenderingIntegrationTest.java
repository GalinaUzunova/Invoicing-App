package com.invoicingmanager.pdf;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pdf_template_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload.dir=build/test-uploads"
})
class PdfTemplateRenderingIntegrationTest {

    private static final String PNG_DATA_URI = "data:image/png;base64,"
            + Base64.getEncoder().encodeToString(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private DocumentPdfService documentPdfService;

    @Nested
    class InvoiceTemplate {

        @Test
        void rendersHeaderLineItemsTotalsAndFooterWithFullDataset() {
            InvoiceEntity invoice = fullInvoice();
            CompanyDetailsEntity company = fullCompany();

            String html = renderInvoice(invoice, company, PNG_DATA_URI);

            assertHeaderSection(html, "Invoice", "INV-FULL-001", "SENT", company, invoice.getCustomer());
            assertLogoPresent(html);
            assertMetaDates(html, "2026-03-01", "2026-03-31");
            assertLineItemsTable(html, List.of("Consulting", "Support"), List.of("120.00", "60.00"));
            assertTotalsSection(html, "150.00", "30.00", "180.00");
            assertNotesSection(html, "Payment due within 30 days.");
            assertFooterSection(html);
        }

        @Test
        void rendersMinimalOptionalFieldsAndMissingDueDatePlaceholder() {
            InvoiceEntity invoice = minimalInvoice();
            CompanyDetailsEntity company = minimalCompany();

            String html = renderInvoice(invoice, company, null);

            assertThat(html).contains("Company details not set");
            assertThat(html).contains("Minimal Customer");
            assertThat(html).doesNotContain("class=\"logo\"");
            assertThat(html).contains(">Due date</td>");
            assertThat(html).contains(">-</td>");
            assertThat(html).doesNotContain("class=\"notes\"");
            assertHeaderSection(html, "Invoice", "INV-MIN-001", "DRAFT", company, invoice.getCustomer());
            assertLineItemsTable(html, List.of("Basic item"), List.of("10.00"));
            assertTotalsSection(html, "10.00", "0.00", "10.00");
            assertFooterSection(html);
        }

        @Test
        void rendersMultipleLineItemsAndBlankDescriptionPlaceholder() {
            InvoiceEntity invoice = invoiceWithLineItems(
                    invoiceLineItem("Alpha", null, "2.00", "25.00", "20.00", "60.00"),
                    invoiceLineItem("Beta", "", "1.00", "40.00", "0.00", "40.00"),
                    invoiceLineItem("Gamma", "Detailed work", "3.00", "10.00", "5.00", "31.50")
            );
            invoice.setInvoiceNumber("INV-MULTI");
            invoice.setSubtotal(new BigDecimal("131.50"));
            invoice.setTaxTotal(new BigDecimal("6.50"));
            invoice.setGrandTotal(new BigDecimal("138.00"));

            String html = renderInvoice(invoice, fullCompany(), null);

            assertLineItemsTable(html, List.of("Alpha", "Beta", "Gamma"), List.of("60.00", "40.00", "31.50"));
            assertThat(html).contains(">-</td>");
            assertThat(html).contains("Detailed work");
        }

        @Test
        void escapesSpecialCharactersInRenderedOutput() {
            InvoiceEntity invoice = minimalInvoice();
            invoice.getCustomer().setName("Acme & Co <script>");
            invoice.setNotes("Terms & conditions <apply>");

            String html = renderInvoice(invoice, fullCompany(), null);

            assertThat(html).contains("Acme &amp; Co &lt;script&gt;");
            assertThat(html).contains("Terms &amp; conditions &lt;apply&gt;");
            assertThat(html).doesNotContain("<script>");
        }

        @Test
        void generatesValidPdfEndToEnd() {
            byte[] pdf = documentPdfService.generateInvoicePdf(fullInvoice(), fullCompany(), PNG_DATA_URI);

            assertThat(pdf).isNotEmpty().startsWith("%PDF".getBytes());
        }
    }

    @Nested
    class EstimateTemplate {

        @Test
        void rendersHeaderLineItemsTotalsAndFooterWithFullDataset() {
            EstimateEntity estimate = fullEstimate();
            CompanyDetailsEntity company = fullCompany();

            String html = renderEstimate(estimate, company, PNG_DATA_URI);

            assertHeaderSection(html, "Quotation", "QUO-FULL-001", "SENT", company, estimate.getCustomer());
            assertLogoPresent(html);
            assertMetaDates(html, "2026-04-01", "2026-04-30");
            assertLineItemsTable(html, List.of("Design", "Build"), List.of("240.00", "360.00"));
            assertTotalsSection(html, "500.00", "100.00", "600.00");
            assertNotesSection(html, "Valid for 30 days.");
            assertFooterSection(html);
        }

        @Test
        void rendersMinimalOptionalFieldsAndMissingExpiryDatePlaceholder() {
            EstimateEntity estimate = minimalEstimate();
            CompanyDetailsEntity company = minimalCompany();

            String html = renderEstimate(estimate, company, null);

            assertThat(html).contains("Company details not set");
            assertThat(html).contains("Minimal Customer");
            assertThat(html).doesNotContain("class=\"logo\"");
            assertThat(html).contains(">Expiry date</td>");
            assertThat(html).contains(">-</td>");
            assertThat(html).doesNotContain("class=\"notes\"");
            assertHeaderSection(html, "Quotation", "QUO-MIN-001", "DRAFT", company, estimate.getCustomer());
            assertLineItemsTable(html, List.of("Basic item"), List.of("10.00"));
            assertTotalsSection(html, "10.00", "0.00", "10.00");
            assertFooterSection(html);
        }

        @Test
        void rendersMultipleLineItemsAndZeroTaxLine() {
            EstimateEntity estimate = estimateWithLineItems(
                    estimateLineItem("Planning", "Phase 1", "1.00", "100.00", "20.00", "120.00"),
                    estimateLineItem("Travel", "-", "2.00", "50.00", "0.00", "100.00")
            );
            estimate.setQuotationNumber("QUO-MULTI");
            estimate.setSubtotal(new BigDecimal("200.00"));
            estimate.setTaxTotal(new BigDecimal("20.00"));
            estimate.setGrandTotal(new BigDecimal("220.00"));

            String html = renderEstimate(estimate, fullCompany(), null);

            assertLineItemsTable(html, List.of("Planning", "Travel"), List.of("120.00", "100.00"));
            assertTotalsSection(html, "200.00", "20.00", "220.00");
        }

        @Test
        void escapesSpecialCharactersInRenderedOutput() {
            EstimateEntity estimate = minimalEstimate();
            estimate.getCustomer().setName("Beta \"Quoted\" <Ltd>");
            estimate.setNotes("Price & delivery <subject to change>");

            String html = renderEstimate(estimate, fullCompany(), null);

            assertThat(html).contains("Beta &quot;Quoted&quot; &lt;Ltd&gt;");
            assertThat(html).contains("Price &amp; delivery &lt;subject to change&gt;");
        }

        @Test
        void generatesValidPdfEndToEnd() {
            byte[] pdf = documentPdfService.generateEstimatePdf(fullEstimate(), fullCompany(), PNG_DATA_URI);

            assertThat(pdf).isNotEmpty().startsWith("%PDF".getBytes());
        }
    }

    private String renderInvoice(InvoiceEntity invoice, CompanyDetailsEntity company, String logoDataUri) {
        Context context = new Context();
        context.setVariable("invoice", invoice);
        context.setVariable("company", company);
        context.setVariable("logoDataUri", logoDataUri);
        return templateEngine.process("pdf/invoice", context);
    }

    private String renderEstimate(EstimateEntity estimate, CompanyDetailsEntity company, String logoDataUri) {
        Context context = new Context();
        context.setVariable("estimate", estimate);
        context.setVariable("company", company);
        context.setVariable("logoDataUri", logoDataUri);
        return templateEngine.process("pdf/estimate", context);
    }

    private void assertHeaderSection(
            String html,
            String documentTitle,
            String documentNumber,
            String status,
            CompanyDetailsEntity company,
            CustomerEntity customer
    ) {
        assertThat(html).contains("class=\"document-header\"");
        assertThat(html).contains(">" + documentTitle + "</p>");
        assertThat(html).contains(documentNumber);
        assertThat(html).contains(">" + status + "</span>");
        assertThat(html).contains(">From</p>");
        assertThat(html).contains(">Bill to</p>");
        if (company.getCompanyName() != null) {
            assertThat(html).contains(company.getCompanyName());
        }
        if (company.getAddress() != null) {
            assertThat(html).contains(company.getAddress());
        }
        if (company.getVatNumber() != null) {
            assertThat(html).contains("VAT reg.");
            assertThat(html).contains(company.getVatNumber());
        }
        assertThat(html).contains(customer.getName());
    }

    private void assertLogoPresent(String html) {
        assertThat(html).contains("class=\"logo\"");
        assertThat(html).contains("data:image/png;base64,");
    }

    private void assertMetaDates(String html, String issueDate, String secondaryDate) {
        assertThat(html).contains(">Issue date</td>");
        assertThat(html).contains(">" + issueDate + "</td>");
        assertThat(html).contains(">" + secondaryDate + "</td>");
    }

    private void assertLineItemsTable(String html, List<String> itemNames, List<String> lineTotals) {
        assertThat(html).contains("<table class=\"items\">");
        assertThat(html).contains(">Item</th>");
        assertThat(html).contains(">Description</th>");
        assertThat(html).contains(">Qty</th>");
        assertThat(html).contains(">Unit price</th>");
        assertThat(html).contains(">Tax %</th>");
        assertThat(html).contains(">Total</th>");
        itemNames.forEach(name -> assertThat(html).contains(name));
        lineTotals.forEach(total -> assertThat(html).contains(total));
    }

    private void assertTotalsSection(String html, String subtotal, String taxTotal, String grandTotal) {
        assertThat(html).contains("class=\"totals-wrap\"");
        assertThat(html).contains("class=\"totals-table\"");
        assertThat(html).contains(">Subtotal</td>");
        assertThat(html).contains(">Tax</td>");
        assertThat(html).contains("class=\"grand\"");
        assertThat(html).contains(">" + subtotal + "</td>");
        assertThat(html).contains(">" + taxTotal + "</td>");
        assertThat(html).contains(">" + grandTotal + "</td>");
    }

    private void assertNotesSection(String html, String notes) {
        assertThat(html).contains("class=\"notes\"");
        assertThat(html).contains(">Notes</strong>");
        assertThat(html).contains(notes);
    }

    private void assertFooterSection(String html) {
        assertThat(html).contains("class=\"footer\"");
        assertThat(html).contains(">Generated by Invoicing Manager</p>");
    }

    private InvoiceEntity fullInvoice() {
        CustomerEntity customer = customer("Acme Ltd", "billing@acme.test", "+4412345678", "1 Main Street");
        InvoiceEntity invoice = baseInvoice("INV-FULL-001", InvoiceStatus.SENT, customer);
        invoice.setIssueDate(LocalDate.of(2026, 3, 1));
        invoice.setDueDate(LocalDate.of(2026, 3, 31));
        invoice.setNotes("Payment due within 30 days.");
        invoice.setSubtotal(new BigDecimal("150.00"));
        invoice.setTaxTotal(new BigDecimal("30.00"));
        invoice.setGrandTotal(new BigDecimal("180.00"));
        invoice.addLineItem(invoiceLineItem("Consulting", "Strategy review", "1.00", "100.00", "20.00", "120.00"));
        invoice.addLineItem(invoiceLineItem("Support", "Monthly retainer", "1.00", "50.00", "20.00", "60.00"));
        return invoice;
    }

    private InvoiceEntity minimalInvoice() {
        CustomerEntity customer = customer("Minimal Customer", null, null, null);
        InvoiceEntity invoice = baseInvoice("INV-MIN-001", InvoiceStatus.DRAFT, customer);
        invoice.setIssueDate(LocalDate.of(2026, 1, 1));
        invoice.setDueDate(null);
        invoice.setSubtotal(new BigDecimal("10.00"));
        invoice.setTaxTotal(BigDecimal.ZERO);
        invoice.setGrandTotal(new BigDecimal("10.00"));
        invoice.addLineItem(invoiceLineItem("Basic item", null, "1.00", "10.00", "0.00", "10.00"));
        return invoice;
    }

    private InvoiceEntity invoiceWithLineItems(InvoiceLineItemEntity... lineItems) {
        InvoiceEntity invoice = baseInvoice("INV-TEMP", InvoiceStatus.DRAFT, customer("Temp", null, null, null));
        invoice.setIssueDate(LocalDate.of(2026, 2, 1));
        for (InvoiceLineItemEntity lineItem : lineItems) {
            invoice.addLineItem(lineItem);
        }
        return invoice;
    }

    private InvoiceEntity baseInvoice(String number, InvoiceStatus status, CustomerEntity customer) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber(number);
        invoice.setStatus(status);
        return invoice;
    }

    private EstimateEntity fullEstimate() {
        CustomerEntity customer = customer("Globex Corp", "quotes@globex.test", "+4498765432", "99 Market Road");
        EstimateEntity estimate = baseEstimate("QUO-FULL-001", EstimateStatus.SENT, customer);
        estimate.setIssueDate(LocalDate.of(2026, 4, 1));
        estimate.setExpiryDate(LocalDate.of(2026, 4, 30));
        estimate.setNotes("Valid for 30 days.");
        estimate.setSubtotal(new BigDecimal("500.00"));
        estimate.setTaxTotal(new BigDecimal("100.00"));
        estimate.setGrandTotal(new BigDecimal("600.00"));
        estimate.addLineItem(estimateLineItem("Design", "UX/UI package", "1.00", "200.00", "20.00", "240.00"));
        estimate.addLineItem(estimateLineItem("Build", "Implementation", "1.00", "300.00", "20.00", "360.00"));
        return estimate;
    }

    private EstimateEntity minimalEstimate() {
        CustomerEntity customer = customer("Minimal Customer", null, null, null);
        EstimateEntity estimate = baseEstimate("QUO-MIN-001", EstimateStatus.DRAFT, customer);
        estimate.setIssueDate(LocalDate.of(2026, 1, 1));
        estimate.setExpiryDate(null);
        estimate.setSubtotal(new BigDecimal("10.00"));
        estimate.setTaxTotal(BigDecimal.ZERO);
        estimate.setGrandTotal(new BigDecimal("10.00"));
        estimate.addLineItem(estimateLineItem("Basic item", null, "1.00", "10.00", "0.00", "10.00"));
        return estimate;
    }

    private EstimateEntity estimateWithLineItems(EstimateLineItemEntity... lineItems) {
        EstimateEntity estimate = baseEstimate("QUO-TEMP", EstimateStatus.DRAFT, customer("Temp", null, null, null));
        estimate.setIssueDate(LocalDate.of(2026, 2, 1));
        for (EstimateLineItemEntity lineItem : lineItems) {
            estimate.addLineItem(lineItem);
        }
        return estimate;
    }

    private EstimateEntity baseEstimate(String number, EstimateStatus status, CustomerEntity customer) {
        EstimateEntity estimate = new EstimateEntity();
        estimate.setCustomer(customer);
        estimate.setQuotationNumber(number);
        estimate.setStatus(status);
        return estimate;
    }

    private CompanyDetailsEntity fullCompany() {
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setCompanyName("Example Company Ltd");
        company.setEmail("hello@example.test");
        company.setPhone("+44111222333");
        company.setAddress("2 Business Street, London");
        company.setVatNumber("GB123456789");
        return company;
    }

    private CompanyDetailsEntity minimalCompany() {
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setCompanyName(null);
        return company;
    }

    private CustomerEntity customer(String name, String email, String phone, String billingAddress) {
        CustomerEntity customer = new CustomerEntity();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setBillingAddress(billingAddress);
        return customer;
    }

    private InvoiceLineItemEntity invoiceLineItem(
            String itemName,
            String description,
            String quantity,
            String unitPrice,
            String taxRate,
            String lineTotal
    ) {
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
        lineItem.setItemName(itemName);
        lineItem.setDescription(description);
        lineItem.setQuantity(new BigDecimal(quantity));
        lineItem.setUnitPrice(new BigDecimal(unitPrice));
        lineItem.setTaxRate(new BigDecimal(taxRate));
        lineItem.setLineTotal(new BigDecimal(lineTotal));
        return lineItem;
    }

    private EstimateLineItemEntity estimateLineItem(
            String itemName,
            String description,
            String quantity,
            String unitPrice,
            String taxRate,
            String lineTotal
    ) {
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName(itemName);
        lineItem.setDescription(description);
        lineItem.setQuantity(new BigDecimal(quantity));
        lineItem.setUnitPrice(new BigDecimal(unitPrice));
        lineItem.setTaxRate(new BigDecimal(taxRate));
        lineItem.setLineTotal(new BigDecimal(lineTotal));
        return lineItem;
    }
}
