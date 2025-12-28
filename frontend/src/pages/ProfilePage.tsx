import { useState, useEffect } from 'react';
import {
  Card,
  Form,
  Input,
  Button,
  Typography,
  message,
  Descriptions,
  Avatar,
  Row,
  Col,
  Divider,
  Tag,
  Spin,
} from 'antd';
import { UserOutlined, MailOutlined, PhoneOutlined, HomeOutlined } from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext';
import api from '../services/api';

const { Title, Text } = Typography;

interface CustomerProfile {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  kycStatus: 'PENDING' | 'VERIFIED' | 'REJECTED' | 'NOT_STARTED';
  status?: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  address?: {
    street: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
  };
  createdAt: string;
  updatedAt?: string;
}

export default function ProfilePage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<CustomerProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    setLoading(true);
    try {
      const response = await api.get<CustomerProfile>('/customers/me');
      setProfile(response.data);
      form.setFieldsValue({
        firstName: response.data.firstName,
        lastName: response.data.lastName,
        phoneNumber: response.data.phoneNumber,
      });
    } catch (error: any) {
      // Profile might not exist yet
      if (error.response?.status === 404) {
        message.info('Please complete your profile');
        setEditing(true);
      } else {
        message.error('Failed to load profile');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (values: any) => {
    setSaving(true);
    try {
      if (profile) {
        // Update existing profile
        await api.put('/customers/me', values);
        message.success('Profile updated successfully');
      } else {
        // Create new profile
        await api.post('/customers', {
          ...values,
          userId: user?.id,
          email: user?.email,
        });
        message.success('Profile created successfully');
      }
      setEditing(false);
      loadProfile();
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to save profile');
    } finally {
      setSaving(false);
    }
  };

  const getKycStatusColor = (status: string) => {
    switch (status) {
      case 'VERIFIED':
        return 'green';
      case 'REJECTED':
        return 'red';
      case 'PENDING':
        return 'orange';
      case 'NOT_STARTED':
      default:
        return 'default';
    }
  };

  const getKycStatusText = (status: string) => {
    switch (status) {
      case 'VERIFIED':
        return 'Verified ✓';
      case 'REJECTED':
        return 'Rejected';
      case 'PENDING':
        return 'Pending Review';
      case 'NOT_STARTED':
      default:
        return 'Not Started';
    }
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
        <UserOutlined /> My Profile
      </Title>

      <Row gutter={[24, 24]}>
        {/* Profile Summary Card */}
        <Col span={24}>
          <Card>
            <Row gutter={24} align="middle">
              <Col>
                <Avatar size={80} icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }} />
              </Col>
              <Col flex="auto">
                <Title level={4} style={{ marginBottom: 4 }}>
                  {profile ? `${profile.firstName} ${profile.lastName}` : user?.email}
                </Title>
                <Text type="secondary">
                  <MailOutlined /> {user?.email}
                </Text>
                <div style={{ marginTop: 8 }}>
                  <Tag color={getKycStatusColor(profile?.kycStatus || 'NOT_STARTED')}>
                    KYC: {getKycStatusText(profile?.kycStatus || 'NOT_STARTED')}
                  </Tag>
                </div>
              </Col>
              <Col>
                {!editing && (
                  <Button type="primary" onClick={() => setEditing(true)}>
                    Edit Profile
                  </Button>
                )}
              </Col>
            </Row>
          </Card>
        </Col>

        {/* Profile Details / Edit Form */}
        <Col span={24}>
          <Card title="Profile Information">
            {editing ? (
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSave}
                initialValues={{
                  firstName: profile?.firstName || user?.firstName || '',
                  lastName: profile?.lastName || user?.lastName || '',
                  phoneNumber: profile?.phoneNumber || '',
                }}
              >
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="firstName"
                      label="First Name"
                      rules={[{ required: true, message: 'Please enter first name' }]}
                    >
                      <Input prefix={<UserOutlined />} placeholder="First Name" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="lastName"
                      label="Last Name"
                      rules={[{ required: true, message: 'Please enter last name' }]}
                    >
                      <Input prefix={<UserOutlined />} placeholder="Last Name" />
                    </Form.Item>
                  </Col>
                </Row>

                <Form.Item name="phoneNumber" label="Phone Number">
                  <Input prefix={<PhoneOutlined />} placeholder="+1 (555) 123-4567" />
                </Form.Item>

                <Divider>Address (Optional)</Divider>

                <Row gutter={16}>
                  <Col span={24}>
                    <Form.Item name={['address', 'street']} label="Street Address">
                      <Input prefix={<HomeOutlined />} placeholder="123 Main St" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name={['address', 'city']} label="City">
                      <Input placeholder="New York" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name={['address', 'state']} label="State/Province">
                      <Input placeholder="NY" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name={['address', 'postalCode']} label="Postal Code">
                      <Input placeholder="10001" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name={['address', 'country']} label="Country">
                      <Input placeholder="USA" />
                    </Form.Item>
                  </Col>
                </Row>

                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={saving} style={{ marginRight: 8 }}>
                    Save Changes
                  </Button>
                  <Button onClick={() => setEditing(false)}>Cancel</Button>
                </Form.Item>
              </Form>
            ) : (
              <Descriptions column={2} bordered>
                <Descriptions.Item label="First Name">
                  {profile?.firstName || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="Last Name">
                  {profile?.lastName || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="Email">{user?.email}</Descriptions.Item>
                <Descriptions.Item label="Phone">
                  {profile?.phoneNumber || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="KYC Status">
                  <Tag color={getKycStatusColor(profile?.kycStatus || 'NOT_STARTED')}>
                    {getKycStatusText(profile?.kycStatus || 'NOT_STARTED')}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Member Since">
                  {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '-'}
                </Descriptions.Item>
                {profile?.address && (
                  <Descriptions.Item label="Address" span={2}>
                    {`${profile.address.street}, ${profile.address.city}, ${profile.address.state} ${profile.address.postalCode}, ${profile.address.country}`}
                  </Descriptions.Item>
                )}
              </Descriptions>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

