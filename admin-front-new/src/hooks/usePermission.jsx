import { useCallback } from 'react';
import { useAuthContext } from './AuthProvider.jsx';

/**
 * 权限检查 hook
 * 用法：const { hasPermission } = usePermission();
 *       hasPermission('system:user:add') → true/false
 */
export function usePermission() {
  const { user } = useAuthContext();
  
  const hasPermission = useCallback((required) => {
    if (!required) return true;
    const permissions = user?.permissions || [];
    if (permissions.includes('*:*:*')) return true;
    const requiredList = Array.isArray(required) ? required : [required];
    return requiredList.some(code => permissions.includes(code));
  }, [user]);

  return { hasPermission };
}
