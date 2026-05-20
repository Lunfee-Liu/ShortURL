# 项目概述

短链接服务（ShortURL Service）：将长 URL 转换为短码，提供 301/302 跳转，记录访问统计。

**容量目标**
- 阶段一：1k QPS，单机部署，MySQL + Redis 单节点
- 阶段二（未来）：100k QPS，支持水平扩展、分库分表、二级缓存、消息队列削峰

**架构原则**：阶段一实现简洁可用，但**接口和数据模型必须为阶段二预留扩展空间**，避免阶段二大规模重写。

---

# 技术栈（严格使用以下版本）

- JDK 21（可保守使用新特性：`var`、record、文本块、新 switch；**业务代码主体保持 Java 8 风格**便于团队成员阅读）
- Spring Boot 3.3.4
- Maven 3.9+
- MyBatis 3.5.x + mybatis-spring-boot-starter
- MyBatis Generator（逆向生成 entity / mapper / xml）
- MySQL 8.0
- Redis 7（Lettuce 客户端，spring-boot-starter-data-redis）
- Kafka 3.x（spring-kafka；用于访问日志异步削峰，消费端批量写 DB）
- JUnit 5 + Mockito + AssertJ
- Lombok

---

# 项目结构

```
src/main/java/com/example/shorturl/
├── controller/       # 仅参数接收、校验、调用 service、组装返回
├── service/          # 业务接口
│   └── impl/         # 业务实现
├── mapper/           # MyBatis Mapper 接口（由 generator 生成 + 手写扩展）
├── entity/           # 数据库实体，后缀 DO（如 ShortUrlDO），由 generator 生成
├── dto/              # 入参对象，后缀 DTO（如 CreateShortUrlDTO）
├── vo/               # 出参对象，后缀 VO（如 ShortUrlVO）
├── config/           # Spring 配置类（Redis、MyBatis、Web 等）
├── exception/        # 自定义异常 + 全局异常处理器
├── common/           # 通用：Result、ErrorCode、PageResult、常量
├── util/             # 工具类
└── generator/        # 短码生成策略
    └── strategy/     # 不同生成策略实现（自增+Base62、Snowflake 等）

src/main/resources/
├── mapper/           # MyBatis XML 映射文件
├── db/migration/     # Flyway 迁移脚本：V1__init.sql 等
├── application.yml
├── application-dev.yml
└── application-prod.yml
```

---

# 编码规范（必须遵守）

## 分层职责
- **Controller** 只做：参数校验（`@Valid`）、调 service、包装 `Result<T>` 返回。**禁止写业务逻辑**。
- **Service** 写业务、管事务、组合调用。**Service 之间互相调用走接口**，不直接调 Impl。
- **Mapper** 只做数据访问，**不能出现业务判断**。

## 命名约定
- Service 接口：`XxxService`；实现：`XxxServiceImpl`，放在 `service/impl/`
- 数据库实体后缀 `DO`（如 `ShortUrlDO`），由 MyBatis Generator 生成，**不要手改生成的实体**
- 入参 `DTO`、出参 `VO`、内部传输 `BO`（如有必要）
- 数据库表名：`snake_case`，复数（`short_urls`、`access_logs`）
- 字段：数据库 `snake_case`，Java `camelCase`

## 依赖注入
- **只用构造器注入**，配合 Lombok `@RequiredArgsConstructor`
- 禁用 `@Autowired` 字段注入和 setter 注入

## 异常处理
- 业务异常统一抛 `BizException(ErrorCode, String)`
- 全局异常处理器 `GlobalExceptionHandler` 兜底
- **禁止** `catch (Exception e) {}` 吞异常，必须打日志或上抛
- **禁止** `e.printStackTrace()`，统一用 `log.error("xxx, param={}", param, e)`

## 日志
- 类上加 `@Slf4j`，**禁用 `System.out.println`**
- 错误日志必须带上下文参数（短码、用户 ID 等）
- 占位符用 `{}`，不要字符串拼接
- 敏感信息（密码、token、完整 URL 中的 query）必须脱敏

## 事务
- 写操作的 Service 方法显式标注 `@Transactional(rollbackFor = Exception.class)`
- **禁止** 在事务方法内调用远程接口（HTTP、Redis 写、MQ 发送）
- 事务方法内的 Redis 操作要么放事务外，要么用 `TransactionSynchronizationManager` 在提交后执行

## 返回值
- Controller 必须返回 `Result<T>`，禁止返回裸对象或 `ResponseEntity` 包裸对象
- Service 返回业务对象，不返回 `Result<T>`
- 集合返回空集合（`List.of()`），**禁止返回 null**
- 单个对象可返回 `Optional<T>`，由 Service 决定语义

## Java 21 新特性使用准则
- ✅ 允许：`var`（局部变量类型明显时）、文本块（多行 SQL）、`Map.of()` / `List.of()`、增强 `instanceof`
- ⚠️ 谨慎：record（仅用于 DTO/VO，不用于 entity）、新 switch 表达式
- ❌ 暂不使用：virtual thread、sealed class、pattern matching for switch（团队不熟悉）

## 数据库
- **禁止** `spring.jpa.hibernate.ddl-auto=update`（本项目不用 JPA）
- 索引设计：高基数字段加索引（短码），长 URL 不加索引
- 所有表必须有 `id`、`created_at`、`updated_at`、`is_deleted`（逻辑删除）

