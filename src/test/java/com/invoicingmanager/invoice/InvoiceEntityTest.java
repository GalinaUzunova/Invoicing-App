package com.invoicingmanager.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceEntityTest {

    @Test
    void lineItemHelpersKeepBothSidesOfRelationshipInSync() {
        InvoiceEntity invoice = new InvoiceEntity();
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();

        invoice.addLineItem(lineItem);

        assertThat(invoice.getLineItems()).containsExactly(lineItem);
        assertThat(lineItem.getInvoice()).isSameAs(invoice);

        invoice.removeLineItem(lineItem);

        assertThat(invoice.getLineItems()).isEmpty();
        assertThat(lineItem.getInvoice()).isNull();
    }

    @Test
    void setLineItemsReplacesCollectionAndAssignsParent() {
        InvoiceEntity invoice = new InvoiceEntity();
        InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();

        invoice.setLineItems(List.of(lineItem));

        assertThat(invoice.getLineItems()).containsExactly(lineItem);
        assertThat(lineItem.getInvoice()).isSameAs(invoice);
    }

    @Test
    void lineItemHelpersRejectNullValues() {
        InvoiceEntity invoice = new InvoiceEntity();

        assertThatThrownBy(() -> invoice.setLineItems(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineItems");
        assertThatThrownBy(() -> invoice.addLineItem(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineItem");
        assertThatThrownBy(() -> invoice.removeLineItem(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineItem");
    }
}
