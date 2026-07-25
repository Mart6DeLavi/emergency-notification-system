package com.sensa.usermanagementservice.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "user_location_data")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLocationDataEntity {

    @Id
    private Long userEntityId;

    private String country;

    private String city;

    private String street;

    private String homeNumber;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
