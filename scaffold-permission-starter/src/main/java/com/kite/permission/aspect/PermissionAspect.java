package com.kite.permission.aspect;

import com.kite.auth.model.LoginUser;
import com.kite.auth.model.LoginUserContext;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.ResultCode;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.permission.annotation.RequiresRoles;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 权限校验切面
 */
@Aspect
@Component
@Order(100)
public class PermissionAspect {
    
    @Before("@annotation(com.kite.permission.annotation.RequiresRoles) || @within(com.kite.permission.annotation.RequiresRoles)")
    public void checkRoles(JoinPoint joinPoint) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        RequiresRoles annotation = getAnnotation(joinPoint, RequiresRoles.class);
        if (annotation == null) {
            return;
        }
        
        String[] requiredRoles = annotation.value();
        List<String> userRoles = loginUser.getRoles();
        
        if (userRoles == null || userRoles.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "用户无任何角色");
        }
        
        boolean hasPermission = annotation.logical() == RequiresRoles.Logical.AND
            ? Arrays.stream(requiredRoles).allMatch(userRoles::contains)
            : Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
        
        if (!hasPermission) {
            throw new BusinessException(ResultCode.FORBIDDEN, "角色权限不足");
        }
    }
    
    @Before("@annotation(com.kite.permission.annotation.RequiresPermissions) || @within(com.kite.permission.annotation.RequiresPermissions)")
    public void checkPermissions(JoinPoint joinPoint) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        RequiresPermissions annotation = getAnnotation(joinPoint, RequiresPermissions.class);
        if (annotation == null) {
            return;
        }
        
        String[] requiredPermissions = annotation.value();
        List<String> userPermissions = loginUser.getPermissions();
        
        if (userPermissions == null || userPermissions.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "用户无任何权限");
        }
        
        // 超级管理员权限
        if (userPermissions.contains("*:*:*")) {
            return;
        }
        
        boolean hasPermission = annotation.logical() == RequiresPermissions.Logical.AND
            ? Arrays.stream(requiredPermissions).allMatch(perm -> hasPermission(userPermissions, perm))
            : Arrays.stream(requiredPermissions).anyMatch(perm -> hasPermission(userPermissions, perm));
        
        if (!hasPermission) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足");
        }
    }
    
    private boolean hasPermission(List<String> userPermissions, String requiredPermission) {
        for (String userPerm : userPermissions) {
            if (userPerm.equals(requiredPermission)) {
                return true;
            }
            // 支持通配符匹配 system:user:* 匹配 system:user:add
            if (userPerm.endsWith("*")) {
                String prefix = userPerm.substring(0, userPerm.length() - 1);
                if (requiredPermission.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private <T extends java.lang.annotation.Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> annotationClass) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        T annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        
        return joinPoint.getTarget().getClass().getAnnotation(annotationClass);
    }
}
