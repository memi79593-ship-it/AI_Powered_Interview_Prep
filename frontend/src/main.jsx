import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
    <BrowserRouter>
        <App />
        <Toaster
            position="top-right"
            toastOptions={{
                style: {
                    background: 'rgba(30,30,50,0.95)',
                    color: '#e2e8f0',
                    border: '1px solid rgba(99,102,241,0.3)',
                    borderRadius: '12px',
                    backdropFilter: 'blur(20px)',
                },
            }}
        />
    </BrowserRouter>
)
