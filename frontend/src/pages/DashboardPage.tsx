import React, { useState, useEffect } from 'react';
import {
  Row,
  Col,
  Card,
  Statistic,
  Button,
  Table,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  message,
  Space,
  Typography,
  Empty,
  Spin,
} from 'antd';
import {
  WalletOutlined,
  PlusOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  SwapOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { walletService } from '../services/walletService';
import { Wallet, TransactionResponse, formatCurrency, generateIdempotencyKey, CURRENCIES } from '../types';

const { Title, Text } = Typography;
const { Option } = Select;

export default function DashboardPage() {
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [selectedWallet, setSelectedWallet] = useState<Wallet | null>(null);
  const [loading, setLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(false);

  // Modals
  const [createWalletModal, setCreateWalletModal] = useState(false);
  const [depositModal, setDepositModal] = useState(false);
  const [withdrawModal, setWithdrawModal] = useState(false);
  const [transferModal, setTransferModal] = useState(false);

  const [form] = Form.useForm();

  useEffect(() => {
    loadWallets();
  }, []);

  useEffect(() => {
    if (selectedWallet) {
      loadTransactions(selectedWallet.id);
    }
  }, [selectedWallet]);

  const loadWallets = async () => {
    setLoading(true);
    try {
      const data = await walletService.getMyWallets();
      setWallets(data);
      if (data.length > 0 && !selectedWallet) {
        setSelectedWallet(data[0]);
      }
    } catch (error: any) {
      message.error('Failed to load wallets');
    } finally {
      setLoading(false);
    }
  };

  const loadTransactions = async (walletId: string) => {
    setTransactionsLoading(true);
    try {
      const data = await walletService.getTransactions(walletId);
      setTransactions(data.content || []);
    } catch (error: any) {
      message.error('Failed to load transactions');
    } finally {
      setTransactionsLoading(false);
    }
  };

  const handleCreateWallet = async (values: { currency: string }) => {
    try {
      const newWallet = await walletService.createWallet(values);
      setWallets([...wallets, newWallet]);
      setCreateWalletModal(false);
      form.resetFields();
      message.success(`${values.currency} wallet created!`);
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to create wallet');
    }
  };

  const handleDeposit = async (values: { amount: number; description?: string }) => {
    if (!selectedWallet) return;
    try {
      await walletService.deposit(selectedWallet.id, {
        amountMinorUnits: Math.round(values.amount * 100),
        idempotencyKey: generateIdempotencyKey(),
        description: values.description,
      });
      setDepositModal(false);
      form.resetFields();
      loadWallets();
      loadTransactions(selectedWallet.id);
      message.success('Deposit successful!');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Deposit failed');
    }
  };

  const handleWithdraw = async (values: { amount: number; description?: string }) => {
    if (!selectedWallet) return;
    try {
      await walletService.withdraw(selectedWallet.id, {
        amountMinorUnits: Math.round(values.amount * 100),
        idempotencyKey: generateIdempotencyKey(),
        description: values.description,
      });
      setWithdrawModal(false);
      form.resetFields();
      loadWallets();
      loadTransactions(selectedWallet.id);
      message.success('Withdrawal successful!');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Withdrawal failed');
    }
  };

  const handleTransfer = async (values: { toWalletId: string; amount: number; description?: string }) => {
    if (!selectedWallet) return;
    try {
      await walletService.transfer({
        fromWalletId: selectedWallet.id,
        toWalletId: values.toWalletId,
        amountMinorUnits: Math.round(values.amount * 100),
        idempotencyKey: generateIdempotencyKey(),
        description: values.description,
      });
      setTransferModal(false);
      form.resetFields();
      loadWallets();
      loadTransactions(selectedWallet.id);
      message.success('Transfer successful!');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Transfer failed');
    }
  };

  const transactionColumns = [
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => {
        const colors: Record<string, string> = {
          DEPOSIT: 'green',
          WITHDRAWAL: 'red',
          TRANSFER_IN: 'blue',
          TRANSFER_OUT: 'orange',
        };
        return <Tag color={colors[type] || 'default'}>{type}</Tag>;
      },
    },
    {
      title: 'Amount',
      dataIndex: 'amountMinorUnits',
      key: 'amount',
      render: (amount: number, record: TransactionResponse) => {
        const isNegative = ['WITHDRAWAL', 'TRANSFER_OUT'].includes(record.type);
        return (
          <Text type={isNegative ? 'danger' : 'success'}>
            {isNegative ? '-' : '+'}
            {formatCurrency(amount, selectedWallet?.currency || 'USD')}
          </Text>
        );
      },
    },
    {
      title: 'Balance After',
      dataIndex: 'balanceAfterMinorUnits',
      key: 'balanceAfter',
      render: (balance: number) => formatCurrency(balance, selectedWallet?.currency || 'USD'),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      render: (desc: string) => desc || '-',
    },
    {
      title: 'Date',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleString(),
    },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Row gutter={[24, 24]}>
        {/* Header */}
        <Col span={24}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Title level={2}>
              <WalletOutlined /> My Wallets
            </Title>
            <Space>
              <Button icon={<ReloadOutlined />} onClick={loadWallets}>
                Refresh
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateWalletModal(true)}>
                New Wallet
              </Button>
            </Space>
          </div>
        </Col>

        {/* Wallet Cards */}
        {wallets.length === 0 ? (
          <Col span={24}>
            <Empty
              description="No wallets yet"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button type="primary" onClick={() => setCreateWalletModal(true)}>
                Create Your First Wallet
              </Button>
            </Empty>
          </Col>
        ) : (
          <>
            {wallets.map((wallet) => (
              <Col xs={24} sm={12} lg={8} xl={6} key={wallet.id}>
                <Card
                  hoverable
                  style={{
                    borderColor: selectedWallet?.id === wallet.id ? '#1890ff' : undefined,
                    borderWidth: selectedWallet?.id === wallet.id ? 2 : 1,
                  }}
                  onClick={() => setSelectedWallet(wallet)}
                >
                  <Statistic
                    title={
                      <Space>
                        <span>{CURRENCIES[wallet.currency]?.name || wallet.currency}</span>
                        <Tag color={wallet.status === 'ACTIVE' ? 'green' : 'red'}>
                          {wallet.status}
                        </Tag>
                      </Space>
                    }
                    value={formatCurrency(wallet.balanceMinorUnits, wallet.currency)}
                    precision={2}
                  />
                </Card>
              </Col>
            ))}

            {/* Action Buttons for Selected Wallet */}
            {selectedWallet && (
              <Col span={24}>
                <Card>
                  <Space size="middle">
                    <Text strong>
                      Selected: {CURRENCIES[selectedWallet.currency]?.name} ({selectedWallet.currency})
                    </Text>
                    <Button
                      type="primary"
                      icon={<ArrowDownOutlined />}
                      onClick={() => setDepositModal(true)}
                    >
                      Deposit
                    </Button>
                    <Button
                      danger
                      icon={<ArrowUpOutlined />}
                      onClick={() => setWithdrawModal(true)}
                    >
                      Withdraw
                    </Button>
                    <Button
                      icon={<SwapOutlined />}
                      onClick={() => setTransferModal(true)}
                    >
                      Transfer
                    </Button>
                  </Space>
                </Card>
              </Col>
            )}

            {/* Transaction History */}
            {selectedWallet && (
              <Col span={24}>
                <Card title="Transaction History">
                  <Table
                    dataSource={transactions}
                    columns={transactionColumns}
                    rowKey="id"
                    loading={transactionsLoading}
                    pagination={{ pageSize: 10 }}
                  />
                </Card>
              </Col>
            )}
          </>
        )}
      </Row>

      {/* Create Wallet Modal */}
      <Modal
        title="Create New Wallet"
        open={createWalletModal}
        onCancel={() => setCreateWalletModal(false)}
        footer={null}
      >
        <Form form={form} onFinish={handleCreateWallet} layout="vertical">
          <Form.Item
            name="currency"
            label="Currency"
            rules={[{ required: true, message: 'Please select a currency' }]}
          >
            <Select placeholder="Select currency" size="large">
              {Object.entries(CURRENCIES).map(([code, info]) => (
                <Option key={code} value={code}>
                  {info.symbol} {info.name} ({code})
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Create Wallet
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Deposit Modal */}
      <Modal
        title={`Deposit to ${selectedWallet?.currency} Wallet`}
        open={depositModal}
        onCancel={() => setDepositModal(false)}
        footer={null}
      >
        <Form form={form} onFinish={handleDeposit} layout="vertical">
          <Form.Item
            name="amount"
            label="Amount"
            rules={[
              { required: true, message: 'Please enter amount' },
              { type: 'number', min: 0.01, message: 'Amount must be greater than 0' },
            ]}
          >
            <InputNumber
              prefix={CURRENCIES[selectedWallet?.currency || 'USD']?.symbol}
              style={{ width: '100%' }}
              size="large"
              precision={2}
              min={0.01}
            />
          </Form.Item>
          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="e.g., Salary, Gift, etc." />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Deposit
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Withdraw Modal */}
      <Modal
        title={`Withdraw from ${selectedWallet?.currency} Wallet`}
        open={withdrawModal}
        onCancel={() => setWithdrawModal(false)}
        footer={null}
      >
        <Form form={form} onFinish={handleWithdraw} layout="vertical">
          <Form.Item
            name="amount"
            label="Amount"
            rules={[
              { required: true, message: 'Please enter amount' },
              { type: 'number', min: 0.01, message: 'Amount must be greater than 0' },
            ]}
          >
            <InputNumber
              prefix={CURRENCIES[selectedWallet?.currency || 'USD']?.symbol}
              style={{ width: '100%' }}
              size="large"
              precision={2}
              min={0.01}
              max={(selectedWallet?.balanceMinorUnits || 0) / 100}
            />
          </Form.Item>
          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="e.g., ATM withdrawal, etc." />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block danger>
              Withdraw
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Transfer Modal */}
      <Modal
        title="Transfer Funds"
        open={transferModal}
        onCancel={() => setTransferModal(false)}
        footer={null}
      >
        <Form form={form} onFinish={handleTransfer} layout="vertical">
          <Form.Item
            name="toWalletId"
            label="Recipient Wallet ID"
            rules={[{ required: true, message: 'Please enter recipient wallet ID' }]}
          >
            <Input placeholder="Enter wallet ID" size="large" />
          </Form.Item>
          <Form.Item
            name="amount"
            label="Amount"
            rules={[
              { required: true, message: 'Please enter amount' },
              { type: 'number', min: 0.01, message: 'Amount must be greater than 0' },
            ]}
          >
            <InputNumber
              prefix={CURRENCIES[selectedWallet?.currency || 'USD']?.symbol}
              style={{ width: '100%' }}
              size="large"
              precision={2}
              min={0.01}
              max={(selectedWallet?.balanceMinorUnits || 0) / 100}
            />
          </Form.Item>
          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="e.g., Payment for services" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Transfer
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

