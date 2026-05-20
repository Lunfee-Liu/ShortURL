package com.example.shorturl.mapper;

import com.example.shorturl.entity.AccessLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AccessLogMapper {
    int insert(AccessLogDO record);
    int batchInsert(List<AccessLogDO> records);
}
