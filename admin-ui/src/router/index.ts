import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/short-urls' },
    { path: '/short-urls', component: () => import('../views/ShortUrlView.vue') },
    { path: '/access-logs', component: () => import('../views/AccessLogView.vue') },
    { path: '/monitor/redis', component: () => import('../views/RedisMonitorView.vue') },
    { path: '/monitor/kafka', component: () => import('../views/KafkaMonitorView.vue') },
  ],
})

export default router
