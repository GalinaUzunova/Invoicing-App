package com.invoicingmanager.customer;

import com.invoicingmanager.user.UserEntity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<CustomerEntity> findAllForUser(UserEntity user) {
        return customerRepository.findByUserOrderByNameAsc(requireArgument(user, "user"));
    }

    @Transactional(readOnly = true)
    public CustomerEntity findByIdForUser(Long id, UserEntity user) {
        Long requiredId = requireArgument(id, "customer id");
        UserEntity requiredUser = requireArgument(user, "user");
        return findByIdAndUser(requiredId, requiredUser).orElseThrow(() -> customerNotFound(requiredId, requiredUser));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerEntity> findByIdAndUser(Long id, UserEntity user) {
        return customerRepository.findByIdAndUser(requireArgument(id, "customer id"), requireArgument(user, "user"));
    }

    @Transactional
    public CustomerEntity create(CustomerDTO customerDTO, UserEntity user) {
        CustomerDTO requiredCustomerDTO = requireArgument(customerDTO, "customer");
        UserEntity requiredUser = requireArgument(user, "user");
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(requiredUser);
        apply(customer, requiredCustomerDTO);
        customerRepository.save(customer);
        return customer;
    }

    @Transactional
    public CustomerEntity update(Long id, CustomerDTO customerDTO, UserEntity user) {
        Long requiredId = requireArgument(id, "customer id");
        CustomerDTO requiredCustomerDTO = requireArgument(customerDTO, "customer");
        UserEntity requiredUser = requireArgument(user, "user");
        return findByIdAndUser(requiredId, requiredUser)
                .map(customer -> updateCustomer(customer, requiredCustomerDTO))
                .orElseThrow(() -> customerNotFound(requiredId, requiredUser));
    }

    @Transactional
    public void delete(Long id, UserEntity user) {
        Long requiredId = requireArgument(id, "customer id");
        UserEntity requiredUser = requireArgument(user, "user");
        findByIdAndUser(requiredId, requiredUser)
                .ifPresentOrElse(
                        customerRepository::delete,
                        () -> {
                            throw customerNotFound(requiredId, requiredUser);
                        }
                );
    }

    public CustomerDTO toDTO(CustomerEntity customer) {
        CustomerEntity requiredCustomer = requireArgument(customer, "customer");
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(requiredCustomer.getId());
        customerDTO.setName(requiredCustomer.getName());
        customerDTO.setEmail(requiredCustomer.getEmail());
        customerDTO.setPhone(requiredCustomer.getPhone());
        customerDTO.setBillingAddress(requiredCustomer.getBillingAddress());
        customerDTO.setCity(requiredCustomer.getCity());
        customerDTO.setCountry(requiredCustomer.getCountry());
        customerDTO.setTaxNumber(requiredCustomer.getTaxNumber());
        customerDTO.setNotes(requiredCustomer.getNotes());
        return customerDTO;
    }

    private void apply(CustomerEntity customer, CustomerDTO customerDTO) {
        customer.setName(trimRequired(customerDTO.getName(), "Customer name"));
        customer.setEmail(trimRequired(customerDTO.getEmail(), "Customer email"));
        customer.setPhone(trimRequired(customerDTO.getPhone(), "Customer phone"));
        customer.setBillingAddress(trim(customerDTO.getBillingAddress()));
        customer.setCity(trim(customerDTO.getCity()));
        customer.setCountry(trim(customerDTO.getCountry()));
        customer.setTaxNumber(trim(customerDTO.getTaxNumber()));
        customer.setNotes(trim(customerDTO.getNotes()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimRequired(String value, String fieldName) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return trimmed;
    }

    private CustomerEntity updateCustomer(CustomerEntity customer, CustomerDTO customerDTO) {
        apply(customer, customerDTO);
        CustomerEntity customerToSave = Objects.requireNonNull(customer, "customer must not be null");
        customerRepository.save(customerToSave);
        return customerToSave;
    }

    private ResponseStatusException customerNotFound(Long id, UserEntity user) {
        log.warn("Customer {} was not found for user {}", id, user.getEmail());
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found.");
    }

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
