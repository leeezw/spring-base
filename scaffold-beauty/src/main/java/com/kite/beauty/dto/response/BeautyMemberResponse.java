package com.kite.beauty.dto.response;

import com.kite.beauty.entity.BeautyMember;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BeautyMemberResponse {

    private Long id;
    private Long tenantId;
    private String memberNo;
    private String name;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private String source;
    private String level;
    private List<String> tags;
    private Long belongStoreId;
    private String belongStoreName;
    private BigDecimal totalConsumeAmount;
    private LocalDateTime lastConsumeTime;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BeautyMemberResponse from(BeautyMember member) {
        if (member == null) {
            return null;
        }
        BeautyMemberResponse response = new BeautyMemberResponse();
        response.setId(member.getId());
        response.setTenantId(member.getTenantId());
        response.setMemberNo(member.getMemberNo());
        response.setName(member.getName());
        response.setPhone(member.getPhone());
        response.setGender(member.getGender());
        response.setBirthday(member.getBirthday());
        response.setSource(member.getSource());
        response.setLevel(member.getLevel());
        response.setTags(member.getTags());
        response.setBelongStoreId(member.getBelongStoreId());
        response.setTotalConsumeAmount(member.getTotalConsumeAmount());
        response.setLastConsumeTime(member.getLastConsumeTime());
        response.setStatus(member.getStatus());
        response.setRemark(member.getRemark());
        response.setCreateTime(member.getCreateTime());
        response.setUpdateTime(member.getUpdateTime());
        return response;
    }
}
