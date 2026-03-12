import { useEffect, useState } from 'react';
import { getUserMenus } from '@/api/menu';
import { convertMenuToRoutes, getCachedRoutes, cacheRoutes } from '@/utils/dynamicRoutes';
import { IRoute } from '@/routes';

/**
 * 动态路由Hook
 * 从后端获取用户菜单并转换为路由
 */
export function useDynamicRoutes() {
  const [routes, setRoutes] = useState<IRoute[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 先尝试从缓存加载
    const cached = getCachedRoutes();
    if (cached) {
      setRoutes(cached);
      setLoading(false);
      return;
    }

    // 从后端获取菜单
    getUserMenus()
      .then((res) => {
        if (res.data.code === 200) {
          const menuRoutes = convertMenuToRoutes(res.data.data);
          setRoutes(menuRoutes);
          cacheRoutes(menuRoutes);
        }
      })
      .catch((err) => {
        console.error('获取用户菜单失败:', err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return { routes, loading };
}
