package com.rinit.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank(message ="email is required")
    @Email(message="email should be a valid email address")
    private String email;

    @NotBlank(message="Password is required")
    @Size(min = 8 , message="password must be at least 8 characters long")
    private String password;

}
