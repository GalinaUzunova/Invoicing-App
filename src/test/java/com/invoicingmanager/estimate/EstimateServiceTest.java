package com.invoicingmanager.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
    void findByIdForUserReturnsEstimateOrThrowsNotFound() {
        UserEntity user = new UserEntity();
        EstimateEntity estimate = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(estimate));
        when(estimateRepository.findByIdAndUser(2L, user)).thenReturn(Optional.empty());

        EstimateService estimateService = service();

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
        when(estimateRepository.save(any(EstimateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EstimateEntity saved = service().create(dto, user);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getStatus()).isEqualTo(EstimateStatus.DRAFT);
        assertThat(saved.getGrandTotal()).isEqualByComparingTo("24.00");
    }

    @Test
    void createRejectsDuplicateQuotationNumberBeforeSaving() {
        UserEntity user = new UserEntity();
        EstimateEntity existing = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, "QUO-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().create(estimateDTO("QUO-001"), user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quotation number already exists");

        verify(estimateRepository, never()).save(any());
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
        when(estimateRepository.save(estimate)).thenReturn(estimate);

        EstimateEntity updated = service().update(3L, estimateDTO("QUO-002"), user);

        assertThat(updated.getQuotationNumber()).isEqualTo("QUO-002");
        assertThat(updated.getGrandTotal()).isEqualByComparingTo("24.00");
    }

    @Test
    void deleteRemovesExistingEstimate() {
        UserEntity user = new UserEntity();
        EstimateEntity estimate = estimate(EstimateStatus.DRAFT);
        when(estimateRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(estimate));

        service().delete(3L, user);

        verify(estimateRepository).delete(estimate);
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
        when(estimateRepository.save(any(EstimateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Line item unit price is required");
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
