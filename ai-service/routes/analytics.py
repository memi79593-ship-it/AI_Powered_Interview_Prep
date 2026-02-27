"""
routes/analytics.py – Day 12 Blueprint.
AI-powered study plan and recommendation generation.
"""
import logging
from flask import Blueprint, request, jsonify
from utils.prompt_templates import study_plan_prompt, followup_prompt
from services.ollama_service import call_ollama
import requests

logger = logging.getLogger(__name__)
analytics_bp = Blueprint("analytics", __name__)


@analytics_bp.route("/generate-study-plan", methods=["POST"])
def generate_study_plan():
    """
    Day 12: Generate a personalized 2-week study plan based on skill profile.

    Body: {
        "role": "Java Developer",
        "avgScore": 55,
        "weakTopics": ["OOP", "Multithreading"],
        "strongTopics": ["Collections"],
        "confidenceLevel": "MEDIUM"
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    role            = data.get("role", "Software Developer")
    avg_score       = data.get("avgScore", 50)
    weak_topics     = data.get("weakTopics", [])
    strong_topics   = data.get("strongTopics", [])
    confidence      = data.get("confidenceLevel", "MEDIUM")

    try:
        prompt = study_plan_prompt(role, avg_score, weak_topics, strong_topics, confidence)
        result = call_ollama(prompt)
        return jsonify({"success": True, "data": result})
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except Exception as e:
        logger.error("generate_study_plan failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500


@analytics_bp.route("/generate-followup", methods=["POST"])
def generate_followup():
    """
    Day 14: Generate a contextual follow-up question based on candidate's answer.

    Body: {
        "question": "What is polymorphism?",
        "answer": "It allows different forms...",
        "personality": "STRICT"
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    question    = data.get("question")
    answer      = data.get("answer")
    personality = data.get("personality", "FRIENDLY")

    if not question or not answer:
        return jsonify({"success": False, "error": "'question' and 'answer' required"}), 400

    try:
        prompt = followup_prompt(question, answer, personality)
        result = call_ollama(prompt)
        return jsonify({"success": True, "followUpQuestion": result})
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except Exception as e:
        logger.error("generate_followup failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500
