import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'

export default function Login({ setToken, setToast }){
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const nav = useNavigate()

  async function submit(e){
    e.preventDefault()
    setError(null)

    try {
      const res = await api.login(email, password)
      localStorage.setItem('token', res.accessToken)
      setToken(res.accessToken)
      setToast({ message: 'Login successful', type: 'success' })
      nav('/wallets')
    } catch (err) {
      setError(err.message)
      setToast({ message: err.message, type: 'error' })
    }
  }

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <form className="w-full max-w-md rounded-3xl border border-white/10 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40" onSubmit={submit}>
        <div className="mb-6">
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-cyan-300">Welcome back</p>
          <h3 className="mt-2 text-3xl font-bold text-white">Login to PayRoute</h3>
        </div>

        {error && <div className="mb-4 rounded-xl border border-rose-500/50 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{error}</div>}

        <div className="space-y-4">
          <input className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none ring-0 placeholder:text-slate-400 focus:border-cyan-500" value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" type="email" />
          <input className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-cyan-500" value={password} onChange={e => setPassword(e.target.value)} placeholder="Password" type="password" />
        </div>

        <button className="mt-6 w-full rounded-xl bg-gradient-to-r from-cyan-500 to-violet-600 px-4 py-3 font-semibold text-slate-950 transition hover:opacity-95">Login</button>
      </form>
    </div>
  )
}
