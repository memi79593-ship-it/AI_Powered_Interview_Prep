"""
routes/subjective.py – Day 10 Blueprint.
Handles subjective question generation and evaluation.
"""
import logging
from flask import Blueprint, request, jsonify
from utils.prompt_templates import subjective_prompt, evaluate_subjective_prompt, performance_summary_prompt, model_answer_prompt
from services.ollama_service import cached_generate, call_ollama
import requests

logger = logging.getLogger(__name__)
subjective_bp = Blueprint("subjective", __name__)


@subjective_bp.route("/generate-subjective", methods=["POST"])
def generate_subjective():
    """
    Day 9: Generate 5 subjective questions.
    Optional weak_topic param → AI focuses on that topic.

    Body: { "role": "Java Developer", "level": "medium", "weakTopic": "OOP" }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    role          = data.get("role")
    level         = data.get("level")
    question_count = data.get("questionCount", 5)  # User-specified question count
    weak_topic    = data.get("weakTopic", None)   # Day 9: personalised

    if not role or not level:
        return jsonify({"success": False, "error": "'role' and 'level' required"}), 400

    try:
        prompt = subjective_prompt(role, level, question_count, weak_topic)
        result = cached_generate(prompt)          # Day 10: cached
        return jsonify({
            "success":       True,
            "role":          role,
            "level":         level,
            "questionCount": question_count,
            "weakTopic":     weak_topic,
            "data":          result
        })
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Ollama not running."}), 503
    except requests.exceptions.Timeout:
        return jsonify({"success": False, "error": "Ollama timed out."}), 504
    except Exception as e:
        logger.error("generate_subjective failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500


@subjective_bp.route("/evaluate-subjective", methods=["POST"])
def evaluate_subjective():
    """
    Day 9: Smarter subjective evaluation.
    Returns score + strengths + weaknesses + improvementTips.

    Body: { "question": "...", "answer": "..." }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    question = data.get("question")
    answer   = data.get("answer")

    if not question or not answer:
        return jsonify({"success": False, "error": "'question' and 'answer' required"}), 400

    try:
        prompt = evaluate_subjective_prompt(question, answer)
        result = call_ollama(prompt)   # NOT cached – evaluations must be fresh
        return jsonify({"success": True, "data": result})
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except requests.exceptions.Timeout:
        return jsonify({"success": False, "error": "Gemini request timed out."}), 504
    except Exception as e:
        logger.error("evaluate_subjective failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500


@subjective_bp.route("/generate-performance-summary", methods=["POST"])
def generate_performance_summary():
    """
    Day 11: AI-generated motivational performance summary + roadmap.

    Body: {
        "role": "Java Developer",
        "score": 6, "total": 10,
        "weakTopics": ["OOP", "Threading"],
        "strongTopics": ["Collections"]
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    role          = data.get("role", "Software Developer")
    score         = data.get("score", 0)
    total         = data.get("total", 10)
    weak_topics   = data.get("weakTopics", [])
    strong_topics = data.get("strongTopics", [])

    try:
        prompt = performance_summary_prompt(role, score, total, weak_topics, strong_topics)
        result = call_ollama(prompt)
        return jsonify({"success": True, "data": result})
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except requests.exceptions.Timeout:
        return jsonify({"success": False, "error": "Gemini request timed out."}), 504
    except Exception as e:
        logger.error("generate_performance_summary failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500

@subjective_bp.route("/generate-model-answer", methods=["POST"])
def generate_model_answer():
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400
    question = data.get("question")
    if not question:
        return jsonify({"success": False, "error": "'question' required"}), 400
    try:
        prompt = model_answer_prompt(question)
        result = call_ollama(prompt)
        try:
            import json as _json
            parsed = _json.loads(result)
            answer = parsed.get("modelAnswer", "").strip()
        except Exception:
            answer = result.strip()
        if not answer:
            return jsonify({"success": False, "error": "Empty model answer"}), 500
        return jsonify({"success": True, "data": answer})
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except requests.exceptions.Timeout:
        return jsonify({"success": False, "error": "Gemini request timed out."}), 504
    except Exception as e:
        logger.error("generate_model_answer failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500
