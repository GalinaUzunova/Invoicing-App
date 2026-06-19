package com.invoicingmanager.company;

import com.invoicingmanager.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_details")
public class CompanyDetailsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @NotBlank
    @Size(max = 200)
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Size(max = 50)
    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Size(max = 500)
    @Column(name = "address", length = 500)
    private String address;

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotBlank
    @Size(max = 50)
    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Size(max = 255)
    @Column(name = "logo_filename", length = 255)
    private String logoFilename;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean hasLogo() {
        return logoFilename != null && !logoFilename.isBlank();
    }

    public boolean hasAnyDetails() {
        return isPresent(companyName)
                || isPresent(vatNumber)
                || isPresent(address)
                || isPresent(email)
                || isPresent(phone)
                || hasLogo();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
