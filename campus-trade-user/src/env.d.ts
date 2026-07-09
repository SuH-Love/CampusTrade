/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'element-plus' {
  const ElementPlus: any
  export default ElementPlus
  export const ElMessage: any
  export const ElMessageBox: any
  export const ElNotification: any
  export const ElLoading: any
  export type FormInstance = any
  export type FormRules = any
}

declare module 'element-plus/es/locale/lang/zh-cn' {
  const zhCn: Record<string, any>
  export default zhCn
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}