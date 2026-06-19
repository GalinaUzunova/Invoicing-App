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
    public List<InvoiceEntity> getRecentInvoices(UserEntity user) {
        return invoiceRepository.findTop5ByUserOrderByIssueDateDescCreatedAtDesc(user);
    }
}
