package com.invoicingmanager.estimate;

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
    public List<EstimateEntity> findAllForUser(UserEntity user, EstimateStatus status) {
        if (status == null) {
            return estimateRepository.findByUserOrderByIssueDateDescCreatedAtDesc(user);
        }
        return estimateRepository.findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(user, status);
    }

    @Transactional(readOnly = true)
    public List<EstimateEntity> findByCustomer(CustomerEntity customer, UserEntity user) {
        return estimateRepository.findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(customer, user);
    }

    @Transactional(readOnly = true)
    public EstimateEntity findByIdForUser(Long id, UserEntity user) {
        return estimateRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estimate not found."));
    }

    @Transactional
    public EstimateEntity create(EstimateDTO dto, UserEntity user) {
        assertQuotationNumberAvailable(dto.getQuotationNumber(), null, user);
        EstimateEntity estimate = new EstimateEntity();
        estimate.setUser(user);
        estimate.setStatus(EstimateStatus.DRAFT);
        apply(estimate, dto, user);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public EstimateEntity update(Long id, EstimateDTO dto, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        assertQuotationNumberAvailable(dto.getQuotationNumber(), id, user);
        apply(estimate, dto, user);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public void delete(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        estimateRepository.delete(estimate);
    }

    @Transactional
    public EstimateEntity markSent(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        if (estimate.getStatus() == EstimateStatus.ACCEPTED || estimate.getStatus() == EstimateStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finalised estimates cannot be sent again.");
        }
        estimate.setStatus(EstimateStatus.SENT);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public EstimateEntity markAccepted(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        estimate.setStatus(EstimateStatus.ACCEPTED);
        return estimateRepository.save(estimate);
    }

    @Transactional
    public EstimateEntity markDeclined(Long id, UserEntity user) {
        EstimateEntity estimate = findByIdForUser(id, user);
        estimate.setStatus(EstimateStatus.DECLINED);
        return estimateRepository.save(estimate);
    }

    public EstimateDTO toDTO(EstimateEntity estimate) {
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
        if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line item is required.");
        }

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
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName(trim(dto.getItemName()));
        lineItem.setDescription(trim(dto.getDescription()));
        lineItem.setQuantity(dto.getQuantity());
        lineItem.setUnitPrice(dto.getUnitPrice());
        lineItem.setTaxRate(dto.getTaxRate());
        return lineItem;
    }

    private void assertQuotationNumberAvailable(String quotationNumber, Long currentEstimateId, UserEntity user) {
        String normalised = trim(quotationNumber);
        estimateRepository.findByUserAndQuotationNumberIgnoreCase(user, normalised)
                .filter(e -> currentEstimateId == null || !e.getId().equals(currentEstimateId))
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Quotation number already exists.");
                });
    }

    private String generateQuotationNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "QUO-" + datePart + "-" + randomPart;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
