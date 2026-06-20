package com.invoicingmanager.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoicingmanager.user.UserEntity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.invocation.Invocation;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void findAllForUserReturnsRepositoryResults() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = customer("Acme Ltd");
        when(customerRepository.findByUserOrderByNameAsc(user)).thenReturn(List.of(customer));

        CustomerService customerService = new CustomerService(customerRepository);

        assertThat(customerService.findAllForUser(user)).containsExactly(customer);
    }

    @Test
    void findByIdForUserReturnsCustomerWhenFound() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = customer("Acme Ltd");
        when(customerRepository.findByIdAndUser(42L, user)).thenReturn(Optional.of(customer));

        CustomerService customerService = new CustomerService(customerRepository);

        assertThat(customerService.findByIdForUser(42L, user)).isSameAs(customer);
    }

    @Test
    void findByIdForUserThrowsNotFoundWhenMissing() {
        UserEntity user = new UserEntity();
        when(customerRepository.findByIdAndUser(42L, user)).thenReturn(Optional.empty());

        CustomerService customerService = new CustomerService(customerRepository);

        assertThatThrownBy(() -> customerService.findByIdForUser(42L, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void createTrimsFieldsAndSavesCustomerForUser() {
        UserEntity user = new UserEntity();
        CustomerDTO dto = customerDTO();

        CustomerService customerService = new CustomerService(customerRepository);
        CustomerEntity saved = customerService.create(dto, user);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo("Acme Ltd");
        assertThat(saved.getEmail()).isEqualTo("billing@acme.test");

        assertThat(savedCustomer().getPhone()).isEqualTo("+441234");
    }

    @Test
    void createThrowsExceptionWhenDtoIsNull() {
        UserEntity user = new UserEntity();

        assertThatThrownBy(() -> new CustomerService(customerRepository).create(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer is required");
    }

    @Test
    void createThrowsExceptionWhenUserIsNull() {
        CustomerDTO dto = customerDTO();

        assertThatThrownBy(() -> new CustomerService(customerRepository).create(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void createTrimsRequiredFieldsAndPreservesNullOptionalDtoFields() {
        UserEntity user = new UserEntity();
        CustomerDTO dto = customerDTO();
        dto.setBillingAddress(null);
        dto.setCity(null);
        dto.setCountry(null);
        dto.setTaxNumber(null);
        dto.setNotes(null);

        CustomerEntity saved = new CustomerService(customerRepository).create(dto, user);

        assertThat(saved.getName()).isEqualTo("Acme Ltd");
        assertThat(saved.getEmail()).isEqualTo("billing@acme.test");
        assertThat(saved.getPhone()).isEqualTo("+441234");
        assertThat(saved.getBillingAddress()).isNull();
        assertThat(saved.getCity()).isNull();
        assertThat(saved.getCountry()).isNull();
        assertThat(saved.getTaxNumber()).isNull();
        assertThat(saved.getNotes()).isNull();
    }

    @Test
    void updateAppliesDtoToExistingCustomer() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = customer("Old Name");
        when(customerRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(customer));

        CustomerService customerService = new CustomerService(customerRepository);
        CustomerEntity updated = customerService.update(7L, customerDTO(), user);

        assertThat(updated.getName()).isEqualTo("Acme Ltd");
        assertThat(updated.getBillingAddress()).isEqualTo("1 Main Street");
    }

    @Test
    void updateThrowsExceptionWhenRequiredParametersAreNull() {
        CustomerService customerService = new CustomerService(customerRepository);
        UserEntity user = new UserEntity();
        CustomerDTO dto = customerDTO();

        assertThatThrownBy(() -> customerService.update(null, dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer id is required");
        assertThatThrownBy(() -> customerService.update(7L, null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer is required");
        assertThatThrownBy(() -> customerService.update(7L, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void deleteRemovesExistingCustomer() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = customer("Acme Ltd");
        when(customerRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(customer));

        CustomerService customerService = new CustomerService(customerRepository);
        customerService.delete(7L, user);

        verify(customerRepository).delete(Objects.requireNonNull(customer, "customer must not be null"));
    }

    @Test
    void lookupAndDeleteMethodsThrowExceptionWhenRequiredParametersAreNull() {
        CustomerService customerService = new CustomerService(customerRepository);
        UserEntity user = new UserEntity();

        assertThatThrownBy(() -> customerService.findAllForUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> customerService.findByIdForUser(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer id is required");
        assertThatThrownBy(() -> customerService.findByIdAndUser(7L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> customerService.delete(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer id is required");
    }

    @Test
    void toDTOCopiesCustomerFields() {
        CustomerEntity customer = customer("Acme Ltd");
        customer.setId(5L);
        customer.setEmail("billing@acme.test");
        customer.setPhone("+441234");
        customer.setBillingAddress("1 Main Street");
        customer.setCity("London");
        customer.setCountry("UK");
        customer.setTaxNumber("GB123");
        customer.setNotes("Important");

        CustomerService customerService = new CustomerService(customerRepository);
        CustomerDTO dto = customerService.toDTO(customer);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getName()).isEqualTo("Acme Ltd");
        assertThat(dto.getTaxNumber()).isEqualTo("GB123");
    }

    @Test
    void toDTOThrowsExceptionWhenCustomerIsNull() {
        assertThatThrownBy(() -> new CustomerService(customerRepository).toDTO(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer is required");
    }

    @Test
    void createRejectsBlankRequiredContactFields() {
        CustomerDTO dto = customerDTO();
        dto.setEmail(" ");

        CustomerService customerService = new CustomerService(customerRepository);

        assertThatThrownBy(() -> customerService.create(dto, new UserEntity()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer email is required");
    }

    @Test
    void constructorRejectsNullRepository() {
        assertThatThrownBy(() -> new CustomerService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerRepository");
    }

    private CustomerEntity customer(String name) {
        CustomerEntity customer = new CustomerEntity();
        customer.setName(name);
        return customer;
    }

    private CustomerDTO customerDTO() {
        CustomerDTO dto = new CustomerDTO();
        dto.setName(" Acme Ltd ");
        dto.setEmail(" billing@acme.test ");
        dto.setPhone(" +441234 ");
        dto.setBillingAddress(" 1 Main Street ");
        dto.setCity(" London ");
        dto.setCountry(" UK ");
        dto.setTaxNumber(" GB123 ");
        dto.setNotes(" Important ");
        return dto;
    }

    private CustomerEntity savedCustomer() {
        return mockingDetails(customerRepository).getInvocations().stream()
                .filter(invocation -> "save".equals(invocation.getMethod().getName()))
                .findFirst()
                .map(this::customerArgument)
                .orElseThrow(() -> new AssertionError("Expected customer to be saved."));
    }

    private CustomerEntity customerArgument(Invocation invocation) {
        Object argument = invocation.getArgument(0);
        assertThat(argument).isInstanceOf(CustomerEntity.class);
        return (CustomerEntity) Objects.requireNonNull(argument, "saved customer must not be null");
    }
}
