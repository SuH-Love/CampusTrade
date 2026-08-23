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

export const getSessionHistory = (sessionId: string) =>
  request.get<never, Array<{ role: string; content: string; timestamp?: number }>>(`/ai/session/${sessionId}/history`)

export const getGoodsSuggestion = (goodsId: number) =>
  request.get<never, { suggestedTitle: string | null; has: boolean }>(`/ai/suggestion/${goodsId}`)

export function chatStream(
  message: string,
  sessionId: string | undefined,
  onMessage: (token: string) => void,
  onDone: () => void,
  onError: (error: string) => void,
  onSession?: (sessionId: string) => void,
  onThinking?: (status: string) => void,
  onToolCall?: (toolCall: { id: string; name: string; args: Record<string, unknown> }) => void,
  onToolResult?: (toolResult: { id: string; name: string; result: string }) => void
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
    let currentEvent = 'message'

    const processLine = (line: string): boolean => {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        const data = line.slice(5).trim()
        if (currentEvent === 'session' && onSession) {
          onSession(data)
        } else if (currentEvent === 'message') {
          try {
            const parsed = JSON.parse(data)
            if (parsed && typeof parsed.content === 'string') {
              onMessage(parsed.content)
            } else {
              onMessage(data)
            }
          } catch {
            onMessage(data)
          }
        } else if (currentEvent === 'thinking' && onThinking) {
          onThinking(data)
        } else if (currentEvent === 'tool_call' && onToolCall) {
          try {
            onToolCall(JSON.parse(data))
          } catch {}
        } else if (currentEvent === 'tool_result' && onToolResult) {
          try {
            onToolResult(JSON.parse(data))
          } catch {}
        } else if (currentEvent === 'done') {
          done = true
          reader.cancel()
          onDone()
          return false
        } else if (currentEvent === 'error') {
          onError(data || 'AI服务暂时不可用')
          return false
        }
      } else if (line === '') {
        currentEvent = 'message'
      }
      return true
    }

    const pump = (): Promise<void> => {
      return reader.read().then(({ done: streamDone, value }) => {
        if (streamDone) {
          if (buffer) {
            processLine(buffer)
            buffer = ''
          }
          if (!done) { done = true; onDone() }
          return
        }
        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (!processLine(line)) return
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

