import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, message, Tabs, Space } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('login');
  const { login, register } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (values: { email: string; password: string }) => {
    setLoading(true);
    try {
      await login(values.email, values.password);
      message.success('Login successful!');
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }) => {
    setLoading(true);
    try {
      await register(values.email, values.password, values.firstName, values.lastName);
      message.success('Registration successful!');
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card
        style={{
          width: 400,
          boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
          borderRadius: 12,
        }}
      >
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Title level={2} style={{ marginBottom: 4 }}>
              💰 Digital Wallet
            </Title>
            <Text type="secondary">Secure multi-currency wallet system</Text>
          </div>

          <Tabs activeKey={activeTab} onChange={setActiveTab} centered>
            <TabPane tab="Sign In" key="login">
              <Form
                name="login"
                onFinish={handleLogin}
                autoComplete="off"
                layout="vertical"
              >
                <Form.Item
                  name="email"
                  rules={[
                    { required: true, message: 'Please enter your email' },
                    { type: 'email', message: 'Invalid email format' },
                  ]}
                >
                  <Input
                    prefix={<MailOutlined />}
                    placeholder="Email"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[{ required: true, message: 'Please enter your password' }]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="Password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    size="large"
                    loading={loading}
                  >
                    Sign In
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>

            <TabPane tab="Sign Up" key="register">
              <Form
                name="register"
                onFinish={handleRegister}
                autoComplete="off"
                layout="vertical"
              >
                <Form.Item
                  name="firstName"
                  rules={[{ required: true, message: 'Please enter your first name' }]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="First Name"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="lastName"
                  rules={[{ required: true, message: 'Please enter your last name' }]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="Last Name"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="email"
                  rules={[
                    { required: true, message: 'Please enter your email' },
                    { type: 'email', message: 'Invalid email format' },
                  ]}
                >
                  <Input
                    prefix={<MailOutlined />}
                    placeholder="Email"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[
                    { required: true, message: 'Please enter your password' },
                    { min: 8, message: 'Password must be at least 8 characters' },
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="Password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    block
                    size="large"
                    loading={loading}
                  >
                    Create Account
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>
          </Tabs>
        </Space>
      </Card>
    </div>
  );
}

