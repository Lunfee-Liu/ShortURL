<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #001529">
      <div style="color: #fff; font-size: 16px; font-weight: 600; padding: 20px 16px; border-bottom: 1px solid #002140">
        ShortURL Admin
      </div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#001529"
        text-color="#ccc"
        active-text-color="#1890ff"
      >
        <el-menu-item index="/short-urls">
          <el-icon><Link /></el-icon>
          <span>短链管理</span>
        </el-menu-item>
        <el-menu-item index="/access-logs">
          <el-icon><List /></el-icon>
          <span>访问日志</span>
        </el-menu-item>
        <el-sub-menu index="monitor">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>监控</span>
          </template>
          <el-menu-item index="/monitor/redis">Redis</el-menu-item>
          <el-menu-item index="/monitor/kafka">Kafka</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #eee; line-height: 60px; font-size: 15px; padding: 0 24px">
        {{ pageTitle }}
      </el-header>
      <el-main style="background: #f0f2f5; padding: 24px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pageTitle = computed(() => {
  const map: Record<string, string> = {
    '/short-urls': '短链管理',
    '/access-logs': '访问日志',
    '/monitor/redis': 'Redis 监控',
    '/monitor/kafka': 'Kafka 监控',
  }
  return map[route.path] ?? 'Admin'
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
</style>
