package com.sensa.usermanagementservice.dto;

import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
import jakarta.validation.constraints.*;

public record ClientRegistrationDto(
        @Size(message = "Name cannot exceed 50 characters", max = 50)
        @NotBlank(message = "Name cannot be blank")
        String name,

        @Size(message = "Second name cannot exceed 50 characters", max = 50)
        @NotBlank(message = "Second name cannot be blank")
        String secondName,

        @NotBlank(message = "Username cannot be blank")
        String username,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email cannot be blank")
        String email,

        @NotBlank(message = "Password cannot be blank")
        String password,

        @Pattern(message = "Invalid phone number format", regexp = "\\+?[0-9]{10,15}")
        @NotBlank(message = "Phone cannot be blank")
        String phoneNumber,

        @NotNull(message = "Age cannot be null")
        @Min(message = "Age cannot be negative", value = 0)
        @Max(message = "Age cannot exceed 150", value = 150)
        Integer age,

        PreferredCommunicationChannel preferredCommunicationChannel) {
}