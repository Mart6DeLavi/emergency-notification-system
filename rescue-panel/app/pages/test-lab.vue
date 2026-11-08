<script setup lang="ts">
import type { TestLabSummary, TestModeState, TestRun, TestRunPage, TestRunPoint } from '~/types'

const api = useApi()
const toast = useToast()

const mode = ref<TestModeState | null>(null)
const summary = ref<TestLabSummary | null>(null)
const runs = ref<TestRun[]>([])
const total = ref(0)
const selected = ref<TestRun | null>(null)
const running = ref(false)
const modePending = ref(false)
const historyLoading = ref(false)
const detailLoading = ref(false)
const deletingId = ref<string | null>(null)
const error = ref('')

const now = ref(Date.now())

const scenario = ref('read')
const vus = ref(10)
const durationSec = ref(60)

const scenarioFilter = ref('all')
const statusFilter = ref('all')

const scenarios = [
  { id: 'read', label: 'Read API', hint: 'alerts + my reports', icon: 'pi pi-search' },
  { id: 'broadcast', label: 'Broadcast', hint: 'operator alert, suppressed', icon: 'pi pi-megaphone' },
  { id: 'mixed', label: 'Mixed', hint: 'read + broadcast', icon: 'pi pi-shuffle' },
]

const presets = [
  { id: 'smoke', label: 'Smoke', vus: 10, duration: 60 },
  { id: 'load', label: 'Load', vus: 50, duration: 180 },
  { id: 'stress', label: 'Stress', vus: 100, duration: 300 },
]

const scenarioFilterOptions = [
  { value: 'all', label: 'All scenarios' },
  ...scenarios.map((item) => ({ value: item.id, label: item.label })),
]

const statusFilterOptions = [
  { value: 'all', label: 'All statuses' },
  { value: 'RUNNING', label: 'Running' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'STOPPED', label: 'Stopped' },
]

const metrics = [
  { id: 'vu', label: 'VU', field: 'vus' },
  { id: 'rps', label: 'RPS', field: 'rps' },
  { id: 'success', label: 'Success', field: 'success' },
  { id: 'p95', label: 'p95', field: 'p95' },
] as const

type MetricId = (typeof metrics)[number]['id']
type MetricField = (typeof metrics)[number]['field']

const metricColors: Record<MetricId, string> = {
  vu: '#8b5cf6',
  rps: '#38bdf8',
  success: '#34d399',
  p95: '#fbbf24',
}

interface ChartPoint {
  x: number
  y: number
  bandLeft: number
  bandWidth: number
  t: number
  value: number
}

interface ChartModel {
  hasData: boolean
  points: ChartPoint[]
  line: string
  area: string
  max: number
  rawMax: number
}

const activeMetric = ref<MetricId>('vu')
const metricTouched = ref(false)
const hoverIndex = ref<number | null>(null)

const menuRun = ref<TestRun | null>(null)
const menuPos = ref({ left: 0, top: 0 })
const confirmDeleteId = ref<string | null>(null)

let poll: ReturnType<typeof setInterval> | null = null
let clock: ReturnType<typeof setInterval> | null = null

const series = computed<TestRunPoint[]>(() => selected.value?.series ?? [])

const activeMetricMeta = computed(() => metrics.find((item) => item.id === activeMetric.value) ?? metrics[0]!)
const chartColor = computed(() => metricColors[activeMetric.value])

const filteredRuns = computed(() =>
  runs.value.filter((run) => {
    const scenarioOk = scenarioFilter.value === 'all' || run.scenario === scenarioFilter.value
    const statusOk = statusFilter.value === 'all' || run.status === statusFilter.value
    return scenarioOk && statusOk
  }),
)

const livePoint = computed<TestRunPoint | null>(() => series.value[series.value.length - 1] ?? null)
const liveVus = computed(() => Math.round(livePoint.value?.vus ?? selected.value?.vus ?? 0))
const liveRps = computed(() => livePoint.value?.rps ?? selected.value?.rps ?? 0)
const liveSuccess = computed(() => livePoint.value?.success ?? selected.value?.successRate ?? 0)
const liveP95 = computed(() => livePoint.value?.p95 ?? selected.value?.p95 ?? 0)

const modeExpiry = computed(() => {
  if (!mode.value?.enabled) return 'live traffic'
  if (!mode.value.expiresAt) return 'no expiry set'
  const ms = new Date(mode.value.expiresAt).getTime() - now.value
  if (ms <= 0) return 'expired'
  return `expires in ${Math.max(1, Math.ceil(ms / 60000))} min`
})

const elapsedSec = computed(() => {
  const run = selected.value
  if (!run) return 0
  const start = new Date(run.startedAt).getTime()
  if (Number.isNaN(start)) return 0
  const end = run.finishedAt
    ? new Date(run.finishedAt).getTime()
    : run.status === 'RUNNING'
      ? now.value
      : start
  if (Number.isNaN(end)) return 0
  return Math.max(0, Math.round((end - start) / 1000))
})

const totalSec = computed(() => selected.value?.durationSec ?? 0)

const progressPct = computed(() => {
  if (!selected.value) return 0
  const totalSeconds = totalSec.value
  if (!totalSeconds) return selected.value.status === 'RUNNING' ? 4 : 100
  return Math.min(100, Math.max(0, (elapsedSec.value / totalSeconds) * 100))
})

const elapsedLabel = computed(() => formatClock(elapsedSec.value))
const totalLabel = computed(() => formatClock(totalSec.value))

