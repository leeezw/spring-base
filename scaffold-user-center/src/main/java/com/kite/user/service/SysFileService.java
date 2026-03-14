package com.kite.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.auth.model.LoginUserContext;
import com.kite.common.exception.BusinessException;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysFile;
import com.kite.user.mapper.SysFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class SysFileService extends ServiceImpl<SysFileMapper, SysFile> {

    @Value("${app.upload.path:/data/upload}")
    private String uploadPath;

    @Value("${app.upload.max-size:20}")
    private int maxSizeMB;

    @Value("${app.upload.storage-type:local}")
    private String defaultStorageType;

    // 预留OSS配置
    @Value("${app.upload.oss.endpoint:}")
    private String ossEndpoint;
    @Value("${app.upload.oss.bucket:}")
    private String ossBucket;
    @Value("${app.upload.oss.access-key:}")
    private String ossAccessKey;
    @Value("${app.upload.oss.secret-key:}")
    private String ossSecretKey;

    private static final String[] ALLOWED_IMAGE_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    };

    /**
     * 上传文件
     * @param file   文件
     * @param module 业务模块(avatar/attachment/export)
     */
    public SysFile upload(MultipartFile file, String module) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > (long) maxSizeMB * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过" + maxSizeMB + "MB");
        }

        String originalName = file.getOriginalFilename();
        String mimeType = file.getContentType();
        String fileType = getFileType(originalName);

        // 头像模块只允许图片
        if ("avatar".equals(module)) {
            boolean isImage = false;
            for (String type : ALLOWED_IMAGE_TYPES) {
                if (type.equals(mimeType)) { isImage = true; break; }
            }
            if (!isImage) throw new BusinessException("头像只支持 JPG/PNG/GIF/WebP 格式");
        }

        // 租户隔离
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) tenantId = 1L;
        String tenantDir = "t" + tenantId;

        // 生成存储路径: /data/upload/{tenantDir}/{module}/{yyyy/MM/dd}/{uuid}.{ext}
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "." + fileType;
        String relativePath = tenantDir + "/" + (module != null ? module : "default") + "/" + datePath + "/" + fileName;

        // 根据存储类型分发
        String storageType = defaultStorageType;
        String accessUrl = null;

        if ("oss".equals(storageType) && !ossEndpoint.isEmpty()) {
            accessUrl = uploadToOss(file, relativePath);
        } else {
            // 本地存储
            storageType = "local";
            uploadToLocal(file, relativePath);
        }

        // 保存数据库记录
        SysFile sysFile = new SysFile();
        sysFile.setFileName(fileName);
        sysFile.setOriginalName(originalName);
        sysFile.setFilePath(relativePath);
        sysFile.setFileSize(file.getSize());
        sysFile.setFileType(fileType);
        sysFile.setMimeType(mimeType);
        sysFile.setStorageType(storageType);
        sysFile.setModule(module);
        sysFile.setCreateTime(LocalDateTime.now());
        sysFile.setCreateBy(LoginUserContext.getUserId());
        sysFile.setTenantId(tenantId);

        save(sysFile);
        return sysFile;
    }

    /**
     * 本地存储
     */
    private void uploadToLocal(MultipartFile file, String relativePath) {
        Path fullPath = Paths.get(uploadPath, relativePath);
        try {
            Files.createDirectories(fullPath.getParent());
            file.transferTo(fullPath.toFile());
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败");
        }
    }

    /**
     * OSS 存储（预留接口，未实现时抛异常）
     * 接入时只需：
     * 1. 引入 OSS SDK 依赖
     * 2. 配置 app.upload.oss.* 参数
     * 3. 实现此方法
     * @return 文件访问URL
     */
    private String uploadToOss(MultipartFile file, String relativePath) {
        // TODO: 接入 OSS/COS/S3
        // 示例伪代码：
        // OSSClient client = new OSSClient(ossEndpoint, ossAccessKey, ossSecretKey);
        // client.putObject(ossBucket, relativePath, file.getInputStream());
        // return "https://" + ossBucket + "." + ossEndpoint + "/" + relativePath;
        throw new BusinessException("OSS 存储尚未配置，请先配置 app.upload.oss.* 参数或使用本地存储");
    }

    /**
     * 获取文件的本地完整路径
     */
    public Path getFilePath(Long fileId) {
        SysFile sysFile = getById(fileId);
        if (sysFile == null) throw new BusinessException("文件不存在");

        if ("oss".equals(sysFile.getStorageType())) {
            throw new BusinessException("OSS 文件请通过URL直接访问");
        }
        return Paths.get(uploadPath, sysFile.getFilePath());
    }

    /**
     * 获取文件访问URL
     * 本地文件返回 /api/system/file/{id}
     * OSS文件返回完整URL
     */
    public String getFileUrl(Long fileId) {
        SysFile sysFile = getById(fileId);
        if (sysFile == null) return null;

        if ("oss".equals(sysFile.getStorageType())) {
            // OSS: 返回完整URL
            return "https://" + ossBucket + "." + ossEndpoint + "/" + sysFile.getFilePath();
        }
        // 本地: 返回API路径
        return "/api/system/file/" + sysFile.getId();
    }

    /**
     * 获取文件信息
     */
    public SysFile getFileInfo(Long fileId) {
        SysFile sysFile = getById(fileId);
        if (sysFile == null) throw new BusinessException("文件不存在");
        return sysFile;
    }

    /**
     * 删除文件（逻辑删除记录 + 物理删除文件）
     */
    public void deleteFile(Long fileId) {
        SysFile sysFile = getById(fileId);
        if (sysFile == null) return;

        if ("local".equals(sysFile.getStorageType())) {
            try {
                Path path = Paths.get(uploadPath, sysFile.getFilePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("物理删除文件失败: {}", e.getMessage());
            }
        } else if ("oss".equals(sysFile.getStorageType())) {
            // TODO: 调用 OSS SDK 删除
            log.info("OSS 文件删除待实现: {}", sysFile.getFilePath());
        }

        removeById(fileId);
    }

    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) return "unknown";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
