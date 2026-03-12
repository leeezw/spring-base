package com.kite.log.model;

import lombok.Data;

/**
 * 操作日志记录
 */
@Data
public class OperationLogRecord {
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 请求方法
     */
    private String method;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 响应数据
     */
    private String responseData;
    
    /**
     * 操作用户ID
     */
    private Long userId;
    
    /**
     * 操作用户名
     */
    private String username;
    
    /**
     * 操作IP
     */
    private String ip;
    
    /**
     * 操作时间
     */
    private Long operationTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 操作状态：0=失败 1=成功
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMsg;
}
