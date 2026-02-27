"""
routes/coding.py – Day 10 Blueprint.
Handles coding question generation and multi-testcase execution.
"""
import logging
from flask import Blueprint, request, jsonify
from utils.prompt_templates import coding_prompt
from services.ollama_service import cached_generate
from services.jdoodle_service import (
    execute_single,
    execute_with_expected,
    execute_multi_testcase
)
import requests

logger = logging.getLogger(__name__)
coding_bp = Blueprint("coding", __name__)


@coding_bp.route("/generate-coding", methods=["POST"])
def generate_coding():
    """
    Day 9: Generate 2 coding challenges with multi-testcase structure.
    Body: { "role": "Java Developer", "level": "medium" }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    role  = data.get("role")
    level = data.get("level")
    if not role or not level:
        return jsonify({"success": False, "error": "'role' and 'level' required"}), 400

    try:
        prompt = coding_prompt(role, level)
        result = cached_generate(prompt)
        return jsonify({"success": True, "role": role, "level": level, "data": result})
    except requests.exceptions.ConnectionError:
        return jsonify({"success": False, "error": "Gemini API unavailable."}), 503
    except Exception as e:
        logger.error("generate_coding failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500


@coding_bp.route("/execute-code", methods=["POST"])
def execute_code():
    """
    Day 9: Execute code with optional multi-testcase evaluation.

    Body (single run):
    { "code": "...", "language": "java", "expectedOutput": "Hello" }

    Body (multi-testcase – Day 9):
    {
        "code": "...",
        "language": "python3",
        "testCases": [
            {"input": "5", "output": "120"},
            {"input": "3", "output": "6"}
        ]
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({"success": False, "error": "Request body required"}), 400

    code       = data.get("code")
    language   = data.get("language", "java")
    test_cases = data.get("testCases", None)
    expected   = data.get("expectedOutput", None)

    if not code:
        return jsonify({"success": False, "error": "'code' is required"}), 400

    try:
        # ── Multi-testcase evaluation (Day 9) ─────────────────────
        if test_cases and isinstance(test_cases, list):
            result = execute_multi_testcase(code, language, test_cases)
            return jsonify(result)

        # ── Single run with optional expected comparison ──────────
        if expected is not None:
            result = execute_with_expected(code, language, expected)
        else:
            jd = execute_single(code, language)
            result = {
                "success":    True,
                "output":     jd.get("output", ""),
                "statusCode": jd.get("statusCode", -1),
                "memory":     jd.get("memory", ""),
                "cpuTime":    jd.get("cpuTime", "")
            }

        return jsonify(result)

    except requests.exceptions.Timeout:
        return jsonify({"success": False, "error": "JDoodle request timed out."}), 504
    except Exception as e:
        logger.error("execute_code failed: %s", e)
        return jsonify({"success": False, "error": str(e)}), 500
