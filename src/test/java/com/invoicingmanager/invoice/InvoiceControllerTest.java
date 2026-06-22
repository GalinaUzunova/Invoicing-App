package com.invoicingmanager.invoice;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.invoicingmanager.company.CompanyDetailsEntity;
import com.invoicingmanager.company.CompanyDetailsService;
import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerService;
import com.invoicingmanager.pdf.DocumentPdfService;
import com.invoicingmanager.security.SecurityConfig;
import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private CompanyDetailsService companyDetailsService;

    @MockitoBean
    private DocumentPdfService documentPdfService;

    @MockitoBean
    private UserService userService;

    @Test
    void invoicesRedirectAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/invoices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void listShowsInvoicesForAuthenticatedUser() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(invoiceService.findAllForUser(user, null)).thenReturn(List.of());

        mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(view().name("invoices/list"))
                .andExpect(model().attributeExists("invoices", "statuses"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void newInvoiceFormIsAvailable() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(invoiceService.newInvoiceDTO(null, user)).thenReturn(new InvoiceDTO());
        when(customerService.findAllForUser(user)).thenReturn(List.of());

        mockMvc.perform(get("/invoices/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("invoices/form"))
                .andExpect(model().attributeExists("invoiceDTO", "customers", "formAction"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void invalidInvoiceIdShowsBadRequestErrorPage() throws Exception {
        mockMvc.perform(get("/invoices/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("statusCode", 400));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void detailIncludesInvoiceAndCompanyDetails() throws Exception {
        UserEntity user = new UserEntity();
        CustomerEntity customer = new CustomerEntity();
        customer.setId(5L);
        customer.setName("Acme Ltd");
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(1L);
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber("INV-001");
        invoice.setStatus(InvoiceStatus.DRAFT);
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(invoiceService.findByIdForUser(1L, user)).thenReturn(invoice);
        when(companyDetailsService.getForUser(user)).thenReturn(new CompanyDetailsEntity());

        mockMvc.perform(get("/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("invoices/detail"))
                .andExpect(model().attributeExists("invoice", "company"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void downloadPdfReturnsInvoicePdf() throws Exception {
        UserEntity user = new UserEntity();
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        CustomerEntity customer = new CustomerEntity();
        customer.setId(5L);
        customer.setName("Acme Ltd");
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(1L);
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber("INV-001");
        invoice.setStatus(InvoiceStatus.DRAFT);
        byte[] pdf = "%PDF-test".getBytes();

        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(invoiceService.findByIdForUser(1L, user)).thenReturn(invoice);
        when(companyDetailsService.getForUser(user)).thenReturn(company);
        when(companyDetailsService.getLogoResource(user)).thenReturn(Optional.empty());
        when(documentPdfService.generateInvoicePdf(invoice, company, null)).thenReturn(pdf);

        mockMvc.perform(get("/invoices/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"invoice-INV-001.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void postInvoiceRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/invoices")
                        .param("customerId", "1")
                        .param("invoiceNumber", "INV-001")
                        .param("issueDate", "2026-01-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void postInvoiceRejectsDueDateBeforeIssueDate() throws Exception {
        UserEntity user = new UserEntity();
        LocalDate issueDate = LocalDate.now().plusDays(5);
        LocalDate dueDate = LocalDate.now().plusDays(1);
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(customerService.findAllForUser(user)).thenReturn(List.of());

        mockMvc.perform(post("/invoices")
                        .with(csrf())
                        .param("customerId", "1")
                        .param("invoiceNumber", "INV-001")
                        .param("issueDate", issueDate.toString())
                        .param("dueDate", dueDate.toString())
                        .param("lineItems[0].itemName", "Service")
                        .param("lineItems[0].quantity", "1.00")
                        .param("lineItems[0].unitPrice", "10.00")
                        .param("lineItems[0].taxRate", "20.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("invoices/form"))
                .andExpect(model().attributeHasFieldErrors("invoiceDTO", "dueDateOnOrAfterIssueDate"));

        verify(invoiceService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void sendInvoiceRedirectsToDetail() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);

        mockMvc.perform(post("/invoices/1/send").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/1"));
    }
}
