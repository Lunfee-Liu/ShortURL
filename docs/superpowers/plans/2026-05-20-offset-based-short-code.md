# Offset-Based Short Code Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将短码生成从"编码后左补位"改为"先加偏移再编码"，消除重复前缀字符，使小 ID 也能产出视觉上自然的 6 位短码。

**Architecture:** 在 `Base62ShortCodeGenerator` 构造时计算 `codeOffset = 62^(minCodeLength-1)`，`generateFromId` 改为 `encode(id + codeOffset, alphabet)`，天然产出 minCodeLength 位结果，彻底删除补位逻辑。偏移值在构造时一次性计算并缓存为 `final` 字段，不影响其他方法。

**Tech Stack:** Java 21, Spring Boot 3.3.4, JUnit 5, AssertJ

---

## 背景：为什么偏移比补位更优雅

| 方案 | ID=1 输出（乱序字母表） | 原因 |
|------|----------------------|------|
| 补位（当前） | `nnnnn6` | 5 个相同字符 + 1 位有效编码 |
| 偏移（目标） | `6nnnn6` | 首位由高位决定，无明显重复前缀 |

偏移公式：`encode(id + 62^(minLength-1), alphabet)` 恒产出 minLength 位，无需额外补位。

---

## 文件变更总览

| 操作 | 文件 |
|------|------|
| 修改 | `src/main/java/com/example/shorturl/generator/strategy/Base62ShortCodeGenerator.java` |
| 修改 | `src/test/java/com/example/shorturl/generator/strategy/Base62ShortCodeGeneratorTest.java` |
| 修改 | `src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java` |

---

## Task 1: 用偏移替换补位逻辑（TDD）

**Files:**
- Modify: `src/main/java/.../generator/strategy/Base62ShortCodeGenerator.java:17-45`
- Test: `src/test/java/.../generator/strategy/Base62ShortCodeGeneratorTest.java`

### 预期值推导（DEFAULT_ALPHABET，minCodeLength=6，offset=916,132,832）

| ID | id + offset | 编码结果（DEFAULT_ALPHABET） |
|----|------------|---------------------------|
| 0  | 916,132,832 = 62⁵       | `"100000"` |
| 1  | 916,132,833 = 62⁵ + 1   | `"100001"` |
| 62 | 916,132,894 = 62⁵ + 62  | `"100010"` |

乱序字母表 `"n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8"`（shuffled[0]='n', shuffled[1]='6'），ID=1 → encode(916,132,833, shuffled) = `"6nnnn6"`（首字符是 `'6'`，不再是 5 个补位字符）。

---

- [ ] **Step 1: 替换 `Base62ShortCodeGeneratorTest.java` 全部内容（这是 RED 步骤：测试会因 `generateFromId` 返回旧值而失败）**

```java
package com.example.shorturl.generator.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62ShortCodeGeneratorTest {

    private static final String STD = Base62ShortCodeGenerator.DEFAULT_ALPHABET;
    private final Base62ShortCodeGenerator generator = new Base62ShortCodeGenerator(STD, 6);

    @Test
    void generateFromId_minLengthGuaranteed_noLeadingRepeat() {
        // ID=1: offset=62^5=916132832, encode(916132833)="100001"
        assertThat(generator.generateFromId(1L)).isEqualTo("100001");
    }

    @Test
    void generateFromId_62() {
        // encode(916132832 + 62) = encode(916132894) = "100010"
        assertThat(generator.generateFromId(62L)).isEqualTo("100010");
    }

    @Test
    void generateFromId_zero() {
        // encode(916132832 + 0) = encode(916132832) = "100000"
        assertThat(generator.generateFromId(0L)).isEqualTo("100000");
    }

    @Test
    void generateFromId_exactlyMinLength() {
        // All small IDs must produce exactly minCodeLength chars (no padding, no overflow)
        for (long id = 0; id <= 1000; id++) {
            assertThat(generator.generateFromId(id).length())
                    .as("ID=%d should produce exactly 6 chars", id)
                    .isEqualTo(6);
        }
    }

    @Test
    void generateFromId_largeId_sevenChars() {
        // ID=62^6=56800235584 → encode(56800235584 + 916132832) = 7 chars
        long id = (long) Math.pow(62, 6);
        assertThat(generator.generateFromId(id).length()).isEqualTo(7);
    }

    @Test
    void generateFromId_shuffledAlphabet_noLeadingRepeatPrefix() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator shuffledGen = new Base62ShortCodeGenerator(shuffled, 6);
        // ID=1 → encode(916132833, shuffled) = "6nnnn6"
        // First char is '6', NOT the pad char 'n' — no leading repeated sequence
        String code = shuffledGen.generateFromId(1L);
        assertThat(code).isEqualTo("6nnnn6");
        assertThat(code).doesNotStartWith("nnnnn");
    }

    @Test
    void generateFromId_shuffledAlphabet_differentFromStandard() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator shuffledGen = new Base62ShortCodeGenerator(shuffled, 6);
        assertThat(shuffledGen.generateFromId(15L)).isNotEqualTo(generator.generateFromId(15L));
    }

    @Test
    void generatePlaceholderStartsWithTilde() {
        assertThat(generator.generatePlaceholder("https://example.com")).startsWith("~");
    }

    @Test
    void generatePlaceholderDifferentUrls() {
        assertThat(generator.generatePlaceholder("https://a.com"))
                .isNotEqualTo(generator.generatePlaceholder("https://b.com"));
    }

    @Test
    void generateReturnsNonEmpty() {
        assertThat(generator.generate("https://example.com")).isNotBlank();
    }

    @Test
    void constructor_invalidAlphabetLength_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("tooshort", 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("62");
    }

    @Test
    void constructor_duplicateCharsInAlphabet_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("a".repeat(62), 6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthZero_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthNine_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 运行，确认 RED**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -Dtest=Base62ShortCodeGeneratorTest \
  -f /Users/lunfee/AI/ShortURL/pom.xml 2>&1 | grep -E "FAIL|expected|BUILD"
```

