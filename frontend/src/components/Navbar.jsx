import React from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

export default function Navbar() {
    const loc = useLocation()
    const nav = useNavigate()
    const email = localStorage.getItem('email')
    const role = localStorage.getItem('role')

    const handleLogout = () => {
        localStorage.clear()
        nav('/')
    }

    const links = [
        { to: '/dashboard', label: '📊 Dashboard' },
        { to: '/start', label: '🚀 Start' },
        { to: '/leaderboard', label: '🏆 Leaderboard' },
        ...(role === 'ADMIN' ? [{ to: '/admin', label: '⚙️ Admin' }] : []),
    ]

    return (
        <nav className="navbar">
            <Link to="/dashboard" className="nav-logo">⚡ AI Interview</Link>
            <div className="nav-links">
                {links.map(l => (
                    <Link key={l.to} to={l.to}
                        className={`nav-link ${loc.pathname === l.to ? 'active' : ''}`}>
                        {l.label}
                    </Link>
                ))}
                {email && <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginLeft: 8 }}>{email}</span>}
                <button className="btn btn-secondary btn-sm" onClick={handleLogout}>Logout</button>
            </div>
        </nav>
    )
}
