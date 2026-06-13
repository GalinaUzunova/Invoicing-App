package com.invoicingmanager.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItemEntity, Long> {
}
