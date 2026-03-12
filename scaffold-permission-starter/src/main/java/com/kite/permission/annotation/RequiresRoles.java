package com.kite.permission.annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRoles {
    
    /**
     * 需要的角色列表
     */
    String[] value();
    
    /**
     * 逻辑关系：AND 或 OR
     */
    Logical logical() default Logical.AND;
    
    enum Logical {
        AND, OR
    }
}
