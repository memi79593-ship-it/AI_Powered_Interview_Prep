import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getDashboard, getAllProfiles, downloadReport } from '../services/api'
import { RadarChart, PolarGrid, PolarAngleAxis, Radar, ResponsiveContainer } from 'recharts'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'

const ConfidenceBadge = ({ level }) => {
    const map = { HIGH: ['badge-high', '🟢'], MEDIUM: ['badge-medium', '🟡'], LOW: ['badge-low', '🔴'] }
    const [cls, icon] = map[level] || map.LOW
    return <span className={`badge ${cls}`}>{icon} {level}</span>
}

export default function Dashboard() {
    const email = localStorage.getItem('email')
    const nav = useNavigate()
    const [dash, setDash] = useState(null)
    const [profiles, setProfiles] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const load = async () => {
            try {
                let e = email
                if (!e || !e.trim()) {
                    const v = await (await import('../services/api')).validate()
                    if (v?.data?.valid && v.data.email) {
                        e = v.data.email
                        localStorage.setItem('email', e)
                    }
                }
                const [d, p] = await Promise.all([getDashboard(e), getAllProfiles(e)])
                setDash(d.data)
                setProfiles(p.data || [])
            } catch (err) {
                toast.error(err?.response?.data?.error || 'Failed to load dashboard')
            } finally {
                setLoading(false)
            }
        }
        load()
    }, [email])

    const handleDownloadPDF = async () => {
        try {
            const res = await downloadReport(email)
            const url = URL.createObjectURL(res.data)
            const link = document.createElement('a')
            link.href = url; link.download = `report-${email}.pdf`; link.click()
            URL.revokeObjectURL(url)
            toast.success('PDF report downloaded!')
        } catch { toast.error('PDF generation failed. Complete a session first.') }
    }

    if (loading) return (
        <><Navbar /><div className="page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}><div className="spinner" /></div></>
    )

    const stats = [
        { value: dash?.totalSessions ?? 0, label: 'Total Sessions' },
        { value: (dash?.averageScore ?? 0).toFixed(1), label: 'Avg Score' },
        { value: dash?.highestScore ?? 0, label: 'Best Score' },
        { value: dash?.recentSessions?.length ?? 0, label: 'Completed' },
    ]

    // For radar chart – role-wise averages
    const radarData = Object.entries(dash?.roleWiseAverage || {}).map(([role, avg]) => ({
        role: role.replace(' Developer', '').replace(' Engineer', ''), score: Math.round(avg)
    }))

    return (
        <>
            <Navbar />
            <div className="page fade-up">

                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1>Welcome back 👋</h1>
                        <p style={{ marginTop: 4 }}>{email}</p>
                    </div>
                    <div className="flex gap-4">
                        <button className="btn btn-secondary" onClick={handleDownloadPDF}>📄 PDF Report</button>
                        <button className="btn btn-primary" onClick={() => nav('/start')}>🚀 New Interview</button>
                    </div>
                </div>

                {/* Stats */}
                <div className="grid-4 mb-6">
                    {stats.map(s => (
                        <div key={s.label} className="card">
                            <div className="stat">
                                <div className="stat-value">{s.value}</div>
                                <div className="stat-label">{s.label}</div>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="grid-2 gap-6 mb-6">
                    {/* Skill Profiles */}
                    <div className="card">
                        <div className="section-header">
                            <span className="section-title">🎯 Skill Profiles</span>
                            <button className="btn btn-secondary btn-sm" onClick={() => nav('/start')}>Practice</button>
                        </div>
                        {profiles.length === 0
                            ? <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Complete an interview to build your profile.</p>
                            : profiles.map(p => (
                                <div key={p.id} className="card card-sm mb-4" style={{ marginTop: 0 }}>
                                    <div className="flex justify-between items-center mb-4">
                                        <strong style={{ fontSize: '1rem' }}>{p.role}</strong>
                                        <ConfidenceBadge level={p.confidenceLevel} />
                                    </div>
                                    <div className="progress mb-4">
                                        <div className="progress-fill" style={{ width: `${p.avgScore}%` }} />
                                    </div>
                                    <div className="flex justify-between" style={{ fontSize: '0.83rem', color: 'var(--text-muted)' }}>
                                        <span>Avg: <strong style={{ color: 'var(--text-primary)' }}>{p.avgScore}%</strong></span>
                                        <span>Sessions: <strong style={{ color: 'var(--text-primary)' }}>{p.totalSessions}</strong></span>
                                    </div>
                                    {p.weakTopics && (
                                        <div className="mt-4" style={{ fontSize: '0.82rem' }}>
                                            <span style={{ color: 'var(--red)' }}>⚠ Weak: </span>
                                            {p.weakTopics.split(',').map(t => <span key={t} className="chip" style={{ marginRight: 4, fontSize: '0.75rem' }}>{t}</span>)}
                                        </div>
                                    )}
                                </div>
                            ))
                        }
                    </div>

                    {/* Role-wise Radar */}
                    <div className="card">
                        <div className="section-header">
                            <span className="section-title">📊 Role Performance</span>
                        </div>
                        {radarData.length > 0 ? (
                            <ResponsiveContainer width="100%" height={240}>
                                <RadarChart data={radarData}>
                                    <PolarGrid stroke="rgba(255,255,255,0.06)" />
                                    <PolarAngleAxis dataKey="role" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                                    <Radar name="Score" dataKey="score" stroke="#6366f1" fill="#6366f1" fillOpacity={0.2} />
                                </RadarChart>
                            </ResponsiveContainer>
                        ) : <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Complete interviews in multiple roles to see your radar.</p>}
                    </div>
                </div>

                {/* Weak Areas + Recent Sessions */}
                <div className="grid-2 gap-6">
                    <div className="card">
                        <div className="section-title mb-4">⚠️ Weak Areas</div>
                        {dash?.weakAreas?.length ? (
                            <div className="flex gap-2" style={{ flexWrap: 'wrap' }}>
                                {dash.weakAreas.map(w => <span key={w} className="badge badge-low">{w}</span>)}
                            </div>
                        ) : <span className="badge badge-high">✨ No weak areas!</span>}

                        {dash?.bestRole && (
                            <div className="mt-6">
                                <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: 6 }}>BEST ROLE</div>
                                <div className="badge badge-high">🏆 {dash.bestRole}</div>
                            </div>
                        )}
                    </div>

                    <div className="card">
                        <div className="section-title mb-4">📋 Recent Sessions</div>
                        {dash?.recentSessions?.length ? (
                            <table className="table">
                                <thead><tr><th>Role</th><th>Type</th><th>Score</th><th></th></tr></thead>
                                <tbody>
                                    {dash.recentSessions.map(s => (
                                        <tr key={s.sessionId}>
                                            <td>{s.role}</td>
                                            <td><span className="chip" style={{ fontSize: '0.75rem' }}>{s.type}</span></td>
                                            <td><strong>{s.score}</strong></td>
                                            <td>
                                                <button className="btn btn-secondary btn-sm"
                                                    onClick={() => nav(`/review/${s.sessionId}`)}>Review</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No sessions yet.</p>}
                    </div>
                </div>
            </div>
        </>
    )
}
