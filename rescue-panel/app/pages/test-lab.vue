<script setup lang="ts">
import type { TestModeState, TestRun, TestRunPage } from '~/types'

const api = useApi()
const toast = useToast()

const mode = ref<TestModeState | null>(null)
const runs = ref<TestRun[]>([])
const total = ref(0)
const selected = ref<TestRun | null>(null)
const loading = ref(true)
const running = ref(false)
const historyLoading = ref(false)
const error = ref('')

const scenario = ref('read')
const vus = ref(10)
const durationSec = ref(60)

const scenarios = [
  { id: 'read', label: 'Read API', hint: 'alerts + my reports' },
  { id: 'broadcast', label: 'Broadcast', hint: 'operator alert, suppressed' },
  { id: 'mixed', label: 'Mixed', hint: 'read + broadcast' },
]

const presets = [
  { id: 'smoke', label: 'Smoke', vus: 10, duration: 60 },
  { id: 'load', label: 'Load', vus: 50, duration: 180 },
  { id: 'stress', label: 'Stress', vus: 100, duration: 300 },
]

let poll: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await Promise.all([loadMode(), loadRuns()])
  loading.value = false
})

onBeforeUnmount(() => {
  if (poll) clearInterval(poll)
})

async function loadMode() {
  try {
    mode.value = await api<TestModeState>('/v1/test-lab/settings')
  } catch (e: any) {
    error.value = e?.message || 'Failed to load test mode'
  }
}

async function loadRuns() {
  historyLoading.value = true
  try {
    const page = await api<TestRunPage>('/v1/test-lab/runs?limit=30')
    runs.value = page.items
    total.value = page.total
  } catch (e: any) {
    error.value = e?.message || 'Failed to load test history'
  } finally {
    historyLoading.value = false
  }
}

async function toggleMode() {
  if (!mode.value) return
  try {
    mode.value = await api<TestModeState>('/v1/test-lab/settings', {
      method: 'PATCH',
      body: { enabled: !mode.value.enabled, ttlSeconds: 1800 },
    })
    toast.add({ severity: 'success', summary: mode.value.enabled ? 'Test mode enabled' : 'Test mode disabled', life: 2500 })
  } catch (e: any) {
    error.value = e?.message || 'Failed to change test mode'
  }
}

function usePreset(preset: (typeof presets)[number]) {
  vus.value = preset.vus
  durationSec.value = preset.duration
}

async function startRun() {
  if (running.value) return
  running.value = true
  error.value = ''
  try {
    const run = await api<TestRun>('/v1/test-lab/runs', {
      method: 'POST',
      body: {
        scenario: scenario.value,
        vus: vus.value,
        durationSec: durationSec.value,
        rampUpSec: 0,
      },
    })
    selected.value = run
    await loadMode()
    startPolling(run.id)
    await loadRuns()
    toast.add({ severity: 'info', summary: 'Load test started', life: 2500 })
  } catch (e: any) {
    error.value = e?.message || 'Failed to start load test'
  } finally {
    running.value = false
  }
}

function startPolling(id: string) {
  if (poll) clearInterval(poll)
  poll = setInterval(async () => {
    try {
      const run = await api<TestRun>(`/v1/test-lab/runs/${id}`)
      selected.value = run
      if (run.status !== 'RUNNING') {
        if (poll) clearInterval(poll)
        poll = null
        await Promise.all([loadRuns(), loadMode()])
      }
    } catch {
      if (poll) clearInterval(poll)
      poll = null
    }
  }, 2000)
}

async function openRun(run: TestRun) {
  selected.value = await api<TestRun>(`/v1/test-lab/runs/${run.id}`)
  if (run.status === 'RUNNING') startPolling(run.id)
}

async function stopRun() {
  if (!selected.value || selected.value.status !== 'RUNNING') return
  try {
    selected.value = await api<TestRun>(`/v1/test-lab/runs/${selected.value.id}/stop`, { method: 'POST' })
    await Promise.all([loadRuns(), loadMode()])
  } catch (e: any) {
    error.value = e?.message || 'Failed to stop run'
  }
}

const expiresLabel = computed(() => {
  if (!mode.value?.expiresAt) return ''
  const ms = new Date(mode.value.expiresAt).getTime() - Date.now()
  if (ms <= 0) return 'expired'
  return `${Math.ceil(ms / 60000)} min`
})

