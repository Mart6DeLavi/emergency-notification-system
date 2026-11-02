<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import { formatDateTime } from '~/utils/emergency'

type AlertType = 'FIRE' | 'ACCIDENT' | 'WEATHER' | 'EVACUATION' | 'OTHER'
type Audience = 'ALL' | 'LOCATION'
type PreviewChannel = 'PUSH' | 'EMAIL' | 'SMS'

interface BroadcastResult {
  broadcastId: string
  recipients: number
  deliveries: number
  filtered: number
  errors: number
}

interface BroadcastHistory {
  id: string
  alertType: AlertType
  title: string
  message: string
  audience: string
  city?: string | null
  street?: string | null
  radiusKm?: number | null
  recipients: number
  deliveries: number
  filtered: number
  errors: number
  status: string
  createdAt: string
}

const alertTypes: Array<{ value: AlertType; label: string; icon: string }> = [
  { value: 'FIRE', label: 'Fire', icon: 'pi pi-bolt' },
  { value: 'ACCIDENT', label: 'Accident', icon: 'pi pi-car' },
  { value: 'WEATHER', label: 'Weather', icon: 'pi pi-cloud' },
  { value: 'EVACUATION', label: 'Evacuation', icon: 'pi pi-directions' },
  { value: 'OTHER', label: 'Other', icon: 'pi pi-ellipsis-h' },
]

const api = useApi()
const toast = useToast()

const alertType = ref<AlertType>('FIRE')
const title = ref('')
const message = ref('')
const audience = ref<Audience>('ALL')
const city = ref('')
const street = ref('')
const radiusKm = ref(2.5)
const previewChannel = ref<PreviewChannel>('PUSH')

const sending = ref(false)
const result = ref<BroadcastResult | null>(null)
const history = ref<BroadcastHistory[]>([])
const historyLoading = ref(false)
const historyEl = ref<HTMLElement | null>(null)

const messageMax = 500

const selectedType = computed(
  () => alertTypes.find((item) => item.value === alertType.value) ?? alertTypes[4]!,
)

const totalAudience = computed(() => {
  if (!result.value) return 0
  return result.value.recipients + result.value.filtered
})

const deliveredPercent = computed(() => {
  if (!result.value || totalAudience.value === 0) return 0
  return Math.round((result.value.recipients / totalAudience.value) * 100)
})

const areaLabel = computed(() =>
  audience.value === 'ALL'
    ? 'All users'
    : [street.value, city.value].filter(Boolean).join(', ') || 'Choose an area',
)

function typeMeta(type: string) {
  return alertTypes.find((item) => item.value === type) ?? alertTypes[4]!
}

function audienceText(item: BroadcastHistory) {
  if (item.audience === 'LOCATION') {
    const area = [item.street, item.city].filter(Boolean).join(', ')
    const radius = item.radiusKm ? ` · ${item.radiusKm} km` : ''
    return area ? `${area}${radius}` : 'By location'
  }
  return 'All users'
}

function validate(): boolean {
  if (!title.value.trim()) {
    toast.add({ severity: 'warn', summary: 'Enter a title', life: 2500 })
    return false
  }
  if (!message.value.trim()) {
    toast.add({ severity: 'warn', summary: 'Enter a message', life: 2500 })
    return false
  }
  if (audience.value === 'LOCATION' && !city.value.trim()) {
    toast.add({ severity: 'warn', summary: 'Enter a city for the area target', life: 2500 })
    return false
  }
  return true
}

async function send() {
  if (!validate()) return

  sending.value = true
  result.value = null
  try {
    const response = await api<BroadcastResult>('/api/v1/notifications/broadcast', {
      method: 'POST',
      body: {
        title: title.value.trim(),
        message: message.value.trim(),
        alertType: alertType.value,
        audience: audience.value,
        city: audience.value === 'LOCATION' ? city.value.trim() : null,
        street: audience.value === 'LOCATION' && street.value.trim() ? street.value.trim() : null,
        radiusKm: audience.value === 'LOCATION' ? radiusKm.value : null,
      },
    })
    result.value = response
    toast.add({
      severity: 'success',
      summary: 'Alert queued',
      detail: `${response.recipients} recipient(s), ${response.deliveries} delivery job(s)`,
      life: 4000,
    })
    await loadHistory()
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Broadcast failed',
      detail: (e as Error).message,
      life: 5000,
    })
  } finally {
    sending.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    history.value = await api<BroadcastHistory[]>('/api/v1/notifications/broadcasts')
  } catch {
    history.value = []
  } finally {
    historyLoading.value = false
  }
}

