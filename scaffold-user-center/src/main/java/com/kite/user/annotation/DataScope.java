package com.kite.user.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 * 标记在Controller或Service方法上，配合DataScopeAspect使用
 * 
 * @param deptAlias  部门表别名（SQL中的表别名，默认空=主表）
 * @param userAlias  用户表别名（仅本人数据时使用）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /** 部门表的别名 */
    String deptAlias() default "";
    /** 用户表的别名 */
    String userAlias() default "";
}
