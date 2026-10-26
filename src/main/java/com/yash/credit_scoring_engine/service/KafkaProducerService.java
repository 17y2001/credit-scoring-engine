package com.yash.credit_scoring_engine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private KafkaTemplate<String, String> kafkaTemplate ;

    public void sendForScoring(String userId, String transactionJson){
        kafkaTemplate.send("raw-statements", userId, transactionJson) ;
        System.out.println("Send to Kafka for scoring - userId: " + userId) ;
    }

    public void sendScoreResult(String userId, String scoreResult){
        kafkaTemplate.send("score-results", userId, scoreResult) ;
        System.out.println("Score result send to kafka - userId: " + userId);
    }

}
