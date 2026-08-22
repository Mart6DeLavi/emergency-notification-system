package com.sensa.usermanagementservice.dto;

import java.util.UUID;

public interface UserLocationResponse {
    UUID getUserId();
    String getEmail();
    String getPhoneNumber();
    String getFirstName();
    String getLastName();
    boolean getPush();
    boolean getEmailEnabled();
    boolean getSms();
}
