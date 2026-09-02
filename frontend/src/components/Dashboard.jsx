import React, { useEffect, useState } from 'react'
import api from '../api'

export default function Dashboard({ setToast }){
  const [wallet, setWallet] = useState(null)
  const [profile, setProfile] = useState(null)
  const [amount, setAmount] = useState('')
  const [pin, setPin] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const loadWallet = async () => {
    try {
      setError(null)
      const res = await api.wallets()
      setWallet(res)
    } catch (err) {
      setWallet(null)
      setError(err.message)
    }
  }

  const loadProfile = async () => {
    try {
      const res = await api.me()
      setProfile(res)
    } catch (err) {
      setProfile(null)
    }
  }

  useEffect(() => {
    loadWallet()
    loadProfile()
  }, [])

  const handleCreateWallet = async () => {
    try {
      setLoading(true)
      const res = await api.createWallet()
      setWallet(res)
      setToast({ message: 'Wallet created successfully', type: 'success' })
    } catch (err) {
      setToast({ message: err.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleDeposit = async () => {
    if (!amount) return
    try {
      setLoading(true)
      const res = await api.deposit(amount)
      setWallet(res)
      setAmount('')
      setToast({ message: 'Deposit successful', type: 'success' })
    } catch (err) {
      setToast({ message: err.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleWithdraw = async () => {
    if (!amount) return
    try {
      setLoading(true)
      const res = await api.withdraw(amount)
      setWallet(res)
      setAmount('')
      setToast({ message: 'Withdrawal successful', type: 'success' })
    } catch (err) {
      setToast({ message: err.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleSetPin = async () => {
    if (!pin || pin.length < 4) {
      setToast({ message: 'Enter a 4-6 digit PIN', type: 'error' })
      return
    }

    try {
      setLoading(true)
      await api.setTransactionPin(pin)
      setPin('')
      await loadProfile()
      setToast({ message: 'Transaction PIN saved', type: 'success' })
    } catch (err) {
      setToast({ message: err.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-slate-950/30">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-cyan-300">Wallet</p>
            <h3 className="mt-2 text-3xl font-bold text-white">Your wallet</h3>
          </div>
          {!wallet && (
            <button className="rounded-xl bg-cyan-500 px-4 py-2 font-semibold text-slate-950 hover:bg-cyan-400" onClick={handleCreateWallet} disabled={loading}>
              {loading ? 'Creating...' : 'Create wallet'}
            </button>
          )}
        </div>

        {error && !wallet && (
          <div className="mt-4 rounded-xl border border-amber-500/40 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
            {error}. Create a wallet to start using the wallet service.
          </div>
        )}

        {wallet && (
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl border border-emerald-400/20 bg-emerald-500/10 p-4">
              <p className="text-xs uppercase tracking-[0.25em] text-emerald-200">Balance</p>
              <p className="mt-2 text-3xl font-black text-white">₹{wallet.balance}</p>
            </div>
            <div className="rounded-2xl border border-violet-400/20 bg-violet-500/10 p-4">
              <p className="text-xs uppercase tracking-[0.25em] text-violet-200">Status</p>
              <p className="mt-2 text-xl font-bold text-white">{wallet.status}</p>
            </div>
            <div className="rounded-2xl border border-sky-400/20 bg-sky-500/10 p-4">
              <p className="text-xs uppercase tracking-[0.25em] text-sky-200">Wallet ID</p>
              <p className="mt-2 text-xl font-bold text-white">#{wallet.id}</p>
            </div>
          </div>
        )}
      </div>

      {wallet && (
        <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-xl shadow-slate-950/30">
            <h4 className="mb-4 text-xl font-bold text-white">Manage funds</h4>
            <div className="space-y-4">
              <input value={amount} onChange={e => setAmount(e.target.value)} className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-cyan-500" placeholder="Amount" type="number" step="0.01" min="0" />
              <div className="flex gap-3">
                <button onClick={handleDeposit} className="flex-1 rounded-xl bg-emerald-500 px-4 py-3 font-semibold text-slate-950 transition hover:bg-emerald-400">Deposit</button>
                <button onClick={handleWithdraw} className="flex-1 rounded-xl bg-amber-500 px-4 py-3 font-semibold text-slate-950 transition hover:bg-amber-400">Withdraw</button>
              </div>
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-slate-900/80 p-6 shadow-xl shadow-slate-950/30">
            <h4 className="mb-2 text-xl font-bold text-white">Profile & safety</h4>
            <div className="space-y-4 text-sm text-slate-300">
              <div className="rounded-xl border border-cyan-500/20 bg-cyan-500/5 p-3">
                <p className="text-xs uppercase tracking-[0.25em] text-cyan-200">Transaction PIN</p>
                <p className="mt-2 text-base font-semibold text-white">
                  {profile?.transactionPinConfigured ? 'Configured and active' : 'Not configured yet'}
                </p>
              </div>
              <div className="space-y-3">
                <input value={pin} onChange={e => setPin(e.target.value)} className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-cyan-500" placeholder="Set 4-6 digit PIN" type="password" inputMode="numeric" pattern="[0-9]*" />
                <button onClick={handleSetPin} className="w-full rounded-xl border border-emerald-500/50 bg-emerald-500/10 px-3 py-3 text-left text-emerald-100 hover:bg-emerald-500/20">Save transaction PIN</button>
              </div>
              <button onClick={loadWallet} className="w-full rounded-xl border border-white/10 bg-white/5 px-3 py-3 text-left hover:bg-white/10">Refresh wallet</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
