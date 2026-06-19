package com.invoicingmanager.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EstimateEntityTest {

    @Test
    void lineItemHelpersKeepBothSidesOfRelationshipInSync() {
        EstimateEntity estimate = new EstimateEntity();
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();

        estimate.addLineItem(lineItem);

        assertThat(estimate.getLineItems()).containsExactly(lineItem);
        assertThat(lineItem.getEstimate()).isSameAs(estimate);

        estimate.removeLineItem(lineItem);

        assertThat(estimate.getLineItems()).isEmpty();
        assertThat(lineItem.getEstimate()).isNull();
    }

    @Test
    void setLineItemsReplacesCollectionAndAssignsParent() {
        EstimateEntity estimate = new EstimateEntity();
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();

        estimate.setLineItems(List.of(lineItem));

        assertThat(estimate.getLineItems()).containsExactly(lineItem);
        assertThat(lineItem.getEstimate()).isSameAs(estimate);
    }

    @Test
    void lineItemHelpersRejectNullValues() {
        EstimateEntity estimate = new EstimateEntity();

        assertThatThrownBy(() -> estimate.setLineItems(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineItems");
        assertThatThrownBy(() -> estimate.addLineItem(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineItem");
        assertThatThrownBy(() -> estimate.removeLineItem(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineItem");
    }
}
