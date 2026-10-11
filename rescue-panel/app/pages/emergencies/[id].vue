<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import type { Emergency, EmergencyStatus, RawEmergency } from '~/types'

const route = useRoute()
const router = useRouter()
const api = useApi()
const toast = useToast()
const confirm = useConfirm()

const emergency = ref<Emergency | null>(null)
const loading = ref(true)
const updating = ref(false)

const id = computed(() => String(route.params.id))

const transitions = computed<EmergencyStatus[]>(() =>
  emergency.value ? allowedTransitions(emergency.value.status) : [],
)

async function load() {
  loading.value = true
  try {
    const raw = await api<RawEmergency>(`/api/v1/emergencies/${id.value}`)
    emergency.value = normalizeEmergency(raw)
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Failed to load emergency',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    loading.value = false
  }
}

async function changeStatus(next: EmergencyStatus) {
  updating.value = true
  try {
    const raw = await api<RawEmergency>(`/api/v1/emergencies/${id.value}/status`, {
      method: 'PATCH',
      body: { status: next },
    })
    emergency.value = normalizeEmergency(raw)
    toast.add({
      severity: 'success',
      summary: `Status updated to ${statusLabel(next)}`,
      life: 2500,
    })
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Failed to update status',
      detail: (e as Error).message,
      life: 4000,
    })
  } finally {
    updating.value = false
  }
}

function confirmStatus(next: EmergencyStatus) {
  if (next !== 'CONFIRMED' && next !== 'BROADCAST') {
    changeStatus(next)
    return
  }
  confirm.require({
    message:
      next === 'CONFIRMED'
        ? 'Confirm this emergency? This will trigger the alert broadcast to all users in the affected area.'
        : 'Mark this emergency as broadcast?',
    header: 'Please confirm',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Yes, proceed',
    rejectLabel: 'Cancel',
    accept: () => changeStatus(next),
  })
}

function fileName(url: string): string {
  const parts = url.split('/')
  return parts[parts.length - 1] || url
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex-between mb-2">
      <Button
        label="Back to list"
        icon="pi pi-arrow-left"
        text
        severity="secondary"
        @click="router.push('/')"
      />
      <Button icon="pi pi-refresh" label="Refresh" text @click="load" />
    </div>

    <div v-if="loading" class="muted"><i class="pi pi-spin pi-spinner" /> Loading…</div>

    <template v-else-if="emergency">
      <Card class="mb-2">
        <template #content>
          <div class="flex-between">
            <div>
              <h2 style="margin: 0 0 0.5rem">{{ emergency.title }}</h2>
              <Tag
                :value="statusLabel(emergency.status)"
                :severity="statusSeverity(emergency.status)"
              />
            </div>
            <div class="flex-between" style="gap: 0.5rem; flex-wrap: wrap">
              <Button
                v-for="next in transitions"
                :key="next"
                :label="statusLabel(next)"
                :severity="next === 'REJECTED' ? 'danger' : 'primary'"
                :disabled="updating"
                @click="confirmStatus(next)"
              />
            </div>
          </div>
        </template>
      </Card>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem">
        <Card>
          <template #title>Details</template>
          <template #content>
            <p><strong>Description:</strong> {{ emergency.description || '—' }}</p>
            <p>
              <strong>Location:</strong>
              {{ [emergency.city, emergency.street, emergency.country].filter(Boolean).join(', ') || '—' }}
            </p>
            <p><strong>Reported at:</strong> {{ formatDateTime(emergency.alarmTimestamp) }}</p>
            <p><strong>Created at:</strong> {{ formatDateTime(emergency.createdAt) }}</p>
            <p><strong>Reporter ID:</strong> <code>{{ emergency.userId }}</code></p>
            <p><strong>Emergency ID:</strong> {{ emergency.id }}</p>
          </template>
        </Card>

        <Card>
          <template #title>Attachments</template>
          <template #content>
            <ul v-if="emergency.files.length" style="margin: 0; padding-left: 1.1rem">
              <li v-for="file in emergency.files" :key="file.fileId" class="mt-1">
                <a :href="file.url" target="_blank" rel="noopener">
                  <i class="pi pi-paperclip" /> {{ fileName(file.url) }}
                </a>
                <span class="muted"> (id: {{ file.fileId }})</span>
              </li>
            </ul>
            <p v-else class="muted">No attachments.</p>
          </template>
        </Card>
      </div>

      <Card class="mt-2">
        <template #title>Location map</template>
        <template #content>
          <LocationMap
            :city="emergency.city"
            :street="emergency.street"
            :country="emergency.country"
            :title="emergency.title"
          />
        </template>
      </Card>
    </template>

    <div v-else class="muted">Emergency not found.</div>
  </div>
</template>