## 短码格式规范
- **字符集**：`shorturl.generator.alphabet` 配置的乱序 Base62 字母表（62 个不重复字符）
- **最小长度**：`shorturl.generator.min-code-length`（默认 6），不足时以字母表首字符左补
- **最大长度**：8（受 `short_code VARCHAR(8)` 约束），这是系统设计容量上限
  - 系统最多支持 62⁸ ≈ 2180 亿条短链；超出属于容量耗尽，不是运行时异常
  - `generateFromId` 内部用 `assert code.length() <= 8` 拦截（需 JVM 启用 `-ea`）
- **反枚举性**：乱序字母表使连续 ID 映射到不连续短码，提升枚举攻击成本
- **不可逆性**：同一配置下相同 ID 总是产生相同短码；切换字母表配置会导致历史映射失效
- **阶段二演进**：切换到号段（Segment）方案后仍使用乱序字母表编码，继承反枚举特性

## MyBatis
- 简单 CRUD 用 generator 生成的 mapper
- 复杂查询写在 XML 中，**不用注解 SQL**（`@Select` 等只在单行简单查询时用）
- 动态 SQL 用 `<if>`、`<choose>`，避免字符串拼接
- 所有查询必须分页或加 limit，**禁止全表扫描**

## 缓存
- 缓存 key 命名：`shorturl:{业务}:{标识}`，如 `shorturl:url:abc123`
- 所有缓存必须设置 TTL，禁止永久缓存
- 防穿透：空值也缓存，TTL 较短（如 60s）
- 防雪崩：TTL 加随机 ±10% 抖动
- Redis 操作失败不能阻断主流程，降级到 DB

---

# 工作流程

## Git
- 分支：`feat/xxx`、`fix/xxx`、`refactor/xxx`、`docs/xxx`
- 提交信息遵循 Conventional Commits
- 主分支：`main`，禁止直推
- 每完成一个独立功能就 commit，不要堆大 commit

## 测试
- Service 层必须有单元测试（Mockito mock 掉 Mapper / Redis）
- 核心工具类（Base62、短码生成器）必须有单元测试
- 覆盖率目标：行覆盖 70%+，分支覆盖 60%+
- 集成测试用 Testcontainers，不依赖本地环境

## 提交前检查
1. `mvn clean verify` 通过
2. 新增 / 修改公共接口要更新 `docs/API.md`
3. 数据库变更要新增 Flyway 脚本，不修改已发布的脚本

---

# 常用命令

```bash
# 前置条件：必须使用 JDK 21（Maven Enforcer 会校验版本，版本不符直接报错）
# 推荐通过 jenv / asdf 管理多版本；或在 shell profile 中设置：
# export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
# export JAVA_HOME=/usr/lib/jvm/java-21-openjdk       # Linux

# 启动依赖环境（MySQL + Redis + Kafka）
docker-compose up -d

# 开发模式运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 跑测试
mvn test

# 完整验证（编译 + 测试 + 打包）
mvn clean verify

# MyBatis Generator 生成代码
mvn mybatis-generator:generate
```

---

# 不要做的事

- ❌ 不要在 Controller 写业务逻辑
- ❌ 不要在 Service 里直接 `new RestTemplate / OkHttpClient`，统一注入
- ❌ 不要 `catch Exception` 后吞掉
- ❌ 不要用 `@Data`，会导致 equals/hashCode 隐患
- ❌ 不要用 `BeanUtils.copyProperties` 做 DTO/VO 转换（性能差且不可控），手写转换方法或用 MapStruct
- ❌ 不要在循环里查数据库（N+1 问题），用批量查询
- ❌ 不要硬编码配置项，统一到 `application.yml`
- ❌ 不要在生产代码里留 `TODO` 不处理，要么做掉要么建 issue
- ❌ 不要直接修改 MyBatis Generator 生成的文件（重新生成会覆盖），扩展写在独立的 mapper 接口和 xml 中

---

# 为阶段二（100k QPS）预留的设计

虽然阶段一只做 1k QPS，但以下设计必须从一开始就做对，避免重写：

1. **短码生成器抽象为 `ShortCodeGenerator` 接口**，便于切换实现（自增+Base62 → Snowflake / 号段）
2. **数据库表预留 `shard_key` 字段**（即使阶段一不分片，字段先占位）
3. **缓存读写抽象为 `ShortUrlCacheService`**，便于阶段二加 Caffeine 本地缓存做二级缓存
4. **访问日志写入抽象为 `AccessLogRecorder` 接口**，M3 直接 Kafka 异步削峰 + 消费端批量写 DB（接口不变，实现可替换）
5. **接口设计支持批量**：`getByShortCodes(List<String>)` 而不是只有单条

---

# 当前进度

- [x] M1: 项目骨架 + 依赖环境（docker-compose、Flyway、MyBatis Generator 跑通）
- [x] M2: 短链生成 + 跳转（含 Redis 缓存）
- [ ] M3: 访问统计（Kafka 异步削峰 + 消费端批量写 DB）
- [ ] M4: Caffeine 二级缓存 + Redis 分布式限流 + Actuator 监控
- [ ] M5: 压测（JMeter / wrk）验证单节点 5-10k QPS + 多节点扩容线性验证

---

# 重要文档索引

需要更深入的设计细节时阅读：
- `docs/ARCHITECTURE.md` —— 整体架构与阶段二演进路径
- `docs/API.md` —— 对外 API 规范
- `docs/DATABASE.md` —— 表结构与索引设计
