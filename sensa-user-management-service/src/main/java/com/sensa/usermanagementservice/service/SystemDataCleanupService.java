package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.repository.SystemDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SystemDataCleanupService {
    private final SystemDataRepository systemDataRepository;

    @Transactional
    public void deleteOldData() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1);
        systemDataRepository.deleteByCreatedAtBefore(cutoffTime);
    }
}
