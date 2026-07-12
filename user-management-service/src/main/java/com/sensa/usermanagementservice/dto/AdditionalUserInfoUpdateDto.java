package com.sensa.usermanagementservice.dto;

import com.sensa.usermanagementservice.model.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdditionalUserInfoUpdateDto(
        Gender gender,

        @Pattern(regexp = " ^(A|B|AB|0)[+-]$", message = "Invalid blood group")
        String bloodGroup,

        @Min(value = 50, message = "Height must be at least 50 cm")
        @Max(value = 300, message = "Height cannot exceed 300 cm")
        Double height,

        @Size(max = 100, message = "Country name cannot exceed 100 characters")
        String country,

        @Size(max = 100, message = "City name cannot exceed 100 characters")
        String city
) { }
