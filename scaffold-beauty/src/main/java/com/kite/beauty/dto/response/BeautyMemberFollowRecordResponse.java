package com.kite.beauty.dto.response;

import com.kite.beauty.entity.BeautyMemberFollowRecord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BeautyMemberFollowRecordResponse {

    private Long id;
    private Long tenantId;
    private Long memberId;
    private Long storeId;
    private String storeName;
    private String followType;
    private String followResult;
    private String content;
    private LocalDateTime nextFollowTime;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BeautyMemberFollowRecordResponse from(BeautyMemberFollowRecord record) {
        if (record == null) {
            return null;
        }
        BeautyMemberFollowRecordResponse response = new BeautyMemberFollowRecordResponse();
        response.setId(record.getId());
        response.setTenantId(record.getTenantId());
        response.setMemberId(record.getMemberId());
        response.setStoreId(record.getStoreId());
        response.setStoreName(record.getStoreName());
        response.setFollowType(record.getFollowType());
        response.setFollowResult(record.getFollowResult());
        response.setContent(record.getContent());
        response.setNextFollowTime(record.getNextFollowTime());
        response.setOperatorId(record.getOperatorId());
        response.setOperatorName(record.getOperatorName());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        return response;
    }
}
