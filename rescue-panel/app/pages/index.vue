<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import type { Emergency, EmergencyStatus, RawEmergency } from '~/types'

const api = useApi()
const toast = useToast()
const router = useRouter()

const emergencies = ref<Emergency[]>([])
const loading = ref(true)
const statusFilter = ref<EmergencyStatus | null>(null)
const search = ref('')

const statusOptions: { label: string; value: EmergencyStatus | null }[] = [
  { label: 'All statuses', value: null },
  { label: 'Reported', value: 'REPORTED' },
  { label: 'Under review', value: 'UNDER_REVIEW' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Broadcast', value: 'BROADCAST' },
  { label: 'Rejected', value: 'REJECTED' },
]

const stats = computed(() => ({
  total: emergencies.value.length,
  open: emergencies.value.filter(
    (e) => e.status === 'REPORTED' || e.status === 'UNDER_REVIEW',
  ).length,
  confirmed: emergencies.value.filter(
    (e) => e.status === 'CONFIRMED' || e.status === 'BROADCAST',
  ).length,
  rejected: emergencies.value.filter((e) => e.status === 'REJECTED').length,
}))

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  return emergencies.value.filter((e) => {
    if (statusFilter.value && e.status !== statusFilter.value) return false
    if (!term) return true
    return [e.title, e.city, e.street, e.country].some((v) =>
      (v ?? '').toLowerCase().includes(term),
    )
  })
})

function statusClass(status: EmergencyStatus) {
  return `status status--${status.toLowerCase()}`
}

async function load() {
  try {
    const raw = await api<RawEmergency[]>('/api/v1/emergencies')
    emergencies.value = raw.map(normalizeEmergency)
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Failed to load emergencies',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

function openEmergency(row: Emergency) {
  router.push(`/emergencies/${row.id}`)
}

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  load()
  timer = setInterval(load, 15000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Emergencies</h1>
        <p class="page-subtitle">Reports awaiting verification and broadcast</p>
      </div>

      <div class="page-actions">
        <div class="search-wrap">
          <i class="pi pi-search" />
          <InputText
            v-model="search"
            class="search-input"
            placeholder="Search title, city, street…"
          />
        </div>
        <Select
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All statuses"
        />
        <Button
          icon="pi pi-refresh"
          label="Refresh"
          severity="secondary"
          outlined
          @click="load"
        />
      </div>
    </div>

    <div class="stats-grid">
      <div class="panel stat">
        <div class="stat-label">Total reports</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="panel stat">
        <div class="stat-label">Open</div>
        <div class="stat-value warning">{{ stats.open }}</div>
      </div>
      <div class="panel stat">
        <div class="stat-label">Confirmed</div>
        <div class="stat-value success">{{ stats.confirmed }}</div>
      </div>
      <div class="panel stat">
        <div class="stat-label">Rejected</div>
        <div class="stat-value danger">{{ stats.rejected }}</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">All reports</h2>
        <span class="muted-sm">{{ filtered.length }} shown</span>
      </div>

      <div v-if="loading" class="panel-body muted">Loading…</div>
      <div v-else-if="!filtered.length" class="panel-body attachment-empty">
        No emergencies found.
      </div>

      <table v-else class="table">
        <thead>
          <tr>
            <th>Status</th>
            <th>Emergency</th>
            <th>Location</th>
            <th>Reported</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in filtered"
            :key="row.id"
            @click="openEmergency(row)"
          >
            <td>
              <span :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
            </td>
            <td>
              <div class="row-title">{{ row.title }}</div>
              <div class="row-sub">#{{ row.id }}</div>
            </td>
            <td>
              {{ [row.city, row.street].filter(Boolean).join(', ') || '—' }}
            </td>
            <td class="muted-sm">
              {{ formatDateTime(row.alarmTimestamp || row.createdAt) }}
            </td>
            <td style="text-align: right">
              <i class="pi pi-chevron-right muted" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
