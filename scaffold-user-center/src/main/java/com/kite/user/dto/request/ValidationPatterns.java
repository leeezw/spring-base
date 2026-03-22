package com.kite.user.dto.request;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String USERNAME = "^[A-Za-z][A-Za-z0-9_]{2,19}$";
    public static final String ROLE_CODE = "^[A-Za-z][A-Za-z0-9_]{1,49}$";
    public static final String TENANT_CODE = "^[A-Za-z][A-Za-z0-9_]{1,49}$";
    public static final String POST_CODE = "^[A-Za-z][A-Za-z0-9_]{1,49}$";
    public static final String DICT_CODE = "^[a-z][a-z0-9_]{1,49}$";
    public static final String PERMISSION_CODE = "^[a-z][a-z0-9_]*(?::[a-z][a-z0-9_]*)*$";
    public static final String MENU_PATH = "^/[A-Za-z0-9/_{}:-]*$";
    public static final String COMPONENT = "^[A-Za-z0-9_./-]{1,200}$";
    public static final String ICON = "^[A-Za-z][A-Za-z0-9_-]{0,99}$";
    public static final String PHONE = "^(1\\d{10}|0\\d{2,3}-?\\d{7,8})$";
    public static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String COLOR = "^(#[0-9A-Fa-f]{3}|#[0-9A-Fa-f]{6}|[A-Za-z]+)$";
    public static final String PACKAGE_NAME = "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$";
    public static final String CLASS_NAME = "^[A-Z][A-Za-z0-9]*$";
    public static final String BUSINESS_NAME = "^[a-z][A-Za-z0-9]*$";
    public static final String JAVA_FIELD = "^[a-zA-Z_$][a-zA-Z0-9_$]*$";
}
