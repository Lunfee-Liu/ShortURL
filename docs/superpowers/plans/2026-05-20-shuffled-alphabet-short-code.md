# Shuffled Alphabet Short Code Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将短码生成从标准顺序 Base62 改为乱序字母表编码，并强制最小长度 6 位，消除单字符短码和顺序枚举问题。

**Architecture:** `Base62` 工具类新增自定义字母表重载方法；`Base62ShortCodeGenerator` 通过构造器注入 alphabet 和 min-code-length 配置，生成时左补字母表首字符至 6 位，超出 8 位时用 assert 拦截；配置统一放 `application.yml`。

**Tech Stack:** Java 21, Spring Boot 3.3.4, Lombok, JUnit 5, AssertJ

---

## 文件变更总览

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `src/main/resources/application.yml` | 新增 `shorturl.generator.*` 配置 |
| 修改 | `src/main/java/.../util/Base62.java` | 新增 `encode(long, String)` 重载 |
| 修改 | `src/main/java/.../generator/strategy/Base62ShortCodeGenerator.java` | 注入配置，乱序字母表，最小长度，assert |
| 修改 | `src/main/java/com/example/shorturl/CLAUDE.md` | 新增短码格式规范章节 |
| 修改 | `src/test/java/.../util/Base62Test.java` | 新增自定义字母表测试用例 |
| 修改 | `src/test/java/.../generator/strategy/Base62ShortCodeGeneratorTest.java` | 更新构造方式与预期值 |
| 修改 | `src/test/java/.../service/impl/ShortUrlServiceImplTest.java` | 更新短码预期值 |

---

## Task 1: 创建分支，添加 generator 配置

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 创建功能分支**

```bash
git checkout -b feat/shuffled-alphabet-short-code
```

- [ ] **Step 2: 在 `application.yml` 的 `shorturl:` 块下新增配置**

将：
```yaml
shorturl:
  base-url: http://localhost:8080
```

改为：
```yaml
shorturl:
  base-url: http://localhost:8080
  generator:
    # 62 个不重复字符的乱序排列，决定短码字符映射关系
    alphabet: "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8"
    min-code-length: 6
```

- [ ] **Step 3: 验证 YAML 格式正确（无缩进错误）**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn validate -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "chore: add shorturl.generator alphabet and min-code-length config"
```

---

## Task 2: Base62 新增自定义字母表编码方法（TDD）

**Files:**
- Modify: `src/main/java/com/example/shorturl/util/Base62.java`
- Test: `src/test/java/com/example/shorturl/util/Base62Test.java`

- [ ] **Step 1: 在 `Base62Test` 末尾追加失败测试**

```java
@Test
void encodeWithCustomAlphabet_singleDigit() {
    // alphabet where index 1 maps to 'X'
    String alpha = "0X23456789ABCDEFGHIJKLMNOPQRSTUVWxabcdefghijklmnopqrstuvwyz!@#$%^&";
    // 62 chars needed — use a known 62-char shuffled set for test
    String alphabet = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
    // encode(1) with this alphabet: 1 % 62 = 1 → alphabet[1] = '6'
    assertThat(Base62.encode(1L, alphabet)).isEqualTo("6");
}

@Test
void encodeWithCustomAlphabet_zero() {
    String alphabet = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
    // encode(0) → first char of alphabet = 'n'
    assertThat(Base62.encode(0L, alphabet)).isEqualTo("n");
}

@Test
void encodeWithCustomAlphabet_multiDigit() {
    String alphabet = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
    // encode(62) with this alphabet:
    //   62 % 62 = 0 → append alphabet[0] = 'n'
    //   62 / 62 = 1 → 1 % 62 = 1 → append alphabet[1] = '6'
    //   reversed: "6n"
    assertThat(Base62.encode(62L, alphabet)).isEqualTo("6n");
}

