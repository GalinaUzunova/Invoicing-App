package com.invoicingmanager.estimate;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.user.UserEntity;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EstimateService {

    private final EstimateRepository estimateRepository;
    private final CustomerRepository customerRepository;
    private final EstimateCalculator estimateCalculator;

    public EstimateService(
            EstimateRepository estimateRepository,
            CustomerRepository customerRepository,
            EstimateCalculator estimateCalculator
    ) {
        this.estimateRepository = estimateRepository;
        this.customerRepository = customerRepository;
        this.estimateCalculator = estimateCalculator;
    }

    public EstimateDTO newEstimateDTO(Long customerId) {
        EstimateDTO dto = new EstimateDTO();
        dto.setCustomerId(customerId);
        dto.setQuotationNumber(generateQuotationNumber());
        dto.setIssueDate(LocalDate.now());
        dto.getLineItems().add(new EstimateLineItemDTO());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<EstimateEntity> findAllForUser(@NotNull UserEntity user, EstimateStatus status) {
        Objects.requireNonNull(user, "user must not be null");
        if (status == null) {
            return estimateRepository.findByUserOrderByIssueDateDescCreatedAtDesc(user);
        }
        return estimateRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(user, status);
    }

    @Transactional(readOnly = true)
    public List<EstimateEntity> findByCustomer(@NotNull CustomerEntity customer, @NotNull UserEntity user) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(user, "user must not be null");
        return estimateRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(customer, user);
    }

    @Transactional(readOnly = true)
    public EstimateEntity findByIdForUser(@NotNull Long id, @NotNull UserEntity user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");
        return estimateRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estimate not found."));
    }

    @Transactional
    public EstimateEntity create(@NotNull EstimateDTO dto, @NotNull UserEntity user) {
        Objects.requireNonNull(dto, "dto must not be null");
        Objects.requireNonNull(user, "user must not be null");
        assertQuotationNumberAvailable(dto.getQuotationNumber(), null, user);
        EstimateEntity estimate = new EstimateEntity();
        estimate.setUser(user);
        estimate.setStatus(EstimateStatus.DRAFT);
        apply(estimate, dto, user);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public EstimateEntity update(@NotNull Long id, @NotNull EstimateDTO dto, @NotNull UserEntity user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(dto, "dto must not be null");
        Objects.requireNonNull(user, "user must not be null");
        EstimateEntity estimate = findByIdForUser(id, user);
        assertQuotationNumberAvailable(dto.getQuotationNumber(), id, user);
        apply(estimate, dto, user);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public void delete(@NotNull Long id, @NotNull UserEntity user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");
        EstimateEntity estimate = findByIdForUser(id, user);
        estimateRepository.delete(estimate);
    }

    @Transactional
    public EstimateEntity markSent(@NotNull Long id, @NotNull UserEntity user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");
        EstimateEntity estimate = findByIdForUser(id, user);
        if (estimate.getStatus() == EstimateStatus.ACCEPTED || estimate.getStatus() == EstimateStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalised estimates cannot be sent again.");
        }
        estimate.setStatus(EstimateStatus.SENT);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public EstimateEntity markAccepted(@NotNull Long id, @NotNull UserEntity user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");
        EstimateEntity estimate = findByIdForUser(id, user);
        estimate.setStatus(EstimateStatus.ACCEPTED);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public EstimateEntity markDeclined(@NotNull Long id, @NotNull UserEntity user) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(user, "user must not be null");
        EstimateEntity estimate = findByIdForUser(id, user);
        estimate.setStatus(EstimateStatus.DECLINED);
        return estimateRepository.save(estimate);
    }

    public EstimateDTO toDTO(@NotNull EstimateEntity estimate) {
        Objects.requireNonNull(estimate, "estimate must not be null");
        EstimateDTO dto = new EstimateDTO();
        dto.setId(estimate.getId());
        dto.setCustomerId(estimate.getCustomer().getId());
        dto.setQuotationNumber(estimate.getQuotationNumber());
        dto.setStatus(estimate.getStatus());
        dto.setIssueDate(estimate.getIssueDate());
        dto.setExpiryDate(estimate.getExpiryDate());
        dto.setNotes(estimate.getNotes());

        for (EstimateLineItemEntity lineItem : estimate.getLineItems()) {
            EstimateLineItemDTO lineItemDTO = new EstimateLineItemDTO();
            lineItemDTO.setItemName(lineItem.getItemName());
            lineItemDTO.setDescription(lineItem.getDescription());
            lineItemDTO.setQuantity(lineItem.getQuantity());
            lineItemDTO.setUnitPrice(lineItem.getUnitPrice());
            lineItemDTO.setTaxRate(lineItem.getTaxRate());
            dto.getLineItems().add(lineItemDTO);
        }

        return dto;
    }

    private void apply(EstimateEntity estimate, EstimateDTO dto, UserEntity user) {
        Objects.requireNonNull(estimate, "estimate must not be null");
        Objects.requireNonNull(dto, "dto must not be null");
        Objects.requireNonNull(user, "user must not be null");

        if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line item is required.");
        }
        if (dto.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is required.");
        }
        validateDateOrder(dto);

        CustomerEntity customer = customerRepository.findByIdAndUser(dto.getCustomerId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));

        estimate.setCustomer(customer);
        estimate.setQuotationNumber(trim(dto.getQuotationNumber()));
        estimate.setIssueDate(dto.getIssueDate());
        estimate.setExpiryDate(dto.getExpiryDate());
        estimate.setNotes(trim(dto.getNotes()));
        estimate.setLineItems(toLineItemEntities(dto.getLineItems()));
        estimateCalculator.recalculate(estimate);
    }

    private List<EstimateLineItemEntity> toLineItemEntities(List<EstimateLineItemDTO> dtos) {
        Objects.requireNonNull(dtos, "dtos must not be null");
        return dtos.stream().map(this::toLineItemEntity).toList();
    }

    private EstimateLineItemEntity toLineItemEntity(EstimateLineItemDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName(trimRequired(dto.getItemName(), "Line item name"));
        lineItem.setDescription(trim(dto.getDescription()));
        BigDecimal quantity = Objects.requireNonNull(dto.getQuantity(), "Line item quantity is required.");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Line item quantity must be greater than zero.");
        }
        lineItem.setQuantity(quantity);
        BigDecimal unitPrice = Objects.requireNonNull(dto.getUnitPrice(), "Line item unit price is required.");
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Line item unit price must be greater than zero.");
        }
        lineItem.setUnitPrice(unitPrice);
        lineItem.setTaxRate(Objects.requireNonNull(dto.getTaxRate(), "Line item tax rate is required."));
        return lineItem;
    }

    private void assertQuotationNumberAvailable(String quotationNumber, Long currentEstimateId, UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        String normalised = trim(quotationNumber);
        if (normalised == null || normalised.isBlank()) {
            throw new IllegalArgumentException("Quotation number is required.");
        }
        estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, normalised)
                .filter(e -> currentEstimateId == null || !e.getId().equals(currentEstimateId))
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Quotation number already exists.");
                });
    }

    private void validateDateOrder(EstimateDTO dto) {
        if (!dto.isExpiryDateOnOrAfterIssueDate()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date cannot be earlier than issue date.");
        }
    }

    private String generateQuotationNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "QUO-" + datePart + "-" + randomPart;
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
