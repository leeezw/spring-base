import { useState, useEffect, useCallback } from 'react';
import { Spin, Tag, Table, Empty } from 'antd';
import {
  UserOutlined, TeamOutlined, ApartmentOutlined, IdcardOutlined,
  SafetyOutlined, CloudOutlined, RiseOutlined, SettingOutlined,
  MenuOutlined, BookOutlined, SolutionOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthContext } from '../hooks/AuthProvider.jsx';
import { Column, Pie } from '@ant-design/charts';
import request from '../api/index.js';
import './Dashboard.css';

export default function Dashboard() {
  const navigate = useNavigate();
  const { user } = useAuthContext();
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState({});
  const [userTrend, setUserTrend] = useState([]);
  const [deptDist, setDeptDist] = useState([]);
  const [roleDist, setRoleDist] = useState([]);
  const [recentLogins, setRecentLogins] = useState([]);
  const [currentTime, setCurrentTime] = useState(new Date());

  // 时钟
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [ov, trend, dept, role, logins] = await Promise.all([
        request.get('/system/dashboard/overview'),
        request.get('/system/dashboard/user-trend?days=7'),
        request.get('/system/dashboard/dept-distribution'),
        request.get('/system/dashboard/role-distribution'),
        request.get('/system/dashboard/recent-logins?limit=5'),
      ]);
      if (ov.code === 200) setOverview(ov.data || {});
      if (trend.code === 200) setUserTrend(trend.data || []);
      if (dept.code === 200) setDeptDist(dept.data || []);
      if (role.code === 200) setRoleDist(role.data || []);
      if (logins.code === 200) setRecentLogins(logins.data || []);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const greeting = (() => {
    const h = currentTime.getHours();
    if (h < 6) return '夜深了';
    if (h < 9) return '早上好';
    if (h < 12) return '上午好';
    if (h < 14) return '中午好';
    if (h < 18) return '下午好';
    return '晚上好';
  })();

  const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
  const dateStr = `${currentTime.getFullYear()}年${currentTime.getMonth() + 1}月${currentTime.getDate()}日 星期${weekDays[currentTime.getDay()]}`;
  const timeStr = currentTime.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });

  // 柱状图配置
  const trendConfig = {
    data: userTrend,
    xField: 'date',
    yField: 'count',
    color: '#667eea',
    columnStyle: { radius: [6, 6, 0, 0] },
    label: { position: 'top', style: { fill: '#8c8c8c', fontSize: 11 } },
    xAxis: { label: { style: { fontSize: 11 } } },
    yAxis: { label: { style: { fontSize: 11 } }, grid: { line: { style: { stroke: '#f0f0f0' } } } },
    meta: { count: { alias: '新增用户' } },
    tooltip: { formatter: (d) => ({ name: '新增用户', value: d.count }) },
    animation: { appear: { duration: 600 } },
  };

  // 饼图配置
  const deptPieConfig = {
    data: deptDist,
    angleField: 'count',
    colorField: 'deptName',
    radius: 0.8,
    innerRadius: 0.5,
    label: { type: 'outer', content: '{name} {value}人', style: { fontSize: 11 } },
    legend: { position: 'bottom', itemHeight: 18 },
    interactions: [{ type: 'element-active' }],
    statistic: {
      title: { content: '总人数', style: { fontSize: '12px', color: '#8c8c8c' } },
      content: {
        style: { fontSize: '20px', fontWeight: 700 },
        formatter: () => deptDist.reduce((s, d) => s + d.count, 0),
      },
    },
    animation: { appear: { duration: 600 } },
  };

  // 快捷入口
  const quickActions = [
    { icon: <UserOutlined />, label: '用户管理', path: '/system/user', color: 'blue' },
    { icon: <SafetyOutlined />, label: '角色管理', path: '/system/role', color: 'green' },
    { icon: <ApartmentOutlined />, label: '部门管理', path: '/system/dept', color: 'orange' },
    { icon: <IdcardOutlined />, label: '岗位管理', path: '/system/post', color: 'purple' },
    { icon: <MenuOutlined />, label: '菜单管理', path: '/system/menu', color: 'cyan' },
    { icon: <BookOutlined />, label: '字典管理', path: '/system/dict', color: 'pink' },
  ];

  // 最近登录表列
  const loginColumns = [
    { title: '用户', dataIndex: 'username', width: 120 },
    { title: '昵称', dataIndex: 'nickname', width: 120 },
    {
      title: '登录时间', dataIndex: 'loginTime', width: 180,
      render: (v) => v ? new Date(v).toLocaleString('zh-CN') : '-',
    },
  ];

  return (
    <div className="dashboard-page">
      <Spin spinning={loading}>
        {/* 欢迎栏 */}
        <div className="dashboard-welcome">
          <div className="welcome-text">
            <h2>{greeting}，{user?.nickname || user?.username || '管理员'} 👋</h2>
            <p>欢迎回到管理控制台，以下是系统概览。</p>
          </div>
          <div className="welcome-time">
            <span className="time-big">{timeStr}</span>
            {dateStr}
          </div>
        </div>

        {/* 统计卡片 */}
        <div className="stat-cards">
          <div className="stat-card" onClick={() => navigate('/system/user')} style={{ cursor: 'pointer' }}>
            <div className="stat-card-icon blue"><UserOutlined /></div>
            <div className="stat-card-info">
              <div className="stat-number">{overview.userCount ?? '-'}</div>
              <div className="stat-label">用户总数</div>
              <div className="stat-sub">活跃 {overview.activeUserCount ?? 0} · 今日新增 {overview.todayNewUsers ?? 0}</div>
            </div>
          </div>
          <div className="stat-card" onClick={() => navigate('/system/role')} style={{ cursor: 'pointer' }}>
            <div className="stat-card-icon green"><SafetyOutlined /></div>
            <div className="stat-card-info">
              <div className="stat-number">{overview.roleCount ?? '-'}</div>
              <div className="stat-label">角色数量</div>
            </div>
          </div>
          <div className="stat-card" onClick={() => navigate('/system/dept')} style={{ cursor: 'pointer' }}>
            <div className="stat-card-icon orange"><ApartmentOutlined /></div>
            <div className="stat-card-info">
              <div className="stat-number">{overview.deptCount ?? '-'}</div>
              <div className="stat-label">部门数量</div>
              <div className="stat-sub">岗位 {overview.postCount ?? 0} 个</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon purple"><CloudOutlined /></div>
            <div className="stat-card-info">
              <div className="stat-number">{overview.onlineCount ?? 0}</div>
              <div className="stat-label">在线用户</div>
              <div className="stat-sub">基于会话统计</div>
            </div>
          </div>
        </div>

        {/* 图表行 */}
        <div className="chart-row">
          <div className="chart-card">
            <div className="chart-card-header">
              <h3><RiseOutlined style={{ color: '#667eea', marginRight: 8 }} />用户增长趋势</h3>
              <span className="chart-extra">近 7 天</span>
            </div>
            <div className="chart-card-body">
              {userTrend.length > 0 ? (
                <Column {...trendConfig} height={230} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: 60 }} />
              )}
            </div>
          </div>
          <div className="chart-card">
            <div className="chart-card-header">
              <h3><TeamOutlined style={{ color: '#f59e0b', marginRight: 8 }} />部门人数分布</h3>
              <span className="chart-extra">{deptDist.length} 个部门</span>
            </div>
            <div className="chart-card-body">
              {deptDist.length > 0 ? (
                <Pie {...deptPieConfig} height={230} />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: 60 }} />
              )}
            </div>
          </div>
        </div>

        {/* 快捷入口 */}
        <div className="quick-actions">
          {quickActions.map((item, i) => (
            <div key={i} className="quick-action-item" onClick={() => navigate(item.path)}>
              <div className={`action-icon ${item.color}`}>{item.icon}</div>
              <span className="action-label">{item.label}</span>
            </div>
          ))}
        </div>

        {/* 角色分布 + 最近登录 */}
        <div className="chart-row">
          <div className="chart-card">
            <div className="chart-card-header">
              <h3><SolutionOutlined style={{ color: '#10b981', marginRight: 8 }} />角色用户分布</h3>
            </div>
            <div className="chart-card-body">
              {roleDist.length > 0 ? (
                <Column
                  data={roleDist}
                  xField="roleName"
                  yField="count"
                  color="#10b981"
                  columnStyle={{ radius: [6, 6, 0, 0] }}
                  label={{ position: 'top', style: { fill: '#8c8c8c', fontSize: 11 } }}
                  meta={{ count: { alias: '用户数' } }}
                  height={230}
                />
              ) : (
                <Empty description="暂无数据" style={{ marginTop: 60 }} />
              )}
            </div>
          </div>
          <div className="login-table-card">
            <div className="login-table-header">
              <SettingOutlined style={{ color: '#8b5cf6' }} />
              <h3>在线会话</h3>
            </div>
            <div className="login-table-body">
              {recentLogins.length > 0 ? (
                <Table
                  dataSource={recentLogins}
                  columns={loginColumns}
                  rowKey="userId"
                  pagination={false}
                  size="small"
                />
              ) : (
                <Empty description="暂无在线会话" style={{ margin: '40px 0' }} />
              )}
            </div>
          </div>
        </div>
      </Spin>
    </div>
  );
}
