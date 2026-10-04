package com.fraudtriage.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="transaction_id", nullable=false)
    private Transaction transaction;
    @Column(name="final_risk_tier", length=16) private String finalRiskTier;
    @Column(name="final_action", length=32) private String finalAction;
    @Column(columnDefinition="TEXT") private String explanation;
    @Column(name="latency_ms") private Long latencyMs;
    @Column(nullable=false) private Instant createdAt=Instant.now();

    public Long getId(){return id;}
    public Transaction getTransaction(){return transaction;} public void setTransaction(Transaction v){transaction=v;}
    public String getFinalRiskTier(){return finalRiskTier;} public void setFinalRiskTier(String v){finalRiskTier=v;}
    public String getFinalAction(){return finalAction;} public void setFinalAction(String v){finalAction=v;}
    public String getExplanation(){return explanation;} public void setExplanation(String v){explanation=v;}
    public Long getLatencyMs(){return latencyMs;} public void setLatencyMs(Long v){latencyMs=v;}
    public Instant getCreatedAt(){return createdAt;}
}
