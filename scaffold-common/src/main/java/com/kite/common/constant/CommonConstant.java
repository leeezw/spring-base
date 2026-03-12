package com.kite.common.constant;

/**
 * 通用常量
 */
public interface CommonConstant {
    
    /**
     * UTF-8 编码
     */
    String UTF8 = "UTF-8";
    
    /**
     * 成功标记
     */
    Integer SUCCESS = 200;
    
    /**
     * 失败标记
     */
    Integer FAIL = 500;
    
    /**
     * 登录用户 Redis Key
     */
    String LOGIN_USER_KEY = "login:user:";
    
    /**
     * Token 黑名单 Redis Key
     */
    String TOKEN_BLACKLIST_KEY = "token:blacklist:";
    
    /**
     * 验证码 Redis Key
     */
    String CAPTCHA_KEY = "captcha:";
    
    /**
     * 限流 Redis Key
     */
    String RATE_LIMIT_KEY = "rate_limit:";
    
    /**
     * 分布式锁 Redis Key
     */
    String LOCK_KEY = "lock:";
    
    /**
     * 默认分页大小
     */
    Long DEFAULT_PAGE_SIZE = 10L;
    
    /**
     * 最大分页大小
     */
    Long MAX_PAGE_SIZE = 100L;
}
