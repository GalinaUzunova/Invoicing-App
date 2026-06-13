package com.invoicingmanager.customer;

import com.invoicingmanager.user.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    List<CustomerEntity> findByUserOrderByNameAsc(UserEntity user);

    Optional<CustomerEntity> findByIdAndUser(Long id, UserEntity user);
}
