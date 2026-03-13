package com.kite.user.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kite.common.response.Result;
import com.kite.permission.annotation.RequiresPermissions;
import com.kite.user.entity.SysOperationLog;
import com.kite.user.entity.SysUser;
import com.kite.user.mapper.SysOperationLogMapper;
import com.kite.user.service.SysUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/export")
@RequiredArgsConstructor
public class ExportController {

    private final SysUserService userService;
    private final SysOperationLogMapper operationLogMapper;

    @GetMapping("/users")
    @RequiresPermissions("system:user:query")
    public void exportUsers(HttpServletResponse response) throws Exception {
        List<SysUser> users = userService.list();
        List<UserExportDTO> data = users.stream().map(u -> {
            UserExportDTO dto = new UserExportDTO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setNickname(u.getNickname());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setStatus(u.getStatus() == 1 ? "启用" : "禁用");
            dto.setCreateTime(u.getCreateTime() != null ? u.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            return dto;
        }).collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode("用户列表.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), UserExportDTO.class).sheet("用户列表").doWrite(data);
    }

    @GetMapping("/operation-logs")
    @RequiresPermissions("system:user:query")
    public void exportOperationLogs(HttpServletResponse response) throws Exception {
        List<SysOperationLog> logs = operationLogMapper.selectList(
            new LambdaQueryWrapper<SysOperationLog>().orderByDesc(SysOperationLog::getCreateTime).last("LIMIT 1000"));
        List<OpLogExportDTO> data = logs.stream().map(l -> {
            OpLogExportDTO dto = new OpLogExportDTO();
            dto.setModule(l.getModule());
            dto.setOperationType(l.getOperationType());
            dto.setDescription(l.getDescription());
            dto.setUsername(l.getUsername());
            dto.setIp(l.getIp());
            dto.setStatus(l.getStatus() == 1 ? "成功" : "失败");
            dto.setDuration(l.getDuration() != null ? l.getDuration() + "ms" : "");
            dto.setCreateTime(l.getCreateTime() != null ? l.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            return dto;
        }).collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode("操作日志.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), OpLogExportDTO.class).sheet("操作日志").doWrite(data);
    }

    @Data
    @ColumnWidth(15)
    public static class UserExportDTO {
        @ExcelProperty("用户ID")
        private Long id;
        @ExcelProperty("用户名")
        private String username;
        @ExcelProperty("昵称")
        private String nickname;
        @ExcelProperty("邮箱")
        @ColumnWidth(25)
        private String email;
        @ExcelProperty("手机号")
        private String phone;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("创建时间")
        @ColumnWidth(20)
        private String createTime;
    }

    @Data
    @ColumnWidth(15)
    public static class OpLogExportDTO {
        @ExcelProperty("模块")
        private String module;
        @ExcelProperty("操作类型")
        private String operationType;
        @ExcelProperty("描述")
        @ColumnWidth(25)
        private String description;
        @ExcelProperty("操作人")
        private String username;
        @ExcelProperty("IP")
        private String ip;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("耗时")
        private String duration;
        @ExcelProperty("时间")
        @ColumnWidth(20)
        private String createTime;
    }
}
