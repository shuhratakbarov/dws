import React, { useState } from 'react';
import { Form, Input, Button, Typography, message, Tabs, Space, Divider } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, WalletOutlined, SafetyOutlined, GlobalOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const { Title, Text, Paragraph } = Typography;

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('login');
  const { login, register } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async (values: { email: string; password: string }) => {
    setLoading(true);
    try {
      await login(values.email, values.password);
      message.success('Welcome back!');
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Login failed. Please check your credentials.');
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
      message.success('Account created successfully!');
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const features = [
    { icon: <WalletOutlined />, title: 'Multi-Currency', desc: 'USD, EUR, GBP, UZS & more' },
    { icon: <SafetyOutlined />, title: 'Secure', desc: 'Bank-grade security' },
    { icon: <GlobalOutlined />, title: 'Instant Transfers', desc: 'Send money globally' },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex' }}>
      {/* Left Panel - Branding */}
      <div
        style={{
          flex: 1,
          background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          padding: 48,
          color: 'white',
        }}
        className="login-brand-panel"
      >
        <div style={{ maxWidth: 400, textAlign: 'center' }}>
          <WalletOutlined style={{ fontSize: 64, marginBottom: 24 }} />
          <Title level={1} style={{ color: 'white', marginBottom: 8 }}>
            Digital Wallet
          </Title>
          <Paragraph style={{ color: 'rgba(255,255,255,0.8)', fontSize: 18, marginBottom: 48 }}>
            Your secure multi-currency digital wallet for seamless transactions
          </Paragraph>

          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            {features.map((feature, index) => (
              <div
                key={index}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 16,
                  padding: '16px 24px',
                  background: 'rgba(255,255,255,0.1)',
                  borderRadius: 12,
                }}
              >
                <div style={{ fontSize: 24 }}>{feature.icon}</div>
                <div style={{ textAlign: 'left' }}>
                  <Text strong style={{ color: 'white', display: 'block' }}>{feature.title}</Text>
                  <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 13 }}>{feature.desc}</Text>
                </div>
              </div>
            ))}
          </Space>
        </div>
      </div>

      {/* Right Panel - Auth Forms */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: 'clamp(16px, 5vw, 48px)',
          background: '#f9fafb',
        }}
      >
        <div style={{ width: '100%', maxWidth: 400 }}>
          <div style={{ textAlign: 'center', marginBottom: 'clamp(16px, 4vw, 32px)' }}>
            <Title level={2} style={{ marginBottom: 8, fontSize: 'clamp(20px, 5vw, 28px)' }}>
              {activeTab === 'login' ? 'Welcome Back' : 'Create Account'}
            </Title>
            <Text type="secondary" style={{ fontSize: 'clamp(12px, 2.5vw, 14px)' }}>
              {activeTab === 'login'
                ? 'Enter your credentials to access your wallet'
                : 'Sign up to start managing your digital wallet'}
            </Text>
          </div>

          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            centered
            items={[
              {
                key: 'login',
                label: 'Sign In',
                children: (
                  <Form
                    name="login"
                    onFinish={handleLogin}
                    autoComplete="off"
                    layout="vertical"
                    size="large"
                    style={{ marginTop: 24 }}
                  >
                    <Form.Item
                      name="email"
                      rules={[
                        { required: true, message: 'Please enter your email' },
                        { type: 'email', message: 'Please enter a valid email' },
                      ]}
                    >
                      <Input
                        prefix={<MailOutlined style={{ color: '#9ca3af' }} />}
                        placeholder="Email address"
                      />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      rules={[{ required: true, message: 'Please enter your password' }]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: '#9ca3af' }} />}
                        placeholder="Password"
                      />
                    </Form.Item>

                    <Form.Item style={{ marginTop: 32 }}>
                      <Button
                        type="primary"
                        htmlType="submit"
                        block
                        loading={loading}
                        style={{ height: 48 }}
                      >
                        Sign In
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
              {
                key: 'register',
                label: 'Sign Up',
                children: (
                  <Form
                    name="register"
                    onFinish={handleRegister}
                    autoComplete="off"
                    layout="vertical"
                    size="large"
                    style={{ marginTop: 24 }}
                  >
                    <div style={{ display: 'flex', gap: 12 }}>
                      <Form.Item
                        name="firstName"
                        rules={[{ required: true, message: 'Required' }]}
                        style={{ flex: 1 }}
                      >
                        <Input
                          prefix={<UserOutlined style={{ color: '#9ca3af' }} />}
                          placeholder="First Name"
                        />
                      </Form.Item>

                      <Form.Item
                        name="lastName"
                        rules={[{ required: true, message: 'Required' }]}
                        style={{ flex: 1 }}
                      >
                        <Input
                          prefix={<UserOutlined style={{ color: '#9ca3af' }} />}
                          placeholder="Last Name"
                        />
                      </Form.Item>
                    </div>

                    <Form.Item
                      name="email"
                      rules={[
                        { required: true, message: 'Please enter your email' },
                        { type: 'email', message: 'Please enter a valid email' },
                      ]}
                    >
                      <Input
                        prefix={<MailOutlined style={{ color: '#9ca3af' }} />}
                        placeholder="Email address"
                      />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      rules={[
                        { required: true, message: 'Please enter a password' },
                        { min: 8, message: 'Password must be at least 8 characters' },
                      ]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: '#9ca3af' }} />}
                        placeholder="Password (min 8 characters)"
                      />
                    </Form.Item>

                    <Form.Item style={{ marginTop: 32 }}>
                      <Button
                        type="primary"
                        htmlType="submit"
                        block
                        loading={loading}
                        style={{ height: 48 }}
                      >
                        Create Account
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
            ]}
          />

          <Divider style={{ margin: '24px 0' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>SECURE & ENCRYPTED</Text>
          </Divider>

          <Text type="secondary" style={{ display: 'block', textAlign: 'center', fontSize: 12 }}>
            By continuing, you agree to our Terms of Service and Privacy Policy
          </Text>
        </div>
      </div>

      {/* Mobile responsive styles */}
      <style>{`
        @media (max-width: 768px) {
          .login-brand-panel {
            display: none !important;
          }
        }
      `}</style>
    </div>
  );
}
