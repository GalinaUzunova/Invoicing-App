package com.invoicingmanager.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyDetailsDTO {

    @NotBlank
    @Size(max = 200)
    private String companyName;

    @Size(max = 50)
    private String vatNumber;

    @Size(max = 500)
    private String address;

    @Email
    @NotBlank
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 50)
    private String phone;

    private boolean hasLogo;
}
