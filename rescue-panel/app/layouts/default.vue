<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const { t, language, setLanguage, restore } = usePanelLocale()

const accountOpen = ref(false)

const email = computed(() => auth.email || '—')
const initials = computed(() => email.value.slice(0, 1).toUpperCase())
const shortUserId = computed(() => {
  const id = auth.userId || ''
  return id.length > 14 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id
})

onMounted(restore)

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

      <div class="nav-label">{{ t('operations') }}</div>

      <NuxtLink
        to="/"
        class="app-nav-item"
        :class="{ active: route.path === '/' }"
      >
        <i class="pi pi-inbox" />
        <span>{{ t('emergencies') }}</span>
      </NuxtLink>

      <NuxtLink
        to="/templates"
        class="app-nav-item"
        :class="{ active: route.path.startsWith('/templates') }"
      >
        <i class="pi pi-file-edit" />
        <span>{{ t('templates') }}</span>
      </NuxtLink>

      <NuxtLink
        to="/broadcast"
        class="app-nav-item"
        :class="{ active: route.path.startsWith('/broadcast') }"
      >
        <i class="pi pi-send" />
        <span>{{ t('notify') }}</span>
      </NuxtLink>

      <NuxtLink
        to="/test-lab"
        class="app-nav-item"
        :class="{ active: route.path.startsWith('/test-lab') }"
      >
        <i class="pi pi-chart-bar" />
        <span>{{ t('testLab') }}</span>
      </NuxtLink>

      <div class="sidebar-spacer" />

      <button type="button" class="account-button" @click="accountOpen = true">
        <span class="avatar">{{ initials }}</span>
        <span class="account-text">
          <strong>{{ t('account') }}</strong>
          <small>{{ email }}</small>
        </span>
        <i class="pi pi-cog" />
      </button>
    </aside>

    <main class="app-main">
      <slot />
    </main>

    <Dialog
      v-model:visible="accountOpen"
      modal
      :header="t('account')"
      :style="{ width: '430px' }"
      :draggable="false"
    >
      <div class="account-profile">
        <span class="avatar avatar-lg">{{ initials }}</span>
        <div class="account-identity">
          <strong>{{ email }}</strong>
          <small>{{ shortUserId }}</small>
        </div>
      </div>

      <div class="account-list">
        <div class="account-row">
          <span>{{ t('theme') }}</span>
          <span class="account-value">
            <i class="pi pi-moon" />
            {{ t('themeDark') }}
          </span>
        </div>
        <div class="account-row">
          <span>{{ t('language') }}</span>
          <div class="segmented">
            <button
              type="button"
              :class="{ active: language === 'en' }"
              @click="setLanguage('en')"
            >
              EN
            </button>
            <button
              type="button"
              :class="{ active: language === 'pl' }"
              @click="setLanguage('pl')"
            >
              PL
            </button>
          </div>
        </div>
        <div class="account-row">
          <span>{{ t('session') }}</span>
          <span class="live-pill">{{ t('live') }}</span>
        </div>
      </div>

      <template #footer>
        <Button
          :label="t('signOut')"
          icon="pi pi-sign-out"
          severity="danger"
          text
          @click="logout"
        />
        <Button
          :label="t('close')"
          severity="secondary"
          @click="accountOpen = false"
        />
      </template>
    </Dialog>
  </div>
</template>