const chart = computed<ChartModel>(() => {
  const points = series.value
  if (!points.length) {
    return { hasData: false, points: [], line: '', area: '', max: 0, rawMax: 0 }
  }

  const field = activeMetricMeta.value.field as MetricField
  const values = points.map((point) => Number(point[field] ?? 0))
  const rawMax = Math.max(...values)
  const max = Math.max(1, rawMax) * 1.08
  const top = 8
  const bottom = 10
  const plot = 100 - top - bottom
  const count = values.length

  const chartPoints: ChartPoint[] = points.map((point, index) => {
    const value = values[index] ?? 0
    const x = count === 1 ? 50 : (index / (count - 1)) * 100
    const y = top + plot - (value / max) * plot
    return {
      x,
      y,
      bandLeft: (index / count) * 100,
      bandWidth: 100 / count + 0.15,
      t: Number(point.t ?? index),
      value,
    }
  })

  const line = chartPoints.map((point) => `${point.x.toFixed(2)},${point.y.toFixed(2)}`).join(' ')
  const first = chartPoints[0]!
  const last = chartPoints[chartPoints.length - 1]!

  return {
    hasData: true,
    points: chartPoints,
    line,
    area: `${first.x.toFixed(2)},100 ${line} ${last.x.toFixed(2)},100`,
    max,
    rawMax,
  }
})

const hoverPoint = computed<ChartPoint | null>(() => {
  if (hoverIndex.value == null) return null
  return chart.value.points[hoverIndex.value] ?? null
})

const tooltipBelow = computed(() => (hoverPoint.value?.y ?? 100) < 32)

const tooltipStyle = computed<Record<string, string>>(() => {
  const point = hoverPoint.value
  if (!point) return {}
  return {
    left: `${Math.min(86, Math.max(14, point.x))}%`,
    top: `${tooltipBelow.value ? point.y + 10 : point.y - 10}%`,
  }
})

const chartMaxLabel = computed(() => (chart.value.hasData ? `max ${formatMetric(chart.value.rawMax)}` : ''))

watch(
  () => selected.value?.id,
  () => {
    metricTouched.value = false
    hoverIndex.value = null
    applyDefaultMetric(selected.value)
  },
)

watch(
  () => series.value.length,
  () => {
    if (!metricTouched.value) applyDefaultMetric(selected.value)
  },
)

onMounted(async () => {
  clock = setInterval(() => {
    now.value = Date.now()
  }, 1000)
  document.addEventListener('click', closeMenu)
  window.addEventListener('scroll', closeMenu, true)
  window.addEventListener('resize', closeMenu)
  await Promise.all([loadMode(), loadRuns(), loadSummary()])
})

onBeforeUnmount(() => {
  stopPolling()
  if (clock) clearInterval(clock)
  document.removeEventListener('click', closeMenu)
  window.removeEventListener('scroll', closeMenu, true)
  window.removeEventListener('resize', closeMenu)
})

function normalizeRun(run: TestRun): TestRun {
  return { ...run, id: run.id || run._id || '' }
}

function applyDefaultMetric(run: TestRun | null) {
  const points = run?.series ?? []
  activeMetric.value = points.some((point) => point.vus != null) ? 'vu' : 'rps'
}

function chooseMetric(id: MetricId) {
  activeMetric.value = id
  metricTouched.value = true
  hoverIndex.value = null
}

async function loadMode() {
  try {
    mode.value = await api<TestModeState>('/api/v1/test-lab/settings')
  } catch (e: any) {
    error.value = e?.message || 'Failed to load test mode'
  }
}

async function loadRuns() {
  historyLoading.value = true
  try {
    const page = await api<TestRunPage>('/api/v1/test-lab/runs?limit=30')
    runs.value = (page.items ?? []).map(normalizeRun)
    total.value = page.total ?? runs.value.length
  } catch (e: any) {
    error.value = e?.message || 'Failed to load test history'
  } finally {
    historyLoading.value = false
  }
}

async function loadSummary() {
  try {
    summary.value = await api<TestLabSummary>('/api/v1/test-lab/summary')
  } catch (e: any) {
    if (!error.value) error.value = e?.message || 'Failed to load test summary'
  }
}

async function refreshRuns() {
  await Promise.all([loadRuns(), loadSummary()])
}

async function setMode(enabled: boolean) {
  if (!mode.value || modePending.value || mode.value.enabled === enabled) return
  modePending.value = true
  error.value = ''
  try {
    mode.value = await api<TestModeState>('/api/v1/test-lab/settings', {
      method: 'PATCH',
      body: { enabled, ttlSeconds: 1800 },
    })
    toast.add({
      severity: 'success',
      summary: enabled ? 'Test mode enabled' : 'Production mode restored',
      life: 2500,
    })
  } catch (e: any) {
    error.value = e?.message || 'Failed to change test mode'
  } finally {
    modePending.value = false
  }
}

function usePreset(preset: (typeof presets)[number]) {
  vus.value = preset.vus
  durationSec.value = preset.duration
}

function presetActive(preset: (typeof presets)[number]) {
  return vus.value === preset.vus && durationSec.value === preset.duration
}

async function startRun() {
  if (running.value) return
  running.value = true
  error.value = ''
  stopPolling()
  try {
    const created = normalizeRun(
      await api<TestRun>('/api/v1/test-lab/runs', {
        method: 'POST',
        body: {
          scenario: scenario.value,
          vus: vus.value,
          durationSec: durationSec.value,
          rampUpSec: 0,
        },
      }),
    )
    selected.value = created
    await Promise.all([loadMode(), loadRuns(), loadSummary()])
    startPolling(created.id)
    toast.add({ severity: 'info', summary: 'Load test started', life: 2500 })
  } catch (e: any) {
    error.value = e?.message || 'Failed to start load test'
  } finally {
    running.value = false
  }
}

