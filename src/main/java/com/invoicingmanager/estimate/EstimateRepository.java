package com.invoicingmanager.estimate;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.user.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimateRepository extends JpaRepository<EstimateEntity, Long> {

    @EntityGraph(attributePaths = "customer")
    List<EstimateEntity> findByUserOrderByIssueDateDescCreatedAtDesc(UserEntity user);

    @EntityGraph(attributePaths = "customer")
    List<EstimateEntity> findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(UserEntity user, EstimateStatus status);

    List<EstimateEntity> findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(CustomerEntity customer, UserEntity user);

    @EntityGraph(attributePaths = {"customer", "lineItems"})
    Optional<EstimateEntity> findByIdAndUser(Long id, UserEntity user);

    Optional<EstimateEntity> findByUserAndQuotationNumberIgnoreCase(UserEntity user, String quotationNumber);
}
