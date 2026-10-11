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
    <div class="flex-between mb-2">
      <div>
        <h2 style="margin: 0">Emergencies</h2>
        <span class="muted">Reports awaiting verification and broadcast</span>
      </div>
    </div>

    <Card>
      <template #content>
        <div class="flex-between mb-2">
          <InputText v-model="search" placeholder="Search title, city, street…" />
          <div class="flex-between" style="gap: 0.75rem">
            <Select
              v-model="statusFilter"
              :options="statusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Status"
            />
            <Button icon="pi pi-refresh" label="Refresh" @click="load" />
          </div>
        </div>

        <DataTable
          :value="filtered"
          :loading="loading"
          dataKey="id"
          stripedRows
          responsiveLayout="scroll"
          emptyMessage="No emergencies found"
        >
          <Column header="Status">
            <template #body="{ data }">
              <Tag
                :value="statusLabel(data.status)"
                :severity="statusSeverity(data.status)"
              />
            </template>
          </Column>
          <Column field="title" header="Title" />
          <Column header="Location">
            <template #body="{ data }">
              {{ [data.city, data.street].filter(Boolean).join(', ') || '—' }}
            </template>
          </Column>
          <Column header="Reported at">
            <template #body="{ data }">
              {{ formatDateTime(data.alarmTimestamp || data.createdAt) }}
            </template>
          </Column>
          <Column header="">
            <template #body="{ data }">
              <Button
                label="View"
                icon="pi pi-arrow-right"
                iconPos="right"
                size="small"
                text
                @click="openEmergency(data)"
              />
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>
