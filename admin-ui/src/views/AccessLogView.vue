<template>
  <el-card>
    <el-form :inline="true" @submit.prevent="loadData" style="margin-bottom: 16px">
      <el-form-item label="短码">
        <el-input v-model="shortCode" placeholder="请输入短码" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="~"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" style="width: 100%">
      <el-table-column prop="shortCode" label="短码" width="110" />
      <el-table-column prop="accessedAt" label="访问时间" width="200" />
      <el-table-column prop="clientIp" label="IP" width="140" />
      <el-table-column prop="userAgent" label="User-Agent" min-width="220" show-overflow-tooltip />
      <el-table-column prop="referer" label="Referer" min-width="180" show-overflow-tooltip />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end; display: flex"
      @change="loadData"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAccessLogs, type AccessLogVO } from '../api/accessLog'

const records = ref<AccessLogVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const shortCode = ref('')
const dateRange = ref<[string, string] | null>(null)
const loading = ref(false)

async function loadData() {
  if (!shortCode.value.trim()) {
    ElMessage.warning('请输入短码')
    return
  }
  loading.value = true
  try {
    const res = await listAccessLogs({
      shortCode: shortCode.value.trim(),
      page: page.value,
      size: pageSize.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
    })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
</script>
