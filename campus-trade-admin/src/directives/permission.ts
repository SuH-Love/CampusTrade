import type { Directive, DirectiveBinding } from 'vue'
import { useAdminStore } from '@/stores/admin'

function checkPermission(binding: DirectiveBinding<string | string[]>): boolean {
  const adminStore = useAdminStore()
  const value = binding.value
  if (!value) return true
  return adminStore.hasPermission(value)
}

export const vPermission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    if (!checkPermission(binding)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el, binding) {
    if (!checkPermission(binding)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export const vRole: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const adminStore = useAdminStore()
    const value = binding.value
    if (!value) return
    if (!adminStore.hasRole(value)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el, binding) {
    const adminStore = useAdminStore()
    const value = binding.value
    if (!value) return
    if (!adminStore.hasRole(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}