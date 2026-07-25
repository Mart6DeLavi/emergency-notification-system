package com.sensa.usermanagementservice.mapper;

import com.sensa.usermanagementservice.data.entity.UserEntity;
import com.sensa.usermanagementservice.data.entity.UserLocationDataEntity;
import com.sensa.usermanagementservice.data.entity.UserMainDataEntity;
import com.sensa.usermanagementservice.data.entity.UserNotificationsSettingsEntity;
import com.sensa.usermanagementservice.dto.NotificationSettingsDto;
import com.sensa.usermanagementservice.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity entity,
                                   UserMainDataEntity mainData,
                                   UserLocationDataEntity location,
                                   UserNotificationsSettingsEntity settings) {
        return UserResponse.builder()
                .userId(entity.getUserId())
                .firstName(mainData.getFirstName())
                .lastName(mainData.getLastName())
                .email(mainData.getEmail())
                .phoneNumber(mainData.getPhoneNumber())
                .country(location.getCountry())
                .city(location.getCity())
                .street(location.getStreet())
                .homeNumber(location.getHomeNumber())
                .notificationSettings(toNotificationSettingsDto(settings))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NotificationSettingsDto toNotificationSettingsDto(UserNotificationsSettingsEntity entity) {
        return new NotificationSettingsDto(
                entity.getPush(),
                entity.getEmailEnabled(),
                entity.getSms()
        );
    }
}
