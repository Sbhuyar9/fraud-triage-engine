package com.fraudtriage.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="agent_verdicts")
public class AgentVerdict {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="transaction_id", nullable=false)
    private Transaction transaction;
    @Column(name="agent_name", nullable=false, length=64) private String agentName;
    @Column(columnDefinition="TEXT") private String reasoning;
    @Column(name="risk_signal", length=64) private String riskSignal;
    @Column(nullable=false) private Double confidence;
    @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();

    public Long getId(){return id;}
    public Transaction getTransaction(){return transaction;} public void setTransaction(Transaction v){transaction=v;}
    public String getAgentName(){return agentName;} public void setAgentName(String v){agentName=v;}
    public String getReasoning(){return reasoning;} public void setReasoning(String v){reasoning=v;}
    public String getRiskSignal(){return riskSignal;} public void setRiskSignal(String v){riskSignal=v;}
    public Double getConfidence(){return confidence;} public void setConfidence(Double v){confidence=v;}
    public Instant getCreatedAt(){return createdAt;}
}