const seriesMax = computed(() => {
  const values = selected.value?.series?.map((point) => point.rps) ?? []
  return Math.max(1, ...values)
})

function statusClass(status?: string) {
  return `tl-status tl-status--${(status || 'unknown').toLowerCase()}`
}

function formatDate(value?: string | null) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

function pct(value?: number) {
  return `${(value ?? 0).toFixed(1)}%`
}

function num(value?: number) {
  return (value ?? 0).toLocaleString('en-US')
}
</script>

<template>
  <div class="page test-lab">
    <header class="tl-head">
      <div>
        <div class="tl-eyebrow">OPERATIONS</div>
        <h1>Test Lab</h1>
        <p>Load tests with suppressed notifications and uploads.</p>
      </div>
      <div class="tl-head-actions">
        <div class="tl-mode" :class="{ active: mode?.enabled }">
          <span class="tl-dot" />
          <span>{{ mode?.enabled ? 'TEST MODE' : 'Production mode' }}</span>
          <small v-if="mode?.enabled">expires in {{ expiresLabel }}</small>
        </div>
        <button type="button" class="tl-switch" :class="{ on: mode?.enabled }" :disabled="!mode" @click="toggleMode">
          <span />
        </button>
      </div>
    </header>

    <div v-if="mode?.enabled" class="tl-banner">
      <i class="pi pi-exclamation-triangle" />
      <span>Notifications disabled · uploads disabled · test run <strong>{{ mode.runId }}</strong></span>
    </div>

    <div v-if="error" class="tl-error">{{ error }}</div>

    <div class="tl-grid">
      <section class="tl-card">
        <div class="tl-card-title">
          <span>Run a load test</span>
          <span class="tl-target">internal gateway</span>
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
            <strong>{{ item.label }}</strong>
            <small>{{ item.hint }}</small>
          </button>
        </div>

        <div class="tl-label">Preset</div>
        <div class="tl-presets">
          <button v-for="preset in presets" :key="preset.id" type="button" class="tl-preset" @click="usePreset(preset)">
            {{ preset.label }} · {{ preset.vus }} VU / {{ preset.duration }}s
          </button>
        </div>

        <div class="tl-fields">
          <label>
            <span>Virtual users</span>
            <input v-model.number="vus" type="number" min="1" max="150" />
          </label>
          <label>
            <span>Duration (sec)</span>
            <input v-model.number="durationSec" type="number" min="10" max="600" />
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
          <span>Latest run</span>
          <span v-if="selected" :class="statusClass(selected.status)">{{ selected.status }}</span>
        </div>

        <div v-if="!selected" class="tl-empty">
          <i class="pi pi-chart-line" />
          <span>Start a test or open a run from history.</span>
        </div>

        <template v-else>
          <div class="tl-run-title">
            <strong>{{ selected.scenario }} · {{ selected.vus }} VU · {{ selected.durationSec }}s</strong>
            <small>{{ formatDate(selected.startedAt) }}</small>
          </div>

          <div class="tl-kpis">
            <div><span>Total requests</span><strong>{{ num(selected.totalRequests) }}</strong></div>
            <div><span>RPS</span><strong>{{ selected.rps.toFixed(1) }}</strong></div>
            <div><span>Success</span><strong>{{ pct(selected.successRate) }}</strong></div>
            <div><span>p95</span><strong>{{ selected.p95.toFixed(0) }} ms</strong></div>
          </div>

          <div class="tl-codes">
            <span><b>2xx</b> {{ num(selected.count2xx) }}</span>
            <span><b>3xx</b> {{ num(selected.count3xx) }}</span>
            <span><b>4xx</b> {{ num(selected.count4xx) }}</span>
            <span><b>5xx</b> {{ num(selected.count5xx) }}</span>
            <span><b>p50</b> {{ selected.p50.toFixed(0) }} ms</span>
            <span><b>p99</b> {{ selected.p99.toFixed(0) }} ms</span>
          </div>

          <div v-if="selected.series?.length" class="tl-chart">
            <div v-for="(point, index) in selected.series" :key="index" class="tl-bar" :style="{ height: `${Math.max(3, (point.rps / seriesMax) * 100)}%` }" />
          </div>

          <div v-if="selected.endpoints?.length" class="tl-endpoints">
            <div class="tl-endpoints-head"><span>Endpoint</span><span>Req</span><span>Err</span><span>p95</span></div>
            <div v-for="endpoint in selected.endpoints" :key="endpoint.path" class="tl-endpoints-row">
              <span>{{ endpoint.path }}</span><span>{{ num(endpoint.requests) }}</span><span>{{ num(endpoint.errors) }}</span><span>{{ endpoint.p95.toFixed(0) }} ms</span>
            </div>
          </div>

          <button v-if="selected.status === 'RUNNING'" type="button" class="tl-stop" @click="stopRun">Stop run</button>
        </template>
      </section>
    </div>

    <section class="tl-card tl-history">
      <div class="tl-card-title">
        <span>Test history <small>{{ total }}</small></span>
        <button type="button" class="tl-refresh" :disabled="historyLoading" @click="loadRuns">
          <i class="pi pi-refresh" /> Refresh
        </button>
      </div>

      <div v-if="!runs.length" class="tl-empty">
        <i class="pi pi-history" />
        <span>No tests yet.</span>
      </div>

      <div v-else class="tl-table">
        <div class="tl-table-head">
          <span>Started</span><span>Scenario</span><span>VUs</span><span>Status</span><span>RPS</span><span>Success</span><span>p95</span>
        </div>
        <button
          v-for="run in runs"
          :key="run.id"
          type="button"
          class="tl-table-row"
          @click="openRun(run)"
        >
          <span>{{ formatDate(run.startedAt) }}</span>
          <span>{{ run.scenario }}</span>
          <span>{{ run.vus }}</span>
          <span><em :class="statusClass(run.status)">{{ run.status }}</em></span>
          <span>{{ run.rps.toFixed(1) }}</span>
          <span>{{ pct(run.successRate) }}</span>
          <span>{{ run.p95.toFixed(0) }} ms</span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.test-lab { display: flex; flex-direction: column; gap: 16px; }
