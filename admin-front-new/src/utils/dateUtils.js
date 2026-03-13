/**
 * 日期时间工具函数
 */

/**
 * 格式化日期时间为 YYYY/MM/DD HH:mm:ss 格式
 * @param {string|Date} dateTime - 日期时间字符串或Date对象
 * @returns {string} 格式化后的日期时间字符串，如果无效则返回 '-'
 */
export function formatDateTime(dateTime) {
  if (!dateTime) return '-';
  
  try {
    let date;
    
    // 如果是字符串，尝试解析
    if (typeof dateTime === 'string') {
      // 处理 ISO 8601 格式：2025-11-29T18:03:04 或 2025-11-29T18:03:04.000Z
      // 也处理后端返回的 yyyy/MM/dd HH:mm:ss 格式
      date = new Date(dateTime);
    } else if (dateTime instanceof Date) {
      date = dateTime;
    } else {
      return '-';
    }
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      return '-';
    }
    
    // 格式化为 YYYY/MM/DD HH:mm:ss
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`;
  } catch (e) {
    console.error('日期格式化失败:', e, dateTime);
    return '-';
  }
}

/**
 * 格式化日期为 YYYY/MM/DD 格式
 * @param {string|Date} date - 日期字符串或Date对象
 * @returns {string} 格式化后的日期字符串，如果无效则返回 '-'
 */
export function formatDate(date) {
  if (!date) return '-';
  
  try {
    let dateObj;
    
    if (typeof date === 'string') {
      dateObj = new Date(date);
    } else if (date instanceof Date) {
      dateObj = date;
    } else {
      return '-';
    }
    
    if (isNaN(dateObj.getTime())) {
      return '-';
    }
    
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    
    return `${year}/${month}/${day}`;
  } catch (e) {
    console.error('日期格式化失败:', e, date);
    return '-';
  }
}

