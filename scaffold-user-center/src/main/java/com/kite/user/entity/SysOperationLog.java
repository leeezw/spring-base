package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseData;
    private Integer status; // 1成功 0失败
    private String errorMsg;
    private String ip;
    private String userAgent;
    private Long duration;
    private Long userId;
    private String username;
    private Long tenantId;
    private LocalDateTime createTime;
}
