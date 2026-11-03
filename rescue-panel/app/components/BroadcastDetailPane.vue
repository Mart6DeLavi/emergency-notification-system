<script setup lang="ts">
import { useToast } from 'primevue/usetoast'
import type { BroadcastHistory } from '~/types'

const props = defineProps<{
  broadcast: BroadcastHistory
}>()

const emit = defineEmits<{ close: [] }>()

const toast = useToast()

const ALERT_META: Record<string, { label: string; icon: string; tone: string }> = {
  FIRE: { label: 'Fire', icon: 'pi pi-bolt', tone: 'fire' },
  ACCIDENT: { label: 'Accident', icon: 'pi pi-car', tone: 'accident' },
  WEATHER: { label: 'Weather', icon: 'pi pi-cloud', tone: 'weather' },
  EVACUATION: { label: 'Evacuation', icon: 'pi pi-directions', tone: 'evacuation' },
  OTHER: { label: 'Other', icon: 'pi pi-ellipsis-h', tone: 'other' },
}

const meta = computed(() => ALERT_META[props.broadcast.alertType] ?? ALERT_META.OTHER!)
const isLocation = computed(() => props.broadcast.audience === 'LOCATION')
const delivered = computed(() => props.broadcast.status === 'DELIVERED')
const shortId = computed(() => props.broadcast.id.slice(-6))
const statusText = computed(() => (delivered.value ? 'Delivered' : 'Filtered'))

const locationText = computed(() =>
  [props.broadcast.street, props.broadcast.city].filter(Boolean).join(', '),
)

const audienceText = computed(() =>
  isLocation.value ? locationText.value || 'By location' : 'All users',
)

const radiusText = computed(() => {
  const value = props.broadcast.radiusKm
  return typeof value === 'number' ? `${value.toFixed(1)} km` : '—'
})

const deliveredPercent = computed(() => {
  const total = props.broadcast.deliveries + props.broadcast.filtered + props.broadcast.errors
  if (total <= 0) return 0
  return Math.round((props.broadcast.deliveries / total) * 100)
})

const donutStyle = computed<Record<string, string>>(() => ({
  '--delivered': `${deliveredPercent.value}%`,
}))

const totalTime = computed(() => {
  const offsets = props.broadcast.deliveryTimeline ?? []
  if (!offsets.length) return '0.0'
  const last = Math.max(...offsets)
  if (last < 100) return '<0.1'
  return (last / 1000).toFixed(1)
})

const peakRate = computed(() => {
  const offsets = props.broadcast.deliveryTimeline ?? []
  if (!offsets.length || !props.broadcast.deliveries) return '—'
  const span = Math.max(Math.max(...offsets), 0)
  if (span < 100) return '—'
  const bucket = Math.max(100, Math.ceil(span / 10))
  const buckets = new Map<number, number>()
  for (const offset of offsets) {
    const key = Math.floor(offset / bucket)
    buckets.set(key, (buckets.get(key) ?? 0) + 1)
  }
  const peak = Math.max(...buckets.values(), 0)
  return `${Math.round(peak / (bucket / 1000))}/s`
})

const avgRate = computed(() => {
  const offsets = props.broadcast.deliveryTimeline ?? []
  if (!offsets.length || !props.broadcast.deliveries) return '—'
  const span = Math.max(...offsets)
  if (span < 100) return '—'
  const elapsed = span / 1000
  return `${Math.round(props.broadcast.deliveries / elapsed)}/s`
})

interface ChartPoint {
  x: number
  y: number
}

