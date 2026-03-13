import { useState, useEffect } from 'react';
import { Table, Space, Button, Select, Pagination, Empty } from 'antd';
import './ProTable.css';

/**
 * ProTable - 多功能表格组件
 * 类似 Ant Design Pro Table，封装常用功能
 * 
 * @param {Object} props
 * @param {Array} props.columns - 表格列配置
 * @param {Function} props.request - 请求函数，返回 Promise<{list: [], total: number}>
 * @param {Object} props.params - 请求参数
 * @param {string} props.rowKey - 行key字段，默认 'id'
 * @param {ReactNode} props.title - 表格标题
 * @param {ReactNode} props.extra - 工具栏右侧操作区
 * @param {boolean} props.showPagination - 是否显示分页，默认 true
 * @param {Array} props.pageSizeOptions - 每页条数选项，默认 [10, 20, 50]
 * @param {Function} props.onDataChange - 数据变化回调 (data, total) => void
 * @param {Object} props.tableProps - 传递给 Table 组件的其他属性
 */
export default function ProTable({
  columns = [],
  request,
  params = {},
  rowKey = 'id',
  title,
  extra,
  showPagination = true,
  pageSizeOptions = [10, 20, 50],
  onDataChange,
  tableProps = {},
}) {
  const [data, setData] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: params.pageNum || 1,
    pageSize: params.pageSize || 10,
  });

  // 请求数据
  const fetchData = async () => {
    if (!request) return;

    setLoading(true);
    try {
      const requestParams = {
        ...params,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      };

      const res = await request(requestParams);
      if (res.code === 200) {
        const list = res.data?.list || res.data || [];
        const total = res.data?.total || res.data?.length || 0;
        setData(list);
        setTotal(total);
        // 触发数据变化回调
        if (onDataChange) {
          onDataChange(list, total);
        }
      }
    } catch (error) {
      console.error('ProTable fetchData error:', error);
      setData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [pagination.current, pagination.pageSize, JSON.stringify(params)]);

  const handleTableChange = (newPagination) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
    });
  };

  const handlePageSizeChange = (pageSize) => {
    setPagination({
      current: 1,
      pageSize,
    });
  };

  return (
    <div className="pro-table">
      {/* 工具栏 */}
      {(title || extra) && (
        <div className="pro-table-header">
          {title && <div className="pro-table-title">{title}</div>}
          {extra && <div className="pro-table-extra">{extra}</div>}
        </div>
      )}

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey={rowKey}
        pagination={false}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无数据"
              className="pro-table-empty"
            />
          ),
        }}
        {...tableProps}
      />

      {/* 分页 */}
      {showPagination && data.length > 0 && (
        <div className="pro-table-pagination">
          <div className="pagination-info">
            共 {total} 条，每页显示
            <Select
              value={pagination.pageSize}
              onChange={handlePageSizeChange}
              style={{ width: 80, margin: '0 8px' }}
            >
              {pageSizeOptions.map(size => (
                <Select.Option key={size} value={size}>{size}</Select.Option>
              ))}
            </Select>
            条
          </div>
          <Pagination
            current={pagination.current}
            total={total}
            pageSize={pagination.pageSize}
            onChange={(page, pageSize) => handleTableChange({ current: page, pageSize })}
            showSizeChanger={false}
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
          />
        </div>
      )}
    </div>
  );
}

