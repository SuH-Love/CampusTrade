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
  onError: (error: string) => void,
  onSession?: (sessionId: string) => void
): { close: () => void } {
  const params = new URLSearchParams({ message })
  if (sessionId) params.append('sessionId', sessionId)

  const token = localStorage.getItem('token') || ''
  const controller = new AbortController()
  let done = false

  fetch(`/api/ai/chat/stream?${params.toString()}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'text/event-stream'
    },
    signal: controller.signal
  }).then(response => {
    if (!response.ok || !response.body) {
      throw new Error(`HTTP ${response.status}`)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const pump = (): Promise<void> => {
      return reader.read().then(({ done: streamDone, value }) => {
        if (streamDone) {
          if (!done) { done = true; onDone() }
          return
        }
        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        let currentEvent = 'message'
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.slice(5)
            if (currentEvent === 'session' && onSession) {
              onSession(data)
            } else if (currentEvent === 'message') {
              onMessage(data)
            } else if (currentEvent === 'done') {
              done = true
              reader.cancel()
              onDone()
              return
            } else if (currentEvent === 'error') {
              onError(data || 'AI服务暂时不可用')
              return
            }
          } else if (line === '') {
            currentEvent = 'message'
          }
        }
        return pump()
      })
    }

    return pump()
  }).catch((err: Error) => {
    if (err.name === 'AbortError') return
    if (!done) onError('AI服务连接失败')
  })

  return {
    close: () => controller.abort()
  }
}
