import { defineStore } from 'pinia'
import type { AuthResponse } from '~/types'

const STORAGE_KEY = 'sensa_rescue_panel_auth'

interface StoredSession {
  token: string | null
  userId: string | null
  email: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const userId = ref<string | null>(null)
  const email = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  function persist() {
    if (!import.meta.client) return
    const payload: StoredSession = {
      token: token.value,
      userId: userId.value,
      email: email.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
  }

  function setSession(session: AuthResponse) {
    token.value = session.token
    userId.value = session.userId
    email.value = session.email
    persist()
  }

  function clear() {
    token.value = null
    userId.value = null
    email.value = null
    if (import.meta.client) localStorage.removeItem(STORAGE_KEY)
  }

  function restore() {
    if (!import.meta.client) return
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    try {
      const parsed = JSON.parse(raw) as StoredSession
      token.value = parsed.token ?? null
      userId.value = parsed.userId ?? null
      email.value = parsed.email ?? null
    } catch {
      clear()
    }
  }

  return {
    token,
    userId,
    email,
    isAuthenticated,
    setSession,
    clear,
    restore,
  }
})
