import request from '@/utils/request'

export interface FaqItem {
  id?: number
  question: string
  answer: string
  category: string
}

export const getFaqList = () =>
  request.get<never, FaqItem[]>('/ai/faq')

export const addFaq = (data: { question: string; answer: string; category: string }) =>
  request.post('/ai/faq', data)

export const updateFaq = (index: number, data: { question: string; answer: string; category: string }) =>
  request.put(`/ai/faq/${index}`, data)

export const deleteFaq = (index: number) =>
  request.delete(`/ai/faq/${index}`)

export const getAiPrompt = () =>
  request.get<never, string>('/ai/prompt')

export const updateAiPrompt = (prompt: string) =>
  request.put('/ai/prompt', { prompt })

export const getFeedbackStats = () =>
  request.get<never, { avgRating: number }>('/ai/feedback/stats')