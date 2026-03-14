import { Form, Input, Select, Space, Button, DatePicker } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import './TableSearchForm.css';

const { Search } = Input;
const { RangePicker } = DatePicker;

/**
 * 表格搜索表单组件
 * 统一的搜索表单控件，包含关键词搜索、状态筛选、日期范围等
 * 
 * @param {Object} props
 * @param {Object} props.form - Form 实例
 * @param {Function} props.onFinish - 提交回调
 * @param {Object} props.initialValues - 初始值
 * @param {Object} props.config - 配置项
 * @param {boolean} props.config.showKeyword - 是否显示关键词搜索，默认 true
 * @param {string} props.config.keywordPlaceholder - 关键词占位符
 * @param {boolean} props.config.showStatus - 是否显示状态筛选，默认 true
 * @param {Array} props.config.statusOptions - 状态选项，默认 [{label: '全部', value: 'all'}, {label: '启用', value: 1}, {label: '禁用', value: 0}]
 * @param {boolean} props.config.showDateRange - 是否显示日期范围，默认 false
 * @param {string} props.config.dateRangePlaceholder - 日期范围占位符
 * @param {string} props.config.dateRangeStartName - 开始时间字段名，默认 'startTime'
 * @param {string} props.config.dateRangeEndName - 结束时间字段名，默认 'endTime'
 * @param {Function} props.onKeywordChange - 关键词变化回调（用于防抖）
 * @param {boolean} props.compact - 紧凑模式，默认 false
 */
export default function TableSearchForm({
  form,
  onFinish,
  initialValues = {},
  config = {},
  onKeywordChange,
  compact = false,
  ...props
}) {
  const {
    showKeyword = true,
    keywordPlaceholder = '请输入关键词',
    showStatus = true,
    statusOptions = [
      { label: '全部', value: 'all' },
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 },
    ],
    showDateRange = false,
    dateRangePlaceholder = ['开始时间', '结束时间'],
    dateRangeStartName = 'startTime',
    dateRangeEndName = 'endTime',
  } = config;

  const handleKeywordChange = (e) => {
    if (onKeywordChange) {
      onKeywordChange(e);
    }
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue(initialValues);
    // 延迟提交，确保值已重置
    setTimeout(() => {
      form.submit();
    }, 100);
  };

  return (
    <div className={`table-search-form ${compact ? 'table-search-form-compact' : ''}`}>
      <Form
        form={form}
        layout="inline"
        initialValues={initialValues}
        onFinish={onFinish}
        className="search-form"
        {...props}
      >
        <div className="search-form-content">
          {showKeyword && (
            <Form.Item name="keyword" className="search-form-item search-form-keyword">
              <Input
                placeholder={keywordPlaceholder}
                allowClear
                prefix={<SearchOutlined />}
                onChange={handleKeywordChange}
                onPressEnter={() => form.submit()}
                className="search-input"
              />
            </Form.Item>
          )}

          {showStatus && (
            <Form.Item name="status" className="search-form-item search-form-status">
              <Select
                placeholder="状态"
                allowClear
                onChange={() => form.submit()}
                className="search-select"
              >
                {statusOptions.map(option => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          )}

          {showDateRange && (
            <Form.Item name="dateRange" className="search-form-item search-form-daterange">
              <RangePicker
                placeholder={dateRangePlaceholder}
                onChange={(dates) => {
                  if (dates && dates.length === 2) {
                    // 设置开始时间和结束时间到表单字段
                    form.setFieldsValue({
                      [dateRangeStartName]: dates[0] ? dates[0].startOf('day').format('YYYY-MM-DD HH:mm:ss') : null,
                      [dateRangeEndName]: dates[1] ? dates[1].endOf('day').format('YYYY-MM-DD HH:mm:ss') : null,
                    });
                  } else {
                    // 清空日期范围时，也清空对应的字段
                    form.setFieldsValue({
                      [dateRangeStartName]: null,
                      [dateRangeEndName]: null,
                    });
                  }
                  setTimeout(() => form.submit(), 100);
                }}
              />
            </Form.Item>
          )}
        </div>

        <div className="search-form-actions">
          <Button
            type="default"
            icon={<ReloadOutlined />}
            onClick={handleReset}
            className="search-reset-btn"
          >
            重置
          </Button>
        </div>
      </Form>
    </div>
  );
}

