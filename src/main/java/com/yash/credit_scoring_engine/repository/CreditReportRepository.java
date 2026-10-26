package com.yash.credit_scoring_engine.repository;

import com.yash.credit_scoring_engine.model.CreditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditReportRepository extends JpaRepository<CreditReport, Long>{
    Optional<CreditReport> findTopByUserIdOrderByGeneratedAtDesc(String userId);
}
