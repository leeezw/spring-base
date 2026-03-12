package com.kite.permission.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    
    /**
     * 数据权限类型
     */
    Type value() default Type.SELF;
    
    /**
     * 部门字段名（用于SQL拼接）
     */
    String deptAlias() default "dept_id";
    
    /**
     * 用户字段名（用于SQL拼接）
     */
    String userAlias() default "user_id";
    
    enum Type {
        /**
         * 全部数据权限
         */
        ALL,
        
        /**
         * 本部门数据权限
         */
        DEPT,
        
        /**
         * 本部门及以下数据权限
         */
        DEPT_AND_CHILD,
        
        /**
         * 仅本人数据权限
         */
        SELF,
        
        /**
         * 自定义数据权限
         */
        CUSTOM
    }
}
