<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => {
  if (route.path.startsWith('/templates')) return 'Notification Templates'
  if (route.path.startsWith('/emergencies')) return 'Emergency Details'
  return 'Emergencies'
})

function logout() {
  auth.clear()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="app-brand">🛟 SENSA Rescue</div>

      <NuxtLink
        to="/"
        class="app-nav-item"
        :class="{ active: route.path === '/' }"
      >
        <i class="pi pi-exclamation-triangle" />
        <span>Emergencies</span>
      </NuxtLink>

      <NuxtLink
        to="/templates"
        class="app-nav-item"
        :class="{ active: route.path.startsWith('/templates') }"
      >
        <i class="pi pi-file-edit" />
        <span>Templates</span>
      </NuxtLink>
    </aside>

    <div class="app-main-wrap">
      <header class="app-topbar">
        <span class="app-topbar-title">{{ pageTitle }}</span>
        <span class="app-topbar-spacer" />
        <span class="muted">{{ auth.email }}</span>
        <Button
          label="Logout"
          icon="pi pi-sign-out"
          text
          severity="secondary"
          @click="logout"
        />
      </header>

      <main class="app-content">
        <slot />
      </main>
    </div>
  </div>
</template>
