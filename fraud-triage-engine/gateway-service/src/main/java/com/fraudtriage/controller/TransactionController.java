package com.fraudtriage.controller;

import com.fraudtriage.dto.*;
import com.fraudtriage.entity.*;
import com.fraudtriage.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service){this.service=service;}

    @PostMapping
    public ResponseEntity<TransactionResponse> submit(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.submit(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> get(@PathVariable Long id) {
        Transaction tx=service.get(id);
        var audits=service.audits(id);
        var latest=audits.isEmpty()?null:audits.get(audits.size()-1);
        return ResponseEntity.ok(new TransactionResponse(tx.getId(),tx.getStatus().name(),
                latest==null?null:latest.getFinalRiskTier(),
                latest==null?null:latest.getExplanation(),
                "/api/v1/transactions/"+id+"/audit-trail"));
    }

    @GetMapping("/{id}/audit-trail")
    public ResponseEntity<AuditTrailResponse> trail(@PathVariable Long id) {
        Transaction tx=service.get(id);
        var av=service.trail(id);
        var audits=service.audits(id);
        var latest=audits.isEmpty()?null:audits.get(audits.size()-1);
        var agents=av.stream().map(v->new AgentVerdictResponse(v.getAgentName(),v.getReasoning(),v.getRiskSignal(),v.getConfidence())).toList();
        return ResponseEntity.ok(new AuditTrailResponse(id,tx.getStatus().name(),
                latest==null?null:latest.getFinalRiskTier(),latest==null?null:latest.getExplanation(),
                agents,latest==null?null:latest.getCreatedAt(),latest==null?null:latest.getLatencyMs()));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(@RequestParam(required=false) TransactionStatus status) {
        return ResponseEntity.ok(service.queue(status).stream().map(tx -> {
            var audits=service.audits(tx.getId());
            var a=audits.isEmpty()?null:audits.get(audits.size()-1);
            return new TransactionResponse(tx.getId(),tx.getStatus().name(),a==null?null:a.getFinalRiskTier(),
                    a==null?null:a.getExplanation(),"/api/v1/transactions/"+tx.getId()+"/audit-trail");
        }).toList());
    }

    @PostMapping("/{id}/override")
    public ResponseEntity<TransactionResponse> override(@PathVariable Long id,@Valid @RequestBody OverrideRequest req) {
        Transaction tx=service.override(id,req.status(),req.reason());
        return ResponseEntity.ok(new TransactionResponse(tx.getId(),tx.getStatus().name(),null,
                "Administrative override applied: "+req.reason(),
                "/api/v1/transactions/"+id+"/audit-trail"));
    }
}