function stopPolling() {
  if (poll) clearInterval(poll)
  poll = null
}

function startPolling(id: string) {
  stopPolling()
  poll = setInterval(async () => {
    try {
      const run = normalizeRun(await api<TestRun>(`/api/v1/test-lab/runs/${id}`))
      if (selected.value?.id === run.id) selected.value = run
      if (run.status !== 'RUNNING') {
        stopPolling()
        await Promise.all([loadRuns(), loadMode(), loadSummary()])
      }
    } catch {
      stopPolling()
    }
  }, 2000)
}

async function openRun(run: TestRun) {
  closeMenu()
  stopPolling()
  const normalized = normalizeRun(run)
  selected.value = normalized
  detailLoading.value = true
  error.value = ''
  try {
    const full = normalizeRun(await api<TestRun>(`/api/v1/test-lab/runs/${normalized.id}`))
    selected.value = full
    if (full.status === 'RUNNING') startPolling(full.id)
  } catch (e: any) {
    error.value = e?.message || 'Failed to open test run'
  } finally {
    detailLoading.value = false
  }
}

async function stopRun() {
  if (!selected.value || selected.value.status !== 'RUNNING') return
  error.value = ''
  try {
    selected.value = normalizeRun(
      await api<TestRun>(`/api/v1/test-lab/runs/${selected.value.id}/stop`, { method: 'POST' }),
    )
    stopPolling()
    await Promise.all([loadRuns(), loadMode(), loadSummary()])
    toast.add({ severity: 'info', summary: 'Run stopped', life: 2200 })
  } catch (e: any) {
    error.value = e?.message || 'Failed to stop run'
  }
}

async function deleteRun(run: TestRun) {
  if (deletingId.value) return
  deletingId.value = run.id
  error.value = ''
  try {
    await api(`/api/v1/test-lab/runs/${run.id}`, { method: 'DELETE' })
    if (selected.value?.id === run.id) {
      stopPolling()
      selected.value = null
    }
    closeMenu()
    await refreshRuns()
    toast.add({ severity: 'success', summary: 'Test run deleted', life: 2200 })
  } catch (e: any) {
    error.value = e?.message || 'Failed to delete run'
  } finally {
    deletingId.value = null
  }
}

function toggleMenu(run: TestRun, event: MouseEvent) {
  event.stopPropagation()
  if (menuRun.value?.id === run.id) {
    closeMenu()
    return
  }

  const target = event.currentTarget as HTMLElement | null
  if (!target) return
  confirmDeleteId.value = null
  menuRun.value = run
  const rect = target.getBoundingClientRect()
  const width = 168
  const left = Math.min(window.innerWidth - width - 8, Math.max(8, rect.right - width))
  const top = rect.bottom + 116 > window.innerHeight ? Math.max(8, rect.top - 108) : rect.bottom + 6
  menuPos.value = { left, top }
}

function closeMenu() {
  menuRun.value = null
  confirmDeleteId.value = null
}

function openFromMenu() {
  const run = menuRun.value
  if (run) void openRun(run)
}

async function deleteFromMenu() {
  const run = menuRun.value
  if (run) await deleteRun(run)
}

function formatMetric(value: number, id: MetricId = activeMetric.value) {
  switch (id) {
    case 'vu':
      return `${Math.round(value).toLocaleString('en-US')} VU`
    case 'rps':
      return `${value.toFixed(1)} req/s`
    case 'success':
      return `${value.toFixed(1)}%`
    case 'p95':
      return `${Math.round(value)} ms`
  }
}

function statusClass(status?: string) {
  return `tl-status tl-status--${(status || 'unknown').toLowerCase()}`
}

function scenarioLabel(id?: string) {
  const match = scenarios.find((item) => item.id === id)
  if (match) return match.label
  if (!id) return '—'
  return id.charAt(0).toUpperCase() + id.slice(1)
}

function formatDate(value?: string | null) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

