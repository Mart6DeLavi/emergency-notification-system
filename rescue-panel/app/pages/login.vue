<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '~/stores/auth'
import type { AuthResponse } from '~/types'

definePageMeta({ layout: false })

const auth = useAuthStore()
const router = useRouter()
const toast = useToast()
const api = useApi()

const email = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!email.value || !password.value) return
  loading.value = true
  try {
    const res = await api<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: { email: email.value, password: password.value },
    })
    auth.setSession(res)
    toast.add({ severity: 'success', summary: 'Signed in', detail: res.email, life: 2500 })
    await router.push('/')
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Login failed',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-split">
    <div class="auth-brand">
      <div class="app-brand" style="padding: 0">
        <div class="brand-mark">
          <i class="pi pi-shield" />
        </div>
        <div>
          <div class="brand-name">SENSA</div>
          <div class="brand-sub" style="color: rgba(255, 255, 255, 0.45)">Rescue</div>
        </div>
      </div>

      <div>
        <h1 class="auth-brand-title">Emergency operations, in one place.</h1>
        <p class="auth-brand-text">
          Review incoming reports, verify incidents and broadcast alerts to the
          affected area.
        </p>
      </div>

      <div class="muted-sm" style="color: rgba(255, 255, 255, 0.4)">
        SENSA · Emergency notification system
      </div>
    </div>

    <div class="auth-form-wrap">
      <div class="auth-form">
        <h2 class="auth-title">Sign in</h2>
        <p class="auth-subtitle">Use your rescue operator account.</p>

        <div class="field">
          <label>Email</label>
          <InputText
            v-model="email"
            type="email"
            class="w-full"
            placeholder="operator@example.com"
            @keyup.enter="submit"
          />
        </div>

        <div class="field">
          <label>Password</label>
          <Password
            v-model="password"
            class="w-full"
            inputClass="w-full"
            :feedback="false"
            toggleMask
            @keyup.enter="submit"
          />
        </div>

        <Button
          class="w-full mt-2"
          label="Sign in"
          :loading="loading"
          :disabled="!email || !password"
          @click="submit"
        />
      </div>
    </div>
  </div>
</template>
