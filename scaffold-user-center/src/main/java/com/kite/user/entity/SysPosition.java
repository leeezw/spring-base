package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.kite.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_position")
public class SysPosition extends BaseEntity {

    private Long postId;
    private String positionCode;
    private String positionName;
    private Integer positionLevel;
    private Integer sortOrder;
    private Integer status;
    private String description;
    private Long tenantId;

    @TableField(exist = false)
    private String postName;
}