function formatClock(value?: number) {
  const seconds = Math.max(0, Math.round(value ?? 0))
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

function formatDuration(value?: number | null) {
  if (value == null || Number.isNaN(value)) return '—'
  const seconds = Math.max(0, Math.round(value))
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return rest ? `${minutes}m ${rest}s` : `${minutes}m`
}

function num(value?: number | null) {
  return Number(value ?? 0).toLocaleString('en-US')
}

function pct(value?: number | null) {
  if (value == null || Number.isNaN(value)) return '—'
  return `${value.toFixed(1)}%`
}
</script>

<template>
  <div class="page test-lab">
    <header class="tl-head">
      <div class="tl-head-copy">
        <div class="tl-eyebrow">OPERATIONS</div>
        <h1>Test Lab</h1>
        <p>Run load tests against the internal gateway without touching live traffic.</p>
      </div>

      <div class="tl-head-actions">
        <div class="tl-mode-card" :class="{ 'is-test': mode?.enabled }">
          <span class="tl-mode-dot" />
          <div class="tl-mode-copy">
            <strong>{{ mode?.enabled ? 'Test mode' : 'Production mode' }}</strong>
            <small>{{ modeExpiry }}</small>
          </div>
        </div>
        <div class="tl-segmented" role="group" aria-label="Traffic mode">
          <button
            type="button"
            :class="{ active: !!mode && !mode.enabled }"
            :disabled="!mode || modePending"
            @click="setMode(false)"
          >
            Production
          </button>
          <button
            type="button"
            :class="{ active: mode?.enabled }"
            :disabled="!mode || modePending"
            @click="setMode(true)"
          >
            Test
          </button>
        </div>
      </div>
    </header>

    <div v-if="mode?.enabled" class="tl-banner">
      <i class="pi pi-exclamation-triangle" />
      <span>Notifications disabled · uploads disabled · test run <strong>{{ mode.runId }}</strong></span>
    </div>

    <div v-if="error" class="tl-error">
      <i class="pi pi-exclamation-circle" />
      <span>{{ error }}</span>
    </div>

    <div class="tl-kpi-grid">
      <article class="tl-kpi">
        <span class="tl-kpi-icon tl-kpi-icon--violet"><i class="pi pi-database" /></span>
        <div>
          <span class="tl-kpi-label">System capacity (est.)</span>
          <strong>≈{{ num(summary?.peakVus ?? 0) }} VU</strong>
          <small>peak tested virtual users</small>
        </div>
      </article>
      <article class="tl-kpi">
        <span class="tl-kpi-icon tl-kpi-icon--info"><i class="pi pi-bolt" /></span>
        <div>
          <span class="tl-kpi-label">Max RPS</span>
          <strong>{{ (summary?.peakRps ?? 0).toFixed(0) }} req/s</strong>
          <small>peak observed throughput</small>
        </div>
      </article>
      <article class="tl-kpi">
        <span class="tl-kpi-icon tl-kpi-icon--success"><i class="pi pi-check-circle" /></span>
        <div>
          <span class="tl-kpi-label">Last success rate</span>
          <strong>{{ summary?.lastScenario ? pct(summary.lastSuccessRate) : '—' }}</strong>
          <small>{{ summary?.lastScenario ? scenarioLabel(summary.lastScenario) : 'No completed runs yet' }}</small>
        </div>
      </article>
      <article class="tl-kpi">
        <span class="tl-kpi-icon tl-kpi-icon--warning"><i class="pi pi-clock" /></span>
        <div>
          <span class="tl-kpi-label">Last test duration</span>
          <strong>{{ summary?.lastStartedAt ? formatDuration(summary.lastDurationSec) : '—' }}</strong>
          <small>{{ summary?.lastStartedAt ? formatDate(summary.lastStartedAt).slice(0, 10) : 'No completed runs yet' }}</small>
        </div>
      </article>
    </div>

    <div class="tl-grid">
      <section class="tl-card">
        <div class="tl-card-title">
          <span>Run a load test</span>
          <span class="tl-target"><i class="pi pi-link" /> internal gateway</span>
        </div>

        <div class="tl-label">Scenario</div>
        <div class="tl-scenarios">
          <button
            v-for="item in scenarios"
            :key="item.id"
            type="button"
            class="tl-scenario"
            :class="{ active: scenario === item.id }"
            @click="scenario = item.id"
          >
            <span class="tl-scenario-icon"><i :class="item.icon" /></span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.hint }}</small>
          </button>
        </div>

        <div class="tl-label">Preset</div>
        <div class="tl-presets">
          <button
            v-for="preset in presets"
            :key="preset.id"
            type="button"
            class="tl-preset"
            :class="{ active: presetActive(preset) }"
            @click="usePreset(preset)"
          >
            <strong>{{ preset.label }}</strong>
            <small>{{ preset.vus }} VU · {{ preset.duration }}s</small>
          </button>
        </div>

        <div class="tl-fields">
          <label>
            <span>Virtual users</span>
            <div class="tl-field-box">
              <input v-model.number="vus" type="number" min="1" max="150" />
              <small>VU</small>
            </div>
          </label>
          <label>
            <span>Duration</span>
            <div class="tl-field-box">
              <input v-model.number="durationSec" type="number" min="10" max="600" />
              <small>sec</small>
            </div>
          </label>
        </div>

        <button type="button" class="tl-run" :disabled="running || !mode" @click="startRun">
          <i :class="running ? 'pi pi-spin pi-spinner' : 'pi pi-play'" />
          {{ running ? 'Starting…' : 'Run load test' }}
        </button>
        <p class="tl-note">Test mode is enabled automatically for the duration of the run.</p>
      </section>

      <section class="tl-card tl-result">
        <div class="tl-card-title">
          <span>Test run</span>
          <div class="tl-run-actions">
            <span v-if="selected" :class="statusClass(selected.status)">{{ selected.status }}</span>
            <button v-if="selected?.status === 'RUNNING'" type="button" class="tl-stop" @click="stopRun">
              <i class="pi pi-stop-circle" /> Stop
            </button>
          </div>
        </div>

        <div v-if="!selected" class="tl-empty tl-empty--detail">
          <i class="pi pi-chart-line" />
          <span>Start a test or open a run from history.</span>
        </div>

        <template v-else>
          <div class="tl-run-meta">
            <strong>{{ scenarioLabel(selected.scenario) }} · {{ selected.vus }} VU · {{ formatDuration(selected.durationSec) }}</strong>
            <small>{{ formatDate(selected.startedAt) }}</small>
          </div>

          <div class="tl-progress">
            <div class="tl-progress-track">
              <span
                class="tl-progress-fill"
                :class="`is-${selected.status.toLowerCase()}`"
                :style="{ width: `${progressPct}%` }"
              />
            </div>
            <div class="tl-progress-meta">
              <span>{{ selected.status === 'RUNNING' ? 'Running' : 'Finished' }}</span>
              <span>{{ elapsedLabel }} / {{ totalLabel }}</span>
            </div>
          </div>

          <div class="tl-mini-grid">
            <button
              type="button"
              class="tl-mini"
              :class="{ active: activeMetric === 'vu' }"
              :style="{ '--metric-color': metricColors.vu }"
              @click="chooseMetric('vu')"
            >
              <span><i class="pi pi-users" /> VU active</span>
              <strong>{{ num(liveVus) }}</strong>
            </button>
            <button
              type="button"
              class="tl-mini"
              :class="{ active: activeMetric === 'rps' }"
              :style="{ '--metric-color': metricColors.rps }"
              @click="chooseMetric('rps')"
            >
              <span><i class="pi pi-bolt" /> RPS</span>
              <strong>{{ liveRps.toFixed(1) }}</strong>
            </button>
            <button
              type="button"
              class="tl-mini"
              :class="{ active: activeMetric === 'success' }"
              :style="{ '--metric-color': metricColors.success }"
              @click="chooseMetric('success')"
            >
              <span><i class="pi pi-check-circle" /> Success rate</span>
              <strong>{{ pct(liveSuccess) }}</strong>
            </button>
            <button
              type="button"
              class="tl-mini"
              :class="{ active: activeMetric === 'p95' }"
              :style="{ '--metric-color': metricColors.p95 }"
              @click="chooseMetric('p95')"
            >
              <span><i class="pi pi-clock" /> p95</span>
              <strong>{{ Math.round(liveP95) }} ms</strong>
            </button>
          </div>

          <div class="tl-chart-head">
            <div class="tl-chart-caption">
              <span>{{ activeMetricMeta.label }} profile</span>
              <small v-if="chart.hasData">{{ chartMaxLabel }}</small>
            </div>
          </div>

          <div
            class="tl-chart"
            :class="{ 'is-empty': !chart.hasData }"
            :style="{ '--chart-color': chartColor }"
            @mouseleave="hoverIndex = null"
          >
            <template v-if="chart.hasData">
              <svg class="tl-chart-svg" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
                <defs>
                  <linearGradient id="tlChartFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="var(--chart-color)" stop-opacity="0.42" />
                    <stop offset="100%" stop-color="var(--chart-color)" stop-opacity="0" />
                  </linearGradient>
                </defs>
                <line
                  v-for="level in [20, 40, 60, 80]"
                  :key="level"
                  :x1="0"
                  :x2="100"
                  :y1="level"
                  :y2="level"
                  class="tl-grid-line"
                />
                <polygon :points="chart.area" fill="url(#tlChartFill)" />
                <polyline :points="chart.line" class="tl-chart-line" vector-effect="non-scaling-stroke" />
                <line
                  v-if="hoverPoint"
                  :x1="hoverPoint.x"
                  :x2="hoverPoint.x"
                  y1="0"
                  y2="100"
                  class="tl-crosshair"
                  vector-effect="non-scaling-stroke"
                />
              </svg>

              <span
                v-for="(point, index) in chart.points"
                :key="`dot-${index}`"
                class="tl-chart-dot"
                :class="{ active: hoverIndex === index }"
                :style="{ left: `${point.x}%`, top: `${point.y}%` }"
              />

              <span class="tl-chart-scale">{{ chartMaxLabel }}</span>

              <span
                v-for="(point, index) in chart.points"
                :key="`zone-${index}`"
                class="tl-hover-zone"
                :style="{ left: `${point.bandLeft}%`, width: `${point.bandWidth}%` }"
                @mouseenter="hoverIndex = index"
              />

              <div
                v-if="hoverPoint"
                class="tl-chart-tooltip"
                :class="{ below: tooltipBelow }"
                :style="tooltipStyle"
              >
                <strong>{{ formatMetric(hoverPoint.value) }}</strong>
                <span>#{{ (hoverIndex ?? 0) + 1 }} · t={{ hoverPoint.t }}s</span>
              </div>
            </template>
            <div v-else class="tl-chart-empty">
              <i class="pi pi-chart-bar" />
              <span>No series data yet</span>
            </div>
          </div>

          <div class="tl-codes">
            <span><b>2xx</b> {{ num(selected.count2xx) }}</span>
            <span><b>3xx</b> {{ num(selected.count3xx) }}</span>
            <span><b>4xx</b> {{ num(selected.count4xx) }}</span>
            <span><b>5xx</b> {{ num(selected.count5xx) }}</span>
            <span><b>p50</b> {{ selected.p50.toFixed(0) }} ms</span>
            <span><b>p99</b> {{ selected.p99.toFixed(0) }} ms</span>
            <span><b>total</b> {{ num(selected.totalRequests) }}</span>
          </div>

          <p v-if="detailLoading" class="tl-detail-loading">
            <i class="pi pi-spin pi-spinner" /> Syncing run details…
          </p>
        </template>
      </section>
    </div>

    <section class="tl-card tl-history">
      <div class="tl-history-head">
        <div class="tl-card-title tl-card-title--flush">
          <span>Test history <small>{{ total }}</small></span>
        </div>
        <div class="tl-filters">
          <label>
            <span>Scenario</span>
            <select v-model="scenarioFilter">
              <option v-for="option in scenarioFilterOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            <span>Status</span>
            <select v-model="statusFilter">
              <option v-for="option in statusFilterOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <button type="button" class="tl-refresh" :disabled="historyLoading" @click="refreshRuns">
            <i :class="historyLoading ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'" /> Refresh
          </button>
        </div>
      </div>

      <div v-if="!filteredRuns.length" class="tl-empty tl-empty--history">
        <i class="pi pi-history" />
        <span>{{ runs.length ? 'No test runs match these filters.' : 'No tests yet. Run your first load test.' }}</span>
      </div>

      <div v-else class="tl-table-wrap">
        <table class="tl-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Started</th>
              <th>Scenario</th>
              <th>VU</th>
              <th>Duration</th>
              <th>RPS (avg)</th>
              <th>Success</th>
              <th>p95 latency</th>
              <th>Status</th>
              <th class="tl-col-actions" />
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(run, index) in filteredRuns"
              :key="run.id"
              :class="{ selected: selected?.id === run.id }"
              @click="openRun(run)"
            >
              <td class="tl-num">{{ filteredRuns.length - index }}</td>
              <td>{{ formatDate(run.startedAt) }}</td>
              <td><span class="tl-tag">{{ scenarioLabel(run.scenario) }}</span></td>
              <td>{{ run.vus }}</td>
              <td>{{ formatDuration(run.durationSec) }}</td>
              <td>{{ run.rps.toFixed(1) }}</td>
              <td>{{ pct(run.successRate) }}</td>
              <td>{{ run.p95.toFixed(0) }} ms</td>
              <td><span :class="statusClass(run.status)">{{ run.status }}</span></td>
              <td class="tl-actions" @click.stop>
                <button type="button" class="tl-icon-btn" title="Run actions" @click="toggleMenu(run, $event)">
                  <i class="pi pi-ellipsis-v" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <Teleport to="body">
      <div
        v-if="menuRun"
        class="tl-menu"
        :style="{ left: `${menuPos.left}px`, top: `${menuPos.top}px` }"
        @click.stop
      >
        <button type="button" class="tl-menu-item" @click="openFromMenu">
          <i class="pi pi-external-link" /> Open
        </button>
        <template v-if="confirmDeleteId === menuRun.id">
          <button
            type="button"
            class="tl-menu-item danger"
            :disabled="deletingId === menuRun.id"
            @click="deleteFromMenu"
          >
            <i :class="deletingId === menuRun.id ? 'pi pi-spin pi-spinner' : 'pi pi-trash'" />
            {{ deletingId === menuRun.id ? 'Deleting…' : 'Confirm delete' }}
          </button>
          <button type="button" class="tl-menu-item" @click="confirmDeleteId = null">Cancel</button>
        </template>
        <button v-else type="button" class="tl-menu-item danger" @click="confirmDeleteId = menuRun.id">
          <i class="pi pi-trash" /> Delete
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.test-lab {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tl-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.tl-eyebrow {
  color: var(--muted-2);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.6px;
}

.tl-head h1 {
  margin: 6px 0 4px;
  font-size: 26px;
  letter-spacing: -0.4px;
}

.tl-head p {
  margin: 0;
  max-width: 520px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.tl-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tl-mode-card {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 12px;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--surface);
}

.tl-mode-card.is-test {
  border-color: rgba(167, 139, 250, 0.45);
  background: linear-gradient(180deg, #1d1830, #171426);
}

.tl-mode-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 0 4px var(--success-soft);
}

.tl-mode-card.is-test .tl-mode-dot {
  background: #a78bfa;
  box-shadow: 0 0 0 4px rgba(167, 139, 250, 0.16);
}

.tl-mode-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tl-mode-copy strong {
  font-size: 10px;
  letter-spacing: 0.3px;
}

.tl-mode-copy small {
  color: var(--muted);
  font-size: 9px;
}

.tl-segmented {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg);
}

