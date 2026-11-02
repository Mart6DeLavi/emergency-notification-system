<script setup lang="ts">
useHead({
  htmlAttrs: { class: 'app-dark' },
})
</script>

<template>
  <div>
    <NuxtRouteAnnouncer />
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
    <Toast position="bottom-right" />
    <ConfirmDialog />
  </div>
</template>
