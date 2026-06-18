package com.invoicingmanager.estimate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimateLineItemRepository extends JpaRepository<EstimateLineItemEntity, Long> {
}
