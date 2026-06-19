package com.invoicingmanager.customer;

import com.invoicingmanager.user.UserEntity;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository must not be null");
    }

    @Transactional(readOnly = true)
    public List<CustomerEntity> findAllForUser(UserEntity user) {
        return customerRepository.findByUserOrderByNameAsc(user);
    }

    @Transactional(readOnly = true)
    public CustomerEntity findByIdForUser(Long id, UserEntity user) {
        return customerRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));
    }

    @Transactional
    public CustomerEntity create(CustomerDTO customerDTO, UserEntity user) {
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        apply(customer, customerDTO);
        return customerRepository.save(customer);
    }

    @Transactional
    public CustomerEntity update(Long id, CustomerDTO customerDTO, UserEntity user) {
        CustomerEntity customer = findByIdForUser(id, user);
        apply(customer, customerDTO);
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id, UserEntity user) {
        CustomerEntity customer = findByIdForUser(id, user);
        customerRepository.delete(customer);
    }

    public CustomerDTO toDTO(CustomerEntity customer) {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(customer.getId());
        customerDTO.setName(customer.getName());
        customerDTO.setEmail(customer.getEmail());
        customerDTO.setPhone(customer.getPhone());
        customerDTO.setBillingAddress(customer.getBillingAddress());
        customerDTO.setCity(customer.getCity());
        customerDTO.setCountry(customer.getCountry());
        customerDTO.setTaxNumber(customer.getTaxNumber());
        customerDTO.setNotes(customer.getNotes());
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
}
