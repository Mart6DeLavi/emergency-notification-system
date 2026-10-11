import type { Emergency, EmergencyStatus, RawEmergency } from '~/types'

export function normalizeEmergency(raw: RawEmergency): Emergency {
  return {
    id: raw.id,
    userId: raw.user_id,
    title: raw.title,
    description: raw.description,
    files: raw.files ?? [],
    country: raw.country,
    city: raw.city,
    street: raw.street,
    alarmTimestamp: raw.alarm_timestamp,
    status: raw.status,
    createdAt: raw.created_at,
  }
}

const TRANSITIONS: Record<EmergencyStatus, EmergencyStatus[]> = {
  REPORTED: ['UNDER_REVIEW', 'REJECTED'],
  UNDER_REVIEW: ['CONFIRMED', 'REJECTED'],
  CONFIRMED: ['BROADCAST', 'REJECTED'],
  BROADCAST: ['REJECTED'],
  REJECTED: [],
}

export function allowedTransitions(status: EmergencyStatus): EmergencyStatus[] {
  return TRANSITIONS[status] ?? []
}

export function statusLabel(status: EmergencyStatus): string {
  switch (status) {
    case 'REPORTED':
      return 'Reported'
    case 'UNDER_REVIEW':
      return 'Under review'
    case 'CONFIRMED':
      return 'Confirmed'
    case 'BROADCAST':
      return 'Broadcast'
    case 'REJECTED':
      return 'Rejected'
    default:
      return status
  }
}

export function statusSeverity(
  status: EmergencyStatus,
): 'secondary' | 'info' | 'warn' | 'success' | 'danger' {
  switch (status) {
    case 'REPORTED':
      return 'secondary'
    case 'UNDER_REVIEW':
      return 'info'
    case 'CONFIRMED':
      return 'warn'
    case 'BROADCAST':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'secondary'
  }
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}
