package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.user.entity.SysDict;
import com.kite.user.entity.SysDictItem;
import com.kite.user.mapper.SysDictMapper;
import com.kite.user.mapper.SysDictItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysDictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper itemMapper;

    public SysDictService(SysDictMapper dictMapper, SysDictItemMapper itemMapper) {
        this.dictMapper = dictMapper;
        this.itemMapper = itemMapper;
    }

    // ============ 字典类型 ============

    public IPage<SysDict> pageDicts(int page, int size, String keyword) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysDict::getDictName, keyword)
                    .or().like(SysDict::getDictCode, keyword));
        }
        wrapper.orderByAsc(SysDict::getSortOrder).orderByAsc(SysDict::getId);
        IPage<SysDict> result = dictMapper.selectPage(new Page<>(page, size), wrapper);

        // 填充每个字典的item数量
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
        dict.setCreateTime(LocalDateTime.now());
        dict.setUpdateTime(LocalDateTime.now());
        dictMapper.insert(dict);
    }

    public void updateDict(SysDict dict) {
        dict.setUpdateTime(LocalDateTime.now());
        dictMapper.updateById(dict);
    }

    @Transactional
    public void deleteDict(Long id) {
        dictMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));
    }

    // ============ 字典数据 ============

    /**
     * 根据字典编码获取数据项（前端最常用的API）
     */
    public List<SysDictItem> getItemsByCode(String dictCode) {
        SysDict dict = dictMapper.selectOne(
                new LambdaQueryWrapper<SysDict>().eq(SysDict::getDictCode, dictCode).eq(SysDict::getStatus, 1));
        if (dict == null) return Collections.emptyList();
        return itemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictId, dict.getId())
                        .eq(SysDictItem::getStatus, 1)
                        .orderByAsc(SysDictItem::getSortOrder));
    }

    /**
     * 批量获取多个字典的数据项
     */
    public Map<String, List<SysDictItem>> getItemsByCodes(List<String> dictCodes) {
        List<SysDict> dicts = dictMapper.selectList(
                new LambdaQueryWrapper<SysDict>().in(SysDict::getDictCode, dictCodes).eq(SysDict::getStatus, 1));
        if (dicts.isEmpty()) return Collections.emptyMap();

        Map<Long, String> idCodeMap = dicts.stream().collect(Collectors.toMap(SysDict::getId, SysDict::getDictCode));
        List<SysDictItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
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
        return itemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictId, dictId)
                        .orderByAsc(SysDictItem::getSortOrder));
    }

    public void createItem(SysDictItem item) {
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.insert(item);
    }

    public void updateItem(SysDictItem item) {
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(item);
    }

    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }
}
