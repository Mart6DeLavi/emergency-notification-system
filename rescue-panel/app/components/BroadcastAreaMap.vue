<script setup lang="ts">
import { LCircle, LMap, LMarker, LTileLayer } from '@vue-leaflet/vue-leaflet'
import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

const props = defineProps<{
  city?: string | null
  street?: string | null
  radiusKm: number
}>()

const coords = ref<[number, number] | null>(null)
const loading = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

const locationText = computed(() =>
  [props.street, props.city].filter(Boolean).join(', '),
)

const radiusMeters = computed(() => Math.max(0.5, props.radiusKm) * 1000)

async function geocode() {
  const parts = [props.street, props.city, 'Poland'].filter(Boolean)
  if (!props.city?.trim() && !props.street?.trim()) {
    coords.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    const q = encodeURIComponent(parts.join(', '))
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${q}`,
    )
    const data = (await res.json()) as Array<{ lat: string; lon: string }>
    coords.value = Array.isArray(data) && data.length
      ? [Number.parseFloat(data[0]!.lat), Number.parseFloat(data[0]!.lon)]
      : null
  } catch {
    coords.value = null
  } finally {
    loading.value = false
  }
}

function scheduleGeocode() {
  if (timer) clearTimeout(timer)
  timer = setTimeout(geocode, 500)
}

watch(() => [props.city, props.street], scheduleGeocode)
onMounted(geocode)
onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})
</script>

<template>
  <div class="area-map">
    <div v-if="loading" class="area-map-empty">
      <i class="pi pi-spin pi-spinner" /> Locating…
    </div>
    <LMap
      v-else-if="coords"
      :key="`${coords[0]}-${coords[1]}`"
      :zoom="13"
      :center="coords"
      :zoom-control="false"
      style="height: 100%; width: 100%; border-radius: 12px; z-index: 0"
    >
      <LTileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution="&copy; OpenStreetMap contributors"
      />
      <LCircle
        :lat-lng="coords"
        :radius="radiusMeters"
        :path-options="{
          color: '#34d399',
          fillColor: '#34d399',
          fillOpacity: 0.14,
          weight: 2,
          dashArray: '5 4',
        }"
      />
      <LMarker :lat-lng="coords" />
    </LMap>
    <div v-else class="area-map-empty">
      <i class="pi pi-map-marker" />
      <span>{{ locationText || 'Enter a city or street to preview the area' }}</span>
    </div>
  </div>
</template>