.tl-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; }
.tl-eyebrow { color: var(--muted); font-size: 10px; letter-spacing: 1.4px; font-weight: 700; }
.tl-head h1 { margin: 4px 0 3px; font-size: 25px; }
.tl-head p { margin: 0; color: var(--muted); font-size: 12px; }
.tl-head-actions { display: flex; align-items: center; gap: 10px; }
.tl-mode { display: flex; align-items: center; gap: 7px; padding: 9px 12px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface); font-size: 10px; font-weight: 700; letter-spacing: .5px; }
.tl-mode.active { border-color: #f59e0b; color: #fbbf24; background: #2a2110; }
.tl-mode small { color: var(--muted); font-weight: 400; letter-spacing: 0; }
.tl-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--success); }
.tl-mode.active .tl-dot { background: #f59e0b; box-shadow: 0 0 0 4px rgba(245,158,11,.15); }
.tl-switch { width: 42px; height: 24px; padding: 3px; border: 1px solid var(--border); border-radius: 99px; background: var(--surface); }
.tl-switch span { display: block; width: 16px; height: 16px; border-radius: 50%; background: var(--muted); transition: .15s; }
.tl-switch.on { background: #143b2f; border-color: #2f7a61; }
.tl-switch.on span { transform: translateX(18px); background: #34d399; }
.tl-banner { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border: 1px solid #7c5b17; border-radius: 10px; background: #241d0e; color: #fcd34d; font-size: 11px; }
.tl-error { padding: 10px 14px; border: 1px solid #7f1d1d; border-radius: 10px; background: #2a1111; color: #fca5a5; font-size: 12px; }
.tl-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; }
.tl-card { border: 1px solid var(--border); border-radius: 12px; background: var(--surface); padding: 16px; }
.tl-card-title { display: flex; justify-content: space-between; align-items: center; color: var(--text); font-size: 12px; font-weight: 700; margin-bottom: 14px; }
.tl-card-title small { color: var(--muted); margin-left: 4px; }
.tl-target { color: var(--muted); font-size: 9px; font-weight: 400; }
.tl-label { color: var(--muted); font-size: 9px; letter-spacing: 1.2px; font-weight: 700; margin: 12px 0 7px; }
.tl-scenarios { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.tl-scenario { display: flex; flex-direction: column; align-items: flex-start; gap: 3px; padding: 9px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg); color: var(--text); text-align: left; }
.tl-scenario strong { font-size: 11px; }
.tl-scenario small { color: var(--muted); font-size: 9px; }
.tl-scenario.active { border-color: var(--accent); background: #0c2930; }
.tl-presets { display: flex; flex-wrap: wrap; gap: 6px; }
.tl-preset { padding: 6px 8px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg); color: var(--muted); font-size: 9px; }
.tl-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 12px; }
.tl-fields label { display: flex; flex-direction: column; gap: 5px; color: var(--muted); font-size: 9px; }
.tl-fields input { width: 100%; padding: 8px 9px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg); color: var(--text); font-size: 11px; }
.tl-run { display: flex; align-items: center; justify-content: center; gap: 7px; width: 100%; margin-top: 14px; padding: 10px; border: 0; border-radius: 8px; background: var(--accent); color: #041014; font-weight: 700; font-size: 11px; }
.tl-run:disabled { opacity: .5; }
.tl-note { margin: 8px 0 0; color: var(--muted); font-size: 9px; }
.tl-result { min-height: 300px; }
.tl-empty { min-height: 190px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 9px; color: var(--muted); font-size: 11px; text-align: center; }
.tl-empty i { font-size: 25px; opacity: .45; }
.tl-status { display: inline-flex; padding: 3px 7px; border-radius: 99px; font-size: 8px; font-weight: 700; letter-spacing: .5px; }
.tl-status--running { background: #3b2a08; color: #fbbf24; }
.tl-status--completed { background: #0f3b2b; color: #34d399; }
.tl-status--failed { background: #3a1010; color: #f87171; }
.tl-status--stopped { background: #2b3442; color: #94a3b8; }
.tl-run-title { display: flex; justify-content: space-between; gap: 8px; font-size: 11px; }
.tl-run-title small { color: var(--muted); font-size: 9px; }
.tl-kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin: 12px 0 8px; }
.tl-kpis div { padding: 8px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg); }
.tl-kpis span { display: block; color: var(--muted); font-size: 8px; margin-bottom: 3px; }
.tl-kpis strong { font-size: 13px; }
.tl-codes { display: flex; flex-wrap: wrap; gap: 7px; color: var(--muted); font-size: 9px; }
.tl-codes b { color: var(--text); margin-right: 2px; }
.tl-chart { display: flex; align-items: flex-end; gap: 2px; height: 52px; margin: 12px 0; padding: 4px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg); }
.tl-bar { flex: 1; min-width: 2px; border-radius: 2px 2px 0 0; background: #22d3ee; opacity: .65; }
.tl-endpoints { margin-top: 10px; font-size: 9px; }
.tl-endpoints-head, .tl-endpoints-row { display: grid; grid-template-columns: 1fr 50px 42px 58px; gap: 5px; padding: 5px 0; border-bottom: 1px solid var(--border); }
.tl-endpoints-head { color: var(--muted); }
.tl-endpoints-row { color: var(--text); }
.tl-stop { margin-top: 12px; padding: 7px 10px; border: 1px solid #7f1d1d; border-radius: 7px; background: #2a1111; color: #fca5a5; font-size: 10px; }
.tl-history { margin-top: 2px; }
.tl-refresh { display: flex; align-items: center; gap: 5px; padding: 5px 8px; border: 1px solid var(--border); border-radius: 6px; background: var(--bg); color: var(--muted); font-size: 9px; }
.tl-table { width: 100%; font-size: 10px; }
.tl-table-head, .tl-table-row { display: grid; grid-template-columns: 1.5fr .8fr .4fr .8fr .5fr .7fr .6fr; gap: 8px; align-items: center; width: 100%; padding: 8px 4px; border: 0; border-bottom: 1px solid var(--border); background: transparent; color: var(--text); text-align: left; }
.tl-table-head { color: var(--muted); font-size: 9px; }
.tl-table-row:hover { background: var(--bg); }
.tl-table-row em { font-style: normal; }
@media (max-width: 1050px) { .tl-grid { grid-template-columns: 1fr; } }
</style>