.tl-segmented button {
  padding: 6px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muted);
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.tl-segmented button.active {
  background: var(--surface-3);
  color: var(--text);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.tl-segmented button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tl-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid #7c5b17;
  border-radius: 10px;
  background: #241d0e;
  color: #fcd34d;
  font-size: 11px;
}

.tl-error {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 14px;
  border: 1px solid #7f1d1d;
  border-radius: 10px;
  background: #2a1111;
  color: #fca5a5;
  font-size: 12px;
}

.tl-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.tl-kpi {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  min-width: 0;
}

.tl-kpi-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--surface-2);
  font-size: 13px;
}

.tl-kpi-icon--violet {
  color: var(--violet);
  background: var(--violet-soft);
  border-color: rgba(167, 139, 250, 0.22);
}

.tl-kpi-icon--info {
  color: var(--info);
  background: var(--info-soft);
  border-color: rgba(147, 197, 253, 0.2);
}

.tl-kpi-icon--success {
  color: var(--success);
  background: var(--success-soft);
  border-color: rgba(52, 211, 153, 0.2);
}

.tl-kpi-icon--warning {
  color: var(--warning);
  background: var(--warning-soft);
  border-color: rgba(251, 191, 36, 0.2);
}

.tl-kpi-label {
  display: block;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.3px;
}

