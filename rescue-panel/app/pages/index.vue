<script setup lang="ts">
import type { Emergency, EmergencyStatus } from '~/types'

const api = useApi()
const toast = useToast()
const search = useState('global-search', () => '')

const emergencies = ref<Emergency[]>([])
const loading = ref(false)
const activeTab = ref<'ALL' | 'OPEN' | 'CONFIRMED' | 'REJECTED'>('ALL')

const tabs = computed(() => [
  { key: 'ALL' as const, label: 'All reports', count: emergencies.value.length },
  { key: 'OPEN' as const, label: 'Open', count: emergencies.value.filter(isOpen).length },
  { key: 'CONFIRMED' as const, label: 'Confirmed', count: emergencies.value.filter(isConfirmed).length },
  { key: 'REJECTED' as const, label: 'Rejected', count: emergencies.value.filter((item) => item.status === 'REJECTED').length },
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

function openEmergency(item: Emergency) {
  navigateTo(`/emergencies/${item.id}`)
}

async function copyId(item: Emergency) {
  try {
    await navigator.clipboard.writeText(String(item.id))
    toast.add({ severity: 'success', summary: 'Copied', detail: `Report #${item.id}`, life: 1800 })
  } catch {
    toast.add({ severity: 'error', summary: 'Could not copy', detail: 'Clipboard is unavailable.', life: 2200 })
  }
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

onMounted(() => {
  load()
  timer = setInterval(load, 20000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <section>
    <div class="page-head">
      <div>
        <p class="eyebrow">Operations</p>
        <h1 class="page-title">Emergencies</h1>
        <p class="page-subtitle">Incoming reports from the field, newest first.</p>
      </div>
      <Button label="Refresh" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="load" />
    </div>

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

    <div class="table-card">
      <div v-if="loading && !emergencies.length" class="state">
        <i class="pi pi-spin pi-spinner" />
        <span>Loading reports…</span>
      </div>

      <div v-else-if="!filtered.length" class="state">
        <i class="pi pi-inbox" />
        <strong>No reports found</strong>
        <span>Try a different tab or clear the search.</span>
      </div>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Emergency</th>
            <th>Location</th>
            <th>Status</th>
            <th>Reported</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filtered" :key="item.id" @click="openEmergency(item)">
            <td>
              <div class="row-main">
                <span class="row-avatar" :style="{ background: avatarColor(item.id) }">{{ initials(item) }}</span>
                <span class="row-title-wrap">
                  <span class="row-title">{{ item.title }}</span>
                  <span class="row-sub">#{{ item.id }} · {{ item.files?.length || 0 }} attachments</span>
                </span>
              </div>
            </td>
            <td>
              <span class="row-location">
                <i class="pi pi-map-marker" />
                {{ location(item) }}
              </span>
            </td>
            <td>
              <span class="status" :class="`status--${item.status.toLowerCase()}`">
                {{ statusLabel(item.status as EmergencyStatus) }}
              </span>
            </td>
            <td><span class="row-time">{{ formatDateTime(item.createdAt) }}</span></td>
            <td class="cell-actions" @click.stop>
              <details class="row-menu">
                <summary><i class="pi pi-ellipsis-v" /></summary>
                <div class="row-menu-pop">
                  <button type="button" @click="openEmergency(item)">
                    <i class="pi pi-arrow-right" /> Open report
                  </button>
                  <button type="button" @click="copyId(item)">
                    <i class="pi pi-copy" /> Copy ID
                  </button>
                </div>
              </details>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
