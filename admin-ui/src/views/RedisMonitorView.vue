<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="缓存 Key 数（采样 ≤200）" :value="stats?.keyCount ?? '-'" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="命中次数" :value="stats?.keyspaceHits ?? '-'" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="未命中次数" :value="stats?.keyspaceMisses ?? '-'" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic
            title="命中率"
            :value="hitRateText"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span>缓存 Key 列表（shorturl:url:*）</span>
          <el-button :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-table :data="stats?.keys ?? []" v-loading="loading" style="width: 100%">
        <el-table-column prop="key" label="Key" min-width="240" />
        <el-table-column prop="ttlSeconds" label="TTL (秒)" width="120" align="center" sortable />
        <el-table-column label="剩余时间" width="200">
          <template #default="{ row }">
            <el-progress
              v-if="row.ttlSeconds > 0"
              :percentage="Math.round((row.ttlSeconds / 86400) * 100)"
              :stroke-width="10"
              :status="row.ttlSeconds < 3600 ? 'warning' : undefined"
            />
            <el-tag v-else type="info" size="small">永不过期</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getRedisStats, type RedisStatsVO } from '../api/monitor'

const stats = ref<RedisStatsVO | null>(null)
const loading = ref(false)

const hitRateText = computed(() => {
  if (!stats.value || stats.value.hitRate < 0) return 'N/A'
  return (stats.value.hitRate * 100).toFixed(4) + '%'
})

async function loadData() {
  loading.value = true
  try {
    stats.value = await getRedisStats()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
