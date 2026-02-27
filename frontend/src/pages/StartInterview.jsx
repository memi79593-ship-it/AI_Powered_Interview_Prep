import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { startSession } from '../services/api'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'

const ROLES = [
    'Java Developer', 'Python Developer', 'C Programmer', 'C++ Programmer',
    'Frontend Developer', 'Backend Developer', 'Mobile Developer',
    'Data Analyst', 'DevOps Engineer', 'Database Administrator', 'Cloud Developer',
]
const TYPES = ['subjective', 'mcq', 'full']
const DIFFICULTIES = ['', 'easy', 'medium', 'hard']
const QUESTION_COUNTS = [3, 5, 10, 15, 20]
const PERSONALITIES = ['FRIENDLY', 'STRICT', 'TECHNICAL']

// Coding roles removed as coding questions are no longer supported

export default function StartInterview() {
    const email = localStorage.getItem('email')
    const nav = useNavigate()

    const [role, setRole] = useState('')
    const [type, setType] = useState('subjective')
    const [difficulty, setDifficulty] = useState('')
    const [questionCount, setQuestionCount] = useState(5)
    const [personality, setPersonality] = useState('FRIENDLY')
    const [mockMode, setMockMode] = useState(false)
    const [loading, setLoading] = useState(false)

    const availableTypes = TYPES

    const handleStart = async () => {
        if (!role) return toast.error('Please select a role')
        setLoading(true)
        try {
            const res = await startSession({
                userEmail: email,
                role,
                type,
                questionCount,
                difficulty: difficulty || null,   // null = adaptive
                personality,
                mockMode,
            })
            toast.success('Session started! 🎯')
            nav(`/interview/${res.data.id}`)
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to start session')
        } finally { setLoading(false) }
    }

    const personalityIcons = { FRIENDLY: '😊', STRICT: '😤', TECHNICAL: '🔬' }
    const personalityDescs = {
        FRIENDLY: 'Supportive and encouraging mentor',
        STRICT: 'Demanding senior interviewer',
        TECHNICAL: 'Deep technical expert with follow-ups',
    }

    return (
        <>
            <Navbar />
            <div className="page fade-up" style={{ maxWidth: 800 }}>
                <div className="mb-8">
                    <h1>🚀 Start Interview</h1>
                    <p className="mt-4">Configure your session — AI adapts difficulty automatically based on your history.</p>
                </div>

                {/* Role Selection */}
                <div className="card mb-6">
                    <h3 className="mb-4">Select Role</h3>
                    <div className="pill-group">
                        {ROLES.map(r => (
                            <button key={r} className={`pill ${role === r ? 'selected' : ''}`}
                                onClick={() => setRole(r)}>
                                {r}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="grid-2 gap-6 mb-6">
                    {/* Interview Type */}
                    <div className="card">
                        <h3 className="mb-4">Interview Type</h3>
                        <div className="flex-col gap-2" style={{ display: 'flex' }}>
                            {availableTypes.map(t => {
                                const icons = { subjective: '✍️', mcq: '🔘', full: '🎯' }
                                const descs = { subjective: 'Open-ended AI-evaluated', mcq: 'Multiple choice auto-scored', full: 'All types combined' }
                                return (
                                    <button key={t} onClick={() => setType(t)}
                                        className={`card card-sm ${type === t ? '' : ''}`}
                                        style={{
                                            textAlign: 'left', cursor: 'pointer', border: `1px solid ${type === t ? 'var(--accent)' : 'var(--border)'}`,
                                            background: type === t ? 'rgba(99,102,241,0.1)' : 'var(--bg-surface)',
                                            transition: 'var(--transition)'
                                        }}>
                                        <div style={{ fontWeight: 700 }}>{icons[t]} {t.charAt(0).toUpperCase() + t.slice(1)}</div>
                                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: 4 }}>{descs[t]}</div>
                                    </button>
                                )
                            })}
                        </div>
                    </div>

                    {/* Difficulty + Personality + Mode */}
                    <div className="flex-col gap-4" style={{ display: 'flex' }}>
                        <div className="card">
                            <h3 className="mb-4">Difficulty</h3>
                            <div className="pill-group">
                                {DIFFICULTIES.map(d => (
                                    <button key={d || 'auto'} className={`pill ${difficulty === d ? 'selected' : ''}`}
                                        onClick={() => setDifficulty(d)}>
                                        {d === '' ? '🤖 Auto (AI Adaptive)' : d.charAt(0).toUpperCase() + d.slice(1)}
                                    </button>
                                ))}
                            </div>
                            {difficulty === '' && <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: 10 }}>AI will set difficulty based on your history</p>}
                        </div>

                        <div className="card">
                            <h3 className="mb-4">Number of Questions</h3>
                            <div className="pill-group">
                                {QUESTION_COUNTS.map(count => (
                                    <button key={count} className={`pill ${questionCount === count ? 'selected' : ''}`}
                                        onClick={() => setQuestionCount(count)}>
                                        {count} Questions
                                    </button>
                                ))}
                            </div>
                            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: 10 }}>Choose how many questions you want in this session</p>
                        </div>

                        <div className="card">
                            <h3 className="mb-4">Interviewer Style</h3>
                            <div className="flex-col gap-2" style={{ display: 'flex' }}>
                                {PERSONALITIES.map(p => (
                                    <button key={p} onClick={() => setPersonality(p)}
                                        style={{
                                            padding: '10px 14px', borderRadius: 10, cursor: 'pointer',
                                            border: `1px solid ${personality === p ? 'var(--accent)' : 'var(--border)'}`,
                                            background: personality === p ? 'rgba(99,102,241,0.1)' : 'transparent',
                                            textAlign: 'left', fontFamily: 'var(--font)', transition: 'var(--transition)'
                                        }}>
                                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{personalityIcons[p]} {p}</div>
                                        <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 2 }}>{personalityDescs[p]}</div>
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                            <input type="checkbox" id="mock" checked={mockMode} onChange={e => setMockMode(e.target.checked)}
                                style={{ width: 18, height: 18, accentColor: 'var(--accent)', cursor: 'pointer' }} />
                            <label htmlFor="mock" style={{ cursor: 'pointer', margin: 0, textTransform: 'none', letterSpacing: 0, fontSize: '0.95rem' }}>
                                <strong>🎬 Mock Mode</strong>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 400, marginTop: 2 }}>Timed, no going back — feels like the real thing</div>
                            </label>
                        </div>
                    </div>
                </div>

                <button className="btn btn-primary btn-lg w-full" onClick={handleStart} disabled={loading || !role}>
                    {loading ? '⏳ Starting...' : `🚀 Start ${role || 'Interview'}`}
                </button>
            </div>
        </>
    )
}
