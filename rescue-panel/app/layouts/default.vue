<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const search = useState('global-search', () => '')

const email = computed(() => auth.email || '—')
const initials = computed(() => (auth.email || 'S').charAt(0).toUpperCase())

function logout() {
  auth.clear()
  navigateTo('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <NuxtLink to="/" class="app-brand">
        <span class="brand-mark"><i class="pi pi-shield" /></span>
        <span>
          <span class="brand-name">SENSA</span>
          <span class="brand-sub">Rescue console</span>
        </span>
      </NuxtLink>

      <div class="nav-label">Operations</div>

      <NuxtLink to="/" class="app-nav-item" :class="{ active: route.path === '/' }">
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
          <span class="avatar avatar-sm">{{ initials }}</span>
          <span class="app-user-email">{{ email }}</span>
        </div>
        <button type="button" class="app-nav-item" style="width: 100%; border: 0; background: none; cursor: pointer" @click="logout">
          <i class="pi pi-sign-out" />
          <span>Sign out</span>
        </button>
      </div>
    </aside>

    <div class="app-main-wrap">
      <header class="app-topbar">
        <div class="topbar-search">
          <i class="pi pi-search" />
          <InputText v-model="search" placeholder="Search reports, locations…" />
        </div>

        <div class="topbar-right">
          <span class="live-pill">Live</span>
          <button class="icon-btn" type="button" aria-label="Notifications">
            <i class="pi pi-bell" />
            <span class="notif-dot" />
          </button>
          <div class="topbar-user">
            <span class="avatar">{{ initials }}</span>
            <span class="topbar-user-email">{{ email }}</span>
            <i class="pi pi-chevron-down topbar-chevron" />
          </div>
        </div>
      </header>

      <main class="app-content">
        <slot />
      </main>
    </div>
  </div>
</template>
