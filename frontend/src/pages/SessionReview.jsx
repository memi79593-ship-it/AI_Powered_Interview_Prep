import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getQuestions, getAnswers, getSession, generateModelAnswer } from '../services/api'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'

export default function SessionReview() {
    const { id } = useParams()
    const nav = useNavigate()
    const [qs, setQs] = useState([])
    const [answers, setAnswers] = useState([])
    const [session, setSession] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        // Fetch data individually to identify which call fails
        console.log('Loading session review for ID:', id);
        
        Promise.all([
            getQuestions(id).catch(err => {
                console.error('Error fetching questions for session ID:', id, err);
                throw err;
            }),
            getAnswers(id).catch(err => {
                console.error('Error fetching answers for session ID:', id, err);
                throw err;
            }),
            getSession(id).catch(err => {
                console.error('Error fetching session for session ID:', id, err);
                throw err;
            })
        ])
            .then(([questionsRes, answersRes, sessionRes]) => {
                console.log('Fetched data - questions:', questionsRes.data?.length, 'answers:', answersRes.data?.length, 'session ID:', sessionRes.data?.id);
                setQs(questionsRes.data || [])
                setAnswers(answersRes.data || [])
                setSession(sessionRes.data)
            })
            .catch(error => {
                console.error('Error loading session review:', error);
                console.error('Session ID:', id);
                toast.error(`Failed to load session: ${error.message || 'Network error'}`);
            })
            .finally(() => setLoading(false))
    }, [id])

    if (loading) return <><Navbar /><div className="page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}><div className="spinner" /></div></>

    return (
        <>
            <Navbar />
            <div className="page fade-up" style={{ maxWidth: 820 }}>
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1>📋 Session Review</h1>
                        <p className="mt-4">Session #{id} — {qs.length} questions</p>
                        {session && (
                            <div className="score-summary" style={{ marginTop: '1rem', padding: '1rem', backgroundColor: 'var(--bg-card)', borderRadius: '8px', textAlign: 'center' }}>
                                {/* Calculate max possible score based on question types */}
                                {(() => {
                                    const maxPossibleScore = qs.reduce((maxScore, q) => {
                                        if (q.type === 'mcq') {
                                            return maxScore + 1; // MCQ: max 1 point
                                        } else {
                                            return maxScore + 10; // Subjective: max 10 points
                                        }
                                    }, 0);
                                    
                                    // Calculate percentage, ensuring it doesn't exceed 100%
                                    const percentage = maxPossibleScore > 0 ? Math.min(100, Math.round((session.score / maxPossibleScore) * 100)) : 0;
                                    
                                    return (
                                        <div>
                                            <h2 style={{ margin: 0, color: session.score >= maxPossibleScore * 0.7 ? 'var(--green)' : session.score >= maxPossibleScore * 0.5 ? 'var(--orange)' : 'var(--red)' }}>
                                                Final Score: <strong>{session.score}/{maxPossibleScore}</strong>
                                            </h2>
                                            <p style={{ margin: '0.5rem 0 0', color: 'var(--text-muted)' }}>
                                                Percentage: {percentage}%
                                            </p>
                                        </div>
                                    );
                                })()}
                            </div>
                        )}
                    </div>
                    <button className="btn btn-secondary" onClick={() => nav('/dashboard')}>← Dashboard</button>
                </div>

                {qs.length === 0
                    ? (
                        <div className="card text-center">
                            <p>No questions found for this session.</p>
                            {session && (
                                <div style={{ marginTop: '1rem' }}>
                                    <p>Session Status: <strong>{session.status}</strong></p>
                                    {/* Calculate max possible score based on question types */}
                                    {(() => {
                                        const maxPossibleScore = qs.reduce((maxScore, q) => {
                                            if (q.type === 'mcq') {
                                                return maxScore + 1; // MCQ: max 1 point
                                            } else {
                                                return maxScore + 10; // Subjective: max 10 points
                                            }
                                        }, 0);
                                        
                                        // Calculate percentage, ensuring it doesn't exceed 100%
                                        const percentage = maxPossibleScore > 0 ? Math.min(100, Math.round((session.score / maxPossibleScore) * 100)) : 0;
                                        
                                        return (
                                            <div>
                                                <p>Final Score: <strong>{session.score}/{maxPossibleScore}</strong></p>
                                                <p>Percentage: <strong>{percentage}%</strong></p>
                                            </div>
                                        );
                                    })()}
                                </div>
                            )}
                        </div>
                      )
                    : qs.map((q, i) => {
                        const answer = answers.find(a => a.questionId === q.id);
                        return (
                            <div key={q.id} className="card mb-6" style={{ position: 'relative' }}>
                                <div className="flex justify-between items-center mb-4">
                                    <span style={{ fontWeight: 800, color: 'var(--text-muted)', fontSize: '0.85rem' }}>Q{i + 1}</span>
                                    <div className="flex gap-2">
                                        {q.type && <span className="chip">{q.type}</span>}
                                        {q.topic && <span className="chip" style={{ background: 'rgba(139,92,246,0.15)', color: 'var(--violet)' }}>{q.topic}</span>}
                                        {answer && (
                                            <span className="chip" style={{
                                                background: answer.score >= 7 ? 'rgba(16,185,129,0.15)' : 
                                                         answer.score >= 4 ? 'rgba(245,158,11,0.15)' : 'rgba(239,68,68,0.15)',
                                                color: answer.score >= 7 ? 'var(--green)' : 
                                                       answer.score >= 4 ? 'var(--orange)' : 'var(--red)'
                                            }}>
                                                Score: {answer.score}{q.type === 'mcq' ? '/1' : '/10'}
                                            </span>
                                        )}
                                    </div>
                                </div>

                                <p style={{ fontSize: '1rem', lineHeight: 1.7, color: 'var(--text-primary)', marginBottom: 16, fontWeight: 500 }}>
                                    {q.questionText}
                                </p>

                                {answer && (
                                    <div className="mb-4">
                                        <h4 style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: 8 }}>Your Answer:</h4>
                                        <div className="alert" style={{ marginBottom: 10, whiteSpace: 'pre-wrap' }}>
                                            {answer.userAnswer}
                                        </div>
                                        
                                        {answer.aiFeedback && (
                                            <div>
                                                <h4 style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: 8 }}>AI Feedback:</h4>
                                                <div className="alert alert-info" style={{ whiteSpace: 'pre-wrap' }}>
                                                    {answer.aiFeedback}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )}

                                {(() => {
                                    const isPlaceholder = q.correctAnswer && q.correctAnswer.trim().startsWith('Model answer not available')
                                    if (q.correctAnswer && q.correctAnswer.trim() !== '' && !isPlaceholder) {
                                        return (
                                            <div className="alert alert-success" style={{ marginBottom: 10 }}>
                                                <strong>Correct Answer:</strong> {q.correctAnswer}
                                                {q.explanation && (
                                                    <div style={{ marginTop: '0.5rem', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                                                        <strong>Explanation:</strong> {q.explanation}
                                                    </div>
                                                )}
                                                {q.options && q.type === 'mcq' && (
                                                    <div style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                                                        <strong>Options:</strong> {q.options}
                                                    </div>
                                                )}
                                            </div>
                                        )
                                    }
                                    if (q.type === 'subjective') {
                                        return (
                                            <div className="alert" style={{ marginBottom: 10 }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                    <span>Correct Answer: Model answer not available.</span>
                                                    <button
                                                        className="btn btn-secondary"
                                                        onClick={async () => {
                                                            try {
                                                                toast.loading('Generating model answer...', { id: `gen-${q.id}` })
                                                                const res = await generateModelAnswer(id, q.id)
                                                                const updated = res.data
                                                                toast.success('Model answer generated', { id: `gen-${q.id}` })
                                                                setQs(prev => prev.map(p => p.id === q.id ? { ...p, correctAnswer: updated.correctAnswer } : p))
                                                            } catch (e) {
                                                                toast.error(e?.response?.data?.error || 'Failed to generate model answer', { id: `gen-${q.id}` })
                                                            }
                                                        }}
                                                    >
                                                        Generate with Ollama
                                                    </button>
                                                </div>
                                            </div>
                                        )
                                    }
                                    return null
                                })()}
                            </div>
                        );
                    })
                }
            </div>
        </>
    )
}
