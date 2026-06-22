package com.invoicingmanager.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.invoicingmanager.integration.IntegrationTestDataSeeder.SeededTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:main_pages_data_init_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.upload.dir=build/test-uploads"
})
@AutoConfigureMockMvc
@Transactional
@Import(IntegrationTestDataSeeder.class)
class MainPagesDataInitializationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationTestDataSeeder dataSeeder;

    private SeededTestData seededData;

    @BeforeEach
    void initializeApplicationData() {
        seededData = dataSeeder.seedMainApplicationData();
    }

    @Test
    void loginAndRegisterPagesArePublicWithoutInitializedSessionData() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Welcome back")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("Create account")));
    }

    @Test
    @WithMockUser(username = IntegrationTestDataSeeder.SEED_USER_EMAIL)
    void dashboardRendersSummaryAndRecentInvoicesFromInitializedData() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("Total revenue")))
                .andExpect(content().string(containsString(seededData.paidInvoice().getGrandTotal().toPlainString())))
                .andExpect(content().string(containsString(seededData.sentInvoice().getGrandTotal().toPlainString())))
                .andExpect(content().string(containsString("Recent invoices")))
                .andExpect(content().string(containsString(seededData.paidInvoice().getInvoiceNumber())))
                .andExpect(content().string(containsString(seededData.sentInvoice().getInvoiceNumber())))
                .andExpect(content().string(containsString(seededData.customer().getName())))
                .andExpect(content().string(not(containsString("No invoices yet"))));
    }

    @Test
    @WithMockUser(username = IntegrationTestDataSeeder.SEED_USER_EMAIL)
    void customersPageListsInitializedCustomerRecords() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(content().string(containsString(seededData.customer().getName())))
                .andExpect(content().string(containsString(seededData.customer().getEmail())))
                .andExpect(content().string(not(containsString("No customers yet"))));
    }

    @Test
    @WithMockUser(username = IntegrationTestDataSeeder.SEED_USER_EMAIL)
    void invoicesPageListsAllInitializedInvoiceStatuses() throws Exception {
        mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(view().name("invoices/list"))
                .andExpect(content().string(containsString(seededData.paidInvoice().getInvoiceNumber())))
                .andExpect(content().string(containsString(seededData.sentInvoice().getInvoiceNumber())))
                .andExpect(content().string(containsString(seededData.draftInvoice().getInvoiceNumber())))
                .andExpect(content().string(not(containsString("No invoices found"))));
    }

    @Test
    @WithMockUser(username = IntegrationTestDataSeeder.SEED_USER_EMAIL)
    void estimatesPageListsInitializedQuotationRecords() throws Exception {
        mockMvc.perform(get("/estimates"))
                .andExpect(status().isOk())
                .andExpect(view().name("estimates/list"))
                .andExpect(content().string(containsString(seededData.estimate().getQuotationNumber())))
                .andExpect(content().string(containsString(seededData.customer().getName())))
                .andExpect(content().string(not(containsString("No estimates found"))));
    }

    @Test
    @WithMockUser(username = IntegrationTestDataSeeder.SEED_USER_EMAIL)
    void companyPageShowsInitializedCompanyDetails() throws Exception {
        mockMvc.perform(get("/company"))
                .andExpect(status().isOk())
                .andExpect(view().name("company/form"))
                .andExpect(content().string(containsString(seededData.company().getCompanyName())))
                .andExpect(content().string(containsString(seededData.company().getEmail())))
                .andExpect(content().string(containsString(seededData.company().getPhone())));
    }

    @Test
    @WithMockUser(username = IntegrationTestDataSeeder.SEED_USER_EMAIL)
    void invoiceDetailPageRendersInitializedInvoiceData() throws Exception {
        mockMvc.perform(get("/invoices/{id}", seededData.paidInvoice().getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("invoices/detail"))
                .andExpect(content().string(containsString(seededData.paidInvoice().getInvoiceNumber())))
                .andExpect(content().string(containsString(seededData.customer().getName())))
                .andExpect(content().string(containsString("PAID")));
    }
}