Expected: 至少 `generateFromId_minLengthGuaranteed_noLeadingRepeat` 等失败，因为当前实现返回 `"000001"` 而非 `"100001"`。

- [ ] **Step 3: 修改 `Base62ShortCodeGenerator.java`：添加 `codeOffset` 字段，更新构造器，重写 `generateFromId`**

将第 17–45 行替换为：

```java
    private final String alphabet;
    private final int minCodeLength;
    private final long codeOffset;  // = 62^(minCodeLength-1), ensures minCodeLength chars without padding

    public Base62ShortCodeGenerator(
            @Value("${shorturl.generator.alphabet:" + DEFAULT_ALPHABET + "}") String alphabet,
            @Value("${shorturl.generator.min-code-length:6}") int minCodeLength) {
        if (alphabet.length() != 62 || alphabet.chars().distinct().count() != 62) {
            throw new IllegalArgumentException(
                    "shorturl.generator.alphabet must be exactly 62 unique characters");
        }
        if (minCodeLength < 1 || minCodeLength > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "shorturl.generator.min-code-length must be between 1 and " + MAX_CODE_LENGTH);
        }
        this.alphabet = alphabet;
        this.minCodeLength = minCodeLength;
        long offset = 1L;
        for (int i = 0; i < minCodeLength - 1; i++) {
            offset *= 62;
        }
        this.codeOffset = offset;
    }

    @Override
    public String generate(String originalUrl) {
        return Base62.encode(hashUrl(originalUrl), alphabet);
    }

    @Override
    public String generateFromId(Long id) {
        String code = Base62.encode(id + codeOffset, alphabet);
        assert code.length() <= MAX_CODE_LENGTH : "short code overflow for id=" + id;
        return code;
    }
```

- [ ] **Step 4: 运行，确认 GREEN**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -Dtest=Base62ShortCodeGeneratorTest \
  -f /Users/lunfee/AI/ShortURL/pom.xml 2>&1 | grep -E "Tests run:|BUILD"
```

Expected: `Tests run: 14, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git -C /Users/lunfee/AI/ShortURL add \
  src/main/java/com/example/shorturl/generator/strategy/Base62ShortCodeGenerator.java \
  src/test/java/com/example/shorturl/generator/strategy/Base62ShortCodeGeneratorTest.java
git -C /Users/lunfee/AI/ShortURL commit -m \
  "refactor: replace padding with offset-based encoding in generateFromId"
```

---

## Task 2: 更新 ShortUrlServiceImplTest 预期短码

**Files:**
- Modify: `src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java:63-75,145`

- [ ] **Step 1: 更新两处短码预期值（ID=1 → "100001"，ID=62 → "100010"）**

将 `createShortUrl` 测试中（约第 63–75 行）：
```java
assertThat(vo.getShortCode()).isEqualTo("000001");
assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/000001");
// ...
verify(shortUrlMapper).updateByPrimaryKey(argThat(record ->
        "000001".equals(record.getShortCode()) && record.getId() == 1L));
```
改为：
```java
assertThat(vo.getShortCode()).isEqualTo("100001");
assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/100001");
// ...
verify(shortUrlMapper).updateByPrimaryKey(argThat(record ->
        "100001".equals(record.getShortCode()) && record.getId() == 1L));
```

将 `createShortUrlBase62EncodingForLargerIds` 测试中（约第 145 行）：
```java
assertThat(vo.getShortCode()).isEqualTo("000010");
```
改为：
```java
assertThat(vo.getShortCode()).isEqualTo("100010");
```

- [ ] **Step 2: 运行全套测试，确认全绿**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test \
  -f /Users/lunfee/AI/ShortURL/pom.xml 2>&1 | grep -E "Tests run:.*Skipped|BUILD"
```

Expected: `Tests run: 58, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git -C /Users/lunfee/AI/ShortURL add \
  src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java
git -C /Users/lunfee/AI/ShortURL commit -m \
  "test: update short code assertions for offset-based encoding"
```

---

## 自检

**覆盖检查：**
- ✅ 删除补位逻辑 → Task 1 Step 3
- ✅ 添加 `codeOffset` 字段 → Task 1 Step 3
- ✅ 偏移在构造时计算，不重复计算 → Task 1 Step 3
- ✅ 小 ID 产出 minLength 位（无重复前缀）→ Task 1 Step 1 `generateFromId_exactlyMinLength`
- ✅ 乱序字母表首字符不再是前缀补位字符 → Task 1 Step 1 `generateFromId_shuffledAlphabet_noLeadingRepeatPrefix`
- ✅ 测试预期值更新 → Task 2

**类型一致性：**
- `codeOffset` 类型 `long`，`id + codeOffset` 两个都是 `long` ✅
- `generateFromId(Long id)` 签名不变 ✅
- `DEFAULT_ALPHABET` 常量不变，Task 2 中 `ShortUrlServiceImplTest` 用它创建 generator ✅

**无占位符：** 所有步骤均包含完整代码和预期输出 ✅
