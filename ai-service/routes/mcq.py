"""
routes/mcq.py – Day 10 Blueprint.
Handles MCQ generation and automatic evaluation.
"""
import logging
from flask import Blueprint, request, jsonify
from utils.prompt_templates import mcq_prompt
from services.ollama_service import cached_generate
import requests

logger = logging.getLogger(__name__)
mcq_bp = Blueprint("mcq", __name__)


@mcq_bp.route("/generate-mcq", methods=["POST"])
def generate_mcq():
    """
    Day 9: Generate 5 MCQ questions, optionally focused on weak topic.
    Body: { "role": "...", "level": "...", "weakTopic": "OOP" }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    role          = data.get("role")
    level         = data.get("level")
    question_count = data.get("questionCount", 5)  # User-specified question count
    weak_topic    = data.get("weakTopic", None)

    if not role or not level:
        return jsonify({"success": False, "error": "'role' and 'level' required"}), 400

    try:
        prompt = mcq_prompt(role, level, question_count, weak_topic)
        result = cached_generate(prompt)
        return jsonify({
            "success":       True,
            "role":          role,
            "level":         level,
            "questionCount": question_count,
            "weakTopic":     weak_topic,
            "data":          result
        })
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except Exception as e:
        logger.error("generate_mcq failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500


@mcq_bp.route("/evaluate-mcq", methods=["POST"])
def evaluate_mcq():
    """
    Pure Python auto-evaluation of MCQ answers (no Ollama needed).

    Body: {
        "questions": [
            { "question": "...", "correctAnswer": "A", "selectedAnswer": "A" }
        ]
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    questions = data.get("questions")
    if not questions or not isinstance(questions, list):
        return jsonify({"success": False, "error": "'questions' must be a non-empty list"}), 400

    score   = 0
    results = []

    for q in questions:
        correct  = q.get("correctAnswer", "").strip().upper()
        selected = q.get("selectedAnswer", "").strip().upper()
        is_ok    = (selected == correct)
        if is_ok:
            score += 1
        results.append({
            "question":      q.get("question", ""),
            "correct":       is_ok,
            "correctAnswer": correct,
            "selectedAnswer": selected
        })

    total      = len(questions)
    percentage = round((score / total) * 100, 2) if total > 0 else 0.0

    return jsonify({
        "success":    True,
        "score":      score,
        "total":      total,
        "percentage": percentage,
        "results":    results
    })
