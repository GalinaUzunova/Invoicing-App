package com.invoicingmanager.invoice;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.estimate.EstimateEntity;
import com.invoicingmanager.estimate.EstimateLineItemEntity;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceCalculator invoiceCalculator;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            InvoiceCalculator invoiceCalculator
    ) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository must not be null");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository must not be null");
        this.invoiceCalculator = Objects.requireNonNull(invoiceCalculator, "invoiceCalculator must not be null");
    }

    public InvoiceDTO newInvoiceDTO(Long customerId, UserEntity user) {
        UserEntity requiredUser = requireArgument(user, "user");
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setCustomerId(customerId);
        invoiceDTO.setInvoiceNumber(generateInvoiceNumber(requiredUser));
        invoiceDTO.setIssueDate(LocalDate.now());
        invoiceDTO.getLineItems().add(new InvoiceLineItemDTO());
        return invoiceDTO;
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> findAllForUser(UserEntity user, InvoiceStatus status) {
        UserEntity requiredUser = requireArgument(user, "user");
        if (status == null) {
            return invoiceRepository.findByUserOrderByIssueDateDescCreatedAtDesc(requiredUser);
        }

        return invoiceRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(requiredUser, status);
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> findByCustomer(CustomerEntity customer, UserEntity user) {
        return invoiceRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(
                requireArgument(customer, "customer"),
                requireArgument(user, "user")
        );
    }

    @Transactional(readOnly = true)
    public InvoiceEntity findByIdForUser(Long id, UserEntity user) {
        Long requiredId = requireArgument(id, "invoice id");
        UserEntity requiredUser = requireArgument(user, "user");
        return findByIdAndUser(requiredId, requiredUser).orElseThrow(() -> invoiceNotFound(requiredId, requiredUser));
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceEntity> findByIdAndUser(Long id, UserEntity user) {
        return invoiceRepository.findByIdAndUser(requireArgument(id, "invoice id"), requireArgument(user, "user"));
    }

    @Transactional
    public InvoiceEntity create(InvoiceDTO invoiceDTO, UserEntity user) {
        InvoiceDTO requiredInvoiceDTO = requireArgument(invoiceDTO, "invoice");
        UserEntity requiredUser = requireArgument(user, "user");
        assertInvoiceNumberAvailable(requiredInvoiceDTO.getInvoiceNumber(), null, requiredUser);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setUser(requiredUser);
        invoice.setStatus(InvoiceStatus.DRAFT);
        apply(invoice, requiredInvoiceDTO, requiredUser);

        return save(invoice);
    }

    @Transactional
    public InvoiceEntity createFromEstimate(EstimateEntity estimate, UserEntity user) {
        EstimateEntity requiredEstimate = requireArgument(estimate, "estimate");
        UserEntity requiredUser = requireArgument(user, "user");

        InvoiceDTO invoiceDTO = new InvoiceDTO();
        CustomerEntity estimateCustomer = requireArgument(requiredEstimate.getCustomer(), "estimate customer");
        invoiceDTO.setCustomerId(requireArgument(estimateCustomer.getId(), "estimate customer id"));
        invoiceDTO.setInvoiceNumber(generateInvoiceNumber(requiredUser));
        invoiceDTO.setIssueDate(requiredEstimate.getIssueDate());
        invoiceDTO.setDueDate(requiredEstimate.getExpiryDate());
        invoiceDTO.setNotes(requiredEstimate.getNotes());

        for (EstimateLineItemEntity estimateLineItem : requiredEstimate.getLineItems()) {
            InvoiceLineItemDTO invoiceLineItemDTO = new InvoiceLineItemDTO();
            invoiceLineItemDTO.setItemName(estimateLineItem.getItemName());
            invoiceLineItemDTO.setDescription(estimateLineItem.getDescription());
            invoiceLineItemDTO.setQuantity(estimateLineItem.getQuantity());
            invoiceLineItemDTO.setUnitPrice(estimateLineItem.getUnitPrice());
            invoiceLineItemDTO.setTaxRate(estimateLineItem.getTaxRate());
            invoiceDTO.getLineItems().add(invoiceLineItemDTO);
        }

        InvoiceEntity invoice = create(invoiceDTO, requiredUser);
        log.info("Created invoice {} from estimate {} for user {}", invoice.getInvoiceNumber(), requiredEstimate.getQuotationNumber(), requiredUser.getEmail());
        return invoice;
    }

    @Transactional
    public InvoiceEntity update(Long id, InvoiceDTO invoiceDTO, UserEntity user) {
        Long requiredId = requireArgument(id, "invoice id");
        InvoiceDTO requiredInvoiceDTO = requireArgument(invoiceDTO, "invoice");
        UserEntity requiredUser = requireArgument(user, "user");
        return findByIdAndUser(requiredId, requiredUser)
                .map(invoice -> updateInvoice(invoice, requiredId, requiredInvoiceDTO, requiredUser))
                .orElseThrow(() -> invoiceNotFound(requiredId, requiredUser));
    }

    @Transactional
    public void delete(Long id, UserEntity user) {
        Long requiredId = requireArgument(id, "invoice id");
        UserEntity requiredUser = requireArgument(user, "user");
        findByIdAndUser(requiredId, requiredUser)
                .ifPresentOrElse(
                        invoiceRepository::delete,
                        () -> {
                            throw invoiceNotFound(requiredId, requiredUser);
                        }
                );
    }

    @Transactional
    public InvoiceEntity markSent(Long id, UserEntity user) {
        InvoiceEntity invoice = findByIdForUser(id, user);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paid invoices cannot be sent again.");
        }

        invoice.setStatus(InvoiceStatus.SENT);
        return save(invoice);
    }

    @Transactional
    public InvoiceEntity markPaid(Long id, UserEntity user) {
        InvoiceEntity invoice = findByIdForUser(id, user);
        invoice.setStatus(InvoiceStatus.PAID);
        return save(invoice);
    }

    public InvoiceDTO toDTO(InvoiceEntity invoice) {
        InvoiceEntity requiredInvoice = requireArgument(invoice, "invoice");
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setId(requiredInvoice.getId());
        invoiceDTO.setCustomerId(requiredInvoice.getCustomer().getId());
        invoiceDTO.setInvoiceNumber(requiredInvoice.getInvoiceNumber());
        invoiceDTO.setStatus(requiredInvoice.getStatus());
        invoiceDTO.setIssueDate(requiredInvoice.getIssueDate());
        invoiceDTO.setDueDate(requiredInvoice.getDueDate());
        invoiceDTO.setNotes(requiredInvoice.getNotes());

        for (InvoiceLineItemEntity lineItem : requiredInvoice.getLineItems()) {
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
        if (invoiceDTO.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is required.");
        }
        if (invoiceDTO.getIssueDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issue date is required.");
        }
        validateDateOrder(invoiceDTO);

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
        InvoiceLineItemDTO requiredLineItemDTO = requireArgument(lineItemDTO, "line item");
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
        lineItem.setItemName(trimRequired(requiredLineItemDTO.getItemName(), "Line item name"));
        lineItem.setDescription(trim(requiredLineItemDTO.getDescription()));
        BigDecimal quantity = requiredLineItemDTO.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Line item quantity must be greater than zero.");
        }
        lineItem.setQuantity(quantity);
        BigDecimal unitPrice = requiredLineItemDTO.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Line item unit price cannot be negative.");
        }
        lineItem.setUnitPrice(unitPrice);
        if (requiredLineItemDTO.getTaxRate() == null) {
            throw new IllegalArgumentException("Line item tax rate is required.");
        }
        lineItem.setTaxRate(requiredLineItemDTO.getTaxRate());
        return lineItem;
    }

    private void assertInvoiceNumberAvailable(String invoiceNumber, Long currentInvoiceId, UserEntity user) {
        String normalizedInvoiceNumber = trim(invoiceNumber);
        if (normalizedInvoiceNumber == null || normalizedInvoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }
        invoiceRepository.findByUserAndInvoiceNumberIgnoreCase(user, normalizedInvoiceNumber)
                .filter(invoice -> currentInvoiceId == null || !invoice.getId().equals(currentInvoiceId))
                .ifPresent(invoice -> {
                    log.warn("Invoice number {} already exists for user {}", normalizedInvoiceNumber, user.getEmail());
                    throw new IllegalArgumentException("Invoice number already exists.");
                });
    }

    private InvoiceEntity updateInvoice(InvoiceEntity invoice, Long id, InvoiceDTO invoiceDTO, UserEntity user) {
        assertInvoiceNumberAvailable(invoiceDTO.getInvoiceNumber(), id, user);
        apply(invoice, invoiceDTO, user);
        return save(invoice);
    }

    private InvoiceEntity save(InvoiceEntity invoice) {
        InvoiceEntity invoiceToSave = Objects.requireNonNull(invoice, "invoice must not be null");
        invoiceRepository.save(invoiceToSave);
        return invoiceToSave;
    }

    private ResponseStatusException invoiceNotFound(Long id, UserEntity user) {
        log.warn("Invoice {} was not found for user {}", id, user.getEmail());
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found.");
    }

    private void validateDateOrder(InvoiceDTO invoiceDTO) {
        if (!invoiceDTO.isDueDateOnOrAfterIssueDate()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date cannot be earlier than issue date.");
        }
    }

    private String generateInvoiceNumber(UserEntity user) {
        long invoiceSequence = invoiceRepository.countByUser(user) + 1;
        String candidate = formatInvoiceNumber(invoiceSequence);

        while (invoiceRepository.existsByUserAndInvoiceNumberIgnoreCase(user, candidate)) {
            invoiceSequence++;
            candidate = formatInvoiceNumber(invoiceSequence);
        }

        return candidate;
    }

    private String formatInvoiceNumber(long invoiceSequence) {
        return "INV-%05d".formatted(invoiceSequence);
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

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
