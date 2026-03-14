import { useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect, useMemo } from 'react';
import { Menu } from 'antd';
import './SidebarMenu.css';

/**
 * 侧边栏菜单组件 - 基于 Ant Design Menu
 * @param {Object} props
 * @param {MenuItem[]} props.items - 菜单项列表
 * @param {Function} [props.onItemClick] - 菜单项点击回调
 */
export default function SidebarMenu({ items = [], onItemClick, collapsed = false }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [openKeys, setOpenKeys] = useState([]);
  const [selectedKeys, setSelectedKeys] = useState([]);

  // 根据当前路径找到激活的菜单项和需要展开的父菜单
  useEffect(() => {
    const findActiveKeys = (menuItems, path = []) => {
      for (const item of menuItems) {
        // 跳过分隔符和分组标题
        if (item.type === 'divider' || item.type === 'group') {
          if (item.children) {
            if (findActiveKeys(item.children, path)) {
              return true;
            }
          }
          continue;
        }

        const currentPath = [...path, item.key];
        
        // 检查当前项是否匹配
        if (item.path && (location.pathname === item.path || location.pathname.startsWith(item.path + '/'))) {
          setSelectedKeys([item.key]);
          // 如果有父级，展开所有父级
          if (path.length > 0) {
            setOpenKeys(path);
          }
          return true;
        }
        
        // 递归检查子菜单
        if (item.children) {
          if (findActiveKeys(item.children, currentPath)) {
            return true;
          }
        }
      }
      return false;
    };
    
    findActiveKeys(items);
  }, [location.pathname, items]);

  // 将菜单项转换为 Ant Design Menu 的 items 格式
  const convertToMenuItems = (menuItems) => {
    return menuItems.map(item => {
      // 支持分隔符
      if (item.type === 'divider') {
        return {
          type: 'divider',
        };
      }

      const menuItem = {
        key: item.key,
        label: item.label,
        icon: item.icon ? <item.icon /> : undefined,
      };

      // 支持分组类型
      if (item.type === 'group') {
        menuItem.type = 'group';
        if (item.children && item.children.length > 0) {
          menuItem.children = convertToMenuItems(item.children);
        }
        return menuItem;
      }

      // 如果有子菜单
      if (item.children && item.children.length > 0) {
        menuItem.children = convertToMenuItems(item.children);
      }

      return menuItem;
    });
  };

  const menuItems = useMemo(() => convertToMenuItems(items), [items]);

  // 处理菜单点击
  const handleMenuClick = ({ key }) => {
    const findItemByKey = (menuItems, targetKey) => {
      for (const item of menuItems) {
        // 跳过分隔符
        if (item.type === 'divider') {
          continue;
        }
        
        if (item.key === targetKey) {
          // 分组类型不处理点击
          if (item.type === 'group') {
            return null;
          }
          return item;
        }
        if (item.children) {
          const found = findItemByKey(item.children, targetKey);
          if (found) return found;
        }
      }
      return null;
    };

    const clickedItem = findItemByKey(items, key);
    if (clickedItem) {
      // 如果有路径且不是外部链接，进行导航
      if (clickedItem.path && clickedItem.path !== '#') {
        if (clickedItem.external) {
          window.open(clickedItem.path, '_blank', 'noopener,noreferrer');
        } else {
          navigate(clickedItem.path);
        }
      }
      
      // 调用回调
      onItemClick?.(clickedItem);
    }
  };

  // 处理子菜单展开/收起
  const handleOpenChange = (keys) => {
    setOpenKeys(keys);
  };

  return (
    <Menu
      mode="inline"
      selectedKeys={selectedKeys}
      openKeys={collapsed ? [] : openKeys}
      onOpenChange={handleOpenChange}
      onClick={handleMenuClick}
      items={menuItems}
      className={`sidebar-menu ${collapsed ? 'sidebar-menu-collapsed' : ''}`}
      inlineCollapsed={collapsed}
    />
  );
}
