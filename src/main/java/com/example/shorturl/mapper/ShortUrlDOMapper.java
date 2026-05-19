package com.example.shorturl.mapper;

import com.example.shorturl.entity.ShortUrlDO;
import com.example.shorturl.entity.ShortUrlDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ShortUrlDOMapper {
    long countByExample(ShortUrlDOExample example);

    int deleteByExample(ShortUrlDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ShortUrlDO row);

    int insertSelective(ShortUrlDO row);

    List<ShortUrlDO> selectByExample(ShortUrlDOExample example);

    ShortUrlDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") ShortUrlDO row, @Param("example") ShortUrlDOExample example);

    int updateByExample(@Param("row") ShortUrlDO row, @Param("example") ShortUrlDOExample example);

    int updateByPrimaryKeySelective(ShortUrlDO row);

    int updateByPrimaryKey(ShortUrlDO row);
}