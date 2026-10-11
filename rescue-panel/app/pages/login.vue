<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '~/stores/auth'
import type { AuthResponse } from '~/types'

definePageMeta({ layout: 'blank' })

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
  <Card class="auth-card">
    <template #title>Rescue Panel — Sign in</template>
    <template #subtitle>Emergency notification system</template>
    <template #content>
      <div class="w-full">
        <label class="muted">Email</label>
        <InputText
          v-model="email"
          type="email"
          class="w-full mt-1"
          placeholder="operator@example.com"
          @keyup.enter="submit"
        />
      </div>
      <div class="w-full mt-2">
        <label class="muted">Password</label>
        <Password
          v-model="password"
          class="w-full mt-1"
          inputClass="w-full"
          :feedback="false"
          toggleMask
          @keyup.enter="submit"
        />
      </div>
      <Button
        class="w-full mt-3"
        label="Sign in"
        icon="pi pi-sign-in"
        :loading="loading"
        :disabled="!email || !password"
        @click="submit"
      />
    </template>
  </Card>
</template>
