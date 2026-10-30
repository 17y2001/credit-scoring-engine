package com.yash.credit_scoring_engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.credit_scoring_engine.model.CreditReport;
import com.yash.credit_scoring_engine.model.Transaction;
import com.yash.credit_scoring_engine.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class KafkaConsumerService {

    @Autowired
    private CreditScoringService creditScoringService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @KafkaListener(topics = "raw-statements", groupId = "credit-scoring-group")
    public void processStatement(
            @Payload String transactionsJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId) {

        System.out.println("Processing statement for userId: " + userId);

        try {
            Transaction[] transactionArray = objectMapper.readValue(transactionsJson, Transaction[].class);
            List<Transaction> transactions = Arrays.asList(transactionArray);

            transactionRepository.saveAll(transactions);

            CreditReport report = creditScoringService.calculateScore(userId, transactions);

            String result = String.format(
                    "userId: %s | Score: %d | Rating: %s | Credits: %.0f | Debits: %.0f",
                    userId, report.getCreditScore(), report.getRating(),
                    report.getTotalCredits(), report.getTotalDebits()
            );

            kafkaProducerService.sendScoreResult(userId, result);

        } catch (Exception e) {
            System.out.println("Error processing statement: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "score-results", groupId = "credit-scoring-group")
    public void handleScoreResult(@Payload String result) {
        System.out.println("SCORE READY: " + result);
    }
}