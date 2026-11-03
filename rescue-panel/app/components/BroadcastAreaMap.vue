<script setup lang="ts">
import { LCircle, LMap, LMarker, LTileLayer } from '@vue-leaflet/vue-leaflet'
import L from 'leaflet'

const props = withDefaults(defineProps<{
  city?: string | null
  street?: string | null
  radiusKm: number
  dark?: boolean
}>(), {
  dark: false,
})

const coords = ref<[number, number] | null>(null)
const loading = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

const tileUrl = computed(() =>
  props.dark
    ? 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}'
    : 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
)

const labelTileUrl = computed(() =>
  'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Reference/MapServer/tile/{z}/{y}/{x}',
)

const tileAttribution = computed(() =>
  props.dark
    ? 'Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ'
    : '&copy; OpenStreetMap contributors',
)

const locationText = computed(() =>
  [props.street, props.city].filter(Boolean).join(', '),
)

const radiusMeters = computed(() => Math.max(0.1, props.radiusKm) * 1000)

const radiusLabel = computed(() =>
  props.radiusKm < 1
    ? `${Math.round(props.radiusKm * 1000)} m`
    : `${props.radiusKm.toFixed(1)} km`,
)

// Place the radius pill on the eastern edge of the circle.
const labelCoords = computed<[number, number] | null>(() => {
  if (!coords.value) return null
  const [lat, lon] = coords.value
  const latRadians = (lat * Math.PI) / 180
  const deltaLon = props.radiusKm / (111.32 * Math.cos(latRadians))
  return [lat - props.radiusKm / 111.32 / 4, lon + deltaLon]
})

const pinIcon = L.divIcon({
  className: 'area-pin',
  html: '<span class="area-pin__body"></span><span class="area-pin__dot"></span>',
  iconSize: [34, 44],
  iconAnchor: [17, 44],
})

const radiusIcon = computed(() =>
  L.divIcon({
    className: 'area-radius-label',
    html: `<span>${radiusLabel.value}</span>`,
    iconSize: [70, 24],
    iconAnchor: [35, 12],
  }),
)

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
</script>

<template>
  <div class="area-map" :class="{ 'area-map--dark': dark }">
    <div v-if="loading" class="area-map-empty">
      <i class="pi pi-spin pi-spinner" /> Locating…
    </div>
    <LMap
      v-else-if="coords"
      :key="`${coords[0]}-${coords[1]}`"
      :zoom="13"
      :center="coords"
      :zoom-control="true"
      style="height: 100%; width: 100%; border-radius: 12px; z-index: 0"
    >
      <LTileLayer
        :url="tileUrl"
        :attribution="tileAttribution"
      />
      <LTileLayer
        v-if="dark"
        :url="labelTileUrl"
      />
      <LCircle
        :lat-lng="coords"
        :radius="radiusMeters"
        :path-options="{
          color: '#ef4444',
          fillColor: '#ef4444',
          fillOpacity: 0.07,
          weight: 2,
          dashArray: '7 6',
        }"
        color="#ef4444"
        fill-color="#ef4444"
        :fill-opacity="0.07"
        :weight="2"
        dash-array="7 6"
      />
      <LMarker :lat-lng="coords" :icon="pinIcon" />
      <LMarker
        v-if="labelCoords"
        :lat-lng="labelCoords"
        :icon="radiusIcon"
        :interactive="false"
      />
    </LMap>
    <div v-else class="area-map-empty">
      <i class="pi pi-map-marker" />
      <span>{{ locationText || 'Enter a city or street to preview the area' }}</span>
    </div>
  </div>
</template>
