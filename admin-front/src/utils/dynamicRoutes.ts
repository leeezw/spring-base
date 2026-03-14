import { MenuItem } from '@/api/menu';
import { IRoute } from '@/routes';

/**
 * 将后端菜单数据转换为前端路由格式
 */
export function convertMenuToRoutes(menus: MenuItem[]): IRoute[] {
  return menus.map((menu) => {
    const route: IRoute = {
      name: menu.menuName,
      key: menu.path.startsWith('/') ? menu.path.substring(1) : menu.path,
    };

    // 如果有子菜单，递归转换
    if (menu.children && menu.children.length > 0) {
      route.children = convertMenuToRoutes(menu.children);
    }

    return route;
  });
}

/**
 * 从localStorage获取缓存的菜单路由
 */
export function getCachedRoutes(): IRoute[] | null {
  const cached = localStorage.getItem('userRoutes');
  if (cached) {
    try {
      return JSON.parse(cached);
    } catch (e) {
      return null;
    }
  }
  return null;
}

/**
 * 缓存菜单路由到localStorage
 */
export function cacheRoutes(routes: IRoute[]) {
  localStorage.setItem('userRoutes', JSON.stringify(routes));
}

/**
 * 清除缓存的路由
 */
export function clearCachedRoutes() {
  localStorage.removeItem('userRoutes');
}
