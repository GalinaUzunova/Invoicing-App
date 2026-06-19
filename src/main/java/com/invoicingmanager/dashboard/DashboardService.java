package com.invoicingmanager.dashboard;

import com.invoicingmanager.invoice.InvoiceEntity;
import com.invoicingmanager.invoice.InvoiceRepository;
import com.invoicingmanager.invoice.InvoiceStatus;
import com.invoicingmanager.user.UserEntity;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;

    public DashboardService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(@NotNull UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        BigDecimal totalRevenue = invoiceRepository.sumGrandTotalByUserAndStatus(user, InvoiceStatus.PAID);
        BigDecimal pendingInvoiceAmount = invoiceRepository.sumGrandTotalByUserAndStatus(user, InvoiceStatus.SENT);
        long pendingInvoiceCount = invoiceRepository.countByUserAndStatus(user, InvoiceStatus.SENT);
        long draftInvoiceCount = invoiceRepository.countByUserAndStatus(user, InvoiceStatus.DRAFT);

        return new DashboardSummaryDTO(
                totalRevenue,
                pendingInvoiceCount,
                pendingInvoiceAmount,
                draftInvoiceCount
        );
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> getRecentInvoices(@NotNull UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        return invoiceRepository.findTop5ByUserOrderByIssueDateDescCreatedAtDesc(user);
    }
}
