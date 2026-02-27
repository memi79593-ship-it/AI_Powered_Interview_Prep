import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { adminUsers, adminAnalytics } from '../services/api'
import Navbar from '../components/Navbar'
import toast from 'react-hot-toast'

export default function Admin() {
    const nav = useNavigate()
    const role = localStorage.getItem('role')
    const [users, setUsers] = useState([])
    const [analytics, setAnalytics] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        if (role !== 'ADMIN') { nav('/dashboard'); return }
        Promise.all([adminUsers(), adminAnalytics()])
            .then(([u, a]) => { setUsers(u.data || []); setAnalytics(a.data) })
            .catch(() => toast.error('Admin access denied or server error'))
            .finally(() => setLoading(false))
    }, [])

    if (loading) return <><Navbar /><div className="page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}><div className="spinner" /></div></>

    const platformStats = analytics ? [
        { label: 'Total Users', value: analytics.totalUsers },
        { label: 'Total Sessions', value: analytics.totalSessions },
        { label: 'Completed', value: analytics.completedSessions },
        { label: 'Avg Score', value: `${analytics.averageScore}%` },
        { label: 'Completion Rate', value: `${analytics.completionRate}%` },
        { label: 'Top Role', value: analytics.mostPopularRole },
    ] : []

    return (
        <>
            <Navbar />
            <div className="page fade-up">
                <div className="mb-8">
                    <h1>⚙️ Admin Dashboard</h1>
                    <p className="mt-4">Platform management and analytics</p>
                </div>

                {/* Platform Stats */}
                <div className="grid-3 mb-8">
                    {platformStats.map(s => (
                        <div key={s.label} className="card">
                            <div className="stat">
                                <div className="stat-value">{s.value}</div>
                                <div className="stat-label">{s.label}</div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Users Table */}
                <div className="card">
                    <div className="section-header mb-6">
                        <span className="section-title">👥 Registered Users</span>
                        <span className="badge badge-accent">{users.length} total</span>
                    </div>
                    <table className="table">
                        <thead>
                            <tr><th>Email</th><th>Role</th><th>Sessions</th><th>Status</th></tr>
                        </thead>
                        <tbody>
                            {users.map(u => (
                                <tr key={u.email}>
                                    <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{u.email}</td>
                                    <td>
                                        <span className={`badge ${u.role === 'ADMIN' ? 'badge-accent' : 'badge-medium'}`}>
                                            {u.role === 'ADMIN' ? '👑 ADMIN' : 'USER'}
                                        </span>
                                    </td>
                                    <td>{u.totalSessions}</td>
                                    <td>
                                        <span className={`badge ${u.active ? 'badge-high' : 'badge-low'}`}>
                                            {u.active ? '🟢 Active' : '🔴 Inactive'}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    )
}
