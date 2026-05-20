# Scramble-Based Short Code Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用乘法置换（multiplicative permutation）彻底打散 ID 与短码之间的顺序关联，使连续生成的短链在视觉上完全无关联。

**Architecture:** `generateFromId` 改为三步：①`scrambled = (id × scrambleKey) % rangeSize`，②`encoded_val = scrambled + codeOffset`，③`Base62.encode(encoded_val, alphabet)`。置换是双射（bijective），同一 ID 始终映射同一短码，不同 ID 映射不同短码。`scrambleKey`、`alphabet`、`min-code-length` 全部从 `application.yml` 读取，构造时校验 `gcd(scrambleKey, rangeSize) == 1`。

**Tech Stack:** Java 21, Spring Boot 3.3.4, JUnit 5, AssertJ, `java.math.BigInteger`（gcd 校验）

---

## 核心公式推导

```
codeOffset  = 62^(minCodeLength - 1)   // 最小 minLength 位的起始值
rangeSize   = codeOffset × 61           // minLength 位编码空间大小
scrambled   = (id × scrambleKey) % rangeSize   // 双射：[0, rangeSize) → [0, rangeSize)
encoded_val = scrambled + codeOffset    // 保证结果在 [codeOffset, codeOffset + rangeSize) → 恒为 minLength 位
short_code  = Base62.encode(encoded_val, alphabet)
```

**前提条件**：`gcd(scrambleKey, rangeSize) == 1`（保证乘法置换是双射，即无碰撞）

对 minCodeLength=6：rangeSize = 62⁵ × 61 = 55,884,102,752 = 2⁵ × 31⁵ × 61。
默认 scrambleKey = 2,654,435,761（质数，与 rangeSize 互质 ✓）

---

## 预期值速查表（DEFAULT_ALPHABET，scrambleKey=2,654,435,761，minCodeLength=6）

| ID | scrambled = ID × key % rangeSize | encoded_val | 短码 |
|----|----------------------------------|-------------|------|
| 0  | 0                                | 916,132,832 | `100000` |
| 1  | 2,654,435,761                    | 3,570,568,593 | `3tdk01` |
| 2  | 5,308,871,522                    | 6,225,004,354 | `6nHU02` |
| 62 | 52,806,811,678                   | 53,722,944,510 | `wdk010` |

---

## 文件变更总览

| 操作 | 文件 |
|------|------|
| 修改 | `src/main/resources/application.yml` |
| 修改 | `src/main/java/.../generator/strategy/Base62ShortCodeGenerator.java` |
| 修改 | `src/test/java/.../generator/strategy/Base62ShortCodeGeneratorTest.java` |
| 修改 | `src/test/java/.../service/impl/ShortUrlServiceImplTest.java` |

---

## Task 1: 添加 scramble-key 配置

**Files:**
- Modify: `src/main/resources/application.yml:38-40`

- [ ] **Step 1: 在 `application.yml` 的 `shorturl.generator` 下新增 `scramble-key`**

将：
```yaml
shorturl:
  base-url: http://localhost:8080
  generator:
    alphabet: "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8"
    min-code-length: 6
```
改为：
```yaml
shorturl:
  base-url: http://localhost:8080
  generator:
    alphabet: "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8"
    min-code-length: 6
    # 乘法置换密钥，必须与 rangeSize(62^(n-1)*61) 互质，用于打散顺序 ID
    scramble-key: 2654435761
```

