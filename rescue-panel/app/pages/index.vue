<script setup lang="ts">
import type { Emergency, EmergencyStatus } from '~/types'

const { t } = usePanelLocale()
const api = useApi()
const toast = useToast()
const route = useRoute()
const router = useRouter()

const emergencies = ref<Emergency[]>([])
const loading = ref(false)
const search = ref('')
const activeTab = ref<'ALL' | 'OPEN' | 'CONFIRMED' | 'REJECTED'>('ALL')
const selectedId = ref<number | null>(null)

const tabs = computed(() => [
  { key: 'ALL' as const, label: t('all'), count: emergencies.value.length },
  { key: 'OPEN' as const, label: t('open'), count: emergencies.value.filter(isOpen).length },
  { key: 'CONFIRMED' as const, label: t('confirmed'), count: emergencies.value.filter(isConfirmed).length },
  { key: 'REJECTED' as const, label: t('rejected'), count: emergencies.value.filter((item) => item.status === 'REJECTED').length },
])

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  return emergencies.value.filter((item) => {
    const matchesTab =
      activeTab.value === 'ALL' ||
      (activeTab.value === 'OPEN' && isOpen(item)) ||
      (activeTab.value === 'CONFIRMED' && isConfirmed(item)) ||
      (activeTab.value === 'REJECTED' && item.status === 'REJECTED')

    if (!matchesTab) return false
    if (!term) return true

    return [item.title, item.description, item.city, item.street, item.country]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term))
  })
})

const selected = computed(() =>
  emergencies.value.find((item) => item.id === selectedId.value) ?? null,
)

function isOpen(item: Emergency) {
  return item.status === 'REPORTED' || item.status === 'UNDER_REVIEW'
}

function isConfirmed(item: Emergency) {
  return item.status === 'CONFIRMED' || item.status === 'BROADCAST'
}

const AVATAR_COLORS = ['#f9c97c', '#a7c7e7', '#cdb4db', '#9ae6b4', '#f7a8a8', '#fde68a', '#bfdbfe']

function avatarColor(id: number) {
  return AVATAR_COLORS[Math.abs(id) % AVATAR_COLORS.length]
}

function initials(item: Emergency) {
  return (item.title || 'E').trim().charAt(0).toUpperCase()
}

function location(item: Emergency) {
  return [item.street, item.city, item.country].filter(Boolean).join(', ') || 'No location'
}

function select(item: Emergency) {
  selectedId.value = item.id
  router.replace({ query: { id: String(item.id) } })
}

async function load() {
  loading.value = true
  try {
    emergencies.value = await api<Emergency[]>('/api/v1/emergencies')
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Could not load emergencies.'
    toast.add({ severity: 'error', summary: 'Load failed', detail: message, life: 3500 })
  } finally {
    loading.value = false
  }
}

let timer: ReturnType<typeof setInterval> | undefined

onMounted(async () => {
  await load()

  const queryId = Number(route.query.id)
  if (Number.isFinite(queryId) && queryId > 0) {
    selectedId.value = queryId
  } else if (filtered.value.length) {
    selectedId.value = filtered.value[0]!.id
  }

  timer = setInterval(load, 20000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="events-layout">
    <section class="island events-list">
      <header class="list-head">
        <div class="list-title-row">
          <div>
            <p class="eyebrow">{{ t('operations') }}</p>
            <h1 class="list-title">{{ t('events') }}</h1>
          </div>
          <span class="live-pill">{{ t('live') }}</span>
          <Button
            icon="pi pi-refresh"
            text
            severity="secondary"
            :loading="loading"
            aria-label="Refresh"
            @click="load"
          />
        </div>

        <InputText
          v-model="search"
          :placeholder="t('search')"
          class="list-search"
        />

        <div class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="tab"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
            <span class="tab-count">{{ tab.count }}</span>
          </button>
        </div>
      </header>

      <div class="events-scroll">
        <div v-if="loading && !emergencies.length" class="state">
          <i class="pi pi-spin pi-spinner" />
          <span>Loading reports…</span>
        </div>

        <div v-else-if="!filtered.length" class="state">
          <i class="pi pi-inbox" />
          <strong>{{ t('noReports') }}</strong>
        </div>

        <button
          v-for="item in filtered"
          :key="item.id"
          type="button"
          class="event-card"
          :class="{ selected: selectedId === item.id }"
          @click="select(item)"
        >
          <div class="event-card-top">
            <span class="row-avatar" :style="{ background: avatarColor(item.id) }">
              {{ initials(item) }}
            </span>
            <span class="event-card-title">{{ item.title }}</span>
            <span class="status" :class="`status--${item.status.toLowerCase()}`">
              {{ statusLabel(item.status as EmergencyStatus) }}
            </span>
          </div>
          <div class="event-card-meta">
            <i class="pi pi-map-marker" />
            {{ location(item) }}
          </div>
          <div class="event-card-foot">
            <span>#{{ item.id }}</span>
            <span>{{ formatDateTime(item.createdAt) }}</span>
            <span>{{ item.files?.length || 0 }} files</span>
          </div>
        </button>
      </div>
    </section>

    <section class="island event-detail">
      <EmergencyDetailPane
        v-if="selectedId"
        :key="selectedId"
        :emergency-id="selectedId"
        @updated="load"
      />
      <div v-else class="state state-fill">
        <i class="pi pi-inbox" />
        <strong>{{ t('selectReport') }}</strong>
      </div>
    </section>
  </div>
</template>
