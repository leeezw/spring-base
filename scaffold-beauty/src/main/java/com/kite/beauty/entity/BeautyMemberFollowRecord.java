package com.kite.beauty.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 美业会员跟进记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("beauty_member_follow_record")
public class BeautyMemberFollowRecord extends BaseEntity {

    private Long tenantId;
    private Long memberId;
    private Long storeId;
    private String followType;
    private String followResult;
    private String content;
    private LocalDateTime nextFollowTime;
    private Long operatorId;

    @TableField(exist = false)
    private String storeName;

    @TableField(exist = false)
    private String operatorName;
}
