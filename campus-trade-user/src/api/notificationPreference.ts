import request from '@/utils/request'

export interface NotificationPreferenceVO {
  id: number
  userId: number
  notificationType: string
  enabled: number
}

export const getMyPreferences = () => request.get<never, NotificationPreferenceVO[]>('/notification-preference')
export const setPreference = (type: string, enabled: number) => request.put(`/notification-preference/${type}`, null, { params: { enabled } })