package com.invoicingmanager.customer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.invoicingmanager.invoice.InvoiceService;
import com.invoicingmanager.security.SecurityConfig;
import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private UserService userService;

    @Test
    void customersRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void listShowsCustomersForAuthenticatedUser() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(customerService.findAllForUser(user)).thenReturn(List.of());

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attributeExists("customers"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void postCustomerRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/customers")
                        .param("name", "Acme Ltd"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void newCustomerFormIsAvailableToAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/customers/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/form"))
                .andExpect(model().attributeExists("customerDTO"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void invalidCustomerIdShowsBadRequestErrorPage() throws Exception {
        mockMvc.perform(get("/customers/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("statusCode", 400));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void missingCustomerShowsNotFoundErrorPage() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(customerService.findByIdForUser(99L, user))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));

        mockMvc.perform(get("/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("statusCode", 404))
                .andExpect(model().attribute("errorMessage", "Customer not found."));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void invalidCustomerServiceRequestShowsBadRequestErrorPage() throws Exception {
        UserEntity user = new UserEntity();
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName("Acme Ltd");
        customerDTO.setEmail("billing@acme.test");
        customerDTO.setPhone("+441234");

        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(customerService.create(org.mockito.ArgumentMatchers.any(CustomerDTO.class), org.mockito.ArgumentMatchers.eq(user)))
                .thenThrow(new IllegalArgumentException("Customer phone is required."));

        mockMvc.perform(post("/customers")
                        .with(Objects.requireNonNull(csrf(), "csrf post processor must not be null"))
                        .param("name", customerDTO.getName())
                        .param("email", customerDTO.getEmail())
                        .param("phone", customerDTO.getPhone()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("statusCode", 400))
                .andExpect(model().attribute("errorMessage", "Customer phone is required."));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void customerDeleteBlockedByRelatedRecordsShowsBadRequestErrorPage() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        doThrow(new DataIntegrityViolationException("customer has related invoices"))
                .when(customerService).delete(7L, user);

        mockMvc.perform(post("/customers/7/delete")
                        .with(Objects.requireNonNull(csrf(), "csrf post processor must not be null")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("statusCode", 400))
                .andExpect(model().attribute("errorMessage", "This action cannot be completed because related records still exist."));
    }
}