.tl-kpi strong {
  display: block;
  margin-top: 5px;
  font-size: 19px;
  letter-spacing: -0.4px;
  white-space: nowrap;
}

.tl-kpi small {
  display: block;
  margin-top: 3px;
  color: var(--muted-2);
  font-size: 9px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tl-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.48fr);
  gap: 16px;
  align-items: start;
}

.tl-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  padding: 16px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
  min-width: 0;
}

.tl-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 12px;
  font-weight: 700;
}

.tl-card-title--flush {
  margin-bottom: 0;
}

.tl-card-title small {
  margin-left: 6px;
  color: var(--muted-2);
  font-size: 9px;
  font-weight: 500;
}

.tl-target {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--muted-2);
  font-size: 9px;
  font-weight: 500;
}

.tl-label {
  margin: 12px 0 7px;
  color: var(--muted);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.2px;
  text-transform: uppercase;
}

.tl-label:first-of-type {
  margin-top: 0;
}

.tl-scenarios {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.tl-scenario {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 10px 9px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg);
  color: var(--text);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.tl-scenario-icon {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  margin-bottom: 2px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface-2);
  color: var(--muted);
  font-size: 10px;
}

.tl-scenario strong {
  font-size: 11px;
}

.tl-scenario small {
  color: var(--muted);
  font-size: 9px;
  line-height: 1.35;
}

