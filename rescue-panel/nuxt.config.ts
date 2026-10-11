import Aura from '@primevue/themes/aura'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  // Internal operations dashboard - client side rendering only
  ssr: false,

  modules: [
    '@pinia/nuxt',
    '@primevue/nuxt-module',
    '@nuxt/eslint',
  ],

  css: [
    'primeicons/primeicons.css',
    'leaflet/dist/leaflet.css',
    '~/assets/css/main.css',
  ],

  primevue: {
    options: {
      ripple: true,
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.app-dark',
        },
      },
    },
  },

  runtimeConfig: {
    public: {
      // Base URL of the API gateway. Override with NUXT_PUBLIC_API_BASE.
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8007',
    },
  },

  app: {
    head: {
      title: 'SENSA — Rescue Panel',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
      ],
    },
  },
})
