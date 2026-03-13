package com.kite.user.controller;

import com.kite.auth.annotation.AllowAnonymous;
import com.kite.common.response.Result;
import com.kite.user.entity.SysFile;
import com.kite.user.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system/file")
@RequiredArgsConstructor
public class SysFileController {

    private final SysFileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "module", defaultValue = "attachment") String module) {
        SysFile sysFile = fileService.upload(file, module);

        Map<String, Object> result = new HashMap<>();
        result.put("id", sysFile.getId());
        result.put("fileName", sysFile.getFileName());
        result.put("originalName", sysFile.getOriginalName());
        result.put("fileSize", sysFile.getFileSize());
        result.put("mimeType", sysFile.getMimeType());
        result.put("url", "/api/system/file/" + sysFile.getId());
        return Result.success(result);
    }

    /**
     * 获取/预览文件（免认证，用于头像等公开资源）
     */
    @AllowAnonymous
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable Long id) {
        try {
            SysFile fileInfo = fileService.getFileInfo(id);
            Path path = fileService.getFilePath(id);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream";

            // 图片直接预览，其他下载
            boolean isImage = contentType.startsWith("image/");
            HttpHeaders headers = new HttpHeaders();
            if (!isImage) {
                String encodedName = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{id}/info")
    public Result<SysFile> getFileInfo(@PathVariable Long id) {
        return Result.success(fileService.getFileInfo(id));
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }
}
