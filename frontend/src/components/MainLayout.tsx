import { Layout, Menu, Avatar, Dropdown, Space, Typography, Badge } from 'antd';
import {
  WalletOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  HistoryOutlined,
  DashboardOutlined,
  BellOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const { Header, Content, Sider } = Layout;
const { Text } = Typography;

export default function MainLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Dashboard',
    },
    {
      key: '/transactions',
      icon: <HistoryOutlined />,
      label: 'Transactions',
    },
    {
      key: '/profile',
      icon: <UserOutlined />,
      label: 'Profile',
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: 'Settings',
    },
  ];

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'My Profile',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'Settings',
      onClick: () => navigate('/settings'),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      danger: true,
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  const getInitials = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    return user?.email?.[0]?.toUpperCase() || 'U';
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={240}
        breakpoint="lg"
        collapsedWidth="0"
        style={{
          background: '#fff',
          borderRight: '1px solid #e5e7eb',
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderBottom: '1px solid #e5e7eb',
            background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
          }}
        >
          <Space>
            <WalletOutlined style={{ fontSize: 24, color: 'white' }} />
            <Text strong style={{ fontSize: 18, color: 'white', letterSpacing: '0.5px' }}>
              Digital Wallet
            </Text>
          </Space>
        </div>

        {/* User Info Section */}
        <div style={{ padding: '20px 16px', borderBottom: '1px solid #e5e7eb' }}>
          <Space direction="vertical" size={4} style={{ width: '100%' }}>
            <Space>
              <Avatar
                size={40}
                style={{
                  background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
                  fontWeight: 600
                }}
              >
                {getInitials()}
              </Avatar>
              <div>
                <Text strong style={{ display: 'block', fontSize: 14 }}>
                  {user?.firstName} {user?.lastName}
                </Text>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {user?.email}
                </Text>
              </div>
            </Space>
          </Space>
        </div>

        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{
            borderRight: 0,
            marginTop: 8,
            padding: '0 8px',
          }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: '#fff',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            borderBottom: '1px solid #e5e7eb',
            height: 64,
          }}
        >
          <Space size={16}>
            <Badge count={0} showZero={false}>
              <BellOutlined style={{ fontSize: 20, color: '#6b7280', cursor: 'pointer' }} />
            </Badge>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
              <Space style={{ cursor: 'pointer', padding: '4px 8px', borderRadius: 8 }}>
                <Avatar
                  size={36}
                  style={{
                    background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
                    fontWeight: 600
                  }}
                >
                  {getInitials()}
                </Avatar>
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content
          style={{
            margin: 0,
            minHeight: 280,
            background: '#f9fafb',
            overflow: 'auto',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
