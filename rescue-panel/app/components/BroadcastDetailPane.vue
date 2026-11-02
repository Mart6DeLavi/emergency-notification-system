<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import type { BroadcastHistory } from '~/types'

const props = defineProps<{
  broadcast: BroadcastHistory
}>()

const toast = useToast()

const ALERT_META: Record<string, { label: string; icon: string }> = {
  FIRE: { label: 'Fire', icon: 'pi pi-bolt' },
  ACCIDENT: { label: 'Accident', icon: 'pi pi-car' },
  WEATHER: { label: 'Weather', icon: 'pi pi-cloud' },
  EVACUATION: { label: 'Evacuation', icon: 'pi pi-directions' },
  OTHER: { label: 'Other', icon: 'pi pi-ellipsis-h' },
}

const meta = computed(() => ALERT_META[props.broadcast.alertType] ?? ALERT_META.OTHER!)
const isLocation = computed(() => props.broadcast.audience === 'LOCATION')
const delivered = computed(() => props.broadcast.status === 'DELIVERED')

const statusClass = computed(() =>
  delivered.value ? 'status status--broadcast' : 'status status--rejected',
)

const statusText = computed(() => (delivered.value ? 'Delivered' : 'Filtered'))

const locationText = computed(() =>
  [props.broadcast.street, props.broadcast.city].filter(Boolean).join(', '),
)

const audienceText = computed(() =>
  isLocation.value
    ? locationText.value || 'By location'
    : 'All users',
)

const radiusText = computed(() => {
  const value = props.broadcast.radiusKm
  return typeof value === 'number' ? `${value.toFixed(1)} km` : '—'
})

function copyText(value: string, label: string) {
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    navigator.clipboard.writeText(value)
  }
  toast.add({ severity: 'success', summary: `${label} copied`, life: 1500 })
}
</script>

<template>
  <div class="detail-pane broadcast-detail">
    <div class="detail-toolbar">
      <span class="muted-sm">Broadcast · #{{ broadcast.id.slice(-6) }}</span>
      <span :class="statusClass">{{ statusText }}</span>
    </div>

    <div class="detail-header">
      <div>
        <div class="detail-status-line">
          <span class="broadcast-type">
            <i :class="meta.icon" />
            {{ meta.label }}
          </span>
          <span class="muted-sm">{{ audienceText }}</span>
        </div>
        <h1 class="detail-title">{{ broadcast.title }}</h1>
        <div class="detail-meta">
          <span><i class="pi pi-clock" />{{ formatDateTime(broadcast.createdAt) }}</span>
          <span><i class="pi pi-users" />{{ broadcast.recipients }} recipients</span>
        </div>
      </div>
    </div>

    <div class="detail-facts grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <article class="fact-card">
        <span class="fact-icon fact-icon--success"><i class="pi pi-users" /></span>
        <div class="fact-body">
          <span class="fact-label">Recipients</span>
          <strong class="fact-value">{{ broadcast.recipients }}</strong>
        </div>
      </article>

      <article class="fact-card">
        <span class="fact-icon fact-icon--info"><i class="pi pi-send" /></span>
        <div class="fact-body">
          <span class="fact-label">Deliveries</span>
          <strong class="fact-value">{{ broadcast.deliveries }}</strong>
        </div>
      </article>

      <article class="fact-card">
        <span class="fact-icon fact-icon--warning"><i class="pi pi-filter" /></span>
        <div class="fact-body">
          <span class="fact-label">Filtered</span>
          <strong class="fact-value">{{ broadcast.filtered }}</strong>
        </div>
      </article>

      <article class="fact-card">
        <span class="fact-icon fact-icon--danger"><i class="pi pi-exclamation-circle" /></span>
        <div class="fact-body">
          <span class="fact-label">Errors</span>
          <strong class="fact-value">{{ broadcast.errors }}</strong>
        </div>
      </article>
    </div>

    <div class="detail-grid grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1.6fr)_minmax(300px,0.9fr)]">
      <div class="detail-main flex min-w-0 flex-col gap-4">
        <section class="panel">
          <div class="panel-head">
            <h2 class="panel-title"><i class="pi pi-envelope" />Message</h2>
            <span class="muted-sm">Sent to users</span>
          </div>
          <p class="broadcast-message-text">{{ broadcast.message }}</p>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h2 class="panel-title"><i class="pi pi-map-marker" />Target area</h2>
            <span class="muted-sm">{{ isLocation ? 'Location' : 'All users' }}</span>
          </div>

          <template v-if="isLocation">
            <div class="broadcast-map">
              <BroadcastAreaMap
                :city="broadcast.city"
                :street="broadcast.street"
                :radius-km="broadcast.radiusKm ?? 1"
              />
            </div>
            <div class="broadcast-area-meta">
              <span><i class="pi pi-map-marker" />{{ locationText || '—' }}</span>
              <span><i class="pi pi-circle" />{{ radiusText }}</span>
            </div>
          </template>

          <div v-else class="audience-empty">
            <i class="pi pi-globe" />
            <strong>All users</strong>
            <span>This alert was sent to every registered user.</span>
          </div>
        </section>
      </div>

      <aside class="detail-side flex min-w-0 flex-col gap-4">
        <section class="panel">
          <div class="panel-head">
            <h2 class="panel-title"><i class="pi pi-chart-bar" />Delivery summary</h2>
          </div>
          <dl class="kv">
            <dt>Status</dt>
            <dd><span :class="statusClass">{{ statusText }}</span></dd>
            <dt>Recipients</dt>
            <dd>{{ broadcast.recipients }}</dd>
            <dt>Deliveries</dt>
            <dd>{{ broadcast.deliveries }}</dd>
            <dt>Filtered</dt>
            <dd>{{ broadcast.filtered }}</dd>
            <dt>Errors</dt>
            <dd>{{ broadcast.errors }}</dd>
          </dl>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h2 class="panel-title"><i class="pi pi-users" />Audience</h2>
          </div>
          <dl class="kv">
            <dt>Audience</dt>
            <dd>{{ isLocation ? 'By location' : 'All users' }}</dd>
            <dt>City</dt>
            <dd>{{ broadcast.city || '—' }}</dd>
            <dt>Street</dt>
            <dd>{{ broadcast.street || '—' }}</dd>
            <dt>Radius</dt>
            <dd>{{ isLocation ? radiusText : '—' }}</dd>
          </dl>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h2 class="panel-title"><i class="pi pi-info-circle" />Alert details</h2>
          </div>
          <dl class="kv">
            <dt>Alert ID</dt>
            <dd class="mono">
              {{ broadcast.id }}
              <button type="button" class="copy-btn" title="Copy alert ID" @click="copyText(broadcast.id, 'Alert ID')">
                <i class="pi pi-copy" />
              </button>
            </dd>
            <dt>Type</dt>
            <dd>{{ meta.label }}</dd>
            <dt>Sent at</dt>
            <dd>{{ formatDateTime(broadcast.createdAt) }}</dd>
          </dl>
        </section>
      </aside>
    </div>
  </div>
</template>