@Test
void encodeWithCustomAlphabet_wrongLength_throws() {
    assertThatThrownBy(() -> Base62.encode(1L, "tooshort"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("62");
}
```

- [ ] **Step 2: 运行，确认失败（方法不存在）**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl . -Dtest=Base62Test -q 2>&1 | tail -5
```

Expected: 编译错误 `cannot find symbol: method encode(long, String)`

- [ ] **Step 3: 在 `Base62.java` 中新增 `encode(long, String)` 方法**

在 `decode` 方法之前插入：

```java
public static String encode(long value, String alphabet) {
    if (alphabet.length() != 62) {
        throw new IllegalArgumentException("alphabet must be exactly 62 characters, got: " + alphabet.length());
    }
    if (value == 0) {
        return String.valueOf(alphabet.charAt(0));
    }
    char[] alpha = alphabet.toCharArray();
    StringBuilder sb = new StringBuilder();
    long n = value;
    while (n > 0) {
        sb.append(alpha[(int) (n % 62)]);
        n /= 62;
    }
    return sb.reverse().toString();
}
```

- [ ] **Step 4: 运行，确认全绿**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -Dtest=Base62Test -q 2>&1 | tail -5
```

Expected: `Tests run: N, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/shorturl/util/Base62.java \
        src/test/java/com/example/shorturl/util/Base62Test.java
git commit -m "feat: add Base62.encode(long, String) overload for custom alphabet"
```

---

## Task 3: 重构 Base62ShortCodeGenerator 使用配置（TDD）

**Files:**
- Modify: `src/main/java/com/example/shorturl/generator/strategy/Base62ShortCodeGenerator.java`
- Test: `src/test/java/com/example/shorturl/generator/strategy/Base62ShortCodeGeneratorTest.java`

- [ ] **Step 1: 用新的测试替换 `Base62ShortCodeGeneratorTest` 的全部内容**

```java
package com.example.shorturl.generator.strategy;

import com.example.shorturl.util.Base62;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62ShortCodeGeneratorTest {

    // 标准字母表，测试中使用，结果可计算验证
    private static final String STD = Base62ShortCodeGenerator.DEFAULT_ALPHABET;
    private final Base62ShortCodeGenerator generator =
            new Base62ShortCodeGenerator(STD, 6);

    @Test
    void generateFromId_paddedToMinLength() {
        // ID=1 → Base62 = "1" → padded to 6 with '0' (STD[0]='0')
        assertThat(generator.generateFromId(1L)).isEqualTo("000001");
    }

    @Test
    void generateFromId_62_paddedToMinLength() {
        // ID=62 → Base62 = "10" → padded to 6
        assertThat(generator.generateFromId(62L)).isEqualTo("000010");
    }

    @Test
    void generateFromId_largeId_nopadding() {
        // ID=62^6 = 56_800_235_584 → naturally 7 chars, no padding needed
        long id = (long) Math.pow(62, 6);
        String code = generator.generateFromId(id);
        assertThat(code.length()).isEqualTo(7);
    }

    @Test
    void generateFromId_zero() {
        assertThat(generator.generateFromId(0L)).isEqualTo("000000");
    }

    @Test
    void generateFromId_roundTripMatchesBase62WithPadding() {
        long id = 123456789L;
        String expected = "0" + Base62.encode(id);   // standard encode = 5 chars, pad 1
        assertThat(generator.generateFromId(id)).isEqualTo(expected);
    }

    @Test
    void generateFromId_shuffledAlphabet_differentFromStandard() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator shuffledGen = new Base62ShortCodeGenerator(shuffled, 6);
        // ID=15 with standard → "F" padded → "00000F"
        // ID=15 with shuffled → different
        assertThat(shuffledGen.generateFromId(15L))
                .isNotEqualTo(generator.generateFromId(15L));
    }

    @Test
    void generateFromId_shuffledAlphabet_padCharIsFirstChar() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator shuffledGen = new Base62ShortCodeGenerator(shuffled, 6);
        // ID=1 → encode = "6" (1 char) → pad to 6 with 'n' (shuffled[0])
        assertThat(shuffledGen.generateFromId(1L)).isEqualTo("nnnnn6");
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
        // 62 chars but with duplicates
        String dup = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                     .substring(0, 62);
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(dup, 6))
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

- [ ] **Step 2: 运行，确认失败（`DEFAULT_ALPHABET` 常量和新构造器不存在）**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -Dtest=Base62ShortCodeGeneratorTest -q 2>&1 | tail -5
```

Expected: 编译错误

- [ ] **Step 3: 用新实现替换 `Base62ShortCodeGenerator.java` 全部内容**

```java
package com.example.shorturl.generator.strategy;

import com.example.shorturl.generator.ShortCodeGenerator;
import com.example.shorturl.util.Base62;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    public static final String DEFAULT_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static final int MAX_CODE_LENGTH = 8;
    private static final int PLACEHOLDER_HASH_LIMIT = 100_000;

    private final String alphabet;
    private final int minCodeLength;

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
    }

    @Override
    public String generate(String originalUrl) {
        return Base62.encode(hashUrl(originalUrl), alphabet);
    }

    @Override
    public String generateFromId(Long id) {
        String code = Base62.encode(id, alphabet);
        assert code.length() <= MAX_CODE_LENGTH : "short code overflow for id=" + id;
        String pad = String.valueOf(alphabet.charAt(0));
        return pad.repeat(Math.max(0, minCodeLength - code.length())) + code;
    }

    @Override
    public String generatePlaceholder(String originalUrl) {
        long hash = hashUrl(originalUrl) ^ System.nanoTime();
        return "~" + Base62.encode(Math.abs(hash) % PLACEHOLDER_HASH_LIMIT, alphabet);
    }

    private static long hashUrl(String url) {
        long h = 0;
        for (int i = 0; i < url.length(); i++) {
            h = 31 * h + url.charAt(i);
        }
        return Math.abs(h);
    }
}
```

- [ ] **Step 4: 运行，确认全绿**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -Dtest=Base62ShortCodeGeneratorTest -q 2>&1 | tail -5
```

