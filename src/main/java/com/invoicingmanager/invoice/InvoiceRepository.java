package com.invoicingmanager.invoice;

import com.invoicingmanager.customer.CustomerEntity;
import com.invoicingmanager.user.UserEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    @EntityGraph(attributePaths = "customer")
    List<InvoiceEntity> findByUserOrderByIssueDateDescCreatedAtDesc(UserEntity user);

    @EntityGraph(attributePaths = "customer")
    List<InvoiceEntity> findByUserAndStatusOrderByIssueDateDescCreatedAtDesc(UserEntity user, InvoiceStatus status);

    @EntityGraph(attributePaths = "customer")
    List<InvoiceEntity> findTop5ByUserOrderByIssueDateDescCreatedAtDesc(UserEntity user);

    List<InvoiceEntity> findByCustomerAndUserOrderByIssueDateDescCreatedAtDesc(CustomerEntity customer, UserEntity user);

    @EntityGraph(attributePaths = {"customer", "lineItems"})
    Optional<InvoiceEntity> findByIdAndUser(Long id, UserEntity user);

    Optional<InvoiceEntity> findByUserAndInvoiceNumberIgnoreCase(UserEntity user, String invoiceNumber);

    boolean existsByUserAndInvoiceNumberIgnoreCase(UserEntity user, String invoiceNumber);

    @Query("""
            select coalesce(sum(invoice.grandTotal), 0)
            from InvoiceEntity invoice
            where invoice.user = :user and invoice.status = :status
            """)
    BigDecimal sumGrandTotalByUserAndStatus(@Param("user") UserEntity user, @Param("status") InvoiceStatus status);

    long countByUserAndStatus(UserEntity user, InvoiceStatus status);
}
