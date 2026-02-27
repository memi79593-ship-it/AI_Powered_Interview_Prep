"""
config.py – Day 10 centralized config.
All settings read from environment variables with safe defaults.
"""
import os
from dotenv import load_dotenv

load_dotenv()

# ── Gemini ─────────────────────────────────────────────────────────
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
MODEL_NAME     = os.getenv("MODEL_NAME", "gemini-2.5-flash")
GEMINI_TIMEOUT = int(os.getenv("GEMINI_TIMEOUT", "60"))
GEMINI_API_BASE = os.getenv("GEMINI_API_BASE", "https://generativelanguage.googleapis.com/v1")

# ── JDoodle ────────────────────────────────────────────────────────
JDOODLE_URL    = "https://api.jdoodle.com/v1/execute"
JDOODLE_ID     = os.getenv("JD_CLIENT_ID",     "2bd6debd97057201de33e29bd853b189")
JDOODLE_SECRET = os.getenv("JD_CLIENT_SECRET", "c5e4cf06204b3f0633ff308884f1363e1c09d7ab3254635188d95f30535be8e3")

JDOODLE_VERSION = {
    "java":    "4",   # JDK 17
    "python3": "3",   # Python 3
    "c":       "5",   # GCC latest
    "cpp":     "5",   # G++ latest
    "cpp17":   "5",
}

# ── Rate Limits ────────────────────────────────────────────────────
RATE_AI_GENERATE = os.getenv("RATE_AI_GENERATE", "10 per minute")
RATE_CODE_EXEC   = os.getenv("RATE_CODE_EXEC",   "30 per minute")
