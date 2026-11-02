<script setup lang="ts">
const route = useRoute()

await navigateTo(
  { path: '/', query: { id: String(route.params.id) } },
  { replace: true },
)
</script>

<template>
  <div class="state state-fill">
    <i class="pi pi-spin pi-spinner" />
    <span>Opening report…</span>
  </div>
</template>
