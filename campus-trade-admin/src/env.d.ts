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
}

declare module 'element-plus/es/locale/lang/zh-cn' {
  const zhCn: any
  export default zhCn
}

declare module 'echarts' {
  export interface ECharts {
    setOption(option: any): void
    resize(): void
    dispose(): void
  }
  export function init(dom: HTMLElement, theme?: string): ECharts
}