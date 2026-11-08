export type EmergencyStatus =
  | 'REPORTED'
  | 'UNDER_REVIEW'
  | 'CONFIRMED'
  | 'BROADCAST'
  | 'REJECTED'

export type TemplateChannel = 'EMAIL' | 'PUSH' | 'SMS'

export interface FileRef {
  fileId: number
  url: string
}

/** Emergency as returned by the Rust emergency-service (snake_case JSON). */
export interface RawEmergency {
  id: number
  user_id: string
  title: string
  description: string | null
  files: FileRef[] | null
  country: string | null
  city: string | null
  street: string | null
  alarm_timestamp: string | null
  status: EmergencyStatus
  created_at: string
}

/** Emergency normalised to camelCase for the UI. */
export interface Emergency {
  id: number
  userId: string
  title: string
  description: string | null
  files: FileRef[]
  country: string | null
  city: string | null
  street: string | null
  alarmTimestamp: string | null
  status: EmergencyStatus
  createdAt: string
}

/** Template as returned by the Kotlin template-service (camelCase JSON). */
export interface Template {
  id: string | null
  templateName: string
  description: string | null
  channel: TemplateChannel
  content: string
  createdBy: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface TemplateRequest {
  templateName: string
  description: string | null
  channel: TemplateChannel
  content: string
}

export interface AuthResponse {
  token: string
  userId: string
  email: string
}

export interface TokenValidationResponse {
  valid: boolean
  userId: string | null
  email: string | null
}

export type AlertType = 'FIRE' | 'ACCIDENT' | 'WEATHER' | 'EVACUATION' | 'OTHER'

/** Operator broadcast stored by notification-service. */
export interface BroadcastHistory {
  id: string
  alertType: AlertType
  title: string
  message: string
  audience: string
  city?: string | null
  street?: string | null
  radiusKm?: number | null
  recipients: number
  deliveries: number
  filtered: number
  errors: number
  status: string
  deliveryTimeline?: number[]
  createdAt: string
}

export type ModerationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** File metadata returned by filesystem-service (camelCase JSON). */
export interface FileResponse {
  id: number
  userId: string
  url: string | null
  filename: string
  emergencySituationId: number | null
  moderationStatus: ModerationStatus
  moderationConfidence: number | null
  createdAt: string
}

export interface TestModeState {
  id?: string
  enabled: boolean
  runId: string | null
  expiresAt: string | null
}

export interface TestRunEndpoint {
  path: string
  requests: number
  errors: number
  p95: number
}

export interface TestRunPoint {
  t?: number
  rps?: number
  p95?: number
  vus?: number
  success?: number
}

export interface TestRun {
  id: string
  _id?: string
  scenario: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'STOPPED'
  vus: number
  durationSec: number
  rampUpSec: number
  target: string
  startedAt: string
  finishedAt: string | null
  totalRequests: number
  rps: number
  successRate: number
  count2xx: number
  count3xx: number
  count4xx: number
  count5xx: number
  p50: number
  p95: number
  p99: number
  endpoints: TestRunEndpoint[]
  series?: TestRunPoint[]
  testMode: boolean
  operatorId: string | null
}

export interface TestRunPage {
  items: TestRun[]
  total: number
}
