package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.common.exception.BusinessException;
import com.kite.mybatis.context.TenantContext;
import com.kite.user.entity.SysDict;
import com.kite.user.entity.SysDictItem;
import com.kite.user.mapper.SysDictItemMapper;
import com.kite.user.mapper.SysDictMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysDictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper itemMapper;

    public SysDictService(SysDictMapper dictMapper, SysDictItemMapper itemMapper) {
        this.dictMapper = dictMapper;
        this.itemMapper = itemMapper;
    }

    public IPage<SysDict> pageDicts(int page, int size, String keyword) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysDict::getDictName, keyword).or().like(SysDict::getDictCode, keyword));
        }
        wrapper.orderByAsc(SysDict::getSortOrder).orderByAsc(SysDict::getId);
        IPage<SysDict> result = dictMapper.selectPage(new Page<>(page, size), wrapper);

        if (!result.getRecords().isEmpty()) {
            List<Long> dictIds = result.getRecords().stream().map(SysDict::getId).collect(Collectors.toList());
            Map<Long, List<SysDictItem>> itemMap = itemMapper.selectList(
                            new LambdaQueryWrapper<SysDictItem>().in(SysDictItem::getDictId, dictIds)
                                    .orderByAsc(SysDictItem::getSortOrder))
                    .stream().collect(Collectors.groupingBy(SysDictItem::getDictId));
            result.getRecords().forEach(d -> d.setItems(itemMap.getOrDefault(d.getId(), Collections.emptyList())));
        }
        return result;
    }

    public List<SysDict> listAll() {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getSortOrder));
    }

    public void createDict(SysDict dict) {
        dict.setDictCode(trim(dict.getDictCode()));
        dict.setDictName(trim(dict.getDictName()));
        dict.setDescription(trimToNull(dict.getDescription()));
        if (dict.getSortOrder() == null) {
            dict.setSortOrder(0);
        }
        dict.setTenantId(resolveTenantId());
        ensureDictCodeUnique(dict.getDictCode(), dict.getTenantId(), null);
        dict.setCreateTime(LocalDateTime.now());
        dict.setUpdateTime(LocalDateTime.now());
        dictMapper.insert(dict);
    }

    public void updateDict(SysDict dict) {
        SysDict existDict = dictMapper.selectById(dict.getId());
        if (existDict == null) {
            throw new BusinessException("字典不存在");
        }
        dict.setDictCode(trim(dict.getDictCode()));
        dict.setDictName(trim(dict.getDictName()));
        dict.setDescription(trimToNull(dict.getDescription()));
        if (dict.getSortOrder() == null) {
            dict.setSortOrder(existDict.getSortOrder());
        }
        dict.setTenantId(existDict.getTenantId());
        ensureDictCodeUnique(dict.getDictCode(), existDict.getTenantId(), dict.getId());
        dict.setUpdateTime(LocalDateTime.now());
        dictMapper.updateById(dict);
    }

    @Transactional
    public void deleteDict(Long id) {
        dictMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));
    }

    public List<SysDictItem> getItemsByCode(String dictCode) {
        SysDict dict = dictMapper.selectOne(
                new LambdaQueryWrapper<SysDict>().eq(SysDict::getDictCode, dictCode).eq(SysDict::getStatus, 1));
        if (dict == null) {
            return Collections.emptyList();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, dict.getId())
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSortOrder));
    }

    public Map<String, List<SysDictItem>> getItemsByCodes(List<String> dictCodes) {
        List<SysDict> dicts = dictMapper.selectList(
                new LambdaQueryWrapper<SysDict>().in(SysDict::getDictCode, dictCodes).eq(SysDict::getStatus, 1));
        if (dicts.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> idCodeMap = dicts.stream().collect(Collectors.toMap(SysDict::getId, SysDict::getDictCode));
        List<SysDictItem> items = itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .in(SysDictItem::getDictId, idCodeMap.keySet())
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSortOrder));

        Map<String, List<SysDictItem>> result = new HashMap<>();
        for (SysDictItem item : items) {
            String code = idCodeMap.get(item.getDictId());
            result.computeIfAbsent(code, k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    public List<SysDictItem> listItemsByDictId(Long dictId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, dictId)
                .orderByAsc(SysDictItem::getSortOrder));
    }

    public void createItem(SysDictItem item) {
        prepareItem(item, null);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.insert(item);
        clearOtherDefaults(item);
    }

    public void updateItem(SysDictItem item) {
        SysDictItem existItem = itemMapper.selectById(item.getId());
        if (existItem == null) {
            throw new BusinessException("字典数据项不存在");
        }
        prepareItem(item, existItem);
        item.setTenantId(existItem.getTenantId());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(item);
        clearOtherDefaults(item);
    }

    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }

    private void ensureDictCodeUnique(String dictCode, Long tenantId, Long excludeId) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getTenantId, tenantId)
                .eq(SysDict::getDictCode, dictCode);
        if (excludeId != null) {
            wrapper.ne(SysDict::getId, excludeId);
        }
        if (dictMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("字典编码已存在");
        }
    }

    private void prepareItem(SysDictItem item, SysDictItem existItem) {
        SysDict dict = dictMapper.selectById(item.getDictId());
        if (dict == null) {
            throw new BusinessException("字典不存在");
        }
        item.setTenantId(dict.getTenantId());
        item.setItemLabel(trim(item.getItemLabel()));
        item.setItemValue(trim(item.getItemValue()));
        item.setItemColor(trimToNull(item.getItemColor()));
        item.setItemIcon(trimToNull(item.getItemIcon()));
        item.setDescription(trimToNull(item.getDescription()));
        if (item.getSortOrder() == null) {
            item.setSortOrder(existItem != null ? existItem.getSortOrder() : 0);
        }
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, item.getDictId())
                .eq(SysDictItem::getItemValue, item.getItemValue());
        if (item.getId() != null) {
            wrapper.ne(SysDictItem::getId, item.getId());
        }
        if (itemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("同一字典下数据值不能重复");
        }
    }

    private void clearOtherDefaults(SysDictItem item) {
        if (item.getIsDefault() == null || item.getIsDefault() != 1 || item.getId() == null) {
            return;
        }
        itemMapper.update(null, new LambdaUpdateWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, item.getDictId())
                .ne(SysDictItem::getId, item.getId())
                .set(SysDictItem::getIsDefault, 0));
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : 1L;
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
