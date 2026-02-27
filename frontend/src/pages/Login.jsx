import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login, register } from '../services/api'
import toast from 'react-hot-toast'

export default function Login() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [mode, setMode] = useState('login')  // 'login' | 'register'
    const [loading, setLoading] = useState(false)
    const nav = useNavigate()

    const handleSubmit = async e => {
        e.preventDefault()
        
        if (!email.trim()) return toast.error('Email is required')
        
        if (mode === 'register') {
            // Password validation
            if (!password) return toast.error('Password is required')
            if (password !== confirmPassword) return toast.error('Passwords do not match')
            
            // Strong password validation
            const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
            if (!passwordRegex.test(password)) {
                return toast.error('Password must be at least 8 characters with uppercase, lowercase, number, and special character')
            }
        }
        
        setLoading(true)
        try {
            const fn = mode === 'login' ? login : register
            const res = await fn({ email: email.trim(), password: password })
            const { token, role } = res.data
            localStorage.setItem('token', token)
            localStorage.setItem('email', email.trim())
            localStorage.setItem('role', role || 'USER')
            toast.success(mode === 'login' ? 'Welcome back! 🚀' : 'Account created! 🎉')
            nav('/dashboard')
        } catch (err) {
            toast.error(err.response?.data?.error || 'Request failed')
        } finally { setLoading(false) }
    }

    return (
        <div className="page-center">
            <div style={{ width: '100%', maxWidth: 420 }}>
                {/* Logo */}
                <div className="text-center mb-8 fade-up">
                    <div className="float" style={{ fontSize: '3.5rem', marginBottom: 12 }}>⚡</div>
                    <h1 className="gradient-text" style={{ fontSize: '2.4rem' }}>AI Interview</h1>
                    <p className="mt-4" style={{ fontSize: '1rem' }}>
                        Your adaptive AI-powered interview coach
                    </p>
                </div>

                {/* Card */}
                <div className="card fade-up" style={{ animationDelay: '0.1s' }}>
                    {/* Toggle */}
                    <div className="flex gap-2 mb-6" style={{ background: 'var(--bg-surface)', borderRadius: 10, padding: 4 }}>
                        {['login', 'register'].map(m => (
                            <button key={m} onClick={() => setMode(m)}
                                style={{
                                    flex: 1, padding: '10px', border: 'none', borderRadius: 8,
                                    background: mode === m ? 'var(--accent)' : 'transparent',
                                    color: mode === m ? 'white' : 'var(--text-secondary)',
                                    fontWeight: 600, cursor: 'pointer', transition: 'var(--transition)',
                                    fontFamily: 'var(--font)', fontSize: '0.9rem'
                                }}>
                                {m === 'login' ? '🔑 Login' : '✨ Register'}
                            </button>
                        ))}
                    </div>

                    <form onSubmit={handleSubmit} className="flex-col gap-4" style={{ display: 'flex' }}>
                        <div>
                            <label htmlFor="email">Email Address</label>
                            <input id="email" type="email" className="input"
                                placeholder="you@example.com"
                                value={email} onChange={e => setEmail(e.target.value)} />
                        </div>
                        
                        {(mode === 'login' || mode === 'register') && (
                            <div>
                                <label htmlFor="password">Password</label>
                                <input id="password" type="password" className="input"
                                    placeholder="Enter password"
                                    value={password} onChange={e => setPassword(e.target.value)} />
                            </div>
                        )}
                        
                        {mode === 'register' && (
                            <div>
                                <label htmlFor="confirmPassword">Confirm Password</label>
                                <input id="confirmPassword" type="password" className="input"
                                    placeholder="Confirm password"
                                    value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} />
                            </div>
                        )}
                        
                        <button type="submit" className="btn btn-primary btn-lg w-full" disabled={loading}>
                            {loading ? '⏳ Please wait...' : mode === 'login' ? '🚀 Enter Platform' : '🎯 Create Account'}
                        </button>
                    </form>

                    {mode === 'register' && (
                        <div className="mt-4">
                            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                                First registered user gets <span style={{ color: 'var(--violet)' }}>Admin</span> access
                            </p>
                            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                                Password must be at least 8 characters with uppercase, lowercase, number, and special character
                            </p>
                        </div>
                    )}
                </div>

                {/* Features */}
                <div className="grid-3 mt-6 fade-up" style={{ animationDelay: '0.2s', gap: 12 }}>
                    {[
                        ['🧠', 'Adaptive AI', 'Difficulty adjusts to you'],
                        ['🎯', 'Weak Topics', 'AI-targeted practice'],
                        ['📊', 'Analytics', 'Real-time performance'],
                    ].map(([icon, title, desc]) => (
                        <div key={title} className="card card-sm text-center" style={{ padding: '16px 12px' }}>
                            <div style={{ fontSize: '1.6rem', marginBottom: 4 }}>{icon}</div>
                            <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>{title}</div>
                            <div style={{ fontSize: '0.77rem', color: 'var(--text-muted)', marginTop: 4 }}>{desc}</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}
