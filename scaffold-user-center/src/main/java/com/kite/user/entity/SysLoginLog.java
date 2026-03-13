package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String ip;
    private String userAgent;
    private Integer status; // 1成功 0失败
    private String message;
    private LocalDateTime loginTime;
    private Long tenantId;
}
