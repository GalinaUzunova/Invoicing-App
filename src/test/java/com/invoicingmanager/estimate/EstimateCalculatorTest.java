package com.invoicingmanager.estimate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EstimateCalculatorTest {

    private final EstimateCalculator estimateCalculator = new EstimateCalculator();

    @Test
    void recalculatesEstimateTotalsFromLineItems() {
        EstimateEntity estimate = new EstimateEntity();
        estimate.addLineItem(lineItem("2.00", "50.00", "20.00"));
        estimate.addLineItem(lineItem("1.50", "10.00", "10.00"));

        estimateCalculator.recalculate(estimate);

        assertThat(estimate.getSubtotal()).isEqualByComparingTo("115.00");
        assertThat(estimate.getTaxTotal()).isEqualByComparingTo("21.50");
        assertThat(estimate.getGrandTotal()).isEqualByComparingTo("136.50");
    }

    @Test
    void treatsNullableMoneyFieldsAsZero() {
        EstimateEntity estimate = new EstimateEntity();
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName("Service");
        estimate.addLineItem(lineItem);

        estimateCalculator.recalculate(estimate);

        assertThat(estimate.getSubtotal()).isEqualByComparingTo("0.00");
        assertThat(estimate.getTaxTotal()).isEqualByComparingTo("0.00");
        assertThat(estimate.getGrandTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNullEstimate() {
        assertThatThrownBy(() -> estimateCalculator.recalculate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("estimate");
    }

    private EstimateLineItemEntity lineItem(String quantity, String unitPrice, String taxRate) {
        EstimateLineItemEntity lineItem = new EstimateLineItemEntity();
        lineItem.setItemName("Service");
        lineItem.setQuantity(new BigDecimal(quantity));
        lineItem.setUnitPrice(new BigDecimal(unitPrice));
        lineItem.setTaxRate(new BigDecimal(taxRate));
        return lineItem;
    }
}
