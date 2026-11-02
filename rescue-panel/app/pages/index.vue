<script setup lang="ts">
import type { BroadcastHistory, Emergency, EmergencyStatus } from '~/types'

type PanelMode = 'BROADCASTS' | 'REPORTS'

const { t } = usePanelLocale()
const api = useApi()
const toast = useToast()
const route = useRoute()
const router = useRouter()

const emergencies = ref<Emergency[]>([])
const broadcasts = ref<BroadcastHistory[]>([])
const loading = ref(false)
const search = ref('')
const mode = ref<PanelMode>('BROADCASTS')
const activeTab = ref<'ALL' | 'OPEN' | 'CONFIRMED' | 'REJECTED'>('ALL')
const selectedId = ref<number | null>(null)
const selectedBroadcastId = ref<string | null>(null)

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

const filteredBroadcasts = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return broadcasts.value

  return broadcasts.value.filter((item) =>
    [item.title, item.message, item.city, item.street, alertMeta(item.alertType).label]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term)),
  )
})

const selected = computed(() =>
  emergencies.value.find((item) => item.id === selectedId.value) ?? null,
)

const selectedBroadcast = computed(() =>
  broadcasts.value.find((item) => item.id === selectedBroadcastId.value) ?? null,
)

const itemsCount = computed(() =>
  mode.value === 'BROADCASTS' ? broadcasts.value.length : emergencies.value.length,
)

function isOpen(item: Emergency) {
  return item.status === 'REPORTED' || item.status === 'UNDER_REVIEW'
}

function isConfirmed(item: Emergency) {
  return item.status === 'CONFIRMED' || item.status === 'BROADCAST'
}

const ALERT_META: Record<string, { label: string; icon: string }> = {
  FIRE: { label: 'Fire', icon: 'pi pi-bolt' },
  ACCIDENT: { label: 'Accident', icon: 'pi pi-car' },
  WEATHER: { label: 'Weather', icon: 'pi pi-cloud' },
  EVACUATION: { label: 'Evacuation', icon: 'pi pi-directions' },
  OTHER: { label: 'Other', icon: 'pi pi-ellipsis-h' },
}

function alertMeta(type: string) {
  return ALERT_META[type] ?? ALERT_META.OTHER!
}

function broadcastAudience(item: BroadcastHistory) {
  if (item.audience !== 'LOCATION') return t('allUsers')
  const area = [item.street, item.city].filter(Boolean).join(', ')
  const radius = item.radiusKm ? ` · ${item.radiusKm} km` : ''
  return area ? `${area}${radius}` : t('byLocation')
}

function broadcastStatusClass(item: BroadcastHistory) {
  return item.status === 'DELIVERED' ? 'status status--broadcast' : 'status status--rejected'
}