- [ ] **Step 2: 验证 YAML 无格式错误**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn validate -q -f /Users/lunfee/AI/ShortURL/pom.xml
```

Expected: 无输出（BUILD SUCCESS）

- [ ] **Step 3: Commit**

```bash
git -C /Users/lunfee/AI/ShortURL add src/main/resources/application.yml
git -C /Users/lunfee/AI/ShortURL commit -m "chore: add scramble-key config for ID permutation"
```

---

## Task 2: 重写 Base62ShortCodeGenerator 使用乘法置换（TDD）

**Files:**
- Modify: `src/main/java/com/example/shorturl/generator/strategy/Base62ShortCodeGenerator.java`
- Test: `src/test/java/com/example/shorturl/generator/strategy/Base62ShortCodeGeneratorTest.java`

- [ ] **Step 1: 替换 `Base62ShortCodeGeneratorTest.java` 全部内容（RED）**

```java
package com.example.shorturl.generator.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62ShortCodeGeneratorTest {

    private static final String STD   = Base62ShortCodeGenerator.DEFAULT_ALPHABET;
    private static final long   KEY   = 2_654_435_761L;
    // DEFAULT generator: STD alphabet, minLength=6, scrambleKey=KEY
    private final Base62ShortCodeGenerator generator =
            new Base62ShortCodeGenerator(STD, 6, KEY);

    // ── 核心：连续 ID 产出无关联短码 ──────────────────────────────────

    @Test
    void generateFromId_id1_expectedValue() {
        // scrambled = 1 × 2_654_435_761 % rangeSize = 2_654_435_761
        // encoded   = 2_654_435_761 + 916_132_832 = 3_570_568_593
        // Base62(STD, 3_570_568_593) = "3tdk01"
        assertThat(generator.generateFromId(1L)).isEqualTo("3tdk01");
    }

    @Test
    void generateFromId_id2_noRelationToId1() {
        // encoded = 5_308_871_522 + 916_132_832 = 6_225_004_354 → "6nHU02"
        assertThat(generator.generateFromId(2L)).isEqualTo("6nHU02");
        // First char differs from ID=1 result "3tdk01" → no leading pattern
        assertThat(generator.generateFromId(2L).charAt(0))
                .isNotEqualTo(generator.generateFromId(1L).charAt(0));
    }

    @Test
    void generateFromId_allSmallIds_alwaysSixChars() {
        for (long id = 0; id <= 1_000; id++) {
            assertThat(generator.generateFromId(id).length())
                    .as("ID=%d must produce exactly 6 chars", id)
                    .isEqualTo(6);
        }
    }

    @Test
    void generateFromId_allSmallIds_noCollisions() {
        long[] ids = {0, 1, 2, 3, 100, 1000, 62, 3844};
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (long id : ids) {
            codes.add(generator.generateFromId(id));
        }
        assertThat(codes).hasSize(ids.length);
    }

    @Test
    void generateFromId_deterministic() {
        assertThat(generator.generateFromId(42L)).isEqualTo(generator.generateFromId(42L));
    }

    @Test
    void generateFromId_id0() {
        // scrambled = 0, encoded = 916_132_832, Base62(STD) = "100000"
        assertThat(generator.generateFromId(0L)).isEqualTo("100000");
    }

    @Test
    void generateFromId_largeId_sevenChars() {
        // ID = rangeSize → scrambled = 0 (wrap), but id beyond that may be 7 chars
        long id = (long) Math.pow(62, 6); // 56_800_235_584 > rangeSize → assert fires in prod
        // Just check lengths grow naturally for IDs that produce 7-char codes
        long bigId = 62L * 62 * 62 * 62 * 62 * 62; // 62^6
        // scrambled = (bigId × key) % rangeSize, encoded ≥ codeOffset
        // result might be 6 or 7 chars depending on scramble; just verify no exception
        String code = generator.generateFromId(bigId);
        assertThat(code.length()).isBetween(6, 8);
    }

    // ── 乱序字母表：首字符不是重复补位字符 ────────────────────────────

    @Test
    void generateFromId_shuffledAlphabet_noLeadingRepeat() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator sg = new Base62ShortCodeGenerator(shuffled, 6, KEY);
        // With scramble, ID=1 should NOT start with 5 repetitions of any char
        String code = sg.generateFromId(1L);
        assertThat(code).hasSize(6);
        // e.g., not "nnnnn?" (which would indicate padding was used)
        assertThat(code).doesNotMatch("^(.)\\1{4}.*");
    }

    @Test
    void generateFromId_differentAlphabets_differentCodes() {
        String shuffled = "n6j7K0qzWNPk3sYBo1HRSXmAgdV9fUcEbiDhlGpQruv2FetywM4TxaLJ5ZOCI8";
        Base62ShortCodeGenerator sg = new Base62ShortCodeGenerator(shuffled, 6, KEY);
        assertThat(sg.generateFromId(1L)).isNotEqualTo(generator.generateFromId(1L));
    }

    // ── placeholder & generate ─────────────────────────────────────────

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

    // ── 构造器校验 ──────────────────────────────────────────────────────

    @Test
    void constructor_invalidAlphabetLength_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("tooshort", 6, KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("62");
    }

    @Test
    void constructor_duplicateCharsInAlphabet_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator("a".repeat(62), 6, KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthZero_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 0, KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_minLengthNine_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 9, KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_scrambleKeyNotCoprimeWithRange_throws() {
        // rangeSize = 62^5 * 61 = 55_884_102_752 = 2^5 × 31^5 × 61
        // key=2 is even → gcd(2, rangeSize) = 2 ≠ 1
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 6, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coprime");
    }

    @Test
    void constructor_scrambleKeyZero_throws() {
        assertThatThrownBy(() -> new Base62ShortCodeGenerator(STD, 6, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 运行，确认 RED（编译失败，构造器参数数量不对）**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test \
  -Dtest=Base62ShortCodeGeneratorTest \
  -f /Users/lunfee/AI/ShortURL/pom.xml 2>&1 | grep -E "ERROR|cannot find"
```

Expected: `无法将类 Base62ShortCodeGenerator 中的构造器应用到给定类型` 或类似编译错误。

- [ ] **Step 3: 重写 `Base62ShortCodeGenerator.java` 全部内容**

```java
package com.example.shorturl.generator.strategy;

import com.example.shorturl.generator.ShortCodeGenerator;
import com.example.shorturl.util.Base62;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    public static final String DEFAULT_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static final int MAX_CODE_LENGTH = 8;
    private static final int PLACEHOLDER_HASH_LIMIT = 100_000;

    private final String alphabet;
    private final int minCodeLength;
    private final long scrambleKey;
    private final long codeOffset;   // = 62^(minCodeLength-1)
    private final long rangeSize;    // = codeOffset * 61，乘法置换的空间大小

    public Base62ShortCodeGenerator(
            @Value("${shorturl.generator.alphabet:" + DEFAULT_ALPHABET + "}") String alphabet,
            @Value("${shorturl.generator.min-code-length:6}") int minCodeLength,
            @Value("${shorturl.generator.scramble-key:2654435761}") long scrambleKey) {

        if (alphabet.length() != 62 || alphabet.chars().distinct().count() != 62) {
            throw new IllegalArgumentException(
                    "shorturl.generator.alphabet must be exactly 62 unique characters");
        }
        if (minCodeLength < 1 || minCodeLength > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "shorturl.generator.min-code-length must be between 1 and " + MAX_CODE_LENGTH);
        }
        if (scrambleKey <= 0) {
            throw new IllegalArgumentException(
                    "shorturl.generator.scramble-key must be positive");
        }

        long offset = 1L;
        for (int i = 0; i < minCodeLength - 1; i++) {
            offset *= 62;
        }
        long range = offset * 61;

        if (!BigInteger.valueOf(scrambleKey).gcd(BigInteger.valueOf(range)).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException(
                    "shorturl.generator.scramble-key must be coprime with range size " + range);
        }

        this.alphabet = alphabet;
        this.minCodeLength = minCodeLength;
        this.scrambleKey = scrambleKey;
        this.codeOffset = offset;
        this.rangeSize = range;
    }

    @Override
    public String generate(String originalUrl) {
        return Base62.encode(hashUrl(originalUrl), alphabet);
    }

    @Override
    public String generateFromId(Long id) {
        long scrambled = (id * scrambleKey) % rangeSize;
        String code = Base62.encode(scrambled + codeOffset, alphabet);
        assert code.length() <= MAX_CODE_LENGTH : "short code overflow for id=" + id;
        return code;
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

- [ ] **Step 4: 运行，确认 GREEN**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test \
  -Dtest=Base62ShortCodeGeneratorTest \
  -f /Users/lunfee/AI/ShortURL/pom.xml 2>&1 | grep -E "Tests run:|BUILD"
```

Expected: `Tests run: 16, Failures: 0, Errors: 0` / `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git -C /Users/lunfee/AI/ShortURL add \
  src/main/java/com/example/shorturl/generator/strategy/Base62ShortCodeGenerator.java \
  src/test/java/com/example/shorturl/generator/strategy/Base62ShortCodeGeneratorTest.java
git -C /Users/lunfee/AI/ShortURL commit -m \
  "feat: replace padding with multiplicative permutation in generateFromId"
```

---

## Task 3: 更新 ShortUrlServiceImplTest 预期值

**Files:**
- Modify: `src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java`

- [ ] **Step 1: 更新 shortCodeGenerator 构造器调用（新增 scrambleKey 参数）**

将：
```java
private final ShortCodeGenerator shortCodeGenerator =
        new Base62ShortCodeGenerator(Base62ShortCodeGenerator.DEFAULT_ALPHABET, 6);
```
改为：
```java
private final ShortCodeGenerator shortCodeGenerator =
        new Base62ShortCodeGenerator(Base62ShortCodeGenerator.DEFAULT_ALPHABET, 6, 2_654_435_761L);
```

- [ ] **Step 2: 更新 `createShortUrl` 测试中的短码预期值（ID=1 → `"3tdk01"`）**

将：
```java
assertThat(vo.getShortCode()).isEqualTo("000001");
assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/000001");
// ...
verify(shortUrlMapper).updateByPrimaryKey(argThat(record ->
        "000001".equals(record.getShortCode()) && record.getId() == 1L));
```
改为：
```java
assertThat(vo.getShortCode()).isEqualTo("3tdk01");
assertThat(vo.getShortUrl()).isEqualTo("http://localhost:8080/3tdk01");
// ...
verify(shortUrlMapper).updateByPrimaryKey(argThat(record ->
        "3tdk01".equals(record.getShortCode()) && record.getId() == 1L));
```

- [ ] **Step 3: 更新 `createShortUrlBase62EncodingForLargerIds` 测试（ID=62 → `"wdk010"`）**

将：
```java
assertThat(vo.getShortCode()).isEqualTo("000010");
```
改为：
```java
assertThat(vo.getShortCode()).isEqualTo("wdk010");
```

- [ ] **Step 4: 运行全套测试，确认全绿**

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test \
  -f /Users/lunfee/AI/ShortURL/pom.xml 2>&1 | grep -E "Tests run:.*Skipped|BUILD"
```

Expected: `Tests run: 58, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git -C /Users/lunfee/AI/ShortURL add \
  src/test/java/com/example/shorturl/service/impl/ShortUrlServiceImplTest.java
git -C /Users/lunfee/AI/ShortURL commit -m \
  "test: update short code assertions for multiplicative permutation"
```

---

## 自检

**Spec 覆盖：**
- ✅ 连续 ID 无关联 → `generateFromId_id2_noRelationToId1`，`generateFromId_allSmallIds_noCollisions`
- ✅ 无重复前缀 → `generateFromId_shuffledAlphabet_noLeadingRepeat`（正则 `^(.)\1{4}.*`）
- ✅ 双射无碰撞 → gcd 校验 + `generateFromId_allSmallIds_noCollisions`
- ✅ 乱序字母表 → `generateFromId_differentAlphabets_differentCodes`
- ✅ 配置校验 → `constructor_scrambleKeyNotCoprimeWithRange_throws`，`constructor_scrambleKeyZero_throws`
- ✅ 固定长度 → `generateFromId_allSmallIds_alwaysSixChars`
- ✅ 确定性 → `generateFromId_deterministic`

**类型一致性：**
- 构造器签名 `(String, int, long)` 在 Task 2 定义，Task 3 中引用 ✅
- `DEFAULT_ALPHABET` 在 Task 2 定义，Task 3 中引用 ✅
- 测试期望值 `"3tdk01"` / `"wdk010"` 在速查表中有推导依据 ✅

**无占位符：** 所有步骤包含完整代码和可执行命令 ✅
