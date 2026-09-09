<template>
  <div class="admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>AI 工具列表</span>
          <el-tag size="small">共 {{ tools.length }} 个工具</el-tag>
        </div>
      </template>

      <div class="tool-section" v-if="queryTools.length">
        <h3 class="section-title">📖 查询工具（{{ queryTools.length }}）</h3>
        <div class="tool-grid">
          <div v-for="t in queryTools" :key="t.name as string" class="tool-card">
            <div class="tool-name">{{ t.name }}</div>
            <el-tooltip :content="String(t.description)" placement="top" :show-after="300" effect="dark">
              <div class="tool-desc">{{ t.description }}</div>
            </el-tooltip>
          </div>
        </div>
      </div>

      <div class="tool-section" v-if="writeTools.length">
        <h3 class="section-title">✏️ 写操作工具（{{ writeTools.length }}）</h3>
        <div class="tool-grid">
          <div v-for="t in writeTools" :key="t.name as string" class="tool-card tool-card-write">
            <div class="tool-name">{{ t.name }}</div>
            <el-tooltip :content="String(t.description)" placement="top" :show-after="300" effect="dark">
              <div class="tool-desc">{{ t.description }}</div>
            </el-tooltip>
          </div>
        </div>
      </div>

      <div class="tool-section" v-if="adminTools.length">
        <h3 class="section-title">🛡️ 管理员工具（{{ adminTools.length }}）</h3>
        <div class="tool-grid">
          <div v-for="t in adminTools" :key="t.name as string" class="tool-card tool-card-admin">
            <div class="tool-name">{{ t.name }}</div>
            <el-tooltip :content="String(t.description)" placement="top" :show-after="300" effect="dark">
              <div class="tool-desc">{{ t.description }}</div>
            </el-tooltip>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAiTools } from '@/api/admin'

const tools = ref<Record<string, unknown>[]>([])
const queryTools = computed(() => tools.value.filter(t => !t.writeOperation && !String(t.name).startsWith('admin_')))
const writeTools = computed(() => tools.value.filter(t => t.writeOperation && !String(t.name).startsWith('admin_')))
const adminTools = computed(() => tools.value.filter(t => String(t.name).startsWith('admin_')))

onMounted(async () => {
  try { tools.value = await getAiTools() } catch { /* ignore */ }
})
</script>

<style scoped lang="scss">
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tool-section { margin-bottom: 24px; }
.section-title { font-size: 15px; margin-bottom: 12px; color: var(--el-text-color-primary); }
.tool-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; }
.tool-card {
  padding: 12px 16px; border-radius: 8px; border: 1px solid var(--el-border-color-lighter);
  border-left: 3px solid #0ACFFE; background: var(--el-fill-color-blank);
  transition: all 0.2s;
  &:hover { border-color: #4FACFE; box-shadow: 0 2px 10px rgba(10, 207, 254, 0.15); }
}
.tool-card-write { border-left-color: #f5af19; &:hover { border-color: #f5af19; box-shadow: 0 2px 10px rgba(245, 175, 25, 0.15); } }
.tool-card-admin { border-left-color: #f5576c; &:hover { border-color: #f5576c; box-shadow: 0 2px 10px rgba(245, 87, 108, 0.15); } }
.tool-name { font-size: 13px; font-weight: 600; margin-bottom: 6px; color: var(--el-text-color-primary); }
.tool-desc { font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>