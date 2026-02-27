import React, { useEffect, useState } from 'react'
import { getLeaderboard, getLeaderboardRole } from '../services/api'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'

const ROLES = [
    '', 'Java Developer', 'Python Developer', 'Data Analyst',
    'Frontend Developer', 'Backend Developer', 'DevOps Engineer',
]

export default function Leaderboard() {
    const [data, setData] = useState([])
    const [role, setRole] = useState('')
    const [loading, setLoading] = useState(true)

    const loadData = async (r) => {
        setLoading(true)
        try {
            const res = r ? await getLeaderboardRole(r) : await getLeaderboard()
            setData(res.data || [])
        } catch { toast.error('Failed to load leaderboard') }
        finally { setLoading(false) }
    }

    useEffect(() => { loadData(role) }, [role])

    const medals = ['🥇', '🥈', '🥉']

    return (
        <>
            <Navbar />
            <div className="page fade-up" style={{ maxWidth: 800 }}>
                <div className="mb-8">
                    <h1>🏆 Leaderboard</h1>
                    <p className="mt-4">Top performers across all roles</p>
                </div>

                {/* Role filter */}
                <div className="card mb-6">
                    <div className="pill-group">
                        {ROLES.map(r => (
                            <button key={r || 'all'} className={`pill ${role === r ? 'selected' : ''}`}
                                onClick={() => setRole(r)}>
                                {r || '🌍 All Roles'}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Table */}
                <div className="card">
                    {loading ? <div className="spinner" style={{ margin: '40px auto' }} /> :
                        data.length === 0
                            ? <p className="text-center" style={{ color: 'var(--text-muted)', padding: '40px 0' }}>No data yet. Complete interviews to appear!</p>
                            : (
                                <table className="table">
                                    <thead>
                                        <tr>
                                            <th>Rank</th><th>Candidate</th><th>Avg Score</th><th>Sessions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {data.map((u, i) => (
                                            <tr key={u.email} style={{ opacity: i < 3 ? 1 : 0.85 }}>
                                                <td style={{ fontSize: '1.4rem', width: 60 }}>{medals[i] || `#${i + 1}`}</td>
                                                <td>
                                                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{u.email}</div>
                                                    {u.role && <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: 2 }}>{u.role}</div>}
                                                </td>
                                                <td>
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                                                        <div className="progress" style={{ width: 80 }}>
                                                            <div className="progress-fill" style={{ width: `${u.avgScore}%` }} />
                                                        </div>
                                                        <strong style={{ color: u.avgScore >= 70 ? 'var(--green)' : u.avgScore >= 50 ? 'var(--amber)' : 'var(--red)' }}>
                                                            {u.avgScore}%
                                                        </strong>
                                                    </div>
                                                </td>
                                                <td>{u.totalSessions}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )
                    }
                </div>
            </div>
        </>
    )
}
