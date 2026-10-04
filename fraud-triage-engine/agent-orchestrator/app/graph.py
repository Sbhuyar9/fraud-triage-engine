from typing import Any, Dict, List, TypedDict
from langgraph.graph import StateGraph, START, END

from .models import AgentFinding
from .tools.fraud_tools import velocity_check, geo_anomaly, history_lookup, risk_scorer
from .rag.retriever import PolicyRetriever

class FraudState(TypedDict, total=False):
    transaction: Dict[str, Any]
    pattern_signals: Dict[str, Any]
    compliance_findings: List[Dict[str, Any]]
    risk_tier: str
    recommended_action: str
    explanation: str
    confidence: float
    agent_verdicts: List[Dict[str, Any]]
    escalated: bool

def build_graph(retriever: PolicyRetriever):
    def pattern_analyst(state: FraudState):
        tx = state["transaction"]
        v = velocity_check(tx)
        g = geo_anomaly(tx)
        h = history_lookup(tx)
        signals = [v, g, h]
        score = sum(x["score"] for x in signals)
        flags = [x["detail"] for x in signals if x["flag"]]
        finding = AgentFinding(
            agentName="PatternAnalyst",
            reasoning="; ".join(flags) if flags else "No material velocity, geography, or value anomaly detected.",
            riskSignal=",".join(x["signal"] for x in signals if x["flag"]) or "NONE",
            confidence=min(0.99, 0.70 + (0.08 * len(flags)))
        ).model_dump()
        return {
            "pattern_signals": {"velocity": v, "geo": g, "history": h, "score": score},
            "agent_verdicts": [finding]
        }

    def compliance_auditor(state: FraudState):
        tx = state["transaction"]
        query = f"amount {tx.get('amount')} country {tx.get('country')} merchant {tx.get('merchant')} velocity {tx.get('recentTransactionCount10m')}"
        docs = retriever.search(query, top_k=3)
        findings = []
        score = 0.0
        for doc in docs:
            text = doc["text"].lower()
            if float(tx.get("amount", 0)) >= 10000 and "10,000" in text:
                findings.append(f"{doc['source']}: high-value threshold applies")
                score += 0.25
            if (tx.get("country") or "").upper() in {"AE", "IR", "KP", "SY"} and "high-risk" in text:
                findings.append(f"{doc['source']}: high-risk jurisdiction review applies")
                score += 0.30
            if int(tx.get("recentTransactionCount10m", 0)) >= 3 and "velocity" in text:
                findings.append(f"{doc['source']}: velocity review applies")
                score += 0.15
        score = min(0.65, score)
        confidence = 0.92 if findings else 0.78
        finding = AgentFinding(
            agentName="ComplianceAuditor",
            reasoning="; ".join(findings) if findings else "No retrieved policy clause directly triggered a compliance flag.",
            riskSignal="POLICY_MATCH" if findings else "NO_POLICY_MATCH",
            confidence=confidence
        ).model_dump()
        return {"compliance_findings": docs, "agent_verdicts": state.get("agent_verdicts", []) + [finding],
                "compliance_score": score}

    def route_after_compliance(state: FraudState):
        pattern_score = state["pattern_signals"]["score"]
        compliance_score = state.get("compliance_score", 0.0)
        conflict = pattern_score > 0 and compliance_score == 0
        confidence = 0.92 if not conflict else 0.55
        return "human_escalation" if confidence < 0.60 or conflict else "decision_agent"

    def human_escalation(state: FraudState):
        finding = AgentFinding(
            agentName="HumanEscalation",
            reasoning="Signals could not be resolved with sufficient confidence; case requires analyst review.",
            riskSignal="LOW_CONFIDENCE_OR_CONFLICT",
            confidence=0.55
        ).model_dump()
        return {
            "risk_tier": "HIGH",
            "recommended_action": "MANUAL_REVIEW",
            "confidence": 0.55,
            "explanation": "The case was escalated because fraud signals and/or compliance evidence did not provide sufficient confidence for automatic decisioning.",
            "agent_verdicts": state.get("agent_verdicts", []) + [finding],
            "escalated": True
        }

    def decision_agent(state: FraudState):
        result = risk_scorer(
            state["pattern_signals"]["score"],
            state.get("compliance_score", 0.0),
            float(state["transaction"]["amount"])
        )
        reasons = []
        ps = state["pattern_signals"]
        for key in ("velocity", "geo", "history"):
            if ps[key]["flag"]:
                reasons.append(ps[key]["detail"])
        policy_reasons = []
        for doc in state.get("compliance_findings", []):
            if doc["score"] > 0.15:
                policy_reasons.append(doc["source"])
        explanation = " | ".join(reasons + ([f"Applicable policy evidence: {', '.join(policy_reasons)}"] if policy_reasons else []))
        if not explanation:
            explanation = "No significant fraud or compliance signal was detected."
        confidence = 0.95 if result["score"] < 0.4 else 0.90
        finding = AgentFinding(
            agentName="Decision",
            reasoning=explanation,
            riskSignal=result["riskTier"],
            confidence=confidence
        ).model_dump()
        return {
            "risk_tier": result["riskTier"],
            "recommended_action": result["action"],
            "confidence": confidence,
            "explanation": explanation,
            "agent_verdicts": state.get("agent_verdicts", []) + [finding]
        }

    builder = StateGraph(FraudState)
    builder.add_node("pattern_analyst", pattern_analyst)
    builder.add_node("compliance_auditor", compliance_auditor)
    builder.add_node("human_escalation", human_escalation)
    builder.add_node("decision_agent", decision_agent)
    builder.add_edge(START, "pattern_analyst")
    builder.add_edge("pattern_analyst", "compliance_auditor")
    builder.add_conditional_edges(
        "compliance_auditor",
        route_after_compliance,
        {"human_escalation": "human_escalation", "decision_agent": "decision_agent"}
    )
    builder.add_edge("human_escalation", END)
    builder.add_edge("decision_agent", END)
    return builder.compile()
