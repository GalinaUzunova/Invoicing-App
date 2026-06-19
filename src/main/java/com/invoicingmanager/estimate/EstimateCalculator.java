package com.invoicingmanager.estimate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EstimateCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;

    public void recalculate(EstimateEntity estimate) {
        Objects.requireNonNull(estimate, "estimate must not be null");
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (EstimateLineItemEntity lineItem : estimate.getLineItems()) {
            LineTotals lineTotals = calculateLineTotals(lineItem);
            lineItem.setLineSubtotal(lineTotals.lineSubtotal());
            lineItem.setTaxAmount(lineTotals.taxAmount());
            lineItem.setLineTotal(lineTotals.lineTotal());

            subtotal = subtotal.add(lineTotals.lineSubtotal());
            taxTotal = taxTotal.add(lineTotals.taxAmount());
            grandTotal = grandTotal.add(lineTotals.lineTotal());
        }

        estimate.setSubtotal(toMoney(subtotal));
        estimate.setTaxTotal(toMoney(taxTotal));
        estimate.setGrandTotal(toMoney(grandTotal));
    }

    private LineTotals calculateLineTotals(EstimateLineItemEntity lineItem) {
        Objects.requireNonNull(lineItem, "lineItem must not be null");
        BigDecimal quantity = nullToZero(lineItem.getQuantity());
        BigDecimal unitPrice = nullToZero(lineItem.getUnitPrice());
        BigDecimal taxRate = nullToZero(lineItem.getTaxRate());

        BigDecimal lineSubtotal = toMoney(quantity.multiply(unitPrice));
        BigDecimal taxAmount = toMoney(lineSubtotal.multiply(taxRate).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP));
        BigDecimal lineTotal = toMoney(lineSubtotal.add(taxAmount));

        return new LineTotals(lineSubtotal, taxAmount, lineTotal);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal toMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record LineTotals(BigDecimal lineSubtotal, BigDecimal taxAmount, BigDecimal lineTotal) {
    }
}
