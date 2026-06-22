package com.invoicingmanager.integration;

import com.invoicingmanager.company.CompanyDetailsEntity;
import com.invoicingmanager.company.CompanyDetailsRepository;
import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.estimate.EstimateEntity;
import com.invoicingmanager.estimate.EstimateLineItemEntity;
import com.invoicingmanager.estimate.EstimateRepository;
import com.invoicingmanager.estimate.EstimateStatus;
import com.invoicingmanager.invoice.InvoiceEntity;
import com.invoicingmanager.invoice.InvoiceLineItemEntity;
import com.invoicingmanager.invoice.InvoiceRepository;
import com.invoicingmanager.invoice.InvoiceStatus;
import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IntegrationTestDataSeeder {

    static final String SEED_USER_EMAIL = "demo@example.com";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final CompanyDetailsRepository companyDetailsRepository;
    private final PasswordEncoder passwordEncoder;

    public IntegrationTestDataSeeder(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            InvoiceRepository invoiceRepository,
            EstimateRepository estimateRepository,
            CompanyDetailsRepository companyDetailsRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository must not be null");
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository must not be null");
        this.estimateRepository = Objects.requireNonNull(estimateRepository, "estimateRepository must not be null");
        this.companyDetailsRepository = Objects.requireNonNull(companyDetailsRepository, "companyDetailsRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    @Transactional
    public SeededTestData seedMainApplicationData() {
        UserEntity user = new UserEntity();
        user.setFirstName("Demo");
        user.setLastName("Owner");
        user.setEmail(SEED_USER_EMAIL);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        user = userRepository.save(user);

        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setName("Acme Ltd");
        customer.setEmail("billing@acme.test");
        customer.setPhone("+441234567890");
        customer.setBillingAddress("1 Main Street, London");
        customer = customerRepository.save(customer);

        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setUser(user);
        company.setCompanyName("Demo Company Ltd");
        company.setEmail("hello@demo.test");
        company.setPhone("+449876543210");
        company.setAddress("2 Business Park");
        company.setVatNumber("GB123456789");
        company = companyDetailsRepository.save(company);

        InvoiceEntity paidInvoice = saveInvoice(
                user,
                customer,
                "INV-PAID-001",
                InvoiceStatus.PAID,
                new BigDecimal("200.00"),
                new BigDecimal("40.00"),
                new BigDecimal("240.00"),
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(20)
        );
        InvoiceEntity sentInvoice = saveInvoice(
                user,
                customer,
                "INV-SENT-001",
                InvoiceStatus.SENT,
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                new BigDecimal("120.00"),
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(25)
        );
        InvoiceEntity draftInvoice = saveInvoice(
                user,
                customer,
                "INV-DRAFT-001",
                InvoiceStatus.DRAFT,
                new BigDecimal("50.00"),
                new BigDecimal("10.00"),
                new BigDecimal("60.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(14)
        );

        EstimateEntity estimate = new EstimateEntity();
        estimate.setUser(user);
        estimate.setCustomer(customer);
        estimate.setQuotationNumber("QUO-001");
        estimate.setStatus(EstimateStatus.SENT);
        estimate.setIssueDate(LocalDate.now().minusDays(3));
        estimate.setExpiryDate(LocalDate.now().plusDays(27));
        estimate.setSubtotal(new BigDecimal("80.00"));
        estimate.setTaxTotal(new BigDecimal("16.00"));
        estimate.setGrandTotal(new BigDecimal("96.00"));
        estimate.addLineItem(estimateLineItem("Discovery", "Initial scoping", "1.00", "80.00", "20.00", "96.00"));
        estimate = estimateRepository.save(estimate);

        return new SeededTestData(
                user,
                customer,
                company,
                paidInvoice,
                sentInvoice,
                draftInvoice,
                estimate
        );
    }

    private InvoiceEntity saveInvoice(
            UserEntity user,
            CustomerEntity customer,
            String invoiceNumber,
            InvoiceStatus status,
            BigDecimal subtotal,
            BigDecimal taxTotal,
            BigDecimal grandTotal,
            LocalDate issueDate,
            LocalDate dueDate
    ) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setUser(user);
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(status);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate);
        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(taxTotal);
        invoice.setGrandTotal(grandTotal);
        invoice.addLineItem(invoiceLineItem("Service", "Professional services", "1.00", subtotal.toPlainString(), "20.00", grandTotal.toPlainString()));
        return invoiceRepository.save(invoice);
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

    record SeededTestData(
            UserEntity user,
            CustomerEntity customer,
            CompanyDetailsEntity company,
            InvoiceEntity paidInvoice,
            InvoiceEntity sentInvoice,
            InvoiceEntity draftInvoice,
            EstimateEntity estimate
    ) {
    }
}
