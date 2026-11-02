<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import type { Emergency, EmergencyStatus, FileResponse, RawEmergency } from '~/types'

const props = defineProps<{
  emergencyId: number
}>()

const emit = defineEmits<{
  (event: 'updated'): void
}>()

const api = useApi()
const toast = useToast()
const confirm = useConfirm()

const emergency = ref<Emergency | null>(null)
const loading = ref(true)
const updating = ref(false)
const attachmentUrls = ref<Record<number, string>>({})
const attachmentDetails = ref<Record<number, FileResponse>>({})
const previewVisible = ref(false)
const preview = ref<{ url: string; type: 'image' | 'video'; name: string } | null>(null)

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
  if (!emergency.value) loading.value = true

  try {
    const raw = await api<RawEmergency>(`/api/v1/emergencies/${props.emergencyId}`)
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
      const needUrl = !attachmentUrls.value[file.fileId]
      const needDetails = !attachmentDetails.value[file.fileId]
      if (!needUrl && !needDetails) return

      try {
        const [urlResponse, details] = await Promise.all([
          needUrl
            ? api<{ url: string }>(`/api/v1/files/${file.fileId}/url`)
            : Promise.resolve(null),
          needDetails
            ? api<FileResponse>(`/api/v1/files/${file.fileId}`)
            : Promise.resolve(null),
        ])
        if (urlResponse) attachmentUrls.value[file.fileId] = urlResponse.url
        if (details) attachmentDetails.value[file.fileId] = details
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

function moderationLabel(fileId: number) {
  const status = attachmentDetails.value[fileId]?.moderationStatus
  if (status === 'APPROVED') return 'AI approved'
  if (status === 'REJECTED') return 'AI rejected'
  if (status === 'PENDING') return 'AI pending'
  return 'AI check unavailable'
}

function moderationClass(fileId: number) {
  const status = attachmentDetails.value[fileId]?.moderationStatus
  if (status === 'APPROVED') return 'ai-pill--approved'
  if (status === 'REJECTED') return 'ai-pill--rejected'
  if (status === 'PENDING') return 'ai-pill--pending'
  return 'ai-pill--unknown'
}

function confidenceText(fileId: number) {
  const value = attachmentDetails.value[fileId]?.moderationConfidence
  return typeof value === 'number' ? `${value.toFixed(1)}%` : '—'
}

function confidenceWidth(fileId: number) {
  const value = attachmentDetails.value[fileId]?.moderationConfidence ?? 0
  return `${Math.min(100, Math.max(0, value))}%`
}

function attachmentName(fileId: number) {
  return attachmentDetails.value[fileId]?.filename || `Attachment #${fileId}`
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
    const raw = await api<RawEmergency>(`/api/v1/emergencies/${props.emergencyId}/status`, {
      method: 'PATCH',
      body: { status: next },
    })
    emergency.value = normalizeEmergency(raw)
    await loadAttachments()
    emit('updated')
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

watch(
  () => props.emergencyId,
  () => {
    attachmentUrls.value = {}
    emergency.value = null
    load()
  },
  { immediate: true },
)

let timer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  timer = setInterval(load, 20000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="detail-pane">
    <div v-if="loading" class="state">
      <i class="pi pi-spin pi-spinner" />
      <span>Loading emergency…</span>
    </div>

    <template v-else-if="emergency">
      <div class="detail-toolbar">
        <span class="muted-sm">Report #{{ emergency.id }}</span>
        <Button
          icon="pi pi-refresh"
          text
          severity="secondary"
          aria-label="Refresh"
          :loading="loading"
          @click="load"
        />
      </div>

      <div class="detail-header">
        <div>
          <div class="detail-status-line">
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
        <div class="detail-main">
          <section class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Overview</h2>
              <span class="muted-sm">Incident report</span>
            </div>
            <p class="detail-description">
              {{ emergency.description || 'No description provided.' }}
            </p>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Status timeline</h2>
            </div>
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
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Location map</h2>
            </div>
            <LocationMap
              :city="emergency.city"
              :street="emergency.street"
              :country="emergency.country"
              :title="emergency.title"
            />
          </section>
        </div>

        <aside class="detail-side">
          <section class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Attachments</h2>
              <span class="muted-sm">{{ emergency.files?.length ?? 0 }}</span>
            </div>
            <div v-if="emergency.files?.length" class="attachment-grid">
              <article
                v-for="file in emergency.files"
                :key="file.fileId"
                class="attachment-card"
              >
                <button type="button" class="attachment-media" @click="openPreview(file)">
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
                </button>
                <div class="attachment-info">
                  <div class="attachment-name" :title="attachmentName(file.fileId)">
                    {{ attachmentName(file.fileId) }}
                  </div>
                  <div class="attachment-ai-row">
                    <span class="ai-pill" :class="moderationClass(file.fileId)">
                      {{ moderationLabel(file.fileId) }}
                    </span>
                    <span class="attachment-confidence">{{ confidenceText(file.fileId) }}</span>
                  </div>
                  <div class="confidence-bar" aria-hidden="true">
                    <span :style="{ width: confidenceWidth(file.fileId) }" />
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="attachment-empty">No attachments uploaded.</div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Location</h2>
            </div>
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
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2 class="panel-title">Report</h2>
            </div>
            <dl class="kv">
              <dt>Report ID</dt>
              <dd>#{{ emergency.id }}</dd>
              <dt>Reporter</dt>
              <dd class="mono">{{ emergency.userId }}</dd>
              <dt>Created</dt>
              <dd>{{ formatDateTime(emergency.createdAt) }}</dd>
            </dl>
          </section>
        </aside>
      </div>
    </template>

    <div v-else class="state">
      <i class="pi pi-exclamation-circle" />
      <strong>Emergency not found</strong>
    </div>

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
