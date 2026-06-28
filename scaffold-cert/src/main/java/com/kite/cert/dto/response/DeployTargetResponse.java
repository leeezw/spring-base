package com.kite.cert.dto.response;

import com.kite.cert.entity.CertDeployTarget;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部署目标响应（不含配置明文）。
 */
@Data
public class DeployTargetResponse {

    private Long id;
    private String name;
    private String type;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;

    public static DeployTargetResponse from(CertDeployTarget e) {
        if (e == null) {
            return null;
        }
        DeployTargetResponse r = new DeployTargetResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setType(e.getType());
        r.setEnabled(e.getEnabled());
        r.setRemark(e.getRemark());
        r.setCreateTime(e.getCreateTime());
        return r;
    }
}