function broadcastStatusLabel(item: BroadcastHistory) {
  return item.status === 'DELIVERED' ? t('delivered') : t('filtered')
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

function selectEmergency(item: Emergency) {
  selectedId.value = item.id
  mode.value = 'REPORTS'
  router.replace({ query: { id: String(item.id) } })
}

function selectBroadcast(item: BroadcastHistory) {
  selectedBroadcastId.value = item.id
  mode.value = 'BROADCASTS'
  router.replace({ query: { broadcast: item.id } })
}

function switchMode(next: PanelMode) {
  if (mode.value === next) return
  mode.value = next

  if (next === 'BROADCASTS') {
    if (!selectedBroadcastId.value && filteredBroadcasts.value.length) {
      selectBroadcast(filteredBroadcasts.value[0]!)
    } else if (selectedBroadcastId.value) {
      router.replace({ query: { broadcast: selectedBroadcastId.value } })
    } else {
      router.replace({ query: {} })
    }
    return
  }

  if (!selectedId.value && filtered.value.length) {
    selectEmergency(filtered.value[0]!)
  } else if (selectedId.value) {
    router.replace({ query: { id: String(selectedId.value) } })
  } else {
    router.replace({ query: {} })
  }
}

async function load(silent = false) {
  loading.value = true
  const [reportsResult, broadcastsResult] = await Promise.allSettled([
    api<Emergency[]>('/api/v1/emergencies'),
    api<BroadcastHistory[]>('/api/v1/notifications/broadcasts'),
  ])

  if (reportsResult.status === 'fulfilled') {
    emergencies.value = reportsResult.value
  } else if (!silent) {
    const message = reportsResult.reason instanceof Error
      ? reportsResult.reason.message
      : 'Could not load emergencies.'
    toast.add({ severity: 'error', summary: 'Load failed', detail: message, life: 3500 })
  }

  if (broadcastsResult.status === 'fulfilled') {
    broadcasts.value = broadcastsResult.value
  } else if (!silent) {
    const message = broadcastsResult.reason instanceof Error
      ? broadcastsResult.reason.message
      : 'Could not load broadcast history.'
    toast.add({ severity: 'error', summary: 'Load failed', detail: message, life: 3500 })
  }

  loading.value = false
}

function selectInitial() {
  const queryBroadcast = typeof route.query.broadcast === 'string' ? route.query.broadcast : ''
  const queryId = Number(route.query.id)

  if (queryBroadcast && broadcasts.value.some((item) => item.id === queryBroadcast)) {
    mode.value = 'BROADCASTS'
    selectedBroadcastId.value = queryBroadcast
    return
  }

  if (Number.isFinite(queryId) && queryId > 0) {
    mode.value = 'REPORTS'
    selectedId.value = queryId
    return
  }

  if (broadcasts.value.length) {
    selectedBroadcastId.value = broadcasts.value[0]!.id
    return
  }

  if (filtered.value.length) {
    mode.value = 'REPORTS'
    selectedId.value = filtered.value[0]!.id
  }
}

let timer: ReturnType<typeof setInterval> | undefined

onMounted(async () => {
  await load()
  selectInitial()
  timer = setInterval(() => load(true), 20000)
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
            @click="load()"
          />
        </div>

        <div class="source-tabs" role="tablist">
          <button
            type="button"
            class="source-tab"
            :class="{ active: mode === 'BROADCASTS' }"
            role="tab"
            :aria-selected="mode === 'BROADCASTS'"
            @click="switchMode('BROADCASTS')"
          >
            <i class="pi pi-send" />
            <span>{{ t('sentByMe') }}</span>
            <span class="source-count">{{ broadcasts.length }}</span>
          </button>
          <button
            type="button"
            class="source-tab"
            :class="{ active: mode === 'REPORTS' }"
            role="tab"
            :aria-selected="mode === 'REPORTS'"
            @click="switchMode('REPORTS')"
          >
            <i class="pi pi-users" />
            <span>{{ t('userReports') }}</span>
            <span class="source-count">{{ emergencies.length }}</span>
          </button>
        </div>

        <InputText
          v-model="search"
          :placeholder="mode === 'BROADCASTS' ? t('searchAlerts') : t('search')"
          class="list-search"
        />

        <div v-if="mode === 'REPORTS'" class="tabs">
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
        <div v-if="loading && !itemsCount" class="state">
          <i class="pi pi-spin pi-spinner" />
          <span>Loading events…</span>
        </div>

        <template v-else-if="mode === 'BROADCASTS'">
          <div v-if="!filteredBroadcasts.length" class="state">
            <i class="pi pi-send" />
            <strong>{{ t('noAlerts') }}</strong>
            <NuxtLink to="/broadcast" class="state-link">{{ t('sendAlert') }}</NuxtLink>
          </div>

          <button
            v-for="item in filteredBroadcasts"
            :key="item.id"
            type="button"
            class="event-card broadcast-card"
            :class="{ selected: selectedBroadcastId === item.id }"
            @click="selectBroadcast(item)"
          >
            <div class="event-card-top">
              <span class="row-avatar broadcast-avatar">
                <i :class="alertMeta(item.alertType).icon" />
              </span>
              <span class="event-card-title">{{ item.title }}</span>
              <span :class="broadcastStatusClass(item)">
                {{ broadcastStatusLabel(item) }}
              </span>
            </div>
            <div class="event-card-meta">
              <i class="pi pi-send" />
              {{ broadcastAudience(item) }}
            </div>
            <div class="event-card-foot">
              <span>{{ item.recipients }} recipients</span>
              <span>{{ item.deliveries }} deliveries</span>
              <span>{{ formatDateTime(item.createdAt) }}</span>
            </div>
          </button>
        </template>

        <template v-else>
          <div v-if="!filtered.length" class="state">
            <i class="pi pi-inbox" />
            <strong>{{ t('noReports') }}</strong>
          </div>

          <button
            v-for="item in filtered"
            :key="item.id"
            type="button"
            class="event-card"
            :class="{ selected: selectedId === item.id }"
            @click="selectEmergency(item)"
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
        </template>
      </div>
    </section>

    <section class="island event-detail">
      <BroadcastDetailPane
        v-if="mode === 'BROADCASTS' && selectedBroadcast"
        :key="selectedBroadcast.id"
        :broadcast="selectedBroadcast"
      />
      <EmergencyDetailPane
        v-else-if="mode === 'REPORTS' && selectedId"
        :key="selectedId"
        :emergency-id="selectedId"
        @updated="load()"
      />
      <div v-else class="state state-fill">
        <i :class="mode === 'BROADCASTS' ? 'pi pi-send' : 'pi pi-inbox'" />
        <strong>{{ mode === 'BROADCASTS' ? t('selectAlert') : t('selectReport') }}</strong>
      </div>
    </section>
  </div>
</template>
