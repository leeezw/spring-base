package com.kite.auth.model;

/**
 * 登录用户上下文
 */
public class LoginUserContext {
    
    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();
    
    /**
     * 设置当前登录用户
     */
    public static void set(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }
    
    /**
     * 获取当前登录用户
     */
    public static LoginUser get() {
        return CONTEXT.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        LoginUser loginUser = get();
        return loginUser != null ? loginUser.getUserId() : null;
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        LoginUser loginUser = get();
        return loginUser != null ? loginUser.getUsername() : null;
    }
    
    /**
     * 清除当前登录用户
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
