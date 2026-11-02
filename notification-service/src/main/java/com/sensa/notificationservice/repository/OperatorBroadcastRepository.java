package com.sensa.notificationservice.repository;

import com.sensa.notificationservice.entity.OperatorBroadcast;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorBroadcastRepository extends MongoRepository<OperatorBroadcast, String> {

    List<OperatorBroadcast> findTop20ByOrderByCreatedAtDesc();

    List<OperatorBroadcast> findTop20ByOperatorIdOrderByCreatedAtDesc(String operatorId);
}
