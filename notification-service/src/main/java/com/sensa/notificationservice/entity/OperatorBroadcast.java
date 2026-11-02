package com.sensa.notificationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "operator_broadcasts")
public class OperatorBroadcast {

    @Id
    private String id;

    @Indexed
    private String alertType;

    private String title;

    private String message;

    private String audience;

    private String city;

    private String street;

    private Double radiusKm;

    private int recipients;

    private int deliveries;

    private int filtered;

    private int errors;

    private String status;

    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
