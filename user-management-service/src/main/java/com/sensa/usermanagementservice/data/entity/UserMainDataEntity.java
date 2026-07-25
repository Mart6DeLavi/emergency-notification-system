package com.sensa.usermanagementservice.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "user_main_data")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMainDataEntity {

    @Id
    private Long userEntityId;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
