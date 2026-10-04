package com.fraudtriage.service;

import com.fraudtriage.dto.*;
import com.fraudtriage.entity.*;
import com.fraudtriage.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactions;
    private final AgentVerdictRepository verdicts;
    private final AuditLogRepository audits;
    private final PreFilterService preFilter;
    private final OrchestratorClient orchestrator;

    public TransactionService(TransactionRepository transactions, AgentVerdictRepository verdicts,
                              AuditLogRepository audits, PreFilterService preFilter,
                              OrchestratorClient orchestrator) {
        this.transactions=transactions; this.verdicts=verdicts; this.audits=audits;
        this.preFilter=preFilter; this.orchestrator=orchestrator;
    }

    @Transactional
    public TransactionResponse submit(TransactionRequest req) {
        if (transactions.findByExternalRef(req.externalRef()).isPresent())
            throw new IllegalArgumentException("externalRef already exists");

        Transaction tx = new Transaction();
        tx.setExternalRef(req.externalRef()); tx.setAccountId(req.accountId());
        tx.setAmount(req.amount()); tx.setCurrency(req.currency().toUpperCase());
        tx.setMerchant(req.merchant()); tx.setCountry(req.country());
        tx.setIpAddress(req.ipAddress());
        tx.setRecentTransactionCount10m(Optional.ofNullable(req.recentTransactionCount10m()).orElse(0));
        tx.setHomeCountry(req.homeCountry());
        tx.setStatus(TransactionStatus.PENDING);
        transactions.save(tx);

        long start=System.currentTimeMillis();
        OrchestrationResponse result;
        if (preFilter.shouldOrchestrate(req)) {
            try {
                result = orchestrator.orchestrate(req);
            } catch (RestClientException ex) {
                // Resilient fallback: if the agent-orchestrator is unreachable, slow, or errors,
                // don't lose the transaction or throw a 500 - route it to manual review so a human
                // makes the final call, and record why.
                log.warn("Agent orchestrator call failed for {}: {}", req.externalRef(), ex.getMessage());
                result = new OrchestrationResponse(req.externalRef(), "MANUAL_REVIEW", "HIGH",
                        "Agent orchestrator was unavailable or timed out; routed to manual review as a safety fallback.",
                        0.0, List.of(new AgentVerdictResponse("Orchestrator", "Call to agent-orchestrator failed: " + ex.getMessage(),
                                "ORCHESTRATOR_UNAVAILABLE", 0.0)));
            }
        } else {
            result = new OrchestrationResponse(req.externalRef(),"APPROVE","LOW",
                    "Pre-filter found no material fraud signal; transaction was approved without agent orchestration.",
                    0.99, List.of(new AgentVerdictResponse("PatternAnalyst","Pre-filter found no threshold breach.","PRE_FILTER_CLEAR",0.99)));
        }

        tx.setStatus(TransactionStatus.fromAgentAction(result.status()));
        transactions.save(tx);

        for (AgentVerdictResponse v : result.agentVerdicts()) {
            AgentVerdict av=new AgentVerdict();
            av.setTransaction(tx); av.setAgentName(v.agentName()); av.setReasoning(v.reasoning());
            av.setRiskSignal(v.riskSignal()); av.setConfidence(v.confidence());
            verdicts.save(av);
        }

        AuditLog audit=new AuditLog();
        audit.setTransaction(tx); audit.setFinalRiskTier(result.riskTier());
        audit.setFinalAction(result.status()); audit.setExplanation(result.explanation());
        audit.setLatencyMs(System.currentTimeMillis()-start); audits.save(audit);

        return new TransactionResponse(tx.getId(),result.status(),result.riskTier(),result.explanation(),
                "/api/v1/transactions/"+tx.getId()+"/audit-trail");
    }

    @Transactional(readOnly=true)
    public Transaction get(Long id) {
        return transactions.findById(id).orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    @Transactional(readOnly=true)
    public List<AgentVerdict> trail(Long id) {
        get(id);
        return verdicts.findByTransactionIdOrderByCreatedAtAsc(id);
    }

    @Transactional(readOnly=true)
    public List<AuditLog> audits(Long id) {
        get(id);
        return audits.findByTransactionIdOrderByCreatedAtAsc(id);
    }

    @Transactional(readOnly=true)
    public List<Transaction> queue(TransactionStatus status) {
        return status == null ? transactions.findAll() : transactions.findByStatus(status);
    }

    @Transactional
    public Transaction override(Long id, String status, String reason) {
        Transaction tx=get(id);
        TransactionStatus newStatus=TransactionStatus.valueOf(status);
        tx.setStatus(newStatus);
        transactions.save(tx);

        AuditLog audit=new AuditLog();
        audit.setTransaction(tx); audit.setFinalRiskTier(newStatus == TransactionStatus.FROZEN ? "HIGH" : "MANUAL");
        audit.setFinalAction(newStatus.name()); audit.setExplanation("ADMIN override: "+reason);
        audit.setLatencyMs(0L); audits.save(audit);

        return tx;
    }
}
