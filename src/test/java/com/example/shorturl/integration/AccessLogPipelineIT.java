package com.example.shorturl.integration;

import com.example.shorturl.service.AccessLogRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Kafka pipeline integration test.
 * Requires docker-compose up -d (MySQL + Redis + Kafka) before running.
 * short_code is VARCHAR(8) — test codes must be ≤ 8 chars.
 */
@SpringBootTest
class AccessLogPipelineIT {

    @Autowired
    private AccessLogRecorder accessLogRecorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM access_logs WHERE short_code LIKE 'it!_%' ESCAPE '!'");
    }

    /** Generate a ≤8-char test short code with "it_" prefix (5 random hex chars). */
    private static String testCode() {
        return "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
    }

    @Test
    void singleRecord_flowsThroughKafkaToDatabase() {
        String shortCode = testCode();   // e.g. "it_a3f9c"  (8 chars)

        accessLogRecorder.recordAccess(
                shortCode, "https://example.com", "192.168.1.1", "curl/8.0", null
        );

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM access_logs WHERE short_code = ?",
                            Integer.class, shortCode
                    );
                    assertThat(count).isEqualTo(1);
                });

        Map<String, Object> row = jdbcTemplate.queryForList(
                "SELECT * FROM access_logs WHERE short_code = ? LIMIT 1", shortCode
        ).get(0);
        assertThat(row.get("ip")).isEqualTo("192.168.1.1");
        assertThat(row.get("user_agent")).isEqualTo("curl/8.0");
        assertThat(row.get("referer")).isNull();
        assertThat(row.get("accessed_at")).isNotNull();
    }

    @Test
    void multipleRecords_allReachDatabase() {
        // run-unique 3-char hex prefix → codes: "it_" + prefix + digit = 8 chars max
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 2);
        int total = 5;

        for (int i = 0; i < total; i++) {
            String code = "it_" + runId + i;   // "it_" + 2 + 1 = 6 chars
            accessLogRecorder.recordAccess(
                    code,
                    "https://example.com/" + i,
                    "10.0.0." + i,
                    "Mozilla/5.0",
                    i % 2 == 0 ? "https://referer.com" : null
            );
        }

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM access_logs WHERE short_code LIKE ?",
                            Integer.class, "it_" + runId + "%"
                    );
                    assertThat(count).isEqualTo(total);
                });
    }
}
