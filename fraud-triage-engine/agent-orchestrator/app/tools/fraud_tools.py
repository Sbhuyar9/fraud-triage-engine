from typing import Dict

HIGH_RISK_COUNTRIES = {"AE", "IR", "KP", "SY"}
MEDIUM_RISK_COUNTRIES = {"NG", "RU"}

def velocity_check(tx: Dict) -> Dict:
    count = int(tx.get("recentTransactionCount10m", 0))
    if count >= 5:
        return {"flag": True, "signal": "HIGH_VELOCITY", "score": 0.35, "detail": f"{count} transactions in 10 minutes"}
    if count >= 3:
        return {"flag": True, "signal": "VELOCITY_ANOMALY", "score": 0.20, "detail": f"{count} transactions in 10 minutes"}
    return {"flag": False, "signal": "NORMAL_VELOCITY", "score": 0.0, "detail": "No unusual 10-minute velocity"}

def geo_anomaly(tx: Dict) -> Dict:
    country = (tx.get("country") or "").upper()
    home = (tx.get("homeCountry") or "").upper()
    if country in HIGH_RISK_COUNTRIES:
        return {"flag": True, "signal": "HIGH_RISK_COUNTRY", "score": 0.30, "detail": f"Destination country {country} is high risk"}
    if home and country and country != home:
        return {"flag": True, "signal": "GEO_MISMATCH", "score": 0.10, "detail": f"Transaction country {country} differs from home country {home}"}
    if country in MEDIUM_RISK_COUNTRIES:
        return {"flag": True, "signal": "MEDIUM_RISK_COUNTRY", "score": 0.15, "detail": f"Destination country {country} is medium risk"}
    return {"flag": False, "signal": "NORMAL_GEO", "score": 0.0, "detail": "No geographic anomaly"}

def history_lookup(tx: Dict) -> Dict:
    amount = float(tx.get("amount", 0))
    if amount >= 10000:
        return {"flag": True, "signal": "HIGH_VALUE", "score": 0.20, "detail": f"High-value transaction ${amount:,.2f}"}
    return {"flag": False, "signal": "NORMAL_HISTORY", "score": 0.0, "detail": "No high-value history signal"}

def risk_scorer(pattern_score: float, compliance_score: float, amount: float) -> Dict:
    score = min(1.0, pattern_score + compliance_score)
    if score >= 0.70:
        tier, action = "HIGH", "FREEZE"
    elif score >= 0.40:
        tier, action = "HIGH", "MANUAL_REVIEW"
    elif score >= 0.20:
        tier, action = "MEDIUM", "MANUAL_REVIEW"
    else:
        tier, action = "LOW", "APPROVE"

    # Compliance and large-value cases should be reviewable rather than silently approved.
    if amount >= 10000 and action == "APPROVE":
        tier, action = "MEDIUM", "MANUAL_REVIEW"

    return {"score": score, "riskTier": tier, "action": action}
