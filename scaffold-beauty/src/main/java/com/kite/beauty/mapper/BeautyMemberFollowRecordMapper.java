package com.kite.beauty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kite.beauty.entity.BeautyMemberFollowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BeautyMemberFollowRecordMapper extends BaseMapper<BeautyMemberFollowRecord> {

    @Select("""
            SELECT r.*,
                   s.store_name,
                   COALESCE(u.nickname, u.username) AS operator_name
            FROM beauty_member_follow_record r
                     LEFT JOIN beauty_store s ON r.store_id = s.id AND s.deleted = 0
                     LEFT JOIN sys_user u ON r.operator_id = u.id AND u.deleted = 0
            WHERE r.deleted = 0
              AND r.member_id = #{memberId}
            ORDER BY r.create_time DESC, r.id DESC
            LIMIT #{limit}
            """)
    List<BeautyMemberFollowRecord> selectByMemberId(@Param("memberId") Long memberId,
                                                    @Param("limit") int limit);
}
