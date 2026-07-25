package com.sensa.usermanagementservice.data.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "user_notification_settings")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserNotificationsSettingsEntity {

    @Id
    private Long userEntityId;

    @Builder.Default
    private Boolean push = true;

    @Builder.Default
    private Boolean emailEnabled = true;

    @Builder.Default
    private Boolean sms = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