.tl-scenario:hover {
  border-color: var(--border-strong);
}

.tl-scenario.active {
  border-color: rgba(167, 139, 250, 0.5);
  background: linear-gradient(180deg, rgba(167, 139, 250, 0.14), rgba(167, 139, 250, 0.04));
}

.tl-scenario.active .tl-scenario-icon {
  color: var(--violet);
  border-color: rgba(167, 139, 250, 0.3);
  background: rgba(167, 139, 250, 0.12);
}

.tl-presets {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.tl-preset {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 7px 6px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  color: var(--muted);
  font-size: 9px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.tl-preset strong {
  color: var(--text);
  font-size: 10px;
}

.tl-preset small {
  font-size: 8px;
}

.tl-preset:hover {
  border-color: var(--border-strong);
}

.tl-preset.active {
  border-color: rgba(167, 139, 250, 0.4);
  background: rgba(167, 139, 250, 0.08);
  color: var(--text);
}

.tl-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 14px;
}

.tl-fields label {
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: var(--muted);
  font-size: 9px;
}

.tl-field-box {
  display: flex;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  transition: border-color 0.15s;
}

.tl-field-box:focus-within {
  border-color: rgba(167, 139, 250, 0.5);
}

.tl-field-box input {
  width: 100%;
  min-width: 0;
  padding: 8px 9px;
  border: 0;
  background: transparent;
  color: var(--text);
  font-size: 11px;
  outline: none;
}

.tl-field-box small {
  padding-right: 9px;
  color: var(--muted-2);
  font-size: 9px;
}

.tl-run {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  margin-top: 16px;
  padding: 11px;
  border: 0;
  border-radius: 9px;
  background: linear-gradient(180deg, #c4b5fd, #a78bfa);
  color: #140f26;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(167, 139, 250, 0.2);
  transition: filter 0.15s, transform 0.15s;
}

.tl-run:hover:not(:disabled) {
  filter: brightness(1.06);
}

.tl-run:active:not(:disabled) {
  transform: translateY(1px);
}

.tl-run:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.tl-note {
  margin: 8px 0 0;
  color: var(--muted-2);
  font-size: 9px;
  line-height: 1.4;
}

.tl-result {
  min-height: 420px;
}

.tl-run-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tl-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: var(--muted);
  font-size: 11px;
  text-align: center;
}

.tl-empty i {
  font-size: 25px;
  opacity: 0.45;
}

.tl-empty--detail {
  min-height: 370px;
}

.tl-empty--history {
  min-height: 150px;
}

.tl-run-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
}

.tl-run-meta small {
  color: var(--muted);
  font-size: 9px;
  white-space: nowrap;
}

.tl-progress {
  margin: 12px 0 14px;
}

.tl-progress-track {
  height: 6px;
  border: 1px solid var(--border);
  border-radius: 99px;
  background: var(--bg);
  overflow: hidden;
}

.tl-progress-fill {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, #7c3aed, #c4b5fd);
  transition: width 0.45s ease;
}

.tl-progress-fill.is-completed {
  background: linear-gradient(90deg, #059669, #34d399);
}

.tl-progress-fill.is-failed {
  background: linear-gradient(90deg, #b91c1c, #f87171);
}

.tl-progress-fill.is-stopped {
  background: linear-gradient(90deg, #475569, #94a3b8);
}

.tl-progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  color: var(--muted);
  font-size: 9px;
}

.tl-mini-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}

.tl-mini {
  display: block;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg);
  color: inherit;
  font: inherit;
  text-align: left;
  min-width: 0;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
}

.tl-mini:hover {
  border-color: var(--border-strong);
}

.tl-mini.active {
  border-color: var(--metric-color, var(--violet));
  box-shadow: 0 0 0 1px var(--metric-color, var(--violet));
  background: var(--surface-2);
}

.tl-mini span {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--muted);
  font-size: 8px;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.tl-mini span i {
  font-size: 9px;
}

.tl-mini.active span,
.tl-mini.active span i {
  color: var(--metric-color, var(--violet));
}

.tl-mini strong {
  display: block;
  margin-top: 5px;
  font-size: 14px;
  letter-spacing: -0.2px;
}

.tl-chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.tl-chart-caption {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 10px;
  font-weight: 600;
}

.tl-chart-caption small {
  color: var(--muted-2);
  font-size: 9px;
  font-weight: 400;
}

