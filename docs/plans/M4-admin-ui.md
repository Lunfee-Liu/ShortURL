# M4 方案：后台管理页面（Admin UI）

> **状态**：✅ 已完成（2026-05-21）
> **分支**：`feat/m4-admin-ui`
> **关键提交**：
> - `c057f33` feat(m4): add Admin backend — CRUD, access-log, Redis/Kafka monitor
> - `5feadae` feat(m4): add Vue 3 Admin UI — short URLs, access logs, Redis/Kafka monitor

---

## Context

M1-M3 完成了核心业务（短链生成、跳转、访问统计）。M4 目标是为运维人员提供一个可视化后台，
覆盖四个模块：短链管理、访问日志、Redis 监控、Kafka 监控。

技术选型：Vue 3 SPA（Vite + Element Plus）放 `admin-ui/` 子目录，后端新增 Admin REST 接口，
无认证，CORS 允许本地开发端口。

---

## 整体结构

```
ShortURL/
├── src/                          ← 现有 Spring Boot 后端（扩展）
└── admin-ui/                     ← 新增 Vue 3 前端项目
```

---

## 一、后端

### 1.1 新增文件

| 类型 | 路径 | 说明 |
|---|---|---|
| Config | `config/WebConfig.java` | CORS，允许 localhost:5173 / :3000 |
| Controller | `controller/admin/AdminShortUrlController.java` | 短链 CRUD |
| Controller | `controller/admin/AdminAccessLogController.java` | 访问日志分页 |
| Controller | `controller/admin/AdminMonitorController.java` | Redis + Kafka 监控 |
| Service 接口 | `service/AdminShortUrlService.java` | |
| Service 实现 | `service/impl/AdminShortUrlServiceImpl.java` | |
| Service 接口 | `service/AdminAccessLogService.java` | |
| Service 实现 | `service/impl/AdminAccessLogServiceImpl.java` | |
| Service 接口 | `service/MonitorService.java` | |
| Service 实现 | `service/impl/MonitorServiceImpl.java` | |
| DTO | `dto/AdminListShortUrlDTO.java` | 短链列表查询入参 |
| DTO | `dto/AdminListAccessLogDTO.java` | 日志列表查询入参 |
| VO | `vo/AdminShortUrlVO.java` | 短链列表/详情出参 |
| VO | `vo/AdminAccessLogVO.java` | 日志列表出参 |
| VO | `vo/RedisStatsVO.java` | Redis 监控出参 |
| VO | `vo/KafkaStatsVO.java` | Kafka 监控出参 |

### 1.2 扩展现有文件

| 文件 | 扩展内容 |
|---|---|
| `mapper/AccessLogMapper.java` | 新增 `selectPageByShortCode` / `countByShortCode` |
| `resources/mapper/AccessLogMapper.xml` | 对应 SQL |
| `resources/mapper/ShortUrlMapper.xml` | 新增带 visit_count 的分页列表查询（LEFT JOIN access_logs） |
| `mapper/ShortUrlMapper.java` | 新增 `selectAdminPage` / `countAdmin` |

### 1.3 接口设计

```
# 短链管理
GET    /api/v1/admin/short-urls?page=1&size=20&keyword=         列表（含访问量）
GET    /api/v1/admin/short-urls/{id}                            详情
DELETE /api/v1/admin/short-urls/{id}                            逻辑删除

# 访问日志（只读）
GET    /api/v1/admin/access-logs?shortCode=&page=1&size=20&startDate=&endDate=

# 监控
GET    /api/v1/admin/monitor/redis    Redis keys + INFO stats（keyspace_hits/misses）
GET    /api/v1/admin/monitor/kafka    topic offset + consumer group lag
```

> 创建短链直接复用现有 `POST /api/v1/short-url`，不重复实现。

### 1.4 关键实现细节

**短链列表（带访问量）**：手写 XML，LEFT JOIN access_logs COUNT，用 `idx_short_code` 索引。
支持 keyword 模糊搜索 short_code 或 original_url（LIKE '%?%'，M4 数据量小可接受）。

