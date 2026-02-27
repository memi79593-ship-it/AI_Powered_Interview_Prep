"""
services/ollama_service.py – Gemini-backed LLM service.
"""
import logging
import hashlib
from functools import lru_cache
import requests
from config import GEMINI_API_KEY, MODEL_NAME, GEMINI_TIMEOUT, GEMINI_API_BASE

logger = logging.getLogger(__name__)


def call_ollama(prompt: str) -> str:
    """
    Send a prompt to Gemini and return the text response.
    Raises requests.RequestException on network failures.
    """
    if not GEMINI_API_KEY:
        raise RuntimeError("GEMINI_API_KEY is not set")
    url = f"{GEMINI_API_BASE}/models/{MODEL_NAME}:generateContent"
    logger.info("Calling Gemini | model=%s | prompt_len=%d", MODEL_NAME, len(prompt))
    payload = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": prompt}]
            }
        ]
    }
    headers = {"x-goog-api-key": GEMINI_API_KEY, "Content-Type": "application/json"}
    response = requests.post(url, json=payload, headers=headers, timeout=GEMINI_TIMEOUT)
    response.raise_for_status()
    data = response.json()
    candidates = data.get("candidates", [])
    text_chunks = []
    if candidates:
        content = candidates[0].get("content", {})
        parts = content.get("parts", [])
        for p in parts:
            t = p.get("text")
            if t:
                text_chunks.append(t)
    result = "\n".join(text_chunks).strip()
    logger.info("Gemini response received | len=%d", len(result))
    return result


# ─── Cached question generation ────────────────────────────────────
# Cache key = hash of prompt so identical role+level combos reuse results.
# maxsize=128 means up to 128 unique role/level/topic combos are cached.

def _prompt_hash(prompt: str) -> str:
    return hashlib.md5(prompt.encode()).hexdigest()


@lru_cache(maxsize=128)
def _cached_ollama(prompt_hash: str, prompt: str) -> str:
    """Internal cached wrapper. Call via cached_generate()."""
    return call_ollama(prompt)


def cached_generate(prompt: str) -> str:
    """
    Cached Gemini call.
    Identical prompts return cached result (fast, no LLM re-generation).
    Use for question generation only (not evaluation).
    """
    ph = _prompt_hash(prompt)
    result = _cached_ollama(ph, prompt)
    logger.debug("Cache hit=%s for prompt hash=%s",
                 _cached_ollama.cache_info().hits > 0, ph)
    return result