.tl-chart {
  position: relative;
  height: 260px;
  margin-top: 9px;
  border: 1px solid var(--border);
  border-radius: 11px;
  background:
    radial-gradient(
      120% 100% at 50% 0%,
      color-mix(in srgb, var(--chart-color, #8b5cf6) 10%, transparent),
      transparent 60%
    ),
    var(--bg);
  overflow: hidden;
}

.tl-chart-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.tl-grid-line {
  stroke: var(--border);
  stroke-width: 1;
  opacity: 0.55;
  vector-effect: non-scaling-stroke;
}

.tl-chart-line {
  fill: none;
  stroke: var(--chart-color, #8b5cf6);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  filter: drop-shadow(
    0 0 6px color-mix(in srgb, var(--chart-color, #8b5cf6) 40%, transparent)
  );
}

.tl-crosshair {
  stroke: color-mix(in srgb, var(--chart-color, #8b5cf6) 40%, transparent);
  stroke-width: 1;
  stroke-dasharray: 3 3;
  vector-effect: non-scaling-stroke;
}

.tl-chart-dot {
  position: absolute;
  z-index: 1;
  width: 5px;
  height: 5px;
  margin: -2.5px 0 0 -2.5px;
  border-radius: 50%;
  background: var(--chart-color, #8b5cf6);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--chart-color, #8b5cf6) 18%, transparent);
  pointer-events: none;
}

.tl-chart-dot.active {
  z-index: 2;
  width: 7px;
  height: 7px;
  margin: -3.5px 0 0 -3.5px;
  background: var(--chart-color, #8b5cf6);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--chart-color, #8b5cf6) 25%, transparent);
}

.tl-chart-scale {
  position: absolute;
  top: 8px;
  left: 10px;
  z-index: 1;
  color: var(--muted-2);
  font-size: 9px;
  pointer-events: none;
}

.tl-hover-zone {
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 2;
}

.tl-chart-tooltip {
  position: absolute;
  z-index: 3;
  padding: 6px 9px;
  border: 1px solid color-mix(in srgb, var(--chart-color, #8b5cf6) 35%, transparent);
  border-radius: 8px;
  background: #191527;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
  transform: translate(-50%, -100%);
  pointer-events: none;
  white-space: nowrap;
}

.tl-chart-tooltip.below {
  transform: translate(-50%, 0);
}

.tl-chart-tooltip strong {
  display: block;
  color: var(--chart-color, #8b5cf6);
  font-size: 12px;
}

.tl-chart-tooltip span {
  display: block;
  margin-top: 2px;
  color: var(--muted);
  font-size: 9px;
}

.tl-chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--muted);
  font-size: 10px;
}

.tl-chart-empty i {
  font-size: 22px;
  opacity: 0.4;
}

.tl-codes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
  color: var(--muted);
  font-size: 9px;
}

.tl-codes span {
  padding: 3px 7px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
}

.tl-codes b {
  margin-right: 3px;
  color: var(--text);
  font-weight: 600;
}

.tl-detail-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0 0;
  color: var(--muted-2);
  font-size: 9px;
}

.tl-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 99px;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.6px;
  white-space: nowrap;
}

.tl-status::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.tl-status--running {
  background: var(--warning-soft);
  color: var(--warning);
}

.tl-status--completed {
  background: var(--success-soft);
  color: var(--success);
}

.tl-status--failed {
  background: var(--danger-soft);
  color: var(--danger);
}

.tl-status--stopped {
  background: rgba(148, 163, 184, 0.12);
  color: #94a3b8;
}

.tl-status--unknown {
  background: var(--surface-2);
  color: var(--muted);
}

.tl-stop {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 9px;
  border: 1px solid #7f1d1d;
  border-radius: 7px;
  background: #2a1111;
  color: #fca5a5;
  font-size: 9px;
  font-weight: 600;
  cursor: pointer;
}

.tl-stop:hover {
  background: #3a1515;
}

.tl-history {
  margin-top: 2px;
}

.tl-history-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tl-filters {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.tl-filters label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--muted);
  font-size: 8px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.tl-filters select {
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--bg);
  color: var(--text);
  font-size: 10px;
  cursor: pointer;
  outline: none;
}

.tl-filters select:focus {
  border-color: rgba(167, 139, 250, 0.5);
}

.tl-refresh {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--bg);
  color: var(--muted);
  font-size: 9px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.tl-refresh:hover:not(:disabled) {
  border-color: var(--border-strong);
  color: var(--text);
}

.tl-refresh:disabled {
  opacity: 0.55;
  cursor: wait;
}

.tl-table-wrap {
  overflow-x: auto;
}

.tl-table {
  width: 100%;
  min-width: 840px;
  border-collapse: collapse;
  font-size: 10px;
}

.tl-table th {
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
  color: var(--muted-2);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.6px;
  text-align: left;
  text-transform: uppercase;
  white-space: nowrap;
}

.tl-table td {
  padding: 9px 10px;
  border-bottom: 1px solid rgba(35, 43, 54, 0.65);
  color: var(--text);
  white-space: nowrap;
}

.tl-table tbody tr {
  cursor: pointer;
  transition: background 0.12s;
}

.tl-table tbody tr:hover {
  background: var(--surface-2);
}

.tl-table tbody tr.selected {
  background: rgba(167, 139, 250, 0.08);
}

.tl-table tbody tr.selected .tl-num {
  box-shadow: inset 2px 0 0 #a78bfa;
}

.tl-table tbody tr:last-child td {
  border-bottom: 0;
}

.tl-num {
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.tl-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border: 1px solid var(--border);
  border-radius: 99px;
  background: var(--surface-2);
  color: var(--muted);
  font-size: 9px;
}

.tl-col-actions {
  width: 40px;
}

.tl-actions {
  text-align: right;
}

.tl-icon-btn {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--muted);
  font-size: 11px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}

.tl-icon-btn:hover {
  border-color: var(--border-strong);
  color: var(--text);
}

.tl-menu {
  position: fixed;
  z-index: 1200;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 168px;
  padding: 5px;
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  background: #161b23;
  box-shadow: var(--shadow);
}

.tl-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 9px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text);
  font-size: 10px;
  text-align: left;
  cursor: pointer;
}

.tl-menu-item:hover {
  background: var(--surface-3);
}

.tl-menu-item.danger {
  color: #fca5a5;
}

.tl-menu-item.danger:hover {
  background: #2a1111;
}

.tl-menu-item:disabled {
  opacity: 0.6;
  cursor: wait;
}

@media (max-width: 1100px) {
  .tl-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .tl-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .tl-kpi-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .tl-scenarios,
  .tl-presets,
  .tl-fields {
    grid-template-columns: minmax(0, 1fr);
  }

  .tl-mini-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tl-head-actions {
    width: 100%;
    justify-content: space-between;
  }

  .tl-chart {
    height: 220px;
  }

  .tl-chart-head {
    align-items: flex-start;
  }
}
</style>
