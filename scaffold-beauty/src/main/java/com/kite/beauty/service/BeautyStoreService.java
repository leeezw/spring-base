package com.kite.beauty.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kite.beauty.dto.request.StoreRequests;
import com.kite.beauty.dto.response.BeautyStoreResponse;
import com.kite.beauty.entity.BeautyStore;
import com.kite.beauty.mapper.BeautyStoreMapper;
import com.kite.beauty.support.BeautyTenantSupport;
import com.kite.common.exception.BusinessException;
import com.kite.common.response.PageResult;
import com.kite.user.entity.SysEmployee;
import com.kite.user.mapper.SysEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeautyStoreService extends ServiceImpl<BeautyStoreMapper, BeautyStore> {

    private final SysEmployeeMapper employeeMapper;

    public PageResult<BeautyStoreResponse> pageStores(int pageNum, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<BeautyStore> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(w -> w.like(BeautyStore::getStoreName, trimmed)
                    .or()
                    .like(BeautyStore::getStoreCode, trimmed)
                    .or()
                    .like(BeautyStore::getPhone, trimmed));
        }
        if (status != null) {
            wrapper.eq(BeautyStore::getStatus, status);
        }
        wrapper.orderByDesc(BeautyStore::getUpdateTime).orderByDesc(BeautyStore::getId);

        IPage<BeautyStore> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> employeeNameMap = loadEmployeeNameMap(page.getRecords());
        List<BeautyStoreResponse> records = page.getRecords().stream()
                .map(store -> toResponse(store, employeeNameMap))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<BeautyStoreResponse> listStores(Boolean activeOnly) {
        LambdaQueryWrapper<BeautyStore> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(activeOnly)) {
            wrapper.eq(BeautyStore::getStatus, 1);
        }
        wrapper.orderByAsc(BeautyStore::getStoreCode);
        List<BeautyStore> stores = list(wrapper);
        Map<Long, String> employeeNameMap = loadEmployeeNameMap(stores);
        return stores.stream().map(store -> toResponse(store, employeeNameMap)).collect(Collectors.toList());
    }

    public BeautyStoreResponse getStoreDetail(Long id) {
        BeautyStore store = requireStore(id);
        return toResponse(store, loadEmployeeNameMap(Collections.singletonList(store)));
    }

    public void addStore(StoreRequests.Save request) {
        checkStoreCodeUnique(request.getStoreCode(), null);
        validateManagerEmployee(request.getManagerEmployeeId());
        BeautyStore store = toEntity(request);
        store.setTenantId(BeautyTenantSupport.currentTenantId());
        save(store);
    }

    public void updateStore(Long id, StoreRequests.Update request) {
        BeautyStore exist = requireStore(id);
        if (!exist.getStoreCode().equals(trim(request.getStoreCode()))) {
            checkStoreCodeUnique(request.getStoreCode(), id);
        }

        validateManagerEmployee(request.getManagerEmployeeId());
        BeautyStore update = toEntity(request);
        update.setId(id);
        update.setTenantId(exist.getTenantId());
        updateById(update);
    }

    public void updateStatus(Long id, Integer status) {
        requireStore(id);
        BeautyStore update = new BeautyStore();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
    }

    public void deleteStore(Long id) {
        requireStore(id);
        removeById(id);
    }

    private BeautyStore requireStore(Long id) {
        BeautyStore store = getById(id);
        if (store == null) {
            throw new BusinessException("门店不存在");
        }
        return store;
    }

    private void validateManagerEmployee(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        SysEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("门店负责人不存在");
        }
    }

    private Map<Long, String> loadEmployeeNameMap(List<BeautyStore> stores) {
        List<Long> employeeIds = stores.stream()
                .map(BeautyStore::getManagerEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName));
    }

    private BeautyStoreResponse toResponse(BeautyStore store, Map<Long, String> employeeNameMap) {
        BeautyStoreResponse response = BeautyStoreResponse.from(store);
        if (response != null && response.getManagerEmployeeId() != null) {
            response.setManagerEmployeeName(employeeNameMap.get(response.getManagerEmployeeId()));
        }
        return response;
    }

    private void checkStoreCodeUnique(String storeCode, Long excludeId) {
        String trimmed = trim(storeCode);
        LambdaQueryWrapper<BeautyStore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BeautyStore::getStoreCode, trimmed);
        if (excludeId != null) {
            wrapper.ne(BeautyStore::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("门店编码已存在");
        }
    }

    private BeautyStore toEntity(StoreRequests.Save request) {
        BeautyStore store = new BeautyStore();
        store.setStoreCode(trim(request.getStoreCode()));
        store.setStoreName(trim(request.getStoreName()));
        store.setManagerEmployeeId(request.getManagerEmployeeId());
        store.setPhone(trimToNull(request.getPhone()));
        store.setProvince(trimToNull(request.getProvince()));
        store.setCity(trimToNull(request.getCity()));
        store.setDistrict(trimToNull(request.getDistrict()));
        store.setAddress(trimToNull(request.getAddress()));
        store.setBusinessHours(request.getBusinessHours());
        store.setStatus(request.getStatus());
        store.setRemark(trimToNull(request.getRemark()));
        return store;
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
