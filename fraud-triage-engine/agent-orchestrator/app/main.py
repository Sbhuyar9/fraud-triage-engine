import os
from fastapi import FastAPI, HTTPException
from .models import TransactionContext, OrchestrationResult
from .graph import build_graph
from .rag.retriever import PolicyRetriever

POLICY_DIR = os.getenv("POLICY_DIR", os.path.join(os.path.dirname(__file__), "rag", "policy_docs"))
retriever = PolicyRetriever(POLICY_DIR)
graph = build_graph(retriever)

app = FastAPI(title="Fraud Triage Agent Orchestrator", version="1.0.0")

@app.get("/health")
def health():
    return {"status": "UP", "policyDocuments": len(retriever.documents)}

@app.post("/orchestrate", response_model=OrchestrationResult)
def orchestrate(tx: TransactionContext):
    try:
        result = graph.invoke({"transaction": tx.model_dump()})
        return OrchestrationResult(
            transactionId=tx.externalRef,
            status=result["recommended_action"],
            riskTier=result["risk_tier"],
            explanation=result["explanation"],
            confidence=result["confidence"],
            agentVerdicts=result["agent_verdicts"]
        )
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Orchestration failed: {exc}")
