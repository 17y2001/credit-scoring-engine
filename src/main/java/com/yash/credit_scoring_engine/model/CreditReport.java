package com.yash.credit_scoring_engine.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_reports")
public class CreditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private int creditScore;
    private String rating;
    private double totalCredits;
    private double totalDebits;
    private double avgMonthlyBalance;
    private LocalDateTime generatedAt;
}