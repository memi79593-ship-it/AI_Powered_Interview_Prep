"""
utils/prompt_templates.py – Day 9 Prompt Engineering Optimization.

ALL Ollama prompts live here. Never inline prompts in route files.
This makes the system maintainable, testable, and easy to tune.
"""


# ═══════════════════════════════════════════════════════════════════
#  DAY 9 – ADAPTIVE + WEAK-TOPIC AWARE PROMPTS
# ═══════════════════════════════════════════════════════════════════

def subjective_prompt(role: str, level: str, question_count: int = 5, weak_topic: str = None) -> str:
    """
    Day 9: Personalized subjective question generation.
    If weak_topic provided → AI focuses questions on that topic.
    """
    focus = f"\nIMPORTANT: Focus questions on the weak topic: **{weak_topic}**." if weak_topic else ""

    return f"""You are an expert technical interviewer.

Generate {question_count} {level} level subjective interview questions for a {role}.{focus}

IMPORTANT: Generate THEORY-BASED questions only. Do NOT generate coding challenges, algorithm problems, or programming exercises. Focus on conceptual, theoretical, and practical knowledge questions.

For each question, also provide a comprehensive model answer that demonstrates the ideal response.

Return ONLY a valid JSON array (no extra text, no markdown):
[
    {{"question": "...", "topic": "...", "modelAnswer": "..." }},
    {{"question": "...", "topic": "...", "modelAnswer": "..." }},
    {{"question": "...", "topic": "...", "modelAnswer": "..." }},
    {{"question": "...", "topic": "...", "modelAnswer": "..." }},
    {{"question": "...", "topic": "...", "modelAnswer": "..." }}    
]"""


def mcq_prompt(role: str, level: str, question_count: int = 5, weak_topic: str = None) -> str:
    """Day 9: MCQ generation, optionally focused on weak topic."""
    focus = f"\nIMPORTANT: Prioritise the weak topic: **{weak_topic}**." if weak_topic else ""

    return f"""You are an expert technical interviewer.

Generate {question_count} {level} level MCQ questions for a {role}.{focus}

IMPORTANT: Generate THEORY-BASED questions only. Do NOT generate coding challenges, algorithm problems, or programming exercises. Focus on conceptual, theoretical, and practical knowledge questions.

CRITICAL REQUIREMENTS:
- Each question MUST have exactly 4 options (A, B, C, D)
- Each question MUST include the "correctAnswer" field with the letter of the correct option
- The correctAnswer must be one of: "A", "B", "C", or "D"
- Provide a concise one-sentence "explanation" that justifies why the correct answer is correct

Return ONLY strict JSON (no extra text, no markdown):
[
    {{
        "question": "...",
        "topic": "...",
        "options": ["A) ...", "B) ...", "C) ...", "D) ..."],
        "correctAnswer": "A",
        "explanation": "..."
    }}
]"""


def coding_prompt(role: str, level: str) -> str:
    """Day 9: Coding challenge with multiple test cases per problem."""
    return f"""You are a technical interviewer creating coding challenges.

Generate 2 {level} coding problems for a {role}.
Each problem MUST include 3 test cases with input/output pairs.

Return ONLY strict JSON (no extra text, no markdown):
[
    {{
        "title": "...",
        "description": "...",
        "constraints": "...",
        "testCases": [
            {{"input": "...", "output": "..."}},
            {{"input": "...", "output": "..."}},
            {{"input": "...", "output": "..."}}
        ]
    }}
]"""


# ═══════════════════════════════════════════════════════════════════
#  DAY 9 – SMARTER EVALUATION PROMPTS
# ═══════════════════════════════════════════════════════════════════

def evaluate_subjective_prompt(question: str, answer: str) -> str:
    """
    Day 9: Professional evaluation across 5 dimensions.
    Returns structured JSON with score, strengths, weaknesses, tips.
    This simulates real HR/tech interviewer feedback.
    """
    return f"""You are a senior technical interviewer and evaluator at a top tech company.

Evaluate the candidate's answer professionally.

Question:
{question}

Candidate's Answer:
{answer}

Evaluate strictly across these dimensions:
1. Technical Accuracy  – Is the information technically correct?
2. Completeness       – Are all key concepts covered?
3. Clarity            – Is the explanation clear and concise?
4. Best Practices     – Does it reflect industry-standard practices?
5. Depth              – Is there sufficient technical depth?

Return ONLY strict JSON (no extra text, no markdown):
{{
    "score": 8,
    "technicalAccuracy": 9,
    "completeness": 7,
    "clarity": 8,
    "bestPractices": 8,
    "depth": 7,
    "strengths": "Clear explanation of core concept. Good use of examples.",
    "weaknesses": "Missed mentioning thread safety and edge cases.",
    "improvementTips": "Study Java Memory Model. Practice explaining with real code snippets.",
    "overallLevel": "Intermediate"
}}"""

def model_answer_prompt(question: str) -> str:
    return f"""You are an expert technical interviewer.

Provide a comprehensive, technically accurate model answer to the following interview question.
Focus on clarity, completeness, and best practices. Avoid markdown; return only plain text.

Question:
{question}

Return ONLY strict JSON:
{{
  "modelAnswer": "..."
}}"""

