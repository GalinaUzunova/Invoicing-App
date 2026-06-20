package com.invoicingmanager.estimate;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.customer.CustomerRepository;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class EstimateService {

    private final EstimateRepository estimateRepository;
    private final CustomerRepository customerRepository;
    private final EstimateCalculator estimateCalculator;

    public EstimateService(
            EstimateRepository estimateRepository,
            CustomerRepository customerRepository,
            EstimateCalculator estimateCalculator
    ) {
        this.estimateRepository = Objects.requireNonNull(estimateRepository, "estimateRepository must not be null");
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository must not be null");
        this.estimateCalculator = Objects.requireNonNull(estimateCalculator, "estimateCalculator must not be null");
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
    public List<EstimateEntity> findAllForUser(UserEntity user, EstimateStatus status) {
        UserEntity requiredUser = requireArgument(user, "user");
        if (status == null) {
            return estimateRepository.findByUserOrderByIssueDateDescCreatedAtDesc(requiredUser);
        }
        return estimateRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(requiredUser, status);
    }

    @Transactional(readOnly = true)
    public List<EstimateEntity> findByCustomer(CustomerEntity customer, UserEntity user) {
        return estimateRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(
                requireArgument(customer, "customer"),
                requireArgument(user, "user")
        );
    }

    @Transactional(readOnly = true)
    public Optional<EstimateEntity> findByIdAndUser(Long id, UserEntity user) {
        return estimateRepository.findByIdAndUser(requireArgument(id, "estimate id"), requireArgument(user, "user"));
    }

    @Transactional(readOnly = true)
    public EstimateEntity findByIdForUser(Long id, UserEntity user) {
        Long requiredId = requireArgument(id, "estimate id");
        UserEntity requiredUser = requireArgument(user, "user");
        return findByIdAndUser(requiredId, requiredUser).orElseThrow(() -> estimateNotFound(requiredId, requiredUser));
    }

    @Transactional
    public EstimateEntity create(EstimateDTO dto, UserEntity user) {
        EstimateDTO requiredDto = requireArgument(dto, "estimate");
        UserEntity requiredUser = requireArgument(user, "user");
        assertQuotationNumberAvailable(requiredDto.getQuotationNumber(), null, requiredUser);
        EstimateEntity estimate = new EstimateEntity();
        estimate.setUser(requiredUser);
        estimate.setStatus(EstimateStatus.DRAFT);
        apply(estimate, requiredDto, requiredUser);
        estimateRepository.save(estimate);
        return estimate;
    }

    @Transactional
    public EstimateEntity update(Long id, EstimateDTO dto, UserEntity user) {
        Long requiredId = requireArgument(id, "estimate id");
        EstimateDTO requiredDto = requireArgument(dto, "estimate");
        UserEntity requiredUser = requireArgument(user, "user");
        EstimateEntity estimate = Objects.requireNonNull(
                estimateRepository.findByIdAndUser(requiredId, requiredUser).orElseThrow(() -> estimateNotFound(requiredId, requiredUser)),
                "Estimate must not be null."
        );
        assertQuotationNumberAvailable(requiredDto.getQuotationNumber(), requiredId, requiredUser);
        apply(estimate, requiredDto, requiredUser);
        estimateRepository.save(estimate);
        return estimate;
    }

    @Transactional
    public void delete(Long id, UserEntity user) {
        Long requiredId = requireArgument(id, "estimate id");
        UserEntity requiredUser = requireArgument(user, "user");
        estimateRepository.findByIdAndUser(requiredId, requiredUser)
                .ifPresentOrElse(
                        estimateRepository::delete,
                        () -> {
                            throw estimateNotFound(requiredId, requiredUser);
                        }
                );
    }

    @Transactional
    public EstimateEntity markSent(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        if (estimate.getStatus() == EstimateStatus.ACCEPTED || estimate.getStatus() == EstimateStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalised estimates cannot be sent again.");
        }
        estimate.setStatus(EstimateStatus.SENT);
        estimateRepository.save(estimate);
        return estimate;
    }

    @Transactional
    public EstimateEntity markAccepted(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        estimate.setStatus(EstimateStatus.ACCEPTED);
        estimateRepository.save(estimate);
        return estimate;
    }

    @Transactional
    public EstimateEntity markDeclined(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        estimate.setStatus(EstimateStatus.DECLINED);
        estimateRepository.save(estimate);
        return estimate;
    }

    public EstimateDTO toDTO(EstimateEntity estimate) {
        EstimateEntity requiredEstimate = requireArgument(estimate, "estimate");
        EstimateDTO dto = new EstimateDTO();
        dto.setId(requiredEstimate.getId());
        dto.setCustomerId(requiredEstimate.getCustomer().getId());
        dto.setQuotationNumber(requiredEstimate.getQuotationNumber());
        dto.setStatus(requiredEstimate.getStatus());
        dto.setIssueDate(requiredEstimate.getIssueDate());
        dto.setExpiryDate(requiredEstimate.getExpiryDate());
        dto.setNotes(requiredEstimate.getNotes());

        for (EstimateLineItemEntity lineItem : requiredEstimate.getLineItems()) {
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
        if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line item is required.");
        }
        if (dto.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is required.");
        }
        if (dto.getIssueDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Issue date is required.");
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
        return dtos.stream().map(this::toLineItemEntity).toList();
    }

    private EstimateLineItemEntity toLineItemEntity(EstimateLineItemDTO dto) {
        EstimateLineItemDTO requiredDto = requireArgument(dto, "line item");
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName(trimRequired(requiredDto.getItemName(), "Line item name"));
        lineItem.setDescription(trim(requiredDto.getDescription()));
        BigDecimal quantity = requiredDto.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Line item quantity must be greater than zero.");
        }
        lineItem.setQuantity(quantity);
        BigDecimal unitPrice = requiredDto.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Line item unit price cannot be negative.");
        }
        lineItem.setUnitPrice(unitPrice);
        if (requiredDto.getTaxRate() == null) {
            throw new IllegalArgumentException("Line item tax rate is required.");
        }
        lineItem.setTaxRate(requiredDto.getTaxRate());
        return lineItem;
    }

    private void assertQuotationNumberAvailable(String quotationNumber, Long currentEstimateId, UserEntity user) {
        String normalised = trim(quotationNumber);
        if (normalised == null || normalised.isBlank()) {
            throw new IllegalArgumentException("Quotation number is required.");
        }
        estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, normalised)
                .filter(e -> currentEstimateId == null || !e.getId().equals(currentEstimateId))
                .ifPresent(e -> {
                    log.warn("Quotation number {} already exists for user {}", normalised, user.getEmail());
                    throw new IllegalArgumentException("Quotation number already exists.");
                });
    }

    private void validateDateOrder(EstimateDTO dto) {
        if (!dto.isExpiryDateOnOrAfterIssueDate()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date cannot be earlier than issue date.");
        }
    }

    private ResponseStatusException estimateNotFound(Long id, UserEntity user) {
        log.warn("Estimate {} was not found for user {}", id, user.getEmail());
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Estimate not found.");
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

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
