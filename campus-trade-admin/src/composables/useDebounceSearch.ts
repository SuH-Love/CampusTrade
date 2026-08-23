import { ref, watch, type Ref } from 'vue'

export function useDebounceSearch(
  searchRef: Ref<string>,
  onSearch: () => void,
  delay = 300
): void {
  let timer: ReturnType<typeof setTimeout> | null = null
  watch(searchRef, () => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(onSearch, delay)
  })
}