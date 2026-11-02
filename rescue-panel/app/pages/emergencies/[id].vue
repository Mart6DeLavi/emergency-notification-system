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
const attachmentUrls = ref<Record<number, string>>({})
const previewVisible = ref(false)
const preview = ref<{ url: string; type: 'image' | 'video'; name: string } | null>(null)

const id = computed(() => String(route.params.id))
const flow: EmergencyStatus[] = ['REPORTED', 'UNDER_REVIEW', 'CONFIRMED', 'BROADCAST']

const transitions = computed<EmergencyStatus[]>(() =>
  emergency.value ? allowedTransitions(emergency.value.status) : [],
)

const currentIndex = computed(() =>
  flow.indexOf(emergency.value?.status ?? 'REPORTED'),
)

const location = computed(() =>
  [emergency.value?.street, emergency.value?.city, emergency.value?.country]
    .filter(Boolean)
    .join(', '),
)

async function load() {
  loading.value = true
  try {
    const raw = await api<RawEmergency>(`/api/v1/emergencies/${id.value}`)
    emergency.value = normalizeEmergency(raw)
    await loadAttachments()
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

async function loadAttachments() {
  const files = emergency.value?.files ?? []

  await Promise.all(
    files.map(async (file) => {
      if (attachmentUrls.value[file.fileId]) return

      try {
        const response = await api<{ url: string }>(
          `/api/v1/files/${file.fileId}/url`,
        )
        attachmentUrls.value[file.fileId] = response.url
      } catch {
        // Keep the placeholder; a later refresh will retry.
      }
    }),
  )
}

function isImage(url: string) {
  const clean = url.split('?')[0] ?? ''
  const extension = clean.split('.').pop()?.toLowerCase() ?? ''
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'heic'].includes(extension)
}

function statusClass(status: EmergencyStatus) {
  return `status status--${status.toLowerCase()}`
}

function openPreview(file: { fileId: number; url: string }) {
  const url = attachmentUrls.value[file.fileId]
  if (!url) return

  preview.value = {
    url,
    type: isImage(file.url) ? 'image' : 'video',
    name: `Attachment #${file.fileId}`,
  }
  previewVisible.value = true
}

async function changeStatus(next: EmergencyStatus) {
  updating.value = true
  try {
    const raw = await api<RawEmergency>(`/api/v1/emergencies/${id.value}/status`, {
      method: 'PATCH',
      body: { status: next },
    })
    emergency.value = normalizeEmergency(raw)
    await loadAttachments()
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

    <div v-if="loading" class="muted">Loading…</div>

    <template v-else-if="emergency">
      <div class="detail-header">
        <div>
          <div class="flex-between" style="justify-content: flex-start; gap: 10px; margin-bottom: 10px">
            <span :class="statusClass(emergency.status)">{{ statusLabel(emergency.status) }}</span>
            <span class="muted-sm">#{{ emergency.id }}</span>
          </div>
          <h1 class="detail-title">{{ emergency.title }}</h1>
          <div class="detail-meta">
            <span><i class="pi pi-map-marker" />{{ location || '—' }}</span>
            <span>
              <i class="pi pi-clock" />
              {{ formatDateTime(emergency.alarmTimestamp || emergency.createdAt) }}
            </span>
          </div>
        </div>

        <div class="detail-actions">
          <Button
            v-for="next in transitions"
            :key="next"
            :label="statusLabel(next)"
            :severity="next === 'REJECTED' ? 'danger' : 'primary'"
            :outlined="next !== 'CONFIRMED'"
            :disabled="updating"
            @click="confirmStatus(next)"
          />
        </div>
      </div>

      <div class="detail-grid">
        <div>
          <div class="panel mb-2">
            <div class="panel-header">
              <h2 class="panel-title">Description</h2>
            </div>
            <div class="panel-body">
              <p style="margin: 0; line-height: 1.65">
                {{ emergency.description || 'No description provided.' }}
              </p>
            </div>
          </div>

          <div class="panel mb-2">
            <div class="panel-header">
              <h2 class="panel-title">Location</h2>
            </div>
            <div class="panel-body">
              <dl class="kv">
                <dt>Address</dt>
                <dd>{{ location || '—' }}</dd>
                <dt>City</dt>
                <dd>{{ emergency.city || '—' }}</dd>
                <dt>Street</dt>
                <dd>{{ emergency.street || '—' }}</dd>
                <dt>Country</dt>
                <dd>{{ emergency.country || '—' }}</dd>
              </dl>
            </div>
          </div>

          <div class="panel">
            <div class="panel-header">
              <h2 class="panel-title">Status timeline</h2>
            </div>
            <div class="panel-body">
              <div v-if="emergency.status === 'REJECTED'" class="attachment-empty">
                This report was rejected by the rescue service.
              </div>
              <div v-else class="timeline">
                <div
                  v-for="(status, index) in flow"
                  :key="status"
                  class="timeline-item"
                  :class="{ done: index < currentIndex, current: index === currentIndex }"
                >
                  <span class="timeline-dot" />
                  <span class="timeline-label">{{ statusLabel(status) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div>
          <div class="panel mb-2">
            <div class="panel-header">
              <h2 class="panel-title">Attachments</h2>
              <span class="muted-sm">{{ emergency.files?.length ?? 0 }}</span>
            </div>
            <div class="panel-body">
              <div v-if="emergency.files?.length" class="attachment-grid">
                <button
                  v-for="file in emergency.files"
                  :key="file.fileId"
                  type="button"
                  class="attachment-tile"
                  @click="openPreview(file)"
                >
                  <img
                    v-if="isImage(file.url) && attachmentUrls[file.fileId]"
                    :src="attachmentUrls[file.fileId]"
                    alt=""
                  />
                  <div v-else class="attachment-fallback">
                    <i
                      :class="attachmentUrls[file.fileId] ? 'pi pi-play-circle' : 'pi pi-spin pi-spinner'"
                    />
                  </div>
                  <span class="attachment-label">#{{ file.fileId }}</span>
                </button>
              </div>
              <div v-else class="attachment-empty">No attachments uploaded.</div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-header">
              <h2 class="panel-title">Report</h2>
            </div>
            <div class="panel-body">
              <dl class="kv">
                <dt>Report ID</dt>
                <dd>#{{ emergency.id }}</dd>
                <dt>Reporter</dt>
                <dd style="font-family: ui-monospace, monospace; font-size: 12px">
                  {{ emergency.userId }}
                </dd>
                <dt>Created</dt>
                <dd>{{ formatDateTime(emergency.createdAt) }}</dd>
              </dl>
            </div>
          </div>
        </div>
      </div>

      <div class="panel mt-2">
        <div class="panel-header">
          <h2 class="panel-title">Location map</h2>
        </div>
        <div class="panel-body">
          <LocationMap
            :city="emergency.city"
            :street="emergency.street"
            :country="emergency.country"
            :title="emergency.title"
          />
        </div>
      </div>
    </template>

    <div v-else class="muted">Emergency not found.</div>

    <Dialog
      v-model:visible="previewVisible"
      modal
      :header="preview?.name"
      :style="{ width: 'min(940px, 94vw)' }"
    >
      <img
        v-if="preview?.type === 'image'"
        :src="preview.url"
        class="preview-media"
        alt=""
      />
      <video
        v-else-if="preview?.type === 'video'"
        :src="preview.url"
        controls
        autoplay
        class="preview-media"
      />
    </Dialog>
  </div>
</template>