const chart = computed(() => {
  const total = Math.max(props.broadcast.deliveries, 0)
  const offsets = [...(props.broadcast.deliveryTimeline ?? [])]
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b)
  const elapsed = offsets.length ? Math.max(offsets[offsets.length - 1]!, 0) : 0
  const maxX = Math.max(elapsed, 1000)
  const maxY = Math.max(total, 1)
  const width = 640
  const height = 220
  const padLeft = 48
  const padRight = 18
  const padTop = 16
  const padBottom = 30
  const plotWidth = width - padLeft - padRight
  const plotHeight = height - padTop - padBottom

  const points: ChartPoint[] = [{ x: 0, y: 0 }]
  if (offsets.length) {
    offsets.forEach((offset, index) => {
      const x = Math.min(offset, maxX)
      points.push({ x, y: index })
      points.push({ x, y: index + 1 })
    })
  } else if (total > 0) {
    points.push({ x: 0, y: total })
  }
  points.push({ x: maxX, y: total })

  const xScale = (value: number) => padLeft + (value / maxX) * plotWidth
  const yScale = (value: number) => padTop + plotHeight - (value / maxY) * plotHeight

  const line = points
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${xScale(point.x).toFixed(2)} ${yScale(point.y).toFixed(2)}`)
    .join(' ')
  const area = `${line} L ${xScale(maxX).toFixed(2)} ${yScale(0).toFixed(2)} L ${xScale(0).toFixed(2)} ${yScale(0).toFixed(2)} Z`

  const xTicks = Array.from({ length: 5 }, (_, index) => {
    const value = (maxX * index) / 4
    return {
      x: xScale(value),
      label: maxX < 10_000 ? `${(value / 1000).toFixed(1)}s` : `${Math.round(value / 1000)}s`,
    }
  })

  const yTicks = Array.from({ length: 5 }, (_, index) => {
    const value = Math.round((maxY * index) / 4)
    return { y: yScale(value), label: String(value) }
  })

  return {
    width,
    height,
    padLeft,
    padRight,
    padTop,
    padBottom,
    points,
    line,
    area,
    xScale,
    yScale,
    xTicks,
    yTicks,
    baseline: yScale(0),
    left: xScale(0),
    gradientId: `delivery-chart-${props.broadcast.id.slice(-6)}`,
  }
})

function copyText(value: string, label: string) {
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    navigator.clipboard.writeText(value)
  }
  toast.add({ severity: 'success', summary: `${label} copied`, life: 1500 })
}
</script>

<template>
  <div class="detail-pane bd-pane">
    <header class="bd-head">
      <div class="bd-head-main">
        <span class="bd-head-icon" :class="`bd-head-icon--${meta.tone}`">
          <i :class="meta.icon" />
        </span>
        <div class="bd-head-text">
          <div class="bd-eyebrow">Broadcast · #{{ shortId }}</div>
          <h1 class="bd-title">{{ broadcast.title }}</h1>
          <div class="bd-meta">
            <span><i class="pi pi-map-marker" />{{ audienceText }}</span>
            <span><i class="pi pi-clock" />{{ formatDateTime(broadcast.createdAt) }}</span>
            <span><i class="pi pi-users" />{{ broadcast.recipients }} recipients</span>
          </div>
        </div>
      </div>

      <div class="bd-head-actions">
        <span class="bd-status" :class="{ 'bd-status--ok': delivered, 'bd-status--warn': !delivered }">
          <i class="pi pi-circle-fill" />
          {{ statusText }}
        </span>
        <button
          type="button"
          class="bd-icon-btn"
          title="Copy alert ID"
          @click="copyText(broadcast.id, 'Alert ID')"
        >
          <i class="pi pi-ellipsis-h" />
        </button>
        <button type="button" class="bd-icon-btn" title="Close" @click="emit('close')">
          <i class="pi pi-times" />
        </button>
      </div>
    </header>

    <section class="bd-kpis">
      <article class="bd-kpi">
        <span class="bd-kpi-icon bd-kpi-icon--green"><i class="pi pi-users" /></span>
        <div>
          <span class="bd-kpi-label">Recipients</span>
          <strong class="bd-kpi-value">{{ broadcast.recipients }}</strong>
          <small>users reached</small>
        </div>
      </article>

      <article class="bd-kpi">
        <span class="bd-kpi-icon bd-kpi-icon--blue"><i class="pi pi-send" /></span>
        <div>
          <span class="bd-kpi-label">Deliveries</span>
          <strong class="bd-kpi-value">{{ broadcast.deliveries }}</strong>
          <small>jobs queued</small>
        </div>
      </article>

      <article class="bd-kpi">
        <span class="bd-kpi-icon bd-kpi-icon--amber"><i class="pi pi-filter" /></span>
        <div>
          <span class="bd-kpi-label">Filtered</span>
          <strong class="bd-kpi-value">{{ broadcast.filtered }}</strong>
          <small>without channels</small>
        </div>
      </article>

      <article class="bd-kpi">
        <span class="bd-kpi-icon bd-kpi-icon--red"><i class="pi pi-exclamation-circle" /></span>
        <div>
          <span class="bd-kpi-label">Errors</span>
          <strong class="bd-kpi-value">{{ broadcast.errors }}</strong>
          <small>failed jobs</small>
        </div>
      </article>
    </section>

    <div class="bd-grid bd-grid--top">
      <section class="bd-card bd-message">
        <div class="bd-card-head">
          <h2><i class="pi pi-envelope" />Message</h2>
          <span>Sent to users</span>
        </div>
        <p>{{ broadcast.message }}</p>
      </section>

      <section class="bd-card bd-summary">
        <div class="bd-card-head">
          <h2><i class="pi pi-chart-bar" />Delivery summary</h2>
        </div>

        <div class="bd-summary-body">
          <div class="bd-donut" :style="donutStyle">
            <div class="bd-donut-inner">
              <strong>{{ deliveredPercent }}%</strong>
              <span>Delivered</span>
            </div>
          </div>

          <div class="bd-summary-stats">
            <div>
              <span><i class="bd-dot bd-dot--green" />Delivered</span>
              <strong>{{ broadcast.deliveries }} ({{ deliveredPercent }}%)</strong>
            </div>
            <div>
              <span><i class="bd-dot bd-dot--grey" />Filtered</span>
              <strong>{{ broadcast.filtered }}</strong>
            </div>
            <div>
              <span><i class="bd-dot bd-dot--red" />Errors</span>
              <strong>{{ broadcast.errors }}</strong>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="bd-grid bd-grid--middle">
      <section class="bd-card bd-target">
        <div class="bd-card-head">
          <h2><i class="pi pi-map-marker" />Target area</h2>
          <span>{{ isLocation ? `Radius: ${radiusText}` : 'All users' }}</span>
        </div>

        <template v-if="isLocation">
          <div class="bd-map">
            <BroadcastAreaMap
              :city="broadcast.city"
              :street="broadcast.street"
              :radius-km="broadcast.radiusKm ?? 1"
              dark
            />
          </div>
        </template>

        <div v-else class="bd-all-users">
          <i class="pi pi-globe" />
          <strong>All users</strong>
          <span>This alert was sent to every registered user.</span>
        </div>
      </section>

      <section class="bd-card bd-audience">
        <div class="bd-card-head">
          <h2><i class="pi pi-users" />Audience</h2>
        </div>
        <dl class="bd-kv">
          <dt>Audience</dt>
          <dd>{{ isLocation ? 'All users in area' : 'All users' }}</dd>
          <dt>City</dt>
          <dd>{{ broadcast.city || '—' }}</dd>
          <dt>Street</dt>
          <dd>{{ broadcast.street || '—' }}</dd>
          <dt>Radius</dt>
          <dd>{{ isLocation ? radiusText : '—' }}</dd>
        </dl>
      </section>
    </div>

    <div class="bd-grid bd-grid--bottom">
      <section class="bd-card bd-chart-card">
        <div class="bd-card-head">
          <h2><i class="pi pi-chart-line" />Delivery over time</h2>
          <span>Queued jobs</span>
        </div>

        <div class="bd-chart-wrap">
          <svg
            class="bd-chart"
            :viewBox="`0 0 ${chart.width} ${chart.height}`"
            role="img"
            aria-label="Delivery timeline"
          >
            <defs>
              <linearGradient :id="chart.gradientId" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#34d399" stop-opacity="0.34" />
                <stop offset="100%" stop-color="#34d399" stop-opacity="0" />
              </linearGradient>
            </defs>

            <line
              v-for="tick in chart.yTicks"
              :key="`y-${tick.label}`"
              :x1="chart.padLeft"
              :x2="chart.width - chart.padRight"
              :y1="tick.y"
              :y2="tick.y"
              class="bd-chart-grid"
            />
            <line
              v-for="tick in chart.xTicks"
              :key="`x-${tick.label}`"
              :x1="tick.x"
              :x2="tick.x"
              :y1="chart.padTop"
              :y2="chart.baseline"
              class="bd-chart-grid bd-chart-grid--x"
            />

            <path :d="chart.area" :fill="`url(#${chart.gradientId})`" />
            <path :d="chart.line" class="bd-chart-line" />

            <circle
              v-for="(point, index) in chart.points"
              :key="`p-${index}`"
              :cx="chart.xScale(point.x)"
              :cy="chart.yScale(point.y)"
              r="2.4"
              class="bd-chart-dot"
            />

            <text
              v-for="tick in chart.yTicks"
              :key="`yl-${tick.label}`"
              :x="chart.padLeft - 10"
              :y="tick.y + 4"
              text-anchor="end"
              class="bd-chart-label"
            >{{ tick.label }}</text>
            <text
              v-for="tick in chart.xTicks"
              :key="`xl-${tick.label}`"
              :x="tick.x"
              :y="chart.height - 8"
              text-anchor="middle"
              class="bd-chart-label"
            >{{ tick.label }}</text>
          </svg>

          <div class="bd-chart-metrics">
            <div>
              <i class="pi pi-clock" />
              <span>Total time</span>
              <strong>{{ totalTime }}s</strong>
            </div>
            <div>
              <i class="pi pi-bolt" />
              <span>Peak rate</span>
              <strong>{{ peakRate }}</strong>
            </div>
            <div>
              <i class="pi pi-chart-bar" />
              <span>Avg. rate</span>
              <strong>{{ avgRate }}</strong>
            </div>
          </div>
        </div>
      </section>

      <section class="bd-card bd-alert-details">
        <div class="bd-card-head">
          <h2><i class="pi pi-info-circle" />Alert details</h2>
        </div>
        <dl class="bd-kv">
          <dt>Alert ID</dt>
          <dd class="bd-id">
            {{ broadcast.id }}
            <button type="button" class="bd-copy" title="Copy alert ID" @click="copyText(broadcast.id, 'Alert ID')">
              <i class="pi pi-copy" />
            </button>
          </dd>
          <dt>Type</dt>
          <dd>{{ meta.label }}</dd>
          <dt>Sent at</dt>
          <dd>{{ formatDateTime(broadcast.createdAt) }}</dd>
          <dt>Status</dt>
          <dd><span class="bd-status bd-status--ok">{{ statusText }}</span></dd>
        </dl>
      </section>
    </div>
  </div>
</template>
