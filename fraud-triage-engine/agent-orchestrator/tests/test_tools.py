from app.tools.fraud_tools import velocity_check, geo_anomaly, history_lookup, risk_scorer

def test_velocity():
    assert velocity_check({"recentTransactionCount10m": 3})["flag"] is True
    assert velocity_check({"recentTransactionCount10m": 0})["flag"] is False

def test_geo():
    assert geo_anomaly({"country": "AE"})["flag"] is True

def test_history():
    assert history_lookup({"amount": 12000})["signal"] == "HIGH_VALUE"

def test_scorer():
    assert risk_scorer(0.0, 0.0, 100.0)["action"] == "APPROVE"
