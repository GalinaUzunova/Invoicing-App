package com.invoicingmanager.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        BigDecimal totalRevenue,
        long pendingInvoiceCount,
        BigDecimal pendingInvoiceAmount,
        long draftInvoiceCount
) {
}
