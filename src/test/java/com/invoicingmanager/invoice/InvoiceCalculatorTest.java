package com.invoicingmanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvoiceCalculatorTest {

    private final InvoiceCalculator invoiceCalculator = new InvoiceCalculator();

    @Test
    void recalculatesInvoiceTotalsFromLineItems() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.addLineItem(lineItem("2.00", "50.00", "20.00"));
        invoice.addLineItem(lineItem("1.50", "10.00", "10.00"));

        invoiceCalculator.recalculate(invoice);

        assertThat(invoice.getSubtotal()).isEqualByComparingTo("115.00");
        assertThat(invoice.getTaxTotal()).isEqualByComparingTo("21.50");
        assertThat(invoice.getGrandTotal()).isEqualByComparingTo("136.50");
    }

    @Test
    void roundsMoneyValuesHalfUpToTwoDecimals() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.addLineItem(lineItem("3.00", "0.333", "20.00"));

        invoiceCalculator.recalculate(invoice);

        assertThat(invoice.getSubtotal()).isEqualByComparingTo("1.00");
        assertThat(invoice.getTaxTotal()).isEqualByComparingTo("0.20");
        assertThat(invoice.getGrandTotal()).isEqualByComparingTo("1.20");
    }

    private InvoiceLineItemEntity lineItem(String quantity, String unitPrice, String taxRate) {
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
        lineItem.setItemName("Service");
        lineItem.setQuantity(new BigDecimal(quantity));
        lineItem.setUnitPrice(new BigDecimal(unitPrice));
        lineItem.setTaxRate(new BigDecimal(taxRate));
        return lineItem;
    }
}
