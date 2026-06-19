package com.invoicingmanager.invoice;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    private final InvoiceCalculator invoiceCalculator = new InvoiceCalculator();

    @Test
    void newInvoiceDTOInitializesDefaults() {
        InvoiceDTO dto = service().newInvoiceDTO(9L);

        assertThat(dto.getCustomerId()).isEqualTo(9L);
        assertThat(dto.getInvoiceNumber()).startsWith("INV-");
        assertThat(dto.getIssueDate()).isEqualTo(LocalDate.now());
        assertThat(dto.getLineItems()).hasSize(1);
    }

    @Test
    void findAllForUserUsesAllOrStatusQuery() {
        UserEntity user = new UserEntity();
        InvoiceEntity draft = invoice(InvoiceStatus.DRAFT);
        InvoiceEntity sent = invoice(InvoiceStatus.SENT);
        when(invoiceRepository.findByUserOrderByIssueDateDescCreatedAtDesc(user)).thenReturn(List.of(draft));
        when(invoiceRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(user, InvoiceStatus.SENT)).thenReturn(List.of(sent));

        InvoiceService invoiceService = service();

        assertThat(invoiceService.findAllForUser(user, null)).containsExactly(draft);
        assertThat(invoiceService.findAllForUser(user, InvoiceStatus.SENT)).containsExactly(sent);
    }

    @Test
    void findByCustomerDelegatesToRepository() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = new CustomerEntity();
        InvoiceEntity invoice = invoice(InvoiceStatus.DRAFT);
        when(invoiceRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(customer, user)).thenReturn(List.of(invoice));

        assertThat(service().findByCustomer(customer, user)).containsExactly(invoice);
    }

    @Test
    void findByIdForUserReturnsInvoiceOrThrowsNotFound() {
        UserEntity user = new UserEntity();
        InvoiceEntity invoice = invoice(InvoiceStatus.DRAFT);
        when(invoiceRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByIdAndUser(2L, user)).thenReturn(Optional.empty());

        InvoiceService invoiceService = service();

        assertThat(invoiceService.findByIdForUser(1L, user)).isSameAs(invoice);
        assertThatThrownBy(() -> invoiceService.findByIdForUser(2L, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invoice not found");
    }

    @Test
    void createSavesInvoiceWithCalculatedTotals() {
        UserEntity user = new UserEntity();
        CustomerEntity customer = customer(5L);
        InvoiceDTO dto = invoiceDTO("INV-001");
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-001")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer));
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceEntity saved = service().create(dto, user);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(saved.getGrandTotal()).isEqualByComparingTo("24.00");

        ArgumentCaptor<InvoiceEntity> captor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getLineItems()).hasSize(1);
    }

    @Test
    void createRejectsDuplicateInvoiceNumberBeforeSaving() {
        UserEntity user = new UserEntity();
        InvoiceEntity existing = invoice(InvoiceStatus.DRAFT);
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service().create(invoiceDTO("INV-001"), user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice number already exists");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updateAppliesChangesToExistingInvoice() {
        UserEntity user = new UserEntity();
        InvoiceEntity invoice = invoice(InvoiceStatus.DRAFT);
        invoice.setId(3L);
        CustomerEntity customer = customer(5L);
        when(invoiceRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-002")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        InvoiceEntity updated = service().update(3L, invoiceDTO("INV-002"), user);

        assertThat(updated.getInvoiceNumber()).isEqualTo("INV-002");
        assertThat(updated.getGrandTotal()).isEqualByComparingTo("24.00");
    }

    @Test
    void deleteRemovesExistingInvoice() {
        UserEntity user = new UserEntity();
        InvoiceEntity invoice = invoice(InvoiceStatus.DRAFT);
        when(invoiceRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(invoice));

        service().delete(3L, user);

        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void markSentRejectsPaidInvoices() {
        UserEntity user = new UserEntity();
        InvoiceEntity invoice = invoice(InvoiceStatus.PAID);
        when(invoiceRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service().markSent(3L, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Paid invoices cannot be sent");
    }

    @Test
    void markSentAndMarkPaidUpdateStatus() {
        UserEntity user = new UserEntity();
        InvoiceEntity draft = invoice(InvoiceStatus.DRAFT);
        InvoiceEntity sent = invoice(InvoiceStatus.SENT);
        when(invoiceRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(draft));
        when(invoiceRepository.findByIdAndUser(2L, user)).thenReturn(Optional.of(sent));
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service().markSent(1L, user).getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(service().markPaid(2L, user).getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void toDTOCopiesInvoiceAndLineItems() {
        InvoiceEntity invoice = invoice(InvoiceStatus.SENT);
        invoice.setId(10L);
        invoice.setCustomer(customer(5L));
        invoice.setInvoiceNumber("INV-010");
        invoice.setIssueDate(LocalDate.of(2026, 1, 1));
        invoice.setDueDate(LocalDate.of(2026, 1, 15));
        invoice.setNotes("Notes");
        invoice.addLineItem(lineItemDTOEntity());

        InvoiceDTO dto = service().toDTO(invoice);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getCustomerId()).isEqualTo(5L);
        assertThat(dto.getInvoiceNumber()).isEqualTo("INV-010");
        assertThat(dto.getLineItems()).hasSize(1);
    }

    @Test
    void createRequiresCustomerAndLineItems() {
        UserEntity user = new UserEntity();
        InvoiceDTO dto = invoiceDTO("INV-001");
        dto.setCustomerId(null);
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Customer is required");

        InvoiceDTO noLines = invoiceDTO("INV-002");
        noLines.getLineItems().clear();
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-002")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(noLines, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("At least one line item");
    }

    @Test
    void createRejectsDueDateBeforeIssueDate() {
        UserEntity user = new UserEntity();
        InvoiceDTO dto = invoiceDTO("INV-003");
        dto.setIssueDate(LocalDate.now().plusDays(5));
        dto.setDueDate(LocalDate.now().plusDays(1));
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-003")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Due date cannot be earlier than issue date");
    }

    @Test
    void createRejectsBlankLineItemName() {
        UserEntity user = new UserEntity();
        InvoiceDTO dto = invoiceDTO("INV-004");
        dto.getLineItems().get(0).setItemName(" ");
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-004")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line item name is required");
    }

    @Test
    void createRejectsMissingLineItemQuantityOrPrice() {
        UserEntity user = new UserEntity();
        InvoiceDTO dto = invoiceDTO("INV-005");
        dto.getLineItems().get(0).setQuantity(null);
        when(invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, "INV-005")).thenReturn(Optional.empty());
        when(customerRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(customer(5L)));

        assertThatThrownBy(() -> service().create(dto, user))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Line item quantity is required");
    }

    private InvoiceService service() {
        return new InvoiceService(invoiceRepository, customerRepository, invoiceCalculator);
    }

    private InvoiceDTO invoiceDTO(String invoiceNumber) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setCustomerId(5L);
        dto.setInvoiceNumber(invoiceNumber);
        dto.setIssueDate(LocalDate.of(2026, 1, 1));
        dto.setDueDate(LocalDate.of(2026, 1, 15));
        dto.getLineItems().add(lineItemDTO());
        return dto;
    }

    private InvoiceLineItemDTO lineItemDTO() {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemName("Service");
        dto.setQuantity(new BigDecimal("2.00"));
        dto.setUnitPrice(new BigDecimal("10.00"));
        dto.setTaxRate(new BigDecimal("20.00"));
        return dto;
    }

    private InvoiceLineItemEntity lineItemDTOEntity() {
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
        lineItem.setItemName("Service");
        lineItem.setQuantity(new BigDecimal("2.00"));
        lineItem.setUnitPrice(new BigDecimal("10.00"));
        lineItem.setTaxRate(new BigDecimal("20.00"));
        return lineItem;
    }

    private InvoiceEntity invoice(InvoiceStatus status) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setStatus(status);
        return invoice;
    }

    private CustomerEntity customer(Long id) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id);
        customer.setName("Acme Ltd");
        return customer;
    }
}
