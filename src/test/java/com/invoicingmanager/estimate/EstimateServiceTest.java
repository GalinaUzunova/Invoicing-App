package com.invoicingmanager.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EstimateServiceTest {

    @Mock
    private EstimateRepository estimateRepository;

    @Mock
    private CustomerRepository customerRepository;

    private final EstimateCalculator estimateCalculator = new EstimateCalculator();

    @Test
    void newEstimateDTOInitializesDefaults() {
        EstimateDTO dto = service().newEstimateDTO(9L);

        assertThat(dto.getCustomerId()).isEqualTo(9L);
        assertThat(dto.getQuotationNumber()).startsWith("QUO-");
        assertThat(dto.getIssueDate()).isEqualTo(LocalDate.now());
        assertThat(dto.getLineItems()).hasSize(1);
    }

    @Test
    void newEstimateDTOHandlesNullOptionalCustomerIdGracefully() {
        EstimateDTO dto = service().newEstimateDTO(null);

        assertThat(dto.getCustomerId()).isNull();
        assertThat(dto.getQuotationNumber()).startsWith("QUO-");
        assertThat(dto.getLineItems()).hasSize(1);
    }

    @Test
    void findAllForUserUsesAllOrStatusQuery() {
        UserEntity user = new UserEntity();
        EstimateEntity draft = estimate(EstimateStatus.DRAFT);
        EstimateEntity sent = estimate(EstimateStatus.SENT);
        when(estimateRepository.findByUserOrderByIssueDateDescCreatedAtDesc(user)).thenReturn(List.of(draft));
        when(estimateRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(user, EstimateStatus.SENT)).thenReturn(List.of(sent));

        EstimateService estimateService = service();

        assertThat(estimateService.findAllForUser(user, null)).containsExactly(draft);
        assertThat(estimateService.findAllForUser(user, EstimateStatus.SENT)).containsExactly(sent);
    }

    @Test
    void findByCustomerDelegatesToRepository() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = new CustomerEntity();
        EstimateEntity estimate = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(customer, user)).thenReturn(List.of(estimate));

        assertThat(service().findByCustomer(customer, user)).containsExactly(estimate);
    }

    @Test
    void findMethodsThrowExceptionWhenRequiredParametersAreNull() {
        EstimateService estimateService = service();
        UserEntity user = new UserEntity();
        CustomerEntity customer = new CustomerEntity();

        assertThatThrownBy(() -> estimateService.findAllForUser(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> estimateService.findByCustomer(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customer is required");
        assertThatThrownBy(() -> estimateService.findByCustomer(customer, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> estimateService.findByIdAndUser(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate id is required");
        assertThatThrownBy(() -> estimateService.findByIdForUser(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void findByIdMethodsReturnEstimateOrThrowNotFound() {
        UserEntity user = new UserEntity();
        EstimateEntity estimate = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(estimate));
        when(estimateRepository.findByIdAndUser(2L, user)).thenReturn(Optional.empty());

        EstimateService estimateService = service();

        assertThat(estimateService.findByIdAndUser(1L, user)).containsSame(estimate);
        assertThat(estimateService.findByIdForUser(1L, user)).isSameAs(estimate);
        assertThatThrownBy(() -> estimateService.findByIdForUser(2L, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Estimate not found");
    }

    @Test
    void createSavesEstimateWithCalculatedTotals() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = customer(5L);
        EstimateDTO dto = estimateDTO("QUO-001");
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-001")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer));

        EstimateEntity saved = service().create(dto, user);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getStatus()).isEqualTo(EstimateStatus.DRAFT);
        assertThat(saved.getGrandTotal()).isEqualByComparingTo("24.00");
    }

    @Test
    void createThrowsExceptionWhenRequiredParametersAreNull() {
        EstimateDTO dto = estimateDTO("QUO-001");
        UserEntity user = new UserEntity();

        assertThatThrownBy(() -> service().create(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate is required");
        assertThatThrownBy(() -> service().create(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void createRejectsDuplicateQuotationNumberBeforeSaving() {
        UserEntity user = new UserEntity();
        EstimateEntity existing = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().create(estimateDTO("QUO-001"), user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quotation number already exists");

        assertThat(org.mockito.Mockito.mockingDetails(estimateRepository).getInvocations())
                .noneMatch(invocation -> "save".equals(invocation.getMethod().getName()));
    }

    @Test
    void updateAppliesChangesToExistingEstimate() {
        UserEntity user = new UserEntity();
        EstimateEntity estimate = estimate(EstimateStatus.DRAFT);
        estimate.setId(3L);
        CustomerEntity customer = customer(5L);
        when(estimateRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(estimate));
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-002")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer));

        EstimateEntity updated = service().update(3L, estimateDTO("QUO-002"), user);

        assertThat(updated.getQuotationNumber()).isEqualTo("QUO-002");
        assertThat(updated.getGrandTotal()).isEqualByComparingTo("24.00");
        verify(estimateRepository).save(estimate);
    }

    @Test
    void updateThrowsExceptionWhenRequiredParametersAreNull() {
        EstimateDTO dto = estimateDTO("QUO-001");
        UserEntity user = new UserEntity();

        assertThatThrownBy(() -> service().update(null, dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate id is required");
        assertThatThrownBy(() -> service().update(3L, null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate is required");
        assertThatThrownBy(() -> service().update(3L, dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void deleteRemovesExistingEstimate() {
        UserEntity user = new UserEntity();
        EstimateEntity estimate = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(estimate));

        service().delete(3L, user);

        verify(estimateRepository).delete(Objects.requireNonNull(estimate, "estimate must not be null"));
    }

    @Test
    void deleteAndStatusMethodsThrowExceptionWhenRequiredParametersAreNull() {
        UserEntity user = new UserEntity();

        assertThatThrownBy(() -> service().delete(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate id is required");
        assertThatThrownBy(() -> service().delete(3L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> service().markSent(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate id is required");
        assertThatThrownBy(() -> service().markAccepted(3L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> service().markDeclined(null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate id is required");
    }

    @Test
    void markSentRejectsFinalisedEstimates() {
        UserEntity user = new UserEntity();
        EstimateEntity accepted = estimate(EstimateStatus.ACCEPTED);
        when(estimateRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(accepted));

        assertThatThrownBy(() -> service().markSent(3L, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Finalised estimates");
    }

    @Test
    void statusTransitionsUpdateEstimateStatus() {
        UserEntity user = new UserEntity();
        EstimateEntity draft = estimate(EstimateStatus.DRAFT);
        EstimateEntity sent = estimate(EstimateStatus.SENT);
        EstimateEntity anotherSent = estimate(EstimateStatus.SENT);
        when(estimateRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(draft));
        when(estimateRepository.findByIdAndUser(2L, user)).thenReturn(Optional.of(sent));
        when(estimateRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(anotherSent));

        assertThat(service().markSent(1L, user).getStatus()).isEqualTo(EstimateStatus.SENT);
        assertThat(service().markAccepted(2L, user).getStatus()).isEqualTo(EstimateStatus.ACCEPTED);
        assertThat(service().markDeclined(3L, user).getStatus()).isEqualTo(EstimateStatus.DECLINED);
    }

    @Test
    void toDTOCopiesEstimateAndLineItems() {
        EstimateEntity estimate = estimate(EstimateStatus.SENT);
        estimate.setId(10L);
        estimate.setCustomer(customer(5L));
        estimate.setQuotationNumber("QUO-010");
        estimate.setIssueDate(LocalDate.of(2026, 1, 1));
        estimate.setExpiryDate(LocalDate.of(2026, 1, 15));
        estimate.setNotes("Notes");
        estimate.addLineItem(lineItemEntity());

        EstimateDTO dto = service().toDTO(estimate);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getCustomerId()).isEqualTo(5L);
        assertThat(dto.getQuotationNumber()).isEqualTo("QUO-010");
        assertThat(dto.getLineItems()).hasSize(1);
    }

    @Test
    void toDTOThrowsExceptionWhenEstimateIsNull() {
        assertThatThrownBy(() -> service().toDTO(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimate is required");
    }

    @Test
    void createRequiresCustomerAndLineItems() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-001");
        dto.setCustomerId(null);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Customer is required");

        EstimateDTO noLines = estimateDTO("QUO-002");
        noLines.getLineItems().clear();
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-002")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(noLines, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("At least one line item");

        EstimateDTO noIssueDate = estimateDTO("QUO-006");
        noIssueDate.setIssueDate(null);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-006")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().create(noIssueDate, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Issue date is required");
    }

    @Test
    void createRejectsExpiryDateBeforeIssueDate() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-003");
        dto.setIssueDate(LocalDate.now().plusDays(5));
        dto.setExpiryDate(LocalDate.now().plusDays(1));
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-003")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Expiry date cannot be earlier than issue date");
    }

    @Test
    void createRejectsBlankLineItemName() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-004");
        dto.getLineItems().get(0).setItemName(" ");
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-004")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line item name is required");
    }

    @Test
    void createRejectsMissingLineItemQuantityOrPrice() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-005");
        dto.getLineItems().get(0).setUnitPrice(null);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-005")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line item unit price cannot be negative");
    }

    @Test
    void createAllowsZeroUnitPrice() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-008");
        dto.getLineItems().get(0).setUnitPrice(BigDecimal.ZERO);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-008")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        EstimateEntity saved = service().create(dto, user);

        assertThat(saved.getLineItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getGrandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createRejectsNegativeUnitPrice() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-009");
        dto.getLineItems().get(0).setUnitPrice(new BigDecimal("-0.01"));
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-009")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line item unit price cannot be negative");
    }

    @Test
    void createRejectsNullUnitPrice() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-010");
        dto.getLineItems().get(0).setUnitPrice(null);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-010")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line item unit price cannot be negative");
    }

    @Test
    void createRejectsNullLineItemEntry() {
        UserEntity user = new UserEntity();
        EstimateDTO dto = estimateDTO("QUO-007");
        dto.getLineItems().add(null);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-007")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line item is required");
    }

    @Test
    void constructorRejectsNullRepository() {
        assertThatThrownBy(() -> new EstimateService(null, customerRepository, estimateCalculator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("estimateRepository");
    }

    @Test
    void constructorRejectsNullCustomerRepository() {
        assertThatThrownBy(() -> new EstimateService(estimateRepository, null, estimateCalculator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerRepository");
    }

    @Test
    void constructorRejectsNullCalculator() {
        assertThatThrownBy(() -> new EstimateService(estimateRepository, customerRepository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("estimateCalculator");
    }

    private EstimateService service() {
        return new EstimateService(estimateRepository, customerRepository, estimateCalculator);
    }

    private EstimateDTO estimateDTO(String quotationNumber) {
        EstimateDTO dto = new EstimateDTO();
        dto.setCustomerId(5L);
        dto.setQuotationNumber(quotationNumber);
        dto.setIssueDate(LocalDate.of(2026, 1, 1));
        dto.setExpiryDate(LocalDate.of(2026, 1, 15));
        dto.getLineItems().add(lineItemDTO());
        return dto;
    }

    private EstimateLineItemDTO lineItemDTO() {
        EstimateLineItemDTO dto = new EstimateLineItemDTO();
        dto.setItemName("Service");
        dto.setQuantity(new BigDecimal("2.00"));
        dto.setUnitPrice(new BigDecimal("10.00"));
        dto.setTaxRate(new BigDecimal("20.00"));
        return dto;
    }

    private EstimateLineItemEntity lineItemEntity() {
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName("Service");
        lineItem.setQuantity(new BigDecimal("2.00"));
        lineItem.setUnitPrice(new BigDecimal("10.00"));
        lineItem.setTaxRate(new BigDecimal("20.00"));
        return lineItem;
    }

    private EstimateEntity estimate(EstimateStatus status) {
        EstimateEntity estimate = new EstimateEntity();
        estimate.setStatus(status);
        return estimate;
    }

    private CustomerEntity customer(Long id) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id);
        customer.setName("Acme Ltd");
        return customer;
    }
}
