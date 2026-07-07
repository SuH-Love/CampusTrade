import request from '@/utils/request'

export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, string>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const deleteImage = (fileUrl: string) =>
  request.delete<any, void>('/file/delete', { params: { fileUrl } })