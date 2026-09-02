import React, { useEffect, useState } from 'react'
import { Routes, Route, Link, Navigate, useNavigate } from 'react-router-dom'
import Login from './components/Login'
import Register from './components/Register'
import Dashboard from './components/Dashboard'
import TransferForm from './components/TransferForm'

function Toast({ message, type = 'success' }) {
  if (!message) return null

  const styles = {
    success: 'border-emerald-500/50 bg-emerald-500/10 text-emerald-100',
    error: 'border-rose-500/50 bg-rose-500/10 text-rose-100',
    info: 'border-sky-500/50 bg-sky-500/10 text-sky-100'
  }

  return (
    <div className={`absolute right-6 top-6 z-50 max-w-sm rounded-xl border px-4 py-3 shadow-2xl backdrop-blur ${styles[type]}`}>
      {message}
    </div>
  )
}

export default function App(){
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [toast, setToast] = useState({ message: '', type: 'success' })
  const navigate = useNavigate()

  useEffect(() => {
    const onStorage = () => setToken(localStorage.getItem('token'))
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  useEffect(() => {
    if (!toast.message) return
    const timer = setTimeout(() => setToast({ message: '', type: 'success' }), 2200)
    return () => clearTimeout(timer)
  }, [toast])

  const logout = () => {
    localStorage.removeItem('token')
    setToken(null)
    setToast({ message: 'Logged out successfully', type: 'info' })
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-50">
      <Toast message={toast.message} type={toast.type} />

      <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
        <nav className="mb-8 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 shadow-xl shadow-slate-950/30 backdrop-blur-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-violet-600 font-bold text-white">P</div>
              <div>
                <p className="text-lg font-bold tracking-tight">PayRoute</p>
                <p className="text-xs text-slate-300">Microservice wallet platform</p>
              </div>
            </div>

            <div className="flex items-center gap-2 text-sm font-medium">
              <Link className="rounded-lg px-3 py-2 text-slate-200 transition hover:bg-white/5 hover:text-white" to="/">Home</Link>
              {token ? (
                <>
                  <Link className="rounded-lg px-3 py-2 text-slate-200 transition hover:bg-white/5 hover:text-white" to="/wallets">Wallets</Link>
                  <Link className="rounded-lg px-3 py-2 text-slate-200 transition hover:bg-white/5 hover:text-white" to="/transfer">Transfer</Link>
                  <button className="rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-slate-200 transition hover:bg-rose-500/20 hover:text-white" onClick={logout}>Logout</button>
                </>
              ) : (
                <>
                  <Link className="rounded-lg px-3 py-2 text-slate-200 transition hover:bg-white/5 hover:text-white" to="/login">Login</Link>
                  <Link className="rounded-lg px-3 py-2 text-slate-200 transition hover:bg-white/5 hover:text-white" to="/register">Register</Link>
                </>
              )}
            </div>
          </div>
        </nav>

        <Routes>
          <Route path="/" element={<HomeScreen />} />
          <Route path="/login" element={<Login setToken={setToken} setToast={setToast} />} />
          <Route path="/register" element={<Register setToast={setToast} />} />
          <Route path="/wallets" element={token ? <Dashboard setToast={setToast} /> : <Navigate to="/login" />} />
          <Route path="/transfer" element={token ? <TransferForm setToast={setToast} /> : <Navigate to="/login" />} />
        </Routes>
      </div>
    </div>
  )
}

function HomeScreen() {
  const features = [
    'JWT auth with auth-service',
    'Wallet management microservice',
    'Transfer and ledger processing',
    'Risk check and validation',
    'Responsive dashboard UI'
  ]

  return (
    <div className="grid gap-6 lg:grid-cols-[1.4fr_0.6fr]">
      <div className="rounded-3xl border border-cyan-400/20 bg-gradient-to-br from-slate-900 via-slate-900 to-cyan-950/60 p-8 shadow-2xl shadow-cyan-950/40">
        <p className="mb-3 inline-flex rounded-full border border-cyan-400/30 bg-cyan-400/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-cyan-200">PayRoute</p>
        <h1 className="mb-4 text-4xl font-black tracking-tight text-white">Secure digital wallet for modern payments</h1>
        <p className="max-w-lg text-base text-slate-300">
          Manage wallets, move funds, and validate transaction risk through a clean microservice-based architecture.
        </p>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link to="/register" className="rounded-xl bg-cyan-500 px-5 py-3 font-semibold text-slate-950 transition hover:bg-cyan-400">Create account</Link>
          <Link to="/login" className="rounded-xl border border-white/10 bg-white/5 px-5 py-3 font-semibold text-white transition hover:bg-white/10">Login</Link>
        </div>
      </div>

      <div className="rounded-3xl border border-violet-500/20 bg-slate-900/70 p-6 shadow-xl shadow-violet-950/30">
        <h2 className="mb-4 text-xl font-bold text-violet-100">Platform features</h2>
        <ul className="space-y-3 text-sm text-slate-300">
          {features.map((feature) => (
            <li key={feature} className="flex items-center gap-3 rounded-xl border border-white/5 bg-white/5 px-3 py-2">
              <span className="inline-flex h-2.5 w-2.5 rounded-full bg-emerald-400" />
              {feature}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

