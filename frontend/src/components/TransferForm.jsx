import React, { useEffect, useState } from 'react'
import api from '../api'

export default function TransferForm({ setToast }){
  const [fromWallet, setFromWallet] = useState('')
  const [toWallet, setToWallet] = useState('')
  const [amount, setAmount] = useState('')
  const [pin, setPin] = useState('')
  const [pendingTransfer, setPendingTransfer] = useState(null)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  useEffect(() => {
    setError(null)
    setSuccess(null)

    if (pendingTransfer) {
      setPendingTransfer(null)
      setPin('')
    }
  }, [fromWallet, toWallet, amount])

  async function submit(e){
    e.preventDefault()
    setError(null)
    setSuccess(null)

    try {
      const idempotencyKey = 'ik-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
      const payload = { receiverWalletId: Number(toWallet), amount: Number(amount) }
      const res = await api.transfer(payload, idempotencyKey)

      if (res?.status === 'PENDING_VERIFICATION') {
        setPendingTransfer({ idempotencyKey, amount: Number(amount), receiverWalletId: Number(toWallet) })
        setSuccess('Medium-risk transfer requires transaction PIN verification before it can proceed.')
        setToast({ message: 'PIN verification required', type: 'info' })
        return
      }

      setSuccess(JSON.stringify(res, null, 2))
      setToast({ message: 'Transfer completed successfully', type: 'success' })
    } catch (err) {
      setError(err.message)
      setToast({ message: err.message, type: 'error' })
    }
  }

  async function verifyPendingTransfer(){
    if (!pendingTransfer || !pin) {
      setError('Enter your transaction PIN to continue.')
      return
    }

    try {
      setError(null)
      const res = await api.verifyTransactionPin(pendingTransfer.idempotencyKey, pin)
      setSuccess(JSON.stringify(res, null, 2))
      setPin('')
      setPendingTransfer(null)
      setToast({ message: 'Transfer approved after PIN verification', type: 'success' })
    } catch (err) {
      setError(err.message)
      setToast({ message: err.message, type: 'error' })
    }
  }

  return (
    <div className="flex items-center justify-center">
      <form className="w-full max-w-xl rounded-3xl border border-white/10 bg-slate-900/80 p-8 shadow-2xl shadow-slate-950/40" onSubmit={submit}>
        <div className="mb-6">
          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-emerald-300">Transfer money</p>
          <h3 className="mt-2 text-3xl font-bold text-white">Send payment</h3>
        </div>

        {error && <div className="mb-4 rounded-xl border border-rose-500/50 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{error}</div>}
        {success && <pre className="mb-4 overflow-auto rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-xs text-emerald-100">{success}</pre>}

        <div className="space-y-4">
          <input value={fromWallet} onChange={e => setFromWallet(e.target.value)} className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-emerald-500" placeholder="Sender wallet id (auto from JWT)" />
          <input value={toWallet} onChange={e => setToWallet(e.target.value)} className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-emerald-500" placeholder="Receiver wallet id" />
          <input value={amount} onChange={e => setAmount(e.target.value)} className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-emerald-500" placeholder="Amount" type="number" step="0.01" min="0" />
        </div>

        <button className="mt-6 w-full rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-500 px-4 py-3 font-semibold text-slate-950 transition hover:opacity-95">Send transfer</button>

        {pendingTransfer && (
          <div className="mt-6 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4">
            <p className="mb-3 text-sm font-semibold text-amber-100">Complete PIN verification</p>
            <input value={pin} onChange={e => setPin(e.target.value)} className="w-full rounded-xl border border-slate-700 bg-slate-950/60 px-4 py-3 text-white outline-none placeholder:text-slate-400 focus:border-amber-500" placeholder="Enter 4-6 digit transaction PIN" type="password" inputMode="numeric" pattern="[0-9]*" />
            <button type="button" onClick={verifyPendingTransfer} className="mt-3 w-full rounded-xl bg-amber-500 px-4 py-3 font-semibold text-slate-950 transition hover:bg-amber-400">Verify and continue transfer</button>
          </div>
        )}
      </form>
    </div>
  )
}
