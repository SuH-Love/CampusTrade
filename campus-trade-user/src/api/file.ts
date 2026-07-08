import request from '@/utils/request'

export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<never, string>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const deleteImage = (fileUrl: string) =>
  request.delete<never, void>('/file/delete', { params: { fileUrl } })
