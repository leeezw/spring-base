package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kite.mybatis.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    private String dictCode;

    private String dictName;

    private String description;

    private Integer status;

    private Integer sortOrder;

}
