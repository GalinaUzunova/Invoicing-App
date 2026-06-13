package com.invoicingmanager.invoice;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.user.UserEntity;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceCalculator invoiceCalculator;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            InvoiceCalculator invoiceCalculator
    ) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.invoiceCalculator = invoiceCalculator;
    }

    public InvoiceDTO newInvoiceDTO(Long customerId) {
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setCustomerId(customerId);
        invoiceDTO.setInvoiceNumber(generateInvoiceNumber());
        invoiceDTO.setIssueDate(LocalDate.now());
        invoiceDTO.getLineItems().add(new InvoiceLineItemDTO());
        return invoiceDTO;
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> findAllForUser(UserEntity user, InvoiceStatus status) {
        if (status == null) {
            return invoiceRepository.findByUserOrderByIssueDateDescCreatedAtDesc(user);
        }

        return invoiceRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(user, status);
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> findByCustomer(CustomerEntity customer, UserEntity user) {
        return invoiceRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(customer, user);
    }

    @Transactional(readOnly = true)
    public InvoiceEntity findByIdForUser(Long id, UserEntity user) {
        return invoiceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found."));
    }

    @Transactional
    public InvoiceEntity create(InvoiceDTO invoiceDTO, UserEntity user) {
        assertInvoiceNumberAvailable(invoiceDTO.getInvoiceNumber(), null, user);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setUser(user);
        invoice.setStatus(InvoiceStatus.DRAFT);
        apply(invoice, invoiceDTO, user);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoiceEntity update(Long id, InvoiceDTO invoiceDTO, UserEntity user) {
        InvoiceEntity invoice = findByIdForUser(id, user);
        assertInvoiceNumberAvailable(invoiceDTO.getInvoiceNumber(), id, user);
        apply(invoice, invoiceDTO, user);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void delete(Long id, UserEntity user) {
        InvoiceEntity invoice = findByIdForUser(id, user);
        invoiceRepository.delete(invoice);
    }

    @Transactional
    public InvoiceEntity markSent(Long id, UserEntity user) {
        InvoiceEntity invoice = findByIdForUser(id, user);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paid invoices cannot be sent again.");
        }

        invoice.setStatus(InvoiceStatus.SENT);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoiceEntity markPaid(Long id, UserEntity user) {
        InvoiceEntity invoice = findByIdForUser(id, user);
        invoice.setStatus(InvoiceStatus.PAID);
        return invoiceRepository.save(invoice);
    }

    public InvoiceDTO toDTO(InvoiceEntity invoice) {
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setId(invoice.getId());
        invoiceDTO.setCustomerId(invoice.getCustomer().getId());
        invoiceDTO.setInvoiceNumber(invoice.getInvoiceNumber());
        invoiceDTO.setStatus(invoice.getStatus());
        invoiceDTO.setIssueDate(invoice.getIssueDate());
        invoiceDTO.setDueDate(invoice.getDueDate());
        invoiceDTO.setNotes(invoice.getNotes());

        for (InvoiceLineItemEntity lineItem : invoice.getLineItems()) {
            InvoiceLineItemDTO lineItemDTO = new InvoiceLineItemDTO();
            lineItemDTO.setItemName(lineItem.getItemName());
            lineItemDTO.setDescription(lineItem.getDescription());
            lineItemDTO.setQuantity(lineItem.getQuantity());
            lineItemDTO.setUnitPrice(lineItem.getUnitPrice());
            lineItemDTO.setTaxRate(lineItem.getTaxRate());
            invoiceDTO.getLineItems().add(lineItemDTO);
        }

        return invoiceDTO;
    }

    private void apply(InvoiceEntity invoice, InvoiceDTO invoiceDTO, UserEntity user) {
        if (invoiceDTO.getLineItems() == null || invoiceDTO.getLineItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line item is required.");
        }

        CustomerEntity customer = customerRepository.findByIdAndUser(invoiceDTO.getCustomerId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));

        invoice.setCustomer(customer);
        invoice.setInvoiceNumber(trim(invoiceDTO.getInvoiceNumber()));
        invoice.setIssueDate(invoiceDTO.getIssueDate());
        invoice.setDueDate(invoiceDTO.getDueDate());
        invoice.setNotes(trim(invoiceDTO.getNotes()));
        invoice.setLineItems(toLineItemEntities(invoiceDTO.getLineItems()));
        invoiceCalculator.recalculate(invoice);
    }

    private List<InvoiceLineItemEntity> toLineItemEntities(List<InvoiceLineItemDTO> lineItemDTOs) {
        return lineItemDTOs.stream()
                .map(this::toLineItemEntity)
                .toList();
    }

    private InvoiceLineItemEntity toLineItemEntity(InvoiceLineItemDTO lineItemDTO) {
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
        lineItem.setItemName(trim(lineItemDTO.getItemName()));
        lineItem.setDescription(trim(lineItemDTO.getDescription()));
        lineItem.setQuantity(lineItemDTO.getQuantity());
        lineItem.setUnitPrice(lineItemDTO.getUnitPrice());
        lineItem.setTaxRate(lineItemDTO.getTaxRate());
        return lineItem;
    }

    private void assertInvoiceNumberAvailable(String invoiceNumber, Long currentInvoiceId, UserEntity user) {
        String normalizedInvoiceNumber = trim(invoiceNumber);
        invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, normalizedInvoiceNumber)
                .filter(invoice -> currentInvoiceId == null || !invoice.getId().equals(currentInvoiceId))
                .ifPresent(invoice -> {
                    throw new IllegalArgumentException("Invoice number already exists.");
                });
    }

    private String generateInvoiceNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "INV-" + datePart + "-" + randomPart;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