Expected: `Tests run: N, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/shorturl/generator/strategy/Base62ShortCodeGenerator.java \
        src/test/java/com/example/shorturl/generator/strategy/Base62ShortCodeGeneratorTest.java
git commit -m "feat: use shuffled alphabet and min-code-length in short code generation"
```

---

## Task 4: 修复 ShortUrlServiceImplTest 预期值

**Files:**
- Modify: `src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java`

- [ ] **Step 1: 更新测试文件中的 `shortCodeGenerator` 声明及两处预期短码**

将：
```java
private final ShortCodeGenerator shortCodeGenerator = new Base62ShortCodeGenerator();
```
改为：
```java
private final ShortCodeGenerator shortCodeGenerator =
        new Base62ShortCodeGenerator(Base62ShortCodeGenerator.DEFAULT_ALPHABET, 6);
```

将 `createShortUrl` 测试中：
```java
assertThat(vo.getShortCode()).isEqualTo("1");
assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/1");
```
改为：
```java
assertThat(vo.getShortCode()).isEqualTo("000001");
assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/000001");
```

将 `createShortUrlBase62EncodingForLargerIds` 测试中：
```java
assertThat(vo.getShortCode()).isEqualTo("10");
```
改为：
```java
assertThat(vo.getShortCode()).isEqualTo("000010");
```

- [ ] **Step 2: 运行所有测试，全绿**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -q 2>&1 | tail -10
```

Expected:
```
Tests run: N, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java
git commit -m "test: update short code assertions for min-length-6 encoding"
```

---

## Task 5: 更新 CLAUDE.md 补充短码格式规范

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: 在 CLAUDE.md 的「数据库」章节之后新增短码格式规范章节**

在 `## 数据库` 与下一个 `##` 标题之间插入：

```markdown
## 短码格式规范

- **字符集**：`shorturl.generator.alphabet` 配置的乱序 Base62 字母表（62 个不重复字符）
- **最小长度**：`shorturl.generator.min-code-length`（默认 6），不足时以字母表首字符左补
- **最大长度**：8（受 `short_code VARCHAR(8)` 约束），这是系统设计容量上限
  - 系统最多支持 62⁸ ≈ 2180 亿条短链；超出属于容量耗尽，不是运行时异常
  - `generateFromId` 内部用 `assert code.length() <= 8` 拦截（需 JVM 启用 `-ea`）
- **反枚举性**：乱序字母表使连续 ID 映射到不连续短码，提升枚举攻击成本
- **不可逆性**：同一配置下相同 ID 总是产生相同短码；切换字母表配置会导致历史映射失效
- **阶段二演进**：切换到号段（Segment）方案后仍使用乱序字母表编码，继承反枚举特性
```

- [ ] **Step 2: 运行测试，确认没有引入其他改动**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: add short code format spec to CLAUDE.md"
```

---

## 自检

**Spec 覆盖检查：**
- ✅ 乱序字母表编码 → Task 3
- ✅ 最小长度 6 位 → Task 3
- ✅ 最大长度 8 位（assert 约束） → Task 3
- ✅ 配置化（application.yml） → Task 1
- ✅ 启动时字母表/长度校验 → Task 3（构造器 throw）
- ✅ 测试更新 → Task 2, 3, 4
- ✅ CLAUDE.md 更新 → Task 5

**类型一致性：**
- `DEFAULT_ALPHABET` 在 Task 3 中定义为 `public static final`，Task 4 中引用 ✅
- `generateFromId(Long id)` 接口签名在所有 Task 中一致 ✅
- 测试中的字母表字符串 `"n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8"` 在 Task 1 配置与 Task 3 测试中一致 ✅
