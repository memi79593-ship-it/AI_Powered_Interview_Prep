"""
app.py – Day 10 Refactored Main Entry Point.
Registers all blueprints, applies rate limiting and CORS globally.
"""
import logging
from flask import Flask, jsonify
from flask_cors import CORS
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
from config import RATE_AI_GENERATE, RATE_CODE_EXEC, MODEL_NAME

# ── Logging Setup – Day 10 ─────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s – %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("ai-service.log", encoding="utf-8")
    ]
)
logger = logging.getLogger(__name__)

# ── App Init ───────────────────────────────────────────────────────
app = Flask(__name__)
CORS(app)

# ── Rate Limiting – Day 8/10 ───────────────────────────────────────
limiter = Limiter(
    get_remote_address,
    app=app,
    default_limits=["200 per day", "50 per hour"],
    storage_uri="memory://"
)

# ── Register Blueprints – Day 10 ───────────────────────────────────
from routes.subjective import subjective_bp
from routes.mcq        import mcq_bp
from routes.coding     import coding_bp
from routes.analytics  import analytics_bp

# Apply per-blueprint rate limits
limiter.limit(RATE_AI_GENERATE)(subjective_bp)
limiter.limit(RATE_AI_GENERATE)(mcq_bp)
limiter.limit(RATE_CODE_EXEC)(coding_bp)
limiter.limit(RATE_AI_GENERATE)(analytics_bp)

app.register_blueprint(subjective_bp)
app.register_blueprint(mcq_bp)
app.register_blueprint(coding_bp)
app.register_blueprint(analytics_bp)


# ── Health ─────────────────────────────────────────────────────────
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": MODEL_NAME})


# ── 429 Rate Limit Error Handler ───────────────────────────────────
@app.errorhandler(429)
def rate_limit_handler(e):
    return jsonify({
        "success": False,
        "error": "Rate limit exceeded. Please wait before making another request.",
        "retryAfter": str(e.description)
    }), 429


# ── Startup ────────────────────────────────────────────────────────
if __name__ == "__main__":
    logger.info("=" * 60)
    logger.info("  AI Interview Service  –  Flask + Gemini + JDoodle")
    logger.info("  Model : %s", MODEL_NAME)
    logger.info("=" * 60)
    logger.info("  POST /generate-subjective     [%s]", RATE_AI_GENERATE)
    logger.info("  POST /evaluate-subjective     [%s]", RATE_AI_GENERATE)
    logger.info("  POST /generate-mcq            [%s]", RATE_AI_GENERATE)
    logger.info("  POST /evaluate-mcq")
    logger.info("  POST /generate-coding         [%s]", RATE_AI_GENERATE)
    logger.info("  POST /execute-code            [%s]", RATE_CODE_EXEC)
    logger.info("  POST /generate-performance-summary")
    logger.info("  GET  /health")
    logger.info("=" * 60)
    app.run(port=5000, debug=True)
