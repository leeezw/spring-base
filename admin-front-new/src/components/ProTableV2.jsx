import { useRef } from 'react';
import { ProTable as AntProTable } from '@ant-design/pro-components';
import './ProTableV2.css';

/**
 * ProTable V2 - 基于 @ant-design/pro-components 的高级表格组件
 * 
 * @param {Object} props
 * @param {React.MutableRefObject} props.actionRef - 表格操作引用，用于外部控制表格刷新等操作
 * @param {Array} props.columns - 表格列配置（ProColumns 格式）
 * @param {Function} props.request - 请求函数，接收 (params, sort, filter) => Promise<{data: [], success: boolean, total: number}>
 * @param {Object} props.params - 额外的请求参数
 * @param {string} props.rowKey - 行key字段，默认 'id'
 * @param {string} props.headerTitle - 表格标题
 * @param {ReactNode[]|Function} props.toolBarRender - 工具栏渲染函数（已废弃，使用 toolbar）
 * @param {Object} props.toolbar - 工具栏配置，包含 filter、menu、actions 等
 * @param {boolean} props.search - 是否显示搜索表单，默认 false
 * @param {Object} props.searchConfig - 搜索表单配置
 * @param {Object} props.pagination - 分页配置
 * @param {boolean} props.cardBordered - 是否显示卡片边框，默认 true
 * @param {Function} props.onDataChange - 数据变化回调 (data, total) => void
 * @param {Object} props.defaultSort - 默认排序配置，如 { createTime: 'desc' }
 * @param {Object} props.rowSelection - 行选择配置
 * @param {Object} props.tableProps - 传递给 ProTable 的其他属性
 */
export default function ProTableV2({
  actionRef: externalActionRef,
  columns = [],
  request,
  params = {},
  rowKey = 'id',
  headerTitle,
  toolBarRender,
  toolbar,
  search = false,
  searchConfig = {},
  pagination,
  cardBordered = true,
  onDataChange,
  defaultSort,
  rowSelection,
  tableProps = {},
}) {
  const internalActionRef = useRef();
  // 使用外部传入的 actionRef，如果没有则使用内部的
  // 注意：如果外部传入了 actionRef，需要确保它被正确绑定到 ProTable
  const actionRef = externalActionRef ?? internalActionRef;

  // 转换请求函数以适配后端 API 格式
  const handleRequest = async (requestParams, sort, filter) => {
    if (!request) {
      return {
        data: [],
        success: false,
        total: 0,
      };
    }

    try {
      // 合并参数：先合并基础 params，再合并 requestParams（包含来自 LightFilter 的筛选参数）
      const finalParams = {
        ...params,
        ...requestParams,
      };

      // 只有在启用分页时才添加分页参数
      if (pagination !== false) {
        const defaultPagination = {
          defaultPageSize: 10,
          pageSizeOptions: ['10', '20', '50', '100'],
          showSizeChanger: true,
          showQuickJumper: true,
        };
        const paginationConfig = pagination || defaultPagination;
        finalParams.pageNum = requestParams.current || 1;
        finalParams.pageSize = requestParams.pageSize || (paginationConfig?.defaultPageSize || 10);
      }

      // 处理 status 参数（如果存在且是 'all'，则移除）
      if (finalParams.status === 'all') {
        delete finalParams.status;
      }

      // 移除 ProTable 内部使用的参数，避免传递给后端
      delete finalParams.current;

      // 处理排序
      if (sort) {
        const sortKeys = Object.keys(sort);
        if (sortKeys.length > 0) {
          const sortKey = sortKeys[0];
          finalParams.sortField = sortKey;
          finalParams.sortOrder = sort[sortKey] === 'ascend' ? 'asc' : 'desc';
        }
      } else if (defaultSort) {
        // 如果没有用户排序，使用默认排序
        const sortKeys = Object.keys(defaultSort);
        if (sortKeys.length > 0) {
          const sortKey = sortKeys[0];
          finalParams.sortField = sortKey;
          finalParams.sortOrder = defaultSort[sortKey] === 'ascend' || defaultSort[sortKey] === 'asc' ? 'asc' : 'desc';
        }
      }

      // 调用请求函数
      const res = await request(finalParams);

      // 处理响应格式
      if (res.code === 200) {
        const list = res.data?.list || res.data?.records || res.data?.pageData?.list || res.data || [];
        const total = res.data?.total || res.data?.pageData?.total || list.length || 0;

        // 触发数据变化回调
        if (onDataChange) {
          onDataChange(list, total);
        }

        return {
          data: list,
          success: true,
          total: total,
        };
      }

      return {
        data: [],
        success: false,
        total: 0,
      };
    } catch (error) {
      console.error('ProTable request error:', error);
      return {
        data: [],
        success: false,
        total: 0,
      };
    }
  };

  return (
    <AntProTable
      actionRef={actionRef}
      columns={columns}
      request={handleRequest}
      rowKey={rowKey}
      headerTitle={headerTitle}
      toolBarRender={toolBarRender}
      toolbar={toolbar}
      search={search}
      searchConfig={{
        labelWidth: 'auto',
        ...searchConfig,
      }}
      params={params}
      defaultSort={defaultSort}
      rowSelection={rowSelection}
      pagination={pagination === undefined ? {
        defaultPageSize: 10,
        pageSizeOptions: ['10', '20', '50', '100'],
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total, range) => {
          // 显示"共 X 条，每页显示 X 条"格式
          if (!range || range.length === 0) {
            return `共 ${total} 条`;
          }
          const pageSize = range[1] - range[0] + 1;
          return `共 ${total} 条，每页显示 ${pageSize} 条`;
        },
      } : pagination}
      cardBordered={cardBordered}
      dateFormatter="string"
      className="pro-table-v2"
      {...tableProps}
    />
  );
}

