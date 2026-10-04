from app.graph import build_graph
from app.rag.retriever import PolicyRetriever
from pathlib import Path

def test_high_risk_transaction():
    base = Path(__file__).parents[1] / "app" / "rag" / "policy_docs"
    graph = build_graph(PolicyRetriever(str(base)))
    result = graph.invoke({"transaction": {
        "externalRef": "TX-1", "accountId": "A1", "amount": 14500,
        "currency": "USD", "merchant": "Global Traders", "country": "AE",
        "ipAddress": "203.0.113.7", "recentTransactionCount10m": 3
    }})
    assert result["recommended_action"] in {"MANUAL_REVIEW", "FREEZE"}
    assert len(result["agent_verdicts"]) >= 3
