package com.invoicingmanager.company;

import com.invoicingmanager.user.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyDetailsRepository extends JpaRepository<CompanyDetailsEntity, Long> {

    Optional<CompanyDetailsEntity> findByUser(UserEntity user);
}
