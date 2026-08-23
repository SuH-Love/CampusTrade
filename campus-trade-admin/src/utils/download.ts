import request from '@/utils/request'

export async function downloadCsv(url: string, filename: string): Promise<void> {
  try {
    const res = await request.get(url, { responseType: 'blob' }) as unknown as Blob
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url2 = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url2
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(url2)
  } catch (e) {
    console.error(e)
  }
}