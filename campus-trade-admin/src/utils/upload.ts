import request from './request'

export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<never, string>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}