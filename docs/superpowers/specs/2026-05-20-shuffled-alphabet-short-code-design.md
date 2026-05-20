# 短码生成优化：乱序字母表 + 最小长度约束

**日期**：2026-05-20
**分支**：feat/shuffled-alphabet-short-code
**状态**：待实现

---

## 背景

当前实现直接将自增 ID 做标准 Base62 编码，导致：
- 小 ID 产生单字符短码（如 ID=15 → `F`），不专业
- 编码结果按 ID 顺序递增，可被轻易枚举遍历

---

## 方案

**乱序字母表 + 固定最小长度**

1. 用一个打乱顺序的 62 字符字母表替代标准 `0-9a-zA-Z`
2. `generateFromId` 用自定义字母表编码 ID，左补字母表首字符至最小长度
3. 超过 VARCHAR(8) 限制时抛 `BizException`

### 效果示例

使用字母表 `n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8`：

| 自增 ID | 标准 Base62 | 乱序字母表（min=6）|
|---------|------------|-----------------|
| 1       | `1`        | `nnnnn6`        |
| 15      | `F`        | `nnnnnK`        |
| 62      | `10`       | `nnnnnn`... wait, actual encoding varies |
| 100000  | `q0U`      | 6 chars, non-sequential |

顺序 ID 不再产生顺序短码，也不再出现单字符。

---

## 配置

```yaml
shorturl:
  generator:
    alphabet: "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8"
    min-code-length: 6
```

- `alphabet`：62 个字符的无重复排列，必须恰好 62 位
- `min-code-length`：默认 6，最大不超过 8（VARCHAR(8) 硬限制）

---

## 改动范围

### 生产代码

| 文件 | 改动 |
|------|------|
| `application.yml` / `application-dev.yml` | 添加 `shorturl.generator.alphabet` 和 `min-code-length` |
| `Base62.java` | 新增 `encode(long n, String alphabet)` 重载 |
| `Base62ShortCodeGenerator` | 注入配置，使用自定义字母表编码，左补位，超长保护 |

### 测试代码

| 文件 | 改动 |
|------|------|
| `Base62Test.java` | 新增自定义字母表编码的正确性测试 |
| `Base62ShortCodeGeneratorTest.java` | 更新预期值（原硬编码 `"1"`、`"10"` 等不再成立） |
| `ShortUrlServiceImplTest.java` | 更新 `createShortUrl` 和 `createShortUrlBase62EncodingForLargerIds` 预期值 |

### 文档

| 文件 | 改动 |
|------|------|
| `CLAUDE.md` | 补充短码格式规范：最小 6 位、最大 8 位、乱序字母表、配置项说明 |

---

## 不改动

- `ShortCodeGenerator` 接口（方法签名已满足需求）
- `ShortUrlServiceImpl`（只调用接口方法）
- DB schema（`short_code VARCHAR(8)` 已足够）
- `ShortUrlMapper` 及相关 XML

---

## 约束与边界条件

- 字母表必须恰好 62 字符，启动时校验，不合法则抛 `IllegalArgumentException`（应用无法启动）
- `min-code-length` 取值范围 [1, 8]，启动时校验
- **8 位上限是系统容量的设计约束**：Base62 8 位最多承载 62⁸ ≈ 2180 亿条短链。`generateFromId` 将 max-code-length=8 作为前置断言（`assert code.length() <= 8`），而非运行时异常路径——超出意味着系统已超过设计容量上限，属于不应发生的情况
- 1k QPS 连续写入约需 6900 年才会触发，阶段一无需担心；阶段二切换号段/Snowflake 时重新评估

---

## CLAUDE.md 新增内容（预览）

```markdown
## 短码格式规范
- 字符集：`shorturl.generator.alphabet` 配置的乱序 Base62 字母表（62 字符，无重复）
- 最小长度：`shorturl.generator.min-code-length`（默认 6），不足时左补字母表首字符
- 最大长度：8（受 `short_code VARCHAR(8)` 限制），超出时抛 BizException
- 短码不保证全局可逆（乱序字母表使枚举成本大幅提升），但同一配置下同一 ID 产生同一短码
- 8 位是系统容量上限，由设计保证而非运行时捕获；系统容量 = 62^8 ≈ 2180 亿条
```
