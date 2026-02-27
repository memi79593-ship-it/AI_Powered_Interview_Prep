import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import StartInterview from './pages/StartInterview.jsx'
import Interview from './pages/Interview.jsx'
import SessionReview from './pages/SessionReview.jsx'
import Leaderboard from './pages/Leaderboard.jsx'
import Admin from './pages/Admin.jsx'

const isLoggedIn = () => !!localStorage.getItem('token')

function Protected({ children }) {
    return isLoggedIn() ? children : <Navigate to="/" replace />
}

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<Login />} />
            <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
            <Route path="/start" element={<Protected><StartInterview /></Protected>} />
            <Route path="/interview/:id" element={<Protected><Interview /></Protected>} />
            <Route path="/review/:id" element={<Protected><SessionReview /></Protected>} />
            <Route path="/leaderboard" element={<Protected><Leaderboard /></Protected>} />
            <Route path="/admin" element={<Protected><Admin /></Protected>} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}
