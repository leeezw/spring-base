package com.kite.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.user.entity.SysPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPost> {

    @Select("SELECT post_id, COUNT(*) as cnt FROM sys_user_post GROUP BY post_id")
    List<Map<String, Object>> countUsersByPost();
}
