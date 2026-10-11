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
