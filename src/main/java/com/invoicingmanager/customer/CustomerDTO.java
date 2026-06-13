package com.invoicingmanager.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDTO {

    private Long id;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    @Size(max = 500)
    private String billingAddress;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String taxNumber;

    @Size(max = 1_000)
    private String notes;
}
