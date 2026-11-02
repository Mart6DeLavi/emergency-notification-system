<script setup lang="ts">
import { useToast } from 'primevue/usetoast'

interface BroadcastResult {
  recipients: number
  deliveries: number
}

const api = useApi()
const toast = useToast()

const title = ref('')
const message = ref('')
const target = ref<'ALL' | 'AREA'>('ALL')
const city = ref('')
const street = ref('')
const loading = ref(false)
const result = ref<BroadcastResult | null>(null)

async function send() {
  if (!title.value.trim()) {
    toast.add({ severity: 'warn', summary: 'Enter a title', life: 2500 })
    return
  }
  if (!message.value.trim()) {
    toast.add({ severity: 'warn', summary: 'Enter a message', life: 2500 })
    return
  }
  if (target.value === 'AREA' && !city.value.trim()) {
    toast.add({ severity: 'warn', summary: 'Enter a city for the area target', life: 2500 })
    return
  }

  loading.value = true
  result.value = null
  try {
    result.value = await api<BroadcastResult>('/api/v1/notifications/broadcast', {
      method: 'POST',
      body: {
        title: title.value.trim(),
        message: message.value.trim(),
        city: target.value === 'AREA' ? city.value.trim() : null,
        street: target.value === 'AREA' && street.value.trim() ? street.value.trim() : null,
      },
    })
    toast.add({
      severity: 'success',
      summary: 'Alert queued',
      detail: `${result.value.recipients} recipient(s), ${result.value.deliveries} delivery job(s)`,
      life: 4000,
    })
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Broadcast failed',
      detail: (e as Error).message,
      life: 5000,
    })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="broadcast-page">
    <header class="page-head">
      <div>
        <span class="eyebrow">Operations</span>
        <h1>Notify users</h1>
        <p>Send an alert from the rescue service to every user or to a specific area.</p>
      </div>
      <span class="live-pill">Live</span>
    </header>

    <div class="broadcast-grid">
      <section class="panel">
        <div class="panel-head">
          <h2 class="panel-title"><i class="pi pi-send" />Compose alert</h2>
        </div>

        <div class="field">
          <label class="field-label" for="broadcast-title">Title</label>
          <InputText
            id="broadcast-title"
            v-model="title"
            class="w-full"
            placeholder="e.g. Fire near the market square"
            maxlength="120"
          />
        </div>

        <div class="field">
          <label class="field-label" for="broadcast-message">Message</label>
          <Textarea
            id="broadcast-message"
            v-model="message"
            rows="6"
            class="w-full"
            placeholder="Describe the situation, the affected area and what people should do."
          />
          <span class="field-hint">Sent as a push, email and/or SMS depending on each user's settings.</span>
        </div>

        <div class="field">
          <span class="field-label">Audience</span>
          <div class="segmented" role="group" aria-label="Audience">
            <button type="button" :class="{ active: target === 'ALL' }" @click="target = 'ALL'">
              All users
            </button>
            <button type="button" :class="{ active: target === 'AREA' }" @click="target = 'AREA'">
              City / street
            </button>
          </div>
        </div>

        <div v-if="target === 'AREA'" class="field-row">
          <div class="field">
            <label class="field-label" for="broadcast-city">City</label>
            <InputText id="broadcast-city" v-model="city" placeholder="Opole" />
          </div>
          <div class="field">
            <label class="field-label" for="broadcast-street">Street (optional)</label>
            <InputText id="broadcast-street" v-model="street" placeholder="Mikołajczyka" />
          </div>
        </div>

        <Button
          label="Send alert"
          icon="pi pi-send"
          class="broadcast-send"
          :loading="loading"
          :disabled="loading"
          @click="send"
        />
      </section>

      <aside class="panel broadcast-preview">
        <div class="panel-head">
          <h2 class="panel-title"><i class="pi pi-eye" />Preview</h2>
          <span class="muted-sm">{{ target === 'ALL' ? 'All users' : city || 'Area' }}</span>
        </div>

        <div class="preview-phone">
          <span class="preview-badge">SENSA alert</span>
          <strong>{{ title || 'Alert title' }}</strong>
          <p>{{ message || 'Your message will appear here.' }}</p>
          <small>Just now · rescue service</small>
        </div>

        <div class="broadcast-note">
          <i class="pi pi-info-circle" />
          <span>
            The alert is stored for each recipient and delivered through the channels enabled in their profile.
          </span>
        </div>

        <div v-if="result" class="broadcast-result">
          <div><strong>{{ result.recipients }}</strong><span>recipients</span></div>
          <div><strong>{{ result.deliveries }}</strong><span>deliveries</span></div>
        </div>
      </aside>
    </div>
  </div>
</template>
