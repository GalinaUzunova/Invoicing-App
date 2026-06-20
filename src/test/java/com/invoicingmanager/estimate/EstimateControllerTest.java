package com.invoicingmanager.estimate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.invoicingmanager.invoice.InvoiceEntity;
import com.invoicingmanager.invoice.InvoiceService;
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

@WebMvcTest(EstimateController.class)
@Import(SecurityConfig.class)
class EstimateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstimateService estimateService;

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
    void estimatesRedirectAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/estimates"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void listShowsEstimatesForAuthenticatedUser() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(estimateService.findAllForUser(user, null)).thenReturn(List.of());

        mockMvc.perform(get("/estimates"))
                .andExpect(status().isOk())
                .andExpect(view().name("estimates/list"))
                .andExpect(model().attributeExists("estimates", "statuses"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void newEstimateFormIsAvailable() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(estimateService.newEstimateDTO(null)).thenReturn(new EstimateDTO());
        when(customerService.findAllForUser(user)).thenReturn(List.of());

        mockMvc.perform(get("/estimates/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("estimates/form"))
                .andExpect(model().attributeExists("estimateDTO", "customers", "formAction"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void detailIncludesEstimateAndCompanyDetails() throws Exception {
        UserEntity user = new UserEntity();
        CustomerEntity customer = new CustomerEntity();
        customer.setId(5L);
        customer.setName("Acme Ltd");
        EstimateEntity estimate = new EstimateEntity();
        estimate.setId(1L);
        estimate.setCustomer(customer);
        estimate.setQuotationNumber("QUO-001");
        estimate.setStatus(EstimateStatus.DRAFT);
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(estimateService.findByIdForUser(1L, user)).thenReturn(estimate);
        when(companyDetailsService.getForUser(user)).thenReturn(new CompanyDetailsEntity());

        mockMvc.perform(get("/estimates/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("estimates/detail"))
                .andExpect(model().attributeExists("estimate", "company"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void downloadPdfReturnsEstimatePdf() throws Exception {
        UserEntity user = new UserEntity();
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        CustomerEntity customer = new CustomerEntity();
        customer.setId(5L);
        customer.setName("Acme Ltd");
        EstimateEntity estimate = new EstimateEntity();
        estimate.setId(1L);
        estimate.setCustomer(customer);
        estimate.setQuotationNumber("QUO-001");
        estimate.setStatus(EstimateStatus.DRAFT);
        byte[] pdf = "%PDF-test".getBytes();

        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(estimateService.findByIdForUser(1L, user)).thenReturn(estimate);
        when(companyDetailsService.getForUser(user)).thenReturn(company);
        when(companyDetailsService.getLogoResource(user)).thenReturn(Optional.empty());
        when(documentPdfService.generateEstimatePdf(estimate, company, null)).thenReturn(pdf);

        mockMvc.perform(get("/estimates/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"quotation-QUO-001.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void postEstimateRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/estimates")
                        .param("customerId", "1")
                        .param("quotationNumber", "QUO-001")
                        .param("issueDate", "2026-01-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void postEstimateRejectsExpiryDateBeforeIssueDate() throws Exception {
        UserEntity user = new UserEntity();
        LocalDate issueDate = LocalDate.now().plusDays(5);
        LocalDate expiryDate = LocalDate.now().plusDays(1);
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(customerService.findAllForUser(user)).thenReturn(List.of());

        mockMvc.perform(post("/estimates")
                        .with(csrf())
                        .param("customerId", "1")
                        .param("quotationNumber", "QUO-001")
                        .param("issueDate", issueDate.toString())
                        .param("expiryDate", expiryDate.toString())
                        .param("lineItems[0].itemName", "Service")
                        .param("lineItems[0].quantity", "1.00")
                        .param("lineItems[0].unitPrice", "10.00")
                        .param("lineItems[0].taxRate", "20.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("estimates/form"))
                .andExpect(model().attributeHasFieldErrors("estimateDTO", "expiryDateOnOrAfterIssueDate"));

        verify(estimateService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void acceptEstimateRedirectsToDetail() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);

        mockMvc.perform(post("/estimates/1/accepted").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estimates/1"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void copyEstimateToInvoiceRedirectsToNewInvoice() throws Exception {
        UserEntity user = new UserEntity();
        EstimateEntity estimate = new EstimateEntity();
        estimate.setId(1L);
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(9L);

        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(estimateService.findByIdForUser(1L, user)).thenReturn(estimate);
        when(invoiceService.createFromEstimate(estimate, user)).thenReturn(invoice);

        mockMvc.perform(post("/estimates/1/copy-to-invoice").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/9"));
    }
}