# ═══════════════════════════════════════════════════════════════════
#  DAY 11 – PERFORMANCE SUMMARY PROMPT
# ═══════════════════════════════════════════════════════════════════

def performance_summary_prompt(role: str, score: int, total: int,
                                weak_topics: list, strong_topics: list) -> str:
    """
    Day 11: AI-generated motivational performance summary.
    Returns an actionable improvement roadmap.
    """
    weak  = ", ".join(weak_topics)   if weak_topics   else "None identified"
    strong = ", ".join(strong_topics) if strong_topics else "None identified"

    return f"""You are a supportive career coach and senior software engineer mentor.

A candidate just completed an interview simulation. Provide a professional performance summary.

Details:
- Role: {role}
- Score: {score}/{total}
- Percentage: {round(score/total*100) if total > 0 else 0}%
- Weak Topics: {weak}
- Strong Topics: {strong}

Write a motivational, actionable improvement plan in the following JSON format:
{{
    "overallLevel": "Beginner | Intermediate | Advanced",
    "summary": "2-3 sentence professional summary of performance",
    "strengths": ["strength 1", "strength 2"],
    "areasToImprove": ["area 1", "area 2"],
    "weeklyRoadmap": {{
        "week1": "Focus on ...",
        "week2": "Practice ...",
        "week3": "Build project that ...",
        "week4": "Mock interview on ..."
    }},
    "topResources": ["resource 1", "resource 2", "resource 3"],
    "encouragement": "A short motivational closing message."
}}"""


# 
#  DAY 12 – STUDY PLAN PROMPT
# 

def study_plan_prompt(role: str, avg_score: float, weak_topics: list,
                      strong_topics: list, confidence: str) -> str:
    weak   = ', '.join(weak_topics)   if weak_topics   else 'None identified'
    strong = ', '.join(strong_topics) if strong_topics else 'None identified'
    return f"""You are an expert technical career coach for software engineers.

Create a personalized 2-week study plan for a {role} candidate.

Profile:
- Average Score: {avg_score}%
- Confidence Level: {confidence}
- Weak Topics: {weak}
- Strong Topics: {strong}

Return ONLY strict JSON (no extra text):
{{
    "weeklyPlan": {{
        "week1": {{
            "focus": "...",
            "dailyTasks": ["Day 1: ...", "Day 2: ...", "Day 3: ...", "Day 4: ...", "Day 5: ..."],
            "resources": ["resource 1", "resource 2"]
        }},
        "week2": {{
            "focus": "...",
            "dailyTasks": ["Day 1: ...", "Day 2: ...", "Day 3: ...", "Day 4: ...", "Day 5: ..."],
            "resources": ["resource 1", "resource 2"]
        }}
    }},
    "priorityTopics": ["topic 1", "topic 2", "topic 3"],
    "practiceProjects": ["project 1", "project 2"],
    "estimatedReadyDate": "2 weeks from today"
}}"""


# 
#  DAY 14 – FOLLOW-UP QUESTION + PERSONALITY PROMPTS
# 

def followup_prompt(question: str, answer: str, personality: str = 'FRIENDLY') -> str:
    personality_map = {
        'STRICT':    'You are a strict senior interviewer. Challenge the candidate with a tough follow-up.',
        'FRIENDLY':  'You are a supportive mentor. Ask a gentle clarifying follow-up question.',
        'TECHNICAL': 'You are a deep-technical expert. Probe edge cases and implementation details.'
    }
    persona = personality_map.get(personality.upper(), personality_map['FRIENDLY'])
    return f"""{persona}

Original Question: {question}
Candidate's Answer: {answer}

Generate exactly ONE follow-up question that probes deeper into their answer.
Return ONLY the follow-up question text, no JSON, no preamble."""


def subjective_prompt_with_personality(role: str, level: str,
                                        weak_topic: str = None,
                                        personality: str = 'FRIENDLY') -> str:
    personality_map = {
        'STRICT':    'You are a demanding senior interviewer. Ask tough, unforgiving questions.',
        'FRIENDLY':  'You are an encouraging mentor. Ask clear, accessible questions.',
        'TECHNICAL': 'You are a deep-tech expert. Focus on implementation, edge cases, and depth.'
    }
    persona = personality_map.get(personality.upper(), personality_map['FRIENDLY'])
    focus   = f"\nIMPORTANT: Focus on weak topic: **{weak_topic}**." if weak_topic else ""
    return f"""{persona}

Generate 5 {level} level subjective interview questions for a {role}.{focus}

IMPORTANT: Generate THEORY-BASED questions only. Do NOT generate coding challenges, algorithm problems, or programming exercises. Focus on conceptual, theoretical, and practical knowledge questions.

Return ONLY strict JSON (no extra text):
[
    {{"question": "...", "topic": "..."}},
    {{"question": "...", "topic": "..."}},
    {{"question": "...", "topic": "..."}},
    {{"question": "...", "topic": "..."}},
    {{"question": "...", "topic": "..."}}
]"""