**Redis 监控**：
```java
// 读 INFO stats
Properties info = stringRedisTemplate.execute(
    (RedisCallback<Properties>) conn -> conn.serverCommands().info("stats"));
// keyspace_hits / keyspace_misses 计算命中率

// SCAN 遍历 shorturl:url:* 的 keys 和 TTL（限制最多返回 200 个，防止大规模扫描）
ScanOptions opts = ScanOptions.scanOptions().match("shorturl:url:*").count(200).build();
```

**Kafka 监控**：
```java
// spring-kafka 已包含 AdminClient，无需新增依赖
try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
    adminClient.listOffsets(topicPartitions)          // end offsets（生产位置）
    adminClient.listConsumerGroupOffsets(groupId)      // committed offsets（消费位置）
    // lag = endOffset - committedOffset
}
```

**CORS**：
```java
// config/WebConfig.java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:5173", "http://localhost:3000")
    .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
    .allowedHeaders("*").maxAge(3600);
```

---

## 二、前端 admin-ui/

### 2.1 技术栈

| 包 | 用途 |
|---|---|
| Vite + Vue 3 + TypeScript | 构建 + 框架 |
| Element Plus | UI 组件库 |
| Vue Router 4 | 路由 |
| Axios | HTTP 请求 |

### 2.2 目录结构

```
admin-ui/
├── package.json
├── vite.config.ts               ← 开发代理 /api → localhost:8080
├── index.html
└── src/
    ├── main.ts
    ├── App.vue                  ← 侧边栏 + 路由出口
    ├── router/index.ts
    ├── api/
    │   ├── http.ts              ← axios 拦截器，统一解包 Result<T>
    │   ├── shortUrl.ts
    │   ├── accessLog.ts
    │   └── monitor.ts
    └── views/
        ├── ShortUrlView.vue     ← 短链管理表格 + 新建 Dialog
        ├── AccessLogView.vue    ← 日志列表 + 短码筛选
        ├── RedisMonitorView.vue ← key 列表 + 命中率卡片
        └── KafkaMonitorView.vue ← topic offset + lag 表格
```

### 2.3 各页面核心交互

**短链管理**：
- 表格：shortCode / originalUrl（截断+tooltip）/ 访问量 / 创建时间 / 操作
- 顶部搜索框 keyword + 新建按钮
- 操作列：复制短链、删除（二次确认）
- 新建弹窗复用现有 `POST /api/v1/short-url`

**访问日志**：
- 必填 shortCode 输入框，可选时间范围
- 表格：访问时间 / IP / UA / Referer

**Redis 监控**：
- 卡片：总 key 数 / 命中次数 / 未命中次数 / 命中率
- key 列表表格：key / TTL(s) / 剩余时间进度条（<1h 标黄）

**Kafka 监控**：
- 表格：partition / end offset / consumer offset / lag
- lag > 0 时标红

---

## 三、开发顺序（实际执行）

1. ✅ 后端 WebConfig（CORS）
2. ✅ DTO / VO 类（6 个）
3. ✅ Mapper 扩展（AccessLogMapper + ShortUrlMapper）
4. ✅ Service 接口 + 实现（3 对）
5. ✅ Controller（3 个 Admin Controller）
6. ✅ `mvn test` 66 个测试全绿
7. ✅ `npm create vite` 初始化前端骨架（Node 26 via brew）
8. ✅ 安装依赖，配置 Router + axios 拦截器
9. ✅ 逐个实现 4 个 View 页面
10. ✅ `npm run build` 通过，curl 验证 6 个 Admin 接口

---

## 四、遗留 / 后续优化

- 短链**编辑**（修改原始 URL）：M4 只做删除，编辑需同步更新缓存，留 M5
- Element Plus 全量引入（主 chunk 1.1MB），生产环境可改为按需引入
- 前端打包集成到 Spring Boot `static/`：M4 仅本地联调，部署方式后续决定
- 访问统计图表（ECharts 日趋势）：未实现，可选
- Admin 认证：未实现（用户确认 M4 不需要）

---

## 五、启动方式

```bash
# 后端
java -jar target/shorturl-0.0.1-SNAPSHOT.jar

# 前端
cd admin-ui && npm run dev
# → http://localhost:5173

# 停止
jps | grep shorturl   # 查后端 PID
kill <PID>
pkill -f vite         # 停前端
```
