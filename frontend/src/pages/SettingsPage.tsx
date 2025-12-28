import { useState, useEffect } from 'react';
import {
  Card,
  Form,
  Switch,
  Button,
  Typography,
  message,
  Divider,
  Select,
  Row,
  Col,
  List,
  Tag,
  Modal,
  Input,
  Spin,
  Alert,
} from 'antd';
import {
  SettingOutlined,
  BellOutlined,
  LockOutlined,
  GlobalOutlined,
  SecurityScanOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';

const { Title, Text } = Typography;
const { Option } = Select;

interface NotificationPreferences {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  transactionAlerts: boolean;
  securityAlerts: boolean;
  marketingEmails: boolean;
}

interface SecuritySettings {
  twoFactorEnabled: boolean;
  lastPasswordChange: string;
  activeSessions: number;
}

export default function SettingsPage() {
  const { logout } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [passwordModal, setPasswordModal] = useState(false);
  const [passwordForm] = Form.useForm();

  const [notifications, setNotifications] = useState<NotificationPreferences>({
    emailEnabled: true,
    smsEnabled: false,
    pushEnabled: true,
    transactionAlerts: true,
    securityAlerts: true,
    marketingEmails: false,
  });

  const [security, setSecurity] = useState<SecuritySettings>({
    twoFactorEnabled: false,
    lastPasswordChange: new Date().toISOString(),
    activeSessions: 1,
  });

  const [preferences, setPreferences] = useState({
    language: 'en',
    currency: 'USD',
    timezone: 'UTC',
    theme: 'light',
  });

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    setLoading(true);
    try {
      // In a real app, fetch from API
      // const response = await api.get('/settings');
      // For now, use defaults
      setLoading(false);
    } catch (error) {
      message.error('Failed to load settings');
      setLoading(false);
    }
  };

  const handleNotificationChange = async (key: keyof NotificationPreferences, value: boolean) => {
    const updated = { ...notifications, [key]: value };
    setNotifications(updated);

    try {
      // await api.put('/settings/notifications', updated);
      message.success('Notification preference updated');
    } catch (error) {
      message.error('Failed to update preference');
      setNotifications(notifications); // Revert
    }
  };

  const handlePreferenceChange = async (key: string, value: string) => {
    const updated = { ...preferences, [key]: value };
    setPreferences(updated);
    message.success('Preference updated');
  };

  const handlePasswordChange = async (values: { currentPassword: string; newPassword: string }) => {
    setSaving(true);
    try {
      await api.post('/auth/change-password', values);
      message.success('Password changed successfully');
      setPasswordModal(false);
      passwordForm.resetFields();
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to change password');
    } finally {
      setSaving(false);
    }
  };

  const handleLogoutAllSessions = () => {
    Modal.confirm({
      title: 'Logout from all devices?',
      icon: <ExclamationCircleOutlined />,
      content: 'This will log you out from all devices including this one.',
      okText: 'Yes, logout all',
      okType: 'danger',
      onOk: async () => {
        try {
          // await api.post('/auth/logout-all');
          message.success('Logged out from all devices');
          logout();
        } catch (error) {
          message.error('Failed to logout from all devices');
        }
      },
    });
  };

  const handleDeleteAccount = () => {
    Modal.confirm({
      title: 'Delete your account?',
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>This action cannot be undone. All your data will be permanently deleted.</p>
          <Alert
            type="warning"
            message="Make sure to withdraw all funds before deleting your account."
            style={{ marginTop: 16 }}
          />
        </div>
      ),
      okText: 'Delete Account',
      okType: 'danger',
      onOk: async () => {
        try {
          // await api.delete('/customers/me');
          message.success('Account deletion requested');
          logout();
        } catch (error) {
          message.error('Failed to delete account');
        }
      },
    });
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: 24, maxWidth: 800, margin: '0 auto' }}>
      <Title level={2}>
        <SettingOutlined /> Settings
      </Title>

      <Row gutter={[24, 24]}>
        {/* Notification Settings */}
        <Col span={24}>
          <Card title={<><BellOutlined /> Notifications</>}>
            <List>
              <List.Item
                actions={[
                  <Switch
                    checked={notifications.emailEnabled}
                    onChange={(v) => handleNotificationChange('emailEnabled', v)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="Email Notifications"
                  description="Receive notifications via email"
                />
              </List.Item>
              <List.Item
                actions={[
                  <Switch
                    checked={notifications.smsEnabled}
                    onChange={(v) => handleNotificationChange('smsEnabled', v)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="SMS Notifications"
                  description="Receive notifications via SMS"
                />
              </List.Item>
              <List.Item
                actions={[
                  <Switch
                    checked={notifications.pushEnabled}
                    onChange={(v) => handleNotificationChange('pushEnabled', v)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="Push Notifications"
                  description="Receive push notifications in browser"
                />
              </List.Item>
              <Divider />
              <List.Item
                actions={[
                  <Switch
                    checked={notifications.transactionAlerts}
                    onChange={(v) => handleNotificationChange('transactionAlerts', v)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="Transaction Alerts"
                  description="Get notified for deposits, withdrawals, and transfers"
                />
              </List.Item>
              <List.Item
                actions={[
                  <Switch
                    checked={notifications.securityAlerts}
                    onChange={(v) => handleNotificationChange('securityAlerts', v)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="Security Alerts"
                  description="Get notified for login attempts and security events"
                />
              </List.Item>
              <List.Item
                actions={[
                  <Switch
                    checked={notifications.marketingEmails}
                    onChange={(v) => handleNotificationChange('marketingEmails', v)}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="Marketing Emails"
                  description="Receive promotional offers and updates"
                />
              </List.Item>
            </List>
          </Card>
        </Col>

        {/* Regional Settings */}
        <Col span={24}>
          <Card title={<><GlobalOutlined /> Regional Settings</>}>
            <Form layout="vertical">
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item label="Language">
                    <Select
                      value={preferences.language}
                      onChange={(v) => handlePreferenceChange('language', v)}
                    >
                      <Option value="en">English</Option>
                      <Option value="ru">Русский</Option>
                      <Option value="uz">O'zbekcha</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item label="Default Currency">
                    <Select
                      value={preferences.currency}
                      onChange={(v) => handlePreferenceChange('currency', v)}
                    >
                      <Option value="USD">USD - US Dollar</Option>
                      <Option value="EUR">EUR - Euro</Option>
                      <Option value="GBP">GBP - British Pound</Option>
                      <Option value="UZS">UZS - Uzbek Sum</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item label="Timezone">
                    <Select
                      value={preferences.timezone}
                      onChange={(v) => handlePreferenceChange('timezone', v)}
                    >
                      <Option value="UTC">UTC</Option>
                      <Option value="America/New_York">Eastern Time (US)</Option>
                      <Option value="Europe/London">London</Option>
                      <Option value="Asia/Tashkent">Tashkent</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item label="Theme">
                    <Select
                      value={preferences.theme}
                      onChange={(v) => handlePreferenceChange('theme', v)}
                    >
                      <Option value="light">Light</Option>
                      <Option value="dark">Dark (Coming Soon)</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </Card>
        </Col>

        {/* Security Settings */}
        <Col span={24}>
          <Card title={<><SecurityScanOutlined /> Security</>}>
            <List>
              <List.Item
                actions={[
                  <Button type="primary" onClick={() => setPasswordModal(true)}>
                    Change Password
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  avatar={<LockOutlined style={{ fontSize: 24 }} />}
                  title="Password"
                  description={`Last changed: ${new Date(security.lastPasswordChange).toLocaleDateString()}`}
                />
              </List.Item>
              <List.Item
                actions={[
                  <Switch
                    checked={security.twoFactorEnabled}
                    onChange={() => message.info('2FA coming soon!')}
                  />,
                ]}
              >
                <List.Item.Meta
                  title="Two-Factor Authentication"
                  description="Add an extra layer of security to your account"
                />
                <Tag color={security.twoFactorEnabled ? 'green' : 'orange'}>
                  {security.twoFactorEnabled ? 'Enabled' : 'Disabled'}
                </Tag>
              </List.Item>
              <List.Item
                actions={[
                  <Button danger onClick={handleLogoutAllSessions}>
                    Logout All
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title="Active Sessions"
                  description={`You are logged in on ${security.activeSessions} device(s)`}
                />
              </List.Item>
            </List>
          </Card>
        </Col>

        {/* Danger Zone */}
        <Col span={24}>
          <Card
            title={<Text type="danger">Danger Zone</Text>}
            style={{ borderColor: '#ff4d4f' }}
          >
            <List>
              <List.Item
                actions={[
                  <Button danger onClick={handleDeleteAccount}>
                    Delete Account
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title="Delete Account"
                  description="Permanently delete your account and all associated data"
                />
              </List.Item>
            </List>
          </Card>
        </Col>
      </Row>

      {/* Change Password Modal */}
      <Modal
        title="Change Password"
        open={passwordModal}
        onCancel={() => setPasswordModal(false)}
        footer={null}
      >
        <Form form={passwordForm} onFinish={handlePasswordChange} layout="vertical">
          <Form.Item
            name="currentPassword"
            label="Current Password"
            rules={[{ required: true, message: 'Please enter current password' }]}
          >
            <Input.Password placeholder="Enter current password" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="New Password"
            rules={[
              { required: true, message: 'Please enter new password' },
              { min: 8, message: 'Password must be at least 8 characters' },
            ]}
          >
            <Input.Password placeholder="Enter new password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="Confirm New Password"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Please confirm new password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Passwords do not match'));
                },
              }),
            ]}
          >
            <Input.Password placeholder="Confirm new password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={saving} block>
              Change Password
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

