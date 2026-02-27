import axios from 'axios'

const BASE = '/api'

const api = axios.create({ baseURL: BASE })

// Auto-attach JWT to every request
api.interceptors.request.use(cfg => {
    const token = localStorage.getItem('token')
    if (token) cfg.headers.Authorization = `Bearer ${token}`
    return cfg
})

// Auth
export const login = d => api.post('/auth/login', d)
export const register = d => api.post('/auth/register', d)
export const validate = () => api.get('/auth/validate')

// Sessions
export const startSession = d => api.post('/session/start', d)
export const getQuestions = id => api.get(`/session/${id}/questions`)
export const submitAnswer = d => api.post('/session/submit-answer', d)
export const completeSession = id => api.post(`/session/complete/${id}`)
export const getSession = id => api.get(`/session/${id}`)
export const getAnswers = id => api.get(`/session/${id}/answers`)
export const getFollowUp = (id, d) => api.post(`/session/${id}/followup`, d)
export const generateModelAnswer = (sid, qid) => api.post(`/session/${sid}/questions/${qid}/model-answer`)

// Code execution removed: coding functionality no longer supported

// Dashboard
export const getDashboard = email => api.get(`/dashboard/${email}`)
export const getRecommend = (email, role) => api.get(`/dashboard/${email}/recommend/${role}`)
export const getWeakTopics = (email, sid) => api.get(`/dashboard/${email}/wtopics/${sid}`)
export const getSummary = (email, d) => api.post(`/dashboard/${email}/summary`, d)

// Skill Profile
export const getProfile = (email, role) => api.get(`/profile/${email}/${role}`)
export const getAllProfiles = email => api.get(`/profile/${email}`)

// Leaderboard
export const getLeaderboard = () => api.get('/leaderboard')
export const getLeaderboardRole = r => api.get(`/leaderboard/${encodeURIComponent(r)}`)

// Report
export const downloadReport = email =>
    api.get(`/report/${email}`, { responseType: 'blob' })

// Admin
export const adminUsers = () => api.get('/admin/users')
export const adminAnalytics = () => api.get('/admin/analytics')

export default api
