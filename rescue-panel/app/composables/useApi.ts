import { useAuthStore } from '~/stores/auth'

interface ApiErrorBody {
  error?: string
  message?: string
}

/**
 * Configured $fetch instance for talking to the API gateway.
 * Automatically attaches the JWT bearer token and normalises error messages.
 */
export function useApi() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  return $fetch.create({
    baseURL: config.public.apiBase,
    onRequest({ options }) {
      if (auth.token) {
        const headers = new Headers(options.headers as HeadersInit)
        headers.set('Authorization', `Bearer ${auth.token}`)
        options.headers = headers
      }
    },
    onResponseError({ response }) {
      const data = response._data as ApiErrorBody | undefined
      const message =
        data?.message || data?.error || response.statusText || 'Request failed'
      throw new Error(message)
    },
  })
}
