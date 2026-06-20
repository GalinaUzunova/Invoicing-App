package com.invoicingmanager.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.invoicingmanager.invoice.InvoiceEntity;
import com.invoicingmanager.invoice.InvoiceRepository;
import com.invoicingmanager.invoice.InvoiceStatus;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Test
    void getSummaryAggregatesRevenuePendingAndDraftInvoices() {
        UserEntity user = new UserEntity();
        when(invoiceRepository.sumGrandTotalByUserAndStatus(user, InvoiceStatus.PAID))
                .thenReturn(new BigDecimal("250.00"));
        when(invoiceRepository.sumGrandTotalByUserAndStatus(user, InvoiceStatus.SENT))
                .thenReturn(new BigDecimal("125.50"));
        when(invoiceRepository.countByUserAndStatus(user, InvoiceStatus.SENT)).thenReturn(3L);
        when(invoiceRepository.countByUserAndStatus(user, InvoiceStatus.DRAFT)).thenReturn(2L);

        DashboardService dashboardService = new DashboardService(invoiceRepository);
        DashboardSummaryDTO summary = dashboardService.getSummary(user);

        assertThat(summary.totalRevenue()).isEqualByComparingTo("250.00");
        assertThat(summary.pendingInvoiceAmount()).isEqualByComparingTo("125.50");
        assertThat(summary.pendingInvoiceCount()).isEqualTo(3);
        assertThat(summary.draftInvoiceCount()).isEqualTo(2);
    }

    @Test
    void getRecentInvoicesDelegatesToRepositoryLimitQuery() {
        UserEntity user = new UserEntity();
        InvoiceEntity invoice = new InvoiceEntity();
        when(invoiceRepository.findTop5ByUserOrderByIssueDateDescCreatedAtDesc(user)).thenReturn(List.of(invoice));

        DashboardService dashboardService = new DashboardService(invoiceRepository);

        assertThat(dashboardService.getRecentInvoices(user)).containsExactly(invoice);
    }

    @Test
    void publicMethodsThrowExceptionWhenUserIsNull() {
        DashboardService dashboardService = new DashboardService(invoiceRepository);

        assertThatThrownBy(() -> dashboardService.getSummary(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> dashboardService.getRecentInvoices(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }
}
