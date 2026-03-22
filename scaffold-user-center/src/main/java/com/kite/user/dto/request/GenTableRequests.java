package com.kite.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class GenTableRequests {

    private GenTableRequests() {
    }

    @Data
    public static class Update {
        @NotNull(message = "生成配置ID不能为空")
        private Long id;

        @NotBlank(message = "表名不能为空")
        private String tableName;

        @NotBlank(message = "类名不能为空")
        @Pattern(regexp = ValidationPatterns.CLASS_NAME, message = "类名需为大驼峰格式")
        private String className;

        @NotBlank(message = "包名不能为空")
        @Pattern(regexp = ValidationPatterns.PACKAGE_NAME, message = "包名格式不正确")
        private String packageName;

        @Size(max = 50, message = "模块名长度不能超过50位")
        private String moduleName;

        @NotBlank(message = "业务名不能为空")
        @Pattern(regexp = ValidationPatterns.BUSINESS_NAME, message = "业务名需为小驼峰或小写字母开头")
        private String businessName;

        @Size(max = 50, message = "功能名长度不能超过50位")
        private String functionName;

        @Size(max = 50, message = "作者长度不能超过50位")
        private String author;

        @Valid
        private List<ColumnUpdate> columns;
    }

    @Data
    public static class ColumnUpdate {
        @NotNull(message = "列配置ID不能为空")
        private Long id;

        @Size(max = 100, message = "列注释长度不能超过100位")
        private String columnComment;

        @NotBlank(message = "Java字段不能为空")
        @Pattern(regexp = ValidationPatterns.JAVA_FIELD, message = "Java字段名格式不正确")
        private String javaField;

        @NotBlank(message = "Java类型不能为空")
        @Pattern(regexp = "^(String|Integer|Long|Double|Boolean|LocalDateTime|LocalDate|Float)$", message = "Java类型无效")
        private String javaType;

        private Boolean isRequired;
        private Boolean isInsert;
        private Boolean isEdit;
        private Boolean isList;
        private Boolean isQuery;

        @NotBlank(message = "查询方式不能为空")
        @Pattern(regexp = "^(EQ|NE|LIKE|GT|GE|LT|LE|BETWEEN)$", message = "查询方式无效")
        private String queryType;

        @NotBlank(message = "表单类型不能为空")
        @Pattern(regexp = "^(input|textarea|inputNumber|select|switch|datetime|imageUpload|fileUpload|editor)$", message = "表单类型无效")
        private String htmlType;

        @Pattern(regexp = "^$|" + ValidationPatterns.DICT_CODE, message = "字典编码格式不正确")
        private String dictType;

        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
    }
}
