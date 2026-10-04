from pathlib import Path
from typing import Dict, List
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

class PolicyRetriever:
    def __init__(self, policy_dir: str):
        self.policy_dir = Path(policy_dir)
        self.documents = []
        for path in sorted(self.policy_dir.glob("*.md")):
            self.documents.append({
                "source": path.name,
                "text": path.read_text(encoding="utf-8")
            })
        if not self.documents:
            raise RuntimeError(f"No policy documents found in {self.policy_dir}")
        self.vectorizer = TfidfVectorizer(stop_words="english")
        self.matrix = self.vectorizer.fit_transform([d["text"] for d in self.documents])

    def search(self, query: str, top_k: int = 3) -> List[Dict]:
        q = self.vectorizer.transform([query])
        scores = cosine_similarity(q, self.matrix)[0]
        indexes = scores.argsort()[::-1][:top_k]
        return [
            {
                "source": self.documents[i]["source"],
                "text": self.documents[i]["text"],
                "score": float(scores[i])
            }
            for i in indexes if scores[i] > 0
        ]
