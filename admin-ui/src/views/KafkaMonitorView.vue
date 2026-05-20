<template>
  <el-card>
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <div>
          <span style="font-weight: 600">Topic：{{ stats?.topic }}</span>
          <el-tag style="margin-left: 12px" size="small">Group: {{ stats?.consumerGroup }}</el-tag>
          <el-tag
            style="margin-left: 8px"
            size="small"
            :type="(stats?.totalLag ?? 0) > 0 ? 'danger' : 'success'"
          >
            总 Lag：{{ stats?.totalLag ?? '-' }}
          </el-tag>
        </div>
        <el-button :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </template>

    <el-table :data="stats?.partitions ?? []" v-loading="loading" style="width: 100%">
      <el-table-column prop="partition" label="Partition" width="110" align="center" />
      <el-table-column prop="endOffset" label="End Offset（生产位置）" align="right" />
      <el-table-column prop="committedOffset" label="Committed Offset（消费位置）" align="right" />
      <el-table-column label="Lag" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.lag > 0 ? 'danger' : 'success'" size="small">{{ row.lag }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getKafkaStats, type KafkaStatsVO } from '../api/monitor'

const stats = ref<KafkaStatsVO | null>(null)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    stats.value = await getKafkaStats()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
