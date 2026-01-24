import { Layout, Menu, Avatar, Dropdown, Space, Typography, Badge, Drawer, Button, Empty, Spin } from 'antd';
import {
  WalletOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  HistoryOutlined,
  DashboardOutlined,
  BellOutlined,
  MenuOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useState, useEffect, useCallback } from 'react';
import { notificationService } from '../services/walletService';

const { Header, Content, Sider } = Layout;
const { Text } = Typography;

interface Notification {
  id: string;
  type: 'TRANSACTION' | 'SECURITY' | 'SYSTEM' | 'PROMOTION';
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export default function MainLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 992);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationDropdownOpen, setNotificationDropdownOpen] = useState(false);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 992);
      if (window.innerWidth >= 992) {
        setMobileMenuOpen(false);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Load notifications
  const loadNotifications = useCallback(async () => {
    setNotificationsLoading(true);
    try {
      const response = await notificationService.getNotifications(0, 10);
      const notifs = response.content || [];
      setNotifications(notifs);
      setUnreadCount(notifs.filter((n: Notification) => !n.read).length);
    } catch (error) {
      // Backend not available - show empty state
      console.log('Notifications service not available');
      setNotifications([]);
      setUnreadCount(0);
    } finally {
      setNotificationsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
    // Refresh notifications every 30 seconds
    const interval = setInterval(loadNotifications, 30000);
    return () => clearInterval(interval);
  }, [loadNotifications]);

  const handleMarkAsRead = async (notificationId: string) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(prev =>
        prev.map(n => n.id === notificationId ? { ...n, read: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.log('Failed to mark notification as read');
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.log('Failed to mark all as read');
    }
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'TRANSACTION':
        return <CheckCircleOutlined style={{ color: '#10b981', fontSize: 16 }} />;
      case 'SECURITY':
        return <CloseCircleOutlined style={{ color: '#ef4444', fontSize: 16 }} />;
      default:
        return <InfoCircleOutlined style={{ color: '#3b82f6', fontSize: 16 }} />;
    }
  };

  const formatTimeAgo = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${diffDays}d ago`;
  };

  const notificationContent = (
    <div style={{
      width: 340,
      maxHeight: 450,
      background: '#fff',
      borderRadius: 12,
      boxShadow: '0 10px 40px rgba(0,0,0,0.15)',
      overflow: 'hidden'
    }}>
      <div style={{
        padding: '16px 20px',
        borderBottom: '1px solid #f0f0f0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: '#fafafa'
      }}>
        <Text strong style={{ fontSize: 16 }}>Notifications</Text>
        {unreadCount > 0 && (
          <Button type="link" size="small" onClick={handleMarkAllAsRead} style={{ padding: 0 }}>
            Mark all read
          </Button>
        )}
      </div>
      {notificationsLoading ? (
        <div style={{ padding: 40, textAlign: 'center' }}>
          <Spin size="small" />
        </div>
      ) : notifications.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={<Text type="secondary">No notifications yet</Text>}
          style={{ padding: '40px 20px' }}
        />
      ) : (
        <div style={{ maxHeight: 350, overflowY: 'auto' }}>
          {notifications.map((item) => (
            <div
              key={item.id}
              onClick={() => !item.read && handleMarkAsRead(item.id)}
              style={{
                padding: '14px 20px',
                borderBottom: '1px solid #f5f5f5',
                background: item.read ? '#fff' : '#f0f7ff',
                cursor: item.read ? 'default' : 'pointer',
                transition: 'background 0.2s',
              }}
            >
              <div style={{ display: 'flex', gap: 12 }}>
                <div style={{
                  width: 36,
                  height: 36,
                  borderRadius: '50%',
                  background: item.read ? '#f5f5f5' : '#e0edff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0
                }}>
                  {getNotificationIcon(item.type)}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                    <Text strong style={{ fontSize: 13, color: item.read ? '#666' : '#1a1a1a' }}>
                      {item.title}
                    </Text>
                    {!item.read && (
                      <div style={{
                        width: 8,
                        height: 8,
                        borderRadius: '50%',
                        background: '#4f46e5',
                        flexShrink: 0,
                        marginTop: 4
                      }} />
                    )}
                  </div>
                  <Text style={{ fontSize: 12, color: '#888', display: 'block', marginTop: 2 }}>
                    {item.message}
                  </Text>
                  <Text style={{ fontSize: 11, color: '#aaa', marginTop: 4, display: 'block' }}>
                    {formatTimeAgo(item.createdAt)}
                  </Text>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      <div style={{
        padding: '12px 20px',
        borderTop: '1px solid #f0f0f0',
        textAlign: 'center',
        background: '#fafafa'
      }}>
        <Button type="link" size="small" onClick={() => { setNotificationDropdownOpen(false); navigate('/settings'); }}>
          Notification Settings
        </Button>
      </div>
    </div>
  );

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

  const handleMenuClick = (key: string) => {
    navigate(key);
    setMobileMenuOpen(false);
  };

  const sidebarContent = (
    <>
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
            <div style={{ overflow: 'hidden' }}>
              <Text strong style={{ display: 'block', fontSize: 14, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.firstName} {user?.lastName}
              </Text>
              <Text type="secondary" style={{ fontSize: 12, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'block', maxWidth: '150px' }}>
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
        onClick={({ key }) => handleMenuClick(key)}
        style={{
          borderRight: 0,
          marginTop: 8,
          padding: '0 8px',
        }}
      />
    </>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* Desktop Sidebar */}
      {!isMobile && (
        <Sider
          width={240}
          style={{
            background: '#fff',
            borderRight: '1px solid #e5e7eb',
            position: 'fixed',
            left: 0,
            top: 0,
            bottom: 0,
            zIndex: 100,
          }}
        >
          {sidebarContent}
        </Sider>
      )}

      {/* Mobile Drawer - Opens as overlay, doesn't break content */}
      <Drawer
        title={null}
        placement="left"
        closable={true}
        onClose={() => setMobileMenuOpen(false)}
        open={mobileMenuOpen}
        width={280}
        styles={{
          body: { padding: 0 },
          wrapper: { zIndex: 1001 }
        }}
        style={{ zIndex: 1001 }}
      >
        {sidebarContent}
      </Drawer>

      <Layout style={{ marginLeft: isMobile ? 0 : 240 }}>
        <Header
          style={{
            padding: '0 16px',
            background: '#fff',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid #e5e7eb',
            height: 64,
            position: 'sticky',
            top: 0,
            zIndex: 99,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {isMobile && (
              <Button
                type="text"
                icon={<MenuOutlined style={{ fontSize: 20 }} />}
                onClick={() => setMobileMenuOpen(true)}
                style={{ padding: '4px 8px', height: 'auto' }}
              />
            )}
            {isMobile && (
              <Space>
                <WalletOutlined style={{ fontSize: 20, color: '#4f46e5' }} />
                <Text strong style={{ fontSize: 16, color: '#4f46e5' }}>DWS</Text>
              </Space>
            )}
          </div>
          <Space size={isMobile ? 8 : 16}>
            <Dropdown
              menu={{ items: [] }}
              dropdownRender={() => notificationContent}
              placement="bottomRight"
              trigger={['click']}
              open={notificationDropdownOpen}
              onOpenChange={setNotificationDropdownOpen}
            >
              <Badge count={unreadCount} size="small" offset={[-2, 2]}>
                <Button
                  type="text"
                  icon={<BellOutlined style={{ fontSize: 20, color: '#6b7280' }} />}
                  style={{ padding: '4px 8px', height: 'auto' }}
                />
              </Badge>
            </Dropdown>
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
            minHeight: 'calc(100vh - 64px)',
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
