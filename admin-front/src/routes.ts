import auth, { AuthParams } from '@/utils/authentication';
import { useEffect, useMemo, useState } from 'react';
import { getUserMenus } from '@/api/menu';
import { convertMenuToRoutes, getCachedRoutes, cacheRoutes, clearCachedRoutes } from '@/utils/dynamicRoutes';

export type IRoute = AuthParams & {
  name: string;
  key: string;
  // 当前页是否展示面包屑
  breadcrumb?: boolean;
  children?: IRoute[];
  // 当前路由是否渲染菜单项，为 true 的话不会在菜单中显示，但可通过路由地址访问。
  ignore?: boolean;
};

// 默认静态路由（用于开发和fallback）
export const defaultRoutes: IRoute[] = [
  {
    name: '系统管理',
    key: 'system',
    children: [
      {
        name: '用户管理',
        key: 'system/user',
      },
      {
        name: '角色管理',
        key: 'system/role',
      },
      {
        name: '权限管理',
        key: 'system/permission',
      },
      {
        name: '菜单管理',
        key: 'system/menu',
      },
      {
        name: '部门管理',
        key: 'system/dept',
      },
      {
        name: '租户管理',
        key: 'system/tenant',
      },
    ],
  },
];

export const getName = (path: string, routes) => {
  return routes.find((item) => {
    const itemPath = `/${item.key}`;
    if (path === itemPath) {
      return item.name;
    } else if (item.children) {
      return getName(path, item.children);
    }
  });
};

export const generatePermission = (role: string) => {
  const actions = role === 'admin' ? ['*'] : ['read'];
  const result = {};
  defaultRoutes.forEach((item) => {
    if (item.children) {
      item.children.forEach((child) => {
        result[child.name] = actions;
      });
    }
  });
  return result;
};

const useRoute = (userPermission): [IRoute[], string] => {
  const [routes, setRoutes] = useState<IRoute[]>(defaultRoutes);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 先尝试从缓存加载
    const cached = getCachedRoutes();
    if (cached && cached.length > 0) {
      setRoutes(cached);
      setLoading(false);
      return;
    }

    // 从后端获取菜单
    getUserMenus()
      .then((res) => {
        if (res.data.code === 200 && res.data.data) {
          const menuRoutes = convertMenuToRoutes(res.data.data);
          if (menuRoutes.length > 0) {
            setRoutes(menuRoutes);
            cacheRoutes(menuRoutes);
          }
        }
      })
      .catch((err) => {
        console.error('获取用户菜单失败:', err);
        // 失败时使用默认路由
        setRoutes(defaultRoutes);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  const filterRoute = (routes: IRoute[], arr = []): IRoute[] => {
    if (!routes.length) {
      return [];
    }
    for (const route of routes) {
      const { requiredPermissions, oneOfPerm } = route;
      let visible = true;
      if (requiredPermissions) {
        visible = auth({ requiredPermissions, oneOfPerm }, userPermission);
      }

      if (!visible) {
        continue;
      }
      if (route.children && route.children.length) {
        const newRoute = { ...route, children: [] };
        filterRoute(route.children, newRoute.children);
        if (newRoute.children.length) {
          arr.push(newRoute);
        }
      } else {
        arr.push({ ...route });
      }
    }

    return arr;
  };

  const [permissionRoute, setPermissionRoute] = useState(routes);

  useEffect(() => {
    const newRoutes = filterRoute(routes);
    setPermissionRoute(newRoutes);
  }, [JSON.stringify(userPermission), JSON.stringify(routes)]);

  const defaultRoute = useMemo(() => {
    const first = permissionRoute[0];
    if (first) {
      const firstRoute = first?.children?.[0]?.key || first.key;
      return firstRoute;
    }
    return '';
  }, [permissionRoute]);

  return [permissionRoute, defaultRoute];
};

export default useRoute;

// 导出清除缓存的函数，用于登出时清理
export { clearCachedRoutes };
