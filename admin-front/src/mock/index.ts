import Mock from 'mockjs';
import { isSSR } from '@/utils/is';

// 注释掉user mock，使用真实后端API
// import './user';
import './message-box';

if (!isSSR) {
  Mock.setup({
    timeout: '500-1500',
  });
}
