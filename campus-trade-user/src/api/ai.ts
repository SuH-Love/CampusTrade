import request from '@/utils/request'

export interface ChatRequest {
  message: string
  sessionId?: string
}

export interface ChatResponse {
  answer: string
  sessionId: string
  fallback: boolean
  hasFaqContext: boolean
}

export interface AiStatus {
  enabled: boolean
  model: string
}

export const chat = (data: ChatRequest) =>
  request.post<never, ChatResponse>('/ai/chat', data)

export const clearSession = (sessionId: string) =>
  request.delete(`/ai/session/${sessionId}`)

export const getAiStatus = () =>
  request.get<never, AiStatus>('/ai/status')

export function chatStream(
  message: string,
  sessionId: string | undefined,
  onMessage: (token: string) => void,
  onDone: () => void,
  onError: (error: string) => void
): { close: () => void } {
  const params = new URLSearchParams({ message })
  if (sessionId) params.append('sessionId', sessionId)

  const eventSource = new EventSource(`/api/ai/chat/stream?${params.toString()}`)

  eventSource.addEventListener('message', (e: MessageEvent) => {
    onMessage(e.data)
  })

  eventSource.addEventListener('done', () => {
    eventSource.close()
    onDone()
  })

  eventSource.addEventListener('error', () => {
    eventSource.close()
    onError('AI服务连接失败')
  })

  return {
    close: () => eventSource.close()
  }
}