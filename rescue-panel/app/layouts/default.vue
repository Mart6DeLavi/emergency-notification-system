<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => {
  if (route.path.startsWith('/templates')) return 'Notification templates'
  if (route.path.startsWith('/emergencies')) return 'Emergency details'
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
      <div class="app-brand">
        <div class="brand-mark">
          <i class="pi pi-shield" />
        </div>
        <div>
          <div class="brand-name">SENSA</div>
          <div class="brand-sub">Rescue</div>
        </div>
      </div>

      <div class="nav-label">Operations</div>

      <NuxtLink
        to="/"
        class="app-nav-item"
        :class="{ active: route.path === '/' }"
      >
        <i class="pi pi-inbox" />
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

      <div class="app-sidebar-footer">
        <div class="app-user">
          <span class="app-user-email">{{ auth.email }}</span>
        </div>
        <button
          type="button"
          class="app-nav-item"
          style="width: 100%; border: 0; background: none; cursor: pointer"
          @click="logout"
        >
          <i class="pi pi-sign-out" />
          <span>Sign out</span>
        </button>
      </div>
    </aside>

    <div class="app-main-wrap">
      <header class="app-topbar">
        <span class="app-topbar-title">{{ pageTitle }}</span>
        <span class="app-topbar-spacer" />
        <span class="status status--confirmed">Live</span>
      </header>

      <main class="app-content">
        <slot />
      </main>
    </div>
  </div>
</template>
