import React, { useEffect, useState, useCallback, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getQuestions, submitAnswer, completeSession, getFollowUp, getSession } from '../services/api'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'

const TIMER_SECONDS = 45 * 60 // 45 minutes

export default function Interview() {
    const { id } = useParams()
    const nav = useNavigate()
    const email = localStorage.getItem('email')

    const [questions, setQuestions] = useState([])
    const [filteredQuestions, setFilteredQuestions] = useState([])
    const [current, setCurrent] = useState(0)
    const [answer, setAnswer] = useState('')
    const [tempAnswers, setTempAnswers] = useState({}) // Store answers temporarily during interview
    const [loading, setLoading] = useState(true)
    const [submitting, setSubmitting] = useState(false)
    const [evaluating, setEvaluating] = useState(false) // Track evaluation status
    const [following, setFollowing] = useState(false)
    const [followUp, setFollowUp] = useState(null)
    const [timeLeft, setTimeLeft] = useState(TIMER_SECONDS)
    const [done, setDone] = useState(false)
    
    // States removed: coding functionality no longer supported

    const timerRef = useRef(null)

    useEffect(() => {
        Promise.all([
            getQuestions(id),
            getSession(id)
        ]).then(([questionsRes, sessionRes]) => {
            const allQuestions = questionsRes.data || [];
            const sessionType = sessionRes.data.type.toLowerCase();
            
            // Filter questions based on session type to ensure only appropriate question types are shown
            // First, get all questions
            let filteredQuestions = allQuestions;
            
            // Filter by type if needed
            if (sessionType === 'subjective') {
                filteredQuestions = allQuestions.filter(q => q.type === 'subjective');
            } else if (sessionType === 'mcq') {
                filteredQuestions = allQuestions.filter(q => q.type === 'mcq');
            } else if (sessionType === 'coding') {
                filteredQuestions = allQuestions.filter(q => q.type === 'coding');
            }
            // For 'full' type, we show all question types
            
            // Limit to the requested question count
            filteredQuestions = filteredQuestions.slice(0, sessionRes.data.questionCount || 5);
            
            setQuestions(allQuestions);
            setFilteredQuestions(filteredQuestions);
        }).catch(() => {
            toast.error('Failed to load questions or session details');
        }).finally(() => {
            setLoading(false);
        });
    }, [id])

    // Timer
    useEffect(() => {
        timerRef.current = setInterval(() => {
            setTimeLeft(t => {
                if (t <= 1) { clearInterval(timerRef.current); handleComplete(); return 0 }
                return t - 1
            })
        }, 1000)
        return () => clearInterval(timerRef.current)
    }, [])

    const fmt = s => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
    const timerClass = timeLeft < 120 ? 'timer danger' : timeLeft < 300 ? 'timer warn' : 'timer'

    const q = filteredQuestions[current]

    const handleSubmitAnswer = async (ans = answer) => {
        if (!ans.trim()) return toast.error('Please enter an answer')
        setSubmitting(true)
        setFollowUp(null)
        
        // Store answer locally instead of submitting to backend
        setTempAnswers(prev => ({
            ...prev,
            [q.id]: ans
        }));
        
        // Clear the answer field
        setAnswer('')
        
        setSubmitting(false)
    }

    const handleGetFollowUp = async () => {
        if (!submitted[q.id]) return toast('Submit your answer first')
        setFollowing(true)
        try {
            const res = await getFollowUp(id, { question: q.questionText, answer: submitted[q.id] })
            setFollowUp(res.data?.followUpQuestion || res.data?.data)
        } catch { toast.error('Follow-up failed') }
        finally { setFollowing(false) }
    }

    // Coding functions removed: coding functionality no longer supported

    // Language detection removed: coding functionality no longer supported

    const handleComplete = useCallback(async () => {
        if (evaluating) return; // Prevent multiple clicks
        
        clearInterval(timerRef.current);
        setEvaluating(true);
        
        try {
            // First, submit all answers collected during the interview
            const answerPromises = Object.entries(tempAnswers).map(([questionId, userAnswer]) => {
                return submitAnswer({ 
                    sessionId: Number(id), 
                    questionId: parseInt(questionId), 
                    answer: userAnswer 
                });
            });
            
            // Wait for all answers to be submitted
            await Promise.all(answerPromises);
            
            // Then complete the session (this triggers batch evaluation)
            await completeSession(id);
            
            // Small delay to ensure evaluation is complete
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Navigate to results page after evaluation
            toast.success('Interview completed! 🎉')
            nav(`/review/${id}`);
        } catch (error) {
            console.error('Error completing interview:', error);
            toast.error('Could not complete session');
            setEvaluating(false);
        }
    }, [id, nav, tempAnswers, evaluating])

    if (loading) return <><Navbar /><div className="page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}><div className="spinner" /></div></>

    const progress = filteredQuestions.length > 0 ? ((current + 1) / filteredQuestions.length) * 100 : 0

    return (
        <>
            <Navbar />
            <div className="page fade-up" style={{ maxWidth: 820 }}>

                {/* Top bar */}
                <div className="flex justify-between items-center mb-6">
                    <div>
                        <h2>Interview Session</h2>
                        <p style={{ fontSize: '0.85rem' }}>Question {current + 1} of {filteredQuestions.length}</p>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className={timerClass}>⏱ {fmt(timeLeft)}</div>
                        <button className="btn btn-success btn-sm" onClick={handleComplete} disabled={evaluating}>
                            {evaluating ? '⏳ Evaluating...' : '✅ Complete Interview'}
                        </button>
                    </div>
                </div>

                {/* Progress */}
                <div className="progress mb-6">
                    <div className="progress-fill" style={{ width: `${progress}%` }} />
                </div>

                {/* Question Card */}
                {q ? (
                    <div className="card mb-6" style={{ borderColor: tempAnswers[q.id] ? 'rgba(16,185,129,0.4)' : 'var(--border)' }}>
                        <div className="flex justify-between items-center mb-4">
                            <span className="chip">{q.type}</span>
                            {q.topic && <span className="chip" style={{ background: 'rgba(139,92,246,0.15)', color: 'var(--violet)' }}>{q.topic}</span>}
                            {tempAnswers[q.id] && <span className="badge badge-high">✓ Answered</span>}
                        </div>

                        <h3 style={{ fontSize: '1.05rem', lineHeight: 1.7, marginBottom: 24 }}>{q.questionText}</h3>

                        {/* Question Type Handling */}
                        {q.type === 'mcq' && q.options ? (
                            /* MCQ Question */
                            <div className="flex-col gap-3" style={{ display: 'flex' }}>
                                {(typeof q.options === 'string' ? JSON.parse(q.options) : q.options).map((opt, i) => (
                                    <button key={i} className={`mcq-option ${tempAnswers[q.id] === opt ? 'selected' : ''}`}
                                        onClick={() => !tempAnswers[q.id] && handleSubmitAnswer(opt)}>
                                        {opt}
                                    </button>
                                ))}
                            </div>
                        ) : (
                            /* Subjective Question */
                            <div>
                                <textarea className="input" rows={5}
                                    placeholder='Type your answer...'
                                    value={answer} onChange={e => setAnswer(e.target.value)}
                                    disabled={!!tempAnswers[q.id]}
                                    style={{ resize: 'vertical' }}
                                />
                                {!tempAnswers[q.id] && (
                                    <button className="btn btn-primary mt-4" onClick={() => handleSubmitAnswer()} disabled={submitting}>
                                        {submitting ? '⏳ Saving...' : '✓ Save Answer'}
                                    </button>
                                )}
                            </div>
                        )}

                        {/* AI Follow-up (Subjective only) */}
                        {q.type === 'subjective' && tempAnswers[q.id] && (
                            <div className="mt-6">
                                <button className="btn btn-secondary btn-sm" onClick={handleGetFollowUp} disabled={following}>
                                    {following ? '⏳ Generating...' : '🤖 Get AI Follow-up'}
                                </button>
                                {followUp && (
                                    <div className="alert alert-info mt-4">
                                        <strong>Follow-up:</strong> {followUp}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="card text-center">
                        <div style={{ fontSize: '3rem', marginBottom: 12 }}>✅</div>
                        <h2>All questions answered!</h2>
                        <p className="mt-4">Click the button below to complete your interview.</p>
                    </div>
                )}

                {/* Navigation */}
                <div className="flex justify-between">
                    <button className="btn btn-secondary" onClick={() => { setCurrent(c => Math.max(0, c - 1)); setFollowUp(null) }} disabled={current === 0}>← Previous</button>
                    {current < filteredQuestions.length - 1
                        ? <button className="btn btn-primary" onClick={() => { setCurrent(c => c + 1); setFollowUp(null) }}>Next →</button>
                        : <button className="btn btn-success" onClick={handleComplete}>🏁 Complete Interview</button>
                    }
                </div>

                {/* Question Map */}
                <div className="flex gap-2 mt-6" style={{ flexWrap: 'wrap' }}>
                    {filteredQuestions.map((q2, i) => (
                        <button key={i} onClick={() => { setCurrent(i); setFollowUp(null) }}
                            style={{
                                width: 36, height: 36, borderRadius: 8, border: `2px solid`,
                                borderColor: i === current ? 'var(--accent)' : tempAnswers[q2.id] ? 'var(--green)' : 'var(--border)',
                                background: i === current ? 'rgba(99,102,241,0.2)' : tempAnswers[q2.id] ? 'rgba(16,185,129,0.1)' : 'transparent',
                                color: 'var(--text-primary)', fontWeight: 600, cursor: 'pointer', fontSize: '0.85rem'
                            }}>{i + 1}</button>
                    ))}
                </div>
            </div>
        </>
    )
}
