import { useState, useEffect, useCallback, useRef } from 'react';
import request from '../api/index.js';

/**
 * 字典数据缓存（页面级，刷新后清空）
 */
const dictCache = {};

/**
 * 通用字典 hook
 * 
 * 用法:
 *   const { items, labelMap, colorMap, getLabel, getColor } = useDict('post_category');
 *   
 *   // 渲染Tag
 *   <Tag color={getColor(record.postCategory)}>{getLabel(record.postCategory)}</Tag>
 *   
 *   // 渲染Select
 *   items.map(i => <Option value={i.itemValue}>{i.itemLabel}</Option>)
 */
export function useDict(dictCode) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  useEffect(() => {
    if (!dictCode) return;

    // 命中缓存
    if (dictCache[dictCode]) {
      setItems(dictCache[dictCode]);
      return;
    }

    setLoading(true);
    request.get(`/system/dict/items/${dictCode}`)
      .then(res => {
        if (res.code === 200 && mounted.current) {
          const data = res.data || [];
          dictCache[dictCode] = data;
          setItems(data);
        }
      })
      .catch(() => {})
      .finally(() => { if (mounted.current) setLoading(false); });
  }, [dictCode]);

  // value → label
  const getLabel = useCallback((value) => {
    const v = String(value);
    const item = items.find(i => String(i.itemValue) === v);
    return item?.itemLabel || value;
  }, [items]);

  // value → color
  const getColor = useCallback((value) => {
    const v = String(value);
    const item = items.find(i => String(i.itemValue) === v);
    return item?.itemColor || 'default';
  }, [items]);

  // labelMap: { "1": "高管", "2": "中层" }
  const labelMap = items.reduce((m, i) => { m[i.itemValue] = i.itemLabel; return m; }, {});
  const colorMap = items.reduce((m, i) => { m[i.itemValue] = i.itemColor; return m; }, {});

  return { items, loading, labelMap, colorMap, getLabel, getColor };
}

/**
 * 批量加载多个字典
 * 
 * 用法:
 *   const dicts = useDicts(['post_category', 'gender', 'permission_type']);
 *   dicts.post_category.getLabel(1)  // "高管"
 */
export function useDicts(codes) {
  const [data, setData] = useState({});
  
  useEffect(() => {
    if (!codes?.length) return;
    
    const uncached = codes.filter(c => !dictCache[c]);
    if (uncached.length === 0) {
      const result = {};
      codes.forEach(c => { result[c] = dictCache[c] || []; });
      setData(result);
      return;
    }

    request.get('/system/dict/items/batch', { params: { codes: uncached.join(',') } })
      .then(res => {
        if (res.code === 200) {
          Object.entries(res.data || {}).forEach(([code, items]) => {
            dictCache[code] = items;
          });
          const result = {};
          codes.forEach(c => { result[c] = dictCache[c] || []; });
          setData(result);
        }
      })
      .catch(() => {});
  }, [codes?.join(',')]);

  // 给每个字典加上 getLabel/getColor 方法
  const result = {};
  for (const code of (codes || [])) {
    const items = data[code] || [];
    result[code] = {
      items,
      getLabel: (v) => { const item = items.find(i => String(i.itemValue) === String(v)); return item?.itemLabel || v; },
      getColor: (v) => { const item = items.find(i => String(i.itemValue) === String(v)); return item?.itemColor || 'default'; },
    };
  }
  return result;
}

/**
 * 清除字典缓存（字典管理页修改后调用）
 */
export function clearDictCache(dictCode) {
  if (dictCode) {
    delete dictCache[dictCode];
  } else {
    Object.keys(dictCache).forEach(k => delete dictCache[k]);
  }
}
