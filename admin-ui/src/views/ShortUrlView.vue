<template>
  <div>
    <el-card style="margin-bottom: 16px">
      <el-row :gutter="12" align="middle">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索短码或原始 URL" clearable @change="loadData" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="showCreate = true" style="margin-left: 8px">新建</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="records" v-loading="loading" style="width: 100%">
        <el-table-column prop="shortCode" label="短码" width="110" />
        <el-table-column label="短链" width="220">
          <template #default="{ row }">
            <el-link :href="row.shortUrl" target="_blank" type="primary">{{ row.shortUrl }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="原始 URL" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ row.originalUrl }}</template>
        </el-table-column>
        <el-table-column prop="visitCount" label="访问量" width="90" align="center" sortable />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="copyLink(row.shortUrl)">复制</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end; display: flex"
        @change="loadData"
      />
    </el-card>

    <!-- Create dialog -->
    <el-dialog v-model="showCreate" title="新建短链" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="原始 URL">
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listShortUrls, deleteShortUrl, createShortUrl, type ShortUrlVO } from '../api/shortUrl'

const records = ref<ShortUrlVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const form = ref({ url: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await listShortUrls({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(row: ShortUrlVO) {
  await ElMessageBox.confirm(`确认删除短码 "${row.shortCode}"？`, '删除确认', { type: 'warning' })
  await deleteShortUrl(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleCreate() {
  if (!form.value.url) return ElMessage.warning('请输入 URL')
  creating.value = true
  try {
    const res = await createShortUrl(form.value.url)
    ElMessage.success(`创建成功：${res.shortUrl}`)
    showCreate.value = false
    form.value.url = ''
    loadData()
  } finally {
    creating.value = false
  }
}

function copyLink(url: string) {
  navigator.clipboard.writeText(url)
  ElMessage.success('已复制')
}

onMounted(loadData)
</script>