function scrollToHistory() {
  historyEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(loadHistory)
</script>

<template>
  <div class="notify-page">
    <header class="notify-head">
      <div>
        <span class="notify-eyebrow">Operations</span>
        <h1>Notify users</h1>
        <p>Send an alert from the rescue service to every user or to a specific area.</p>
      </div>
      <div class="notify-head-actions">
        <span class="live-pill">Live</span>
        <button type="button" class="notify-ghost-btn" @click="scrollToHistory">
          <i class="pi pi-history" />
          Broadcast history
        </button>
      </div>
    </header>

    <div class="notify-grid">
      <section class="notify-card notify-compose">
        <div class="notify-card-head">
          <h2><i class="pi pi-send" /> Compose alert</h2>
        </div>

        <div class="notify-field">
          <span class="notify-label">Alert type</span>
          <div class="alert-type-grid">
            <button
              v-for="item in alertTypes"
              :key="item.value"
              type="button"
              class="alert-type"
              :class="[`alert-type--${item.value.toLowerCase()}`, { active: alertType === item.value }]"
              @click="alertType = item.value"
            >
              <i :class="item.icon" />
              <span>{{ item.label }}</span>
            </button>
          </div>
        </div>

        <div class="notify-field">
          <label class="notify-label" for="notify-title">Title</label>
          <InputText
            id="notify-title"
            v-model="title"
            class="notify-input"
            maxlength="120"
            placeholder="Fire in the city center"
          />
        </div>

        <div class="notify-field">
          <label class="notify-label" for="notify-message">Message</label>
          <Textarea
            id="notify-message"
            v-model="message"
            rows="5"
            class="notify-input"
            :maxlength="messageMax"
            placeholder="A fire has been reported at Stanisława Mikołajczyka 14. Avoid the area and follow the services' instructions."
          />
          <span class="notify-counter">{{ message.length }} / {{ messageMax }}</span>
        </div>

        <div class="notify-field">
          <span class="notify-label">Audience</span>
          <div class="notify-segmented">
            <button
              type="button"
              :class="{ active: audience === 'ALL' }"
              @click="audience = 'ALL'"
            >
              All users
            </button>
            <button
              type="button"
              :class="{ active: audience === 'LOCATION' }"
              @click="audience = 'LOCATION'"
            >
              By location
            </button>
            <button type="button" disabled title="Role targeting is not available yet">
              By role
            </button>
            <button type="button" disabled title="Custom groups are not available yet">
              Custom group
            </button>
          </div>
        </div>

        <div v-if="audience === 'LOCATION'" class="notify-area">
          <div class="notify-field-row">
            <div class="notify-field">
              <label class="notify-label" for="notify-city">City</label>
              <InputText id="notify-city" v-model="city" class="notify-input" placeholder="Opole" />
            </div>
            <div class="notify-field">
              <label class="notify-label" for="notify-street">Street (optional)</label>
              <InputText
                id="notify-street"
                v-model="street"
                class="notify-input"
                placeholder="Mikołajczyka"
              />
            </div>
          </div>

          <div class="notify-radius">
            <div class="notify-radius-head">
              <span class="notify-label">Radius</span>
              <strong>{{ radiusKm.toFixed(1) }} km</strong>
            </div>
            <Slider
              v-model="radiusKm"
              :min="0.5"
              :max="10"
              :step="0.5"
              class="notify-slider"
            />
            <div class="notify-radius-scale">
              <span>0.5 km</span>
              <span>10 km</span>
            </div>
          </div>
        </div>

        <button type="button" class="notify-send" :disabled="sending" @click="send">
          <i :class="sending ? 'pi pi-spin pi-spinner' : 'pi pi-send'" />
          {{ sending ? 'Sending…' : 'Send alert' }}
        </button>
      </section>

      <section class="notify-card notify-preview">
        <div class="notify-card-head">
          <h2><i class="pi pi-eye" /> Preview</h2>
          <div class="preview-tabs">
            <button
              v-for="channel in (['PUSH', 'EMAIL', 'SMS'] as PreviewChannel[])"
              :key="channel"
              type="button"
              :class="{ active: previewChannel === channel }"
              @click="previewChannel = channel"
            >
              {{ channel === 'PUSH' ? 'Push' : channel === 'EMAIL' ? 'Email' : 'SMS' }}
            </button>
          </div>
        </div>

        <div class="phone-frame">
          <div class="phone-statusbar">
            <span>9:41</span>
            <span class="phone-statusbar-icons">
              <i class="pi pi-wifi" />
              <i class="pi pi-mobile" />
              <i class="pi pi-battery" />
            </span>
          </div>
          <div class="phone-clock">
            <strong>20:42</strong>
            <span>Mon, 2 Nov</span>
          </div>

          <div v-if="previewChannel === 'PUSH'" class="phone-notification">
            <div class="phone-notification-icon"><i class="pi pi-shield" /></div>
            <div class="phone-notification-body">
              <div class="phone-notification-meta">
                <span>SENSA Rescue</span>
                <small>now</small>
              </div>
              <strong>{{ title || 'Alert title' }}</strong>
              <p>{{ message || 'Your message will appear here.' }}</p>
            </div>
          </div>

          <div v-else-if="previewChannel === 'EMAIL'" class="phone-email">
            <span class="phone-email-label">EMAIL · INBOX</span>
            <strong>{{ title || 'Alert title' }}</strong>
            <span class="phone-email-from">SENSA Rescue &lt;alerts@sensanotify.com&gt;</span>
            <p>{{ message || 'Your message will appear here.' }}</p>
            <span class="phone-email-footer">SENSA · Emergency notification system</span>
          </div>

          <div v-else class="phone-sms">
            <span class="phone-email-label">SMS · SENSA</span>
            <div class="phone-sms-bubble">
              <strong>{{ title || 'Alert title' }}</strong>
              <p>{{ message || 'Your message will appear here.' }}</p>
            </div>
          </div>

          <div class="phone-home-indicator" />
        </div>

        <p class="notify-preview-note">
          <i class="pi pi-info-circle" />
          The alert is stored for each recipient and delivered through their enabled channels.
        </p>
      </section>

      <aside class="notify-side">
        <section class="notify-card delivery-card">
          <div class="notify-card-head">
            <h2><i class="pi pi-chart-bar" /> Delivery overview</h2>
          </div>

          <div class="delivery-main">
            <div
              class="delivery-donut"
              :style="{ '--delivered': `${deliveredPercent}%` }"
            >
              <div class="delivery-donut-inner">
                <strong>{{ result ? result.recipients : '—' }}</strong>
                <span>Recipients</span>
              </div>
            </div>

            <div class="delivery-stats">
              <div>
                <span><i class="delivery-dot delivery-dot--success" /> Will receive</span>
                <strong>{{ result ? result.recipients : 0 }}</strong>
              </div>
              <div>
                <span><i class="delivery-dot delivery-dot--muted" /> Filtered</span>
                <strong>{{ result ? result.filtered : 0 }}</strong>
              </div>
              <div>
                <span><i class="delivery-dot delivery-dot--danger" /> Errors (est.)</span>
                <strong>{{ result ? result.errors : 0 }}</strong>
              </div>
            </div>
          </div>

          <div class="delivery-eta">
            <i class="pi pi-clock" />
            <span>Estimated delivery time</span>
            <strong>&lt; 10 seconds</strong>
          </div>
        </section>

        <section class="notify-card target-card">
          <div class="notify-card-head">
            <h2><i class="pi pi-map-marker" /> Target area</h2>
            <span class="target-area-label">{{ areaLabel }}</span>
          </div>
          <BroadcastAreaMap
            v-if="audience === 'LOCATION'"
            :city="city"
            :street="street"
            :radius-km="radiusKm"
          />
          <div v-else class="target-all">
            <i class="pi pi-globe" />
            <strong>All users</strong>
            <span>No area filter — every registered user will receive this alert.</span>
          </div>
        </section>
      </aside>
    </div>

    <section ref="historyEl" class="notify-card history-card">
      <div class="notify-card-head">
        <h2><i class="pi pi-history" /> Broadcast history</h2>
        <button type="button" class="notify-link" @click="loadHistory">
          Refresh
          <i class="pi pi-refresh" />
        </button>
      </div>

      <div v-if="historyLoading && !history.length" class="history-empty">
        <i class="pi pi-spin pi-spinner" /> Loading history…
      </div>
      <div v-else-if="!history.length" class="history-empty">
        <i class="pi pi-send" />
        <strong>No broadcasts yet</strong>
        <span>Sent alerts will appear here with delivery statistics.</span>
      </div>
      <div v-else class="history-table-wrap">
        <table class="history-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Title</th>
              <th>Type</th>
              <th>Audience</th>
              <th>Delivered</th>
              <th>Status</th>
              <th>Sent at</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in history" :key="item.id">
              <td class="history-id">#{{ history.length - index }}</td>
              <td class="history-title">
                <strong>{{ item.title }}</strong>
                <span>{{ item.message }}</span>
              </td>
              <td>
                <span class="type-chip" :class="`type-chip--${item.alertType?.toLowerCase() || 'other'}`">
                  <i :class="typeMeta(item.alertType).icon" />
                  {{ typeMeta(item.alertType).label }}
                </span>
              </td>
              <td class="history-audience">
                <i class="pi pi-users" />
                {{ audienceText(item) }}
              </td>
              <td class="history-delivered">
                {{ item.recipients }} / {{ item.recipients + item.filtered }}
              </td>
              <td>
                <span
                  class="history-status"
                  :class="item.status === 'DELIVERED' ? 'history-status--ok' : 'history-status--muted'"
                >
                  <i class="pi pi-check-circle" />
                  {{ item.status === 'DELIVERED' ? 'Delivered' : 'Filtered' }}
                </span>
              </td>
              <td class="history-date">{{ formatDateTime(item.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
