from typing import Any, Dict, List, Literal, Optional
from pydantic import BaseModel, Field

Action = Literal["APPROVE", "FREEZE", "MANUAL_REVIEW"]
RiskTier = Literal["LOW", "MEDIUM", "HIGH"]

class TransactionContext(BaseModel):
    externalRef: str
    accountId: str
    amount: float = Field(gt=0)
    currency: str = Field(min_length=3, max_length=8)
    merchant: Optional[str] = None
    country: Optional[str] = None
    ipAddress: Optional[str] = None
    recentTransactionCount10m: int = Field(default=0, ge=0)
    homeCountry: Optional[str] = None

class AgentFinding(BaseModel):
    agentName: str
    reasoning: str
    riskSignal: str
    confidence: float = Field(ge=0, le=1)

class OrchestrationResult(BaseModel):
    transactionId: str
    status: Action
    riskTier: RiskTier
    explanation: str
    confidence: float = Field(ge=0, le=1)
    agentVerdicts: List[AgentFinding]
