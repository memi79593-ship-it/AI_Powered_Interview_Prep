"""
services/jdoodle_service.py – Day 10 Service Separation + Day 9 Multi-Testcase.

Handles all JDoodle API communication.
Day 9: Multi-testcase evaluation using concurrent.futures for speed.
"""
import logging
import concurrent.futures
import requests
from config import JDOODLE_URL, JDOODLE_ID, JDOODLE_SECRET, JDOODLE_VERSION

logger = logging.getLogger(__name__)

JDOODLE_TIMEOUT = 30  # seconds per execution


def execute_single(code: str, language: str, stdin: str = "") -> dict:
    """
    Execute code once via JDoodle API.
    Returns raw JDoodle response dict.
    """
    version_index = JDOODLE_VERSION.get(language.lower(), "0")

    payload = {
        "clientId":     JDOODLE_ID,
        "clientSecret": JDOODLE_SECRET,
        "script":       code,
        "language":     language.lower(),
        "versionIndex": version_index,
        "stdin":        stdin
    }

    logger.info("JDoodle execute | lang=%s | stdin=%s", language, stdin[:50] if stdin else "")
    response = requests.post(JDOODLE_URL, json=payload, timeout=JDOODLE_TIMEOUT)
    return response.json()


def execute_with_expected(code: str, language: str, expected: str) -> dict:
    """
    Execute once and compare output to expected string.
    Returns execution result + isCorrect flag.
    """
    jd = execute_single(code, language)
    actual     = jd.get("output", "").strip()
    is_correct = (actual == expected.strip())

    return {
        "output":         actual,
        "statusCode":     jd.get("statusCode", -1),
        "memory":         jd.get("memory", ""),
        "cpuTime":        jd.get("cpuTime", ""),
        "isCorrect":      is_correct,
        "expectedOutput": expected,
        "actualOutput":   actual
    }


# ─── Day 9: Multi-testcase evaluation ─────────────────────────────

def _run_test(args):
    """Worker function for concurrent test execution."""
    code, language, test_case = args
    stdin  = test_case.get("input", "")
    expected = test_case.get("output", "").strip()

    try:
        jd     = execute_single(code, language, stdin)
        actual = jd.get("output", "").strip()
        passed = (actual == expected)
    except Exception as e:
        logger.warning("Test case execution failed: %s", e)
        actual = ""
        passed = False

    return {
        "input":          stdin,
        "expectedOutput": expected,
        "actualOutput":   actual,
        "passed":         passed
    }


def execute_multi_testcase(code: str, language: str, test_cases: list) -> dict:
    """
    Day 9: Run all test cases concurrently using ThreadPoolExecutor.
    Returns score, percentage, and per-test results.

    test_cases format: [{"input": "5", "output": "120"}, ...]
    """
    if not test_cases:
        return {"error": "No test cases provided"}

    args = [(code, language, tc) for tc in test_cases]

    # Run all test cases in parallel (IO-bound → threads are ideal)
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        results = list(executor.map(_run_test, args))

    total  = len(results)
    passed = sum(1 for r in results if r["passed"])
    score  = round((passed / total) * 100, 2) if total > 0 else 0.0

    logger.info("Multi-testcase | passed=%d/%d | score=%.1f%%", passed, total, score)

    return {
        "success":    True,
        "passed":     passed,
        "total":      total,
        "score":      score,
        "testResults": results
    }
