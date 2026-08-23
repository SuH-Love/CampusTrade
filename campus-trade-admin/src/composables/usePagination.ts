import { ref } from 'vue'

export function usePagination(defaultPageSize = 10) {
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  const handleSizeChange = (size: number, loadData: () => void) => {
    pageSize.value = size
    pageNum.value = 1
    loadData()
  }

  const handleCurrentChange = (loadData: () => void) => {
    loadData()
  }

  const resetPage = () => {
    pageNum.value = 1
  }

  return {
    pageNum,
    pageSize,
    total,
    handleSizeChange,
    handleCurrentChange,
    resetPage,
  }
}