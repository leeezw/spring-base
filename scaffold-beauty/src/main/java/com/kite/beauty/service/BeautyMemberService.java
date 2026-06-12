package com.kite.beauty.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.auth.model.LoginUserContext;
import com.kite.beauty.dto.request.FollowRecordRequests;
import com.kite.beauty.dto.request.MemberRequests;
import com.kite.beauty.dto.response.BeautyAppointmentResponse;
import com.kite.beauty.dto.response.BeautyMemberResponse;
import com.kite.beauty.dto.response.BeautyMemberFollowRecordResponse;
import com.kite.beauty.entity.BeautyMember;
import com.kite.beauty.entity.BeautyMemberFollowRecord;
import com.kite.beauty.entity.BeautyStore;
import com.kite.beauty.mapper.BeautyMemberFollowRecordMapper;
import com.kite.beauty.mapper.BeautyMemberMapper;
import com.kite.beauty.mapper.BeautyStoreMapper;
import com.kite.beauty.support.BeautyTenantSupport;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeautyMemberService extends ServiceImpl<BeautyMemberMapper, BeautyMember> {

    private static final DateTimeFormatter MEMBER_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BeautyStoreMapper storeMapper;
    private final BeautyAppointmentService appointmentService;
    private final BeautyMemberFollowRecordMapper followRecordMapper;

    public PageResult<BeautyMemberResponse> pageMembers(int pageNum, int pageSize,
                                                        String keyword, Long belongStoreId,
                                                        String level, Integer status) {
        LambdaQueryWrapper<BeautyMember> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(BeautyMember::getName, trimmed)
                    .or()
                    .like(BeautyMember::getPhone, trimmed)
                    .or()
                    .like(BeautyMember::getMemberNo, trimmed));
        }
        if (belongStoreId != null) {
            wrapper.eq(BeautyMember::getBelongStoreId, belongStoreId);
        }
        if (StringUtils.hasText(level)) {
            wrapper.eq(BeautyMember::getLevel, level.trim());
        }
        if (status != null) {
            wrapper.eq(BeautyMember::getStatus, status);
        }
        wrapper.orderByDesc(BeautyMember::getUpdateTime).orderByDesc(BeautyMember::getId);

        IPage<BeautyMember> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> storeNameMap = loadStoreNameMap(page.getRecords());
        List<BeautyMemberResponse> records = page.getRecords().stream()
                .map(member -> toResponse(member, storeNameMap))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public BeautyMemberResponse getMemberDetail(Long id) {
        BeautyMember member = requireMember(id);
        return toResponse(member, loadStoreNameMap(Collections.singletonList(member)));
    }

    public List<BeautyAppointmentResponse> listAppointments(Long id, int limit) {
        requireMember(id);
        return appointmentService.listMemberAppointments(id, limit);
    }

    public List<BeautyMemberFollowRecordResponse> listFollowRecords(Long id, int limit) {
        requireMember(id);
        return followRecordMapper.selectByMemberId(id, clampLimit(limit)).stream()
                .map(BeautyMemberFollowRecordResponse::from)
                .collect(Collectors.toList());
    }

    public void addMember(MemberRequests.Save request) {
        checkPhoneUnique(request.getPhone(), null);
        validateStore(request.getBelongStoreId());
        BeautyMember member = toEntity(request);
        member.setTenantId(BeautyTenantSupport.currentTenantId());
        member.setMemberNo(generateMemberNo());
        member.setTotalConsumeAmount(BigDecimal.ZERO);
        save(member);
    }

    public void updateMember(Long id, MemberRequests.Update request) {
        BeautyMember exist = requireMember(id);
        String phone = trim(request.getPhone());
        if (!exist.getPhone().equals(phone)) {
            checkPhoneUnique(phone, id);
        }
        validateStore(request.getBelongStoreId());
        BeautyMember update = toEntity(request);
        update.setId(id);
        update.setTenantId(exist.getTenantId());
        update.setMemberNo(exist.getMemberNo());
        update.setTotalConsumeAmount(exist.getTotalConsumeAmount());
        update.setLastConsumeTime(exist.getLastConsumeTime());
        updateById(update);
    }

    public void updateStatus(Long id, Integer status) {
        requireMember(id);
        BeautyMember update = new BeautyMember();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    public void addFollowRecord(Long id, FollowRecordRequests.Save request) {
        BeautyMember member = requireMember(id);
        if (request.getStoreId() != null) {
            validateStore(request.getStoreId());
        }

        BeautyMemberFollowRecord record = new BeautyMemberFollowRecord();
        record.setTenantId(member.getTenantId() != null ? member.getTenantId() : BeautyTenantSupport.currentTenantId());
        record.setMemberId(id);
        record.setStoreId(request.getStoreId());
        record.setFollowType(trimToNull(request.getFollowType()));
        record.setFollowResult(trimToNull(request.getFollowResult()));
        record.setContent(trim(request.getContent()));
        record.setNextFollowTime(request.getNextFollowTime());
        record.setOperatorId(LoginUserContext.getUserId());
        followRecordMapper.insert(record);
    }

    public void deleteMember(Long id) {
        requireMember(id);
        removeById(id);
    }

    private BeautyMember requireMember(Long id) {
        BeautyMember member = getById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return member;
    }

    private void validateStore(Long storeId) {
        BeautyStore store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException("归属门店不存在");
        }
        if (store.getStatus() != null && store.getStatus() == 0) {
            throw new BusinessException("停业门店不能作为会员归属门店");
        }
    }

    private void checkPhoneUnique(String phone, Long excludeId) {
        String trimmed = trim(phone);
        LambdaQueryWrapper<BeautyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BeautyMember::getPhone, trimmed);
        if (excludeId != null) {
            wrapper.ne(BeautyMember::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("手机号已存在会员档案");
        }
    }

    private String generateMemberNo() {
        for (int i = 0; i < 5; i++) {
            String memberNo = "M" + LocalDateTime.now().format(MEMBER_NO_TIME_FORMATTER)
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (count(new LambdaQueryWrapper<BeautyMember>().eq(BeautyMember::getMemberNo, memberNo)) == 0) {
                return memberNo;
            }
        }
        throw new BusinessException("会员编号生成失败，请重试");
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }

    private Map<Long, String> loadStoreNameMap(List<BeautyMember> members) {
        List<Long> storeIds = members.stream()
                .map(BeautyMember::getBelongStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return storeMapper.selectBatchIds(storeIds).stream()
                .collect(Collectors.toMap(BeautyStore::getId, BeautyStore::getStoreName));
    }

    private BeautyMemberResponse toResponse(BeautyMember member, Map<Long, String> storeNameMap) {
        BeautyMemberResponse response = BeautyMemberResponse.from(member);
        if (response != null && response.getBelongStoreId() != null) {
            response.setBelongStoreName(storeNameMap.get(response.getBelongStoreId()));
        }
        return response;
    }

    private BeautyMember toEntity(MemberRequests.Save request) {
        BeautyMember member = new BeautyMember();
        member.setName(trim(request.getName()));
        member.setPhone(trim(request.getPhone()));
        member.setGender(request.getGender() == null ? 0 : request.getGender());
        member.setBirthday(request.getBirthday());
        member.setSource(trimToNull(request.getSource()));
        member.setLevel(StringUtils.hasText(request.getLevel()) ? request.getLevel().trim() : "normal");
        member.setTags(normalizeTags(request.getTags()));
        member.setBelongStoreId(request.getBelongStoreId());
        member.setStatus(request.getStatus());
        member.setRemark(trimToNull(request.getRemark()));
        return member;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
