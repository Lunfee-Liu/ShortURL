package com.example.shorturl.mapper;

import com.example.shorturl.entity.ShortUrlDO;
import com.example.shorturl.vo.AdminShortUrlVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShortUrlMapper {

    int insert(ShortUrlDO row);

    ShortUrlDO selectByPrimaryKey(Long id);

    ShortUrlDO selectByShortCode(@Param("shortCode") String shortCode);

    int updateByPrimaryKey(ShortUrlDO row);

    int deleteByPrimaryKey(Long id);

    // ── Admin queries ────────────────────────────────────────────────────────

    /** Paginated list with visit_count from access_logs. keyword may be null. */
    List<AdminShortUrlVO> selectAdminPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countAdmin(@Param("keyword") String keyword);
}
