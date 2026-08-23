/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}


declare module 'echarts' {
  export interface ECharts {
    setOption(option: any): void
    resize(): void
    dispose(): void
  }
  export function init(dom: HTMLElement, theme?: string): ECharts
}