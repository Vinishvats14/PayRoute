const AUTH_API_URL = import.meta.env.VITE_AUTH_API_URL || 'http://localhost:8081'
const WALLET_API_URL = import.meta.env.VITE_WALLET_API_URL || 'http://localhost:8082'
const TRANSACTION_API_URL = import.meta.env.VITE_TRANSACTION_API_URL || 'http://localhost:8083'
const RISK_API_URL = import.meta.env.VITE_RISK_API_URL || 'http://localhost:8084'

async function request(baseUrl, path, opts = {}){
  const token = localStorage.getItem('token')
  const headers = { ...(opts.headers || {}) }

  if (token) {
    headers['Authorization'] = 'Bearer ' + token
  }

  if (opts.body && !(headers['Content-Type'] || headers['content-type'])) {
    headers['Content-Type'] = 'application/json'
  }

  const res = await fetch(baseUrl + path, { ...opts, headers })
  if (res.status === 204) return null

  const body = await res.json().catch(() => null)
  if (!res.ok) throw new Error(body?.message || body?.error || 'Request failed')
  return body
}

export async function login(email, password){
  const res = await request(AUTH_API_URL, '/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  })

  if (!res?.accessToken) {
    throw new Error('Login response did not include a token')
  }

  return res
}

export async function register(email, password){
  return request(AUTH_API_URL, '/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  })
}

export async function me(){
  return request(AUTH_API_URL, '/auth/me', { method: 'GET' })
}

export async function setTransactionPin(pin){
  return request(AUTH_API_URL, '/auth/transaction-pin', {
    method: 'POST',
    body: JSON.stringify({ pin })
  })
}

export async function verifyTransactionPin(idempotencyKey, pin){
  return request(TRANSACTION_API_URL, '/transactions/verify-pin', {
    method: 'POST',
    body: JSON.stringify({ idempotencyKey, pin })
  })
}

export async function wallets(){
  return request(WALLET_API_URL, '/wallets', { method: 'GET' })
}

export async function createWallet(){
  return request(WALLET_API_URL, '/wallets', { method: 'POST' })
}

export async function deposit(amount){
  return request(WALLET_API_URL, '/wallets/deposit', {
    method: 'POST',
    body: JSON.stringify({ amount: Number(amount) })
  })
}

export async function withdraw(amount){
  return request(WALLET_API_URL, '/wallets/withdraw', {
    method: 'POST',
    body: JSON.stringify({ amount: Number(amount) })
  })
}

export async function transfer(payload, idempotencyKey){
  const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
  return request(TRANSACTION_API_URL, '/transactions/transfer', {
    method: 'POST',
    body: JSON.stringify(payload),
    headers
  })
}

export async function riskCheck(payload){
  return request(RISK_API_URL, '/risk/check', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export default {
  login,
  register,
  me,
  setTransactionPin,
  verifyTransactionPin,
  wallets,
  createWallet,
  deposit,
  withdraw,
  transfer,
  riskCheck
}
