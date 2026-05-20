package com.example.shorturl.mapper;

import com.example.shorturl.entity.AccessLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AccessLogMapper {

    int insert(AccessLogDO record);

    int batchInsert(List<AccessLogDO> records);

    // ── Admin queries ────────────────────────────────────────────────────────

    List<AccessLogDO> selectPageByShortCode(
            @Param("shortCode") String shortCode,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countByShortCode(
            @Param("shortCode") String shortCode,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
