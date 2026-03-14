import axios from 'axios';

export interface MenuItem {
  id: number;
  menuName: string;
  path: string;
  component: string;
  icon: string;
  parentId: number;
  sortOrder: number;
  visible: number;
  status: number;
  children?: MenuItem[];
}

/**
 * 获取当前用户的菜单树
 */
export function getUserMenus() {
  return axios.get<MenuItem[]>('/api/system/menu/user-menus');
}
