<script setup lang="ts">
import { LMap, LTileLayer, LMarker, LPopup } from '@vue-leaflet/vue-leaflet'
import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

const props = defineProps<{
  city?: string | null
  street?: string | null
  country?: string | null
  title?: string
}>()

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const coords = ref<[number, number] | null>(null)
const loading = ref(true)

const locationText = computed(() =>
  [props.street, props.city, props.country].filter(Boolean).join(', '),
)

async function geocode() {
  const parts = [props.street, props.city, props.country].filter(Boolean)
  if (!parts.length) {
    loading.value = false
    return
  }
  try {
    const q = encodeURIComponent(parts.join(', '))
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${q}`,
    )
    const data = (await res.json()) as Array<{ lat: string; lon: string }>
    if (Array.isArray(data) && data.length) {
      coords.value = [Number.parseFloat(data[0]!.lat), Number.parseFloat(data[0]!.lon)]
    }
  } catch {
    coords.value = null
  } finally {
    loading.value = false
  }
}

onMounted(geocode)
</script>

<template>
  <div>
    <div v-if="loading" class="muted">
      <i class="pi pi-spin pi-spinner" /> Locating…
    </div>
    <LMap
      v-else-if="coords"
      :zoom="14"
      :center="coords"
      style="height: 320px; border-radius: 8px; z-index: 0"
    >
      <LTileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution="&copy; OpenStreetMap contributors"
      />
      <LMarker :lat-lng="coords">
        <LPopup>{{ title || locationText }}</LPopup>
      </LMarker>
    </LMap>
    <div v-else class="muted">
      No map location available for “{{ locationText || 'unknown' }}”.
    </div>
  </div>
</template>
