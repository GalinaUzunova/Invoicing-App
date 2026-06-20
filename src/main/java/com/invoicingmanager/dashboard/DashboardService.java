package com.invoicingmanager.dashboard;

import com.invoicingmanager.invoice.InvoiceEntity;
import com.invoicingmanager.invoice.InvoiceRepository;
import com.invoicingmanager.invoice.InvoiceStatus;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;

    public DashboardService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository must not be null");
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(UserEntity user) {
        UserEntity requiredUser = requireArgument(user, "user");
        BigDecimal totalRevenue = invoiceRepository.sumGrandTotalByUserAndStatus(requiredUser, InvoiceStatus.PAID);
        BigDecimal pendingInvoiceAmount = invoiceRepository.sumGrandTotalByUserAndStatus(requiredUser, InvoiceStatus.SENT);
        long pendingInvoiceCount = invoiceRepository.countByUserAndStatus(requiredUser, InvoiceStatus.SENT);
        long draftInvoiceCount = invoiceRepository.countByUserAndStatus(requiredUser, InvoiceStatus.DRAFT);

        return new DashboardSummaryDTO(
                totalRevenue,
                pendingInvoiceCount,
                pendingInvoiceAmount,
                draftInvoiceCount
        );
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> getRecentInvoices(UserEntity user) {
        return invoiceRepository.findTop5ByUserOrderByIssueDateDescCreatedAtDesc(requireArgument(user, "user"));
    }

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
