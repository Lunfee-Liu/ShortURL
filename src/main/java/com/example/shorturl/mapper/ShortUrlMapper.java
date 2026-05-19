package com.example.shorturl.mapper;

import com.example.shorturl.entity.ShortUrlDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShortUrlMapper {

    int insert(ShortUrlDO row);

    ShortUrlDO selectByPrimaryKey(Long id);

    ShortUrlDO selectByShortCode(@Param("shortCode") String shortCode);

    List<ShortUrlDO> selectAll();

    int updateByPrimaryKey(ShortUrlDO row);

    int deleteByPrimaryKey(Long id);
}
