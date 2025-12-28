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
  Tooltip,
} from 'antd';
import {
  WalletOutlined,
  PlusOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  SwapOutlined,
  ReloadOutlined,
  CopyOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  EuroCircleOutlined,
  PoundCircleOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { walletService } from '../services/walletService';
import { Wallet, TransactionResponse, formatCurrency, generateIdempotencyKey, CURRENCIES } from '../types';

const { Title, Text } = Typography;
const { Option } = Select;

// Currency icons and colors
const WALLET_STYLES: Record<string, { gradient: string; icon: React.ReactNode }> = {
  USD: { gradient: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', icon: <DollarOutlined /> },
  EUR: { gradient: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', icon: <EuroCircleOutlined /> },
  GBP: { gradient: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)', icon: <PoundCircleOutlined /> },
  UZS: { gradient: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)', icon: <WalletOutlined /> },
  JPY: { gradient: 'linear-gradient(135deg, #ec4899 0%, #db2777 100%)', icon: <WalletOutlined /> },
};

export default function DashboardPage() {
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [selectedWallet, setSelectedWallet] = useState<Wallet | null>(null);
  const [loading, setLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Modals
  const [createWalletModal, setCreateWalletModal] = useState(false);
  const [depositModal, setDepositModal] = useState(false);
  const [withdrawModal, setWithdrawModal] = useState(false);
  const [transferModal, setTransferModal] = useState(false);

  // Loading states for operations (prevents double-clicks)
  const [depositLoading, setDepositLoading] = useState(false);
  const [withdrawLoading, setWithdrawLoading] = useState(false);
  const [transferLoading, setTransferLoading] = useState(false);
  const [createWalletLoading, setCreateWalletLoading] = useState(false);

  // Idempotency keys (generated when modal opens, not on submit)
  const [currentIdempotencyKey, setCurrentIdempotencyKey] = useState<string>('');

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
      // Update selectedWallet with fresh data
      if (selectedWallet) {
        const updated = data.find(w => w.id === selectedWallet.id);
        if (updated) {
          setSelectedWallet(updated);
        }
      } else if (data.length > 0) {
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
    if (createWalletLoading) return; // Prevent double-clicks
    setCreateWalletLoading(true);
    try {
      const newWallet = await walletService.createWallet(values);
      setWallets([...wallets, newWallet]);
      setSelectedWallet(newWallet);
      setCreateWalletModal(false);
      form.resetFields();
      message.success(`${values.currency} wallet created successfully!`);
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to create wallet');
    } finally {
      setCreateWalletLoading(false);
    }
  };

  const handleDeposit = async (values: { amount: number; description?: string }) => {
    if (!selectedWallet || depositLoading) return; // Prevent double-clicks
    setDepositLoading(true);
    try {
      await walletService.deposit(selectedWallet.id, {
        amountMinorUnits: Math.round(values.amount * 100),
        idempotencyKey: currentIdempotencyKey, // Use pre-generated key
        description: values.description,
      });
      setDepositModal(false);
      form.resetFields();
      loadWallets();
      loadTransactions(selectedWallet.id);
      message.success('Deposit successful!');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Deposit failed');
    } finally {
      setDepositLoading(false);
    }
  };

  const handleWithdraw = async (values: { amount: number; description?: string }) => {
    if (!selectedWallet || withdrawLoading) return; // Prevent double-clicks
    setWithdrawLoading(true);
    try {
      await walletService.withdraw(selectedWallet.id, {
        amountMinorUnits: Math.round(values.amount * 100),
        idempotencyKey: currentIdempotencyKey, // Use pre-generated key
        description: values.description,
      });
      setWithdrawModal(false);
      form.resetFields();
      loadWallets();
      loadTransactions(selectedWallet.id);
      message.success('Withdrawal successful!');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Withdrawal failed');
    } finally {
      setWithdrawLoading(false);
    }
  };

  const handleTransfer = async (values: { toWalletId: string; amount: number; description?: string }) => {
    if (!selectedWallet || transferLoading) return; // Prevent double-clicks
    setTransferLoading(true);
    try {
      await walletService.transfer({
        fromWalletId: selectedWallet.id,
        toWalletId: values.toWalletId,
        amountMinorUnits: Math.round(values.amount * 100),
        idempotencyKey: currentIdempotencyKey, // Use pre-generated key
        description: values.description,
      });
      setTransferModal(false);
      form.resetFields();
      loadWallets();
      loadTransactions(selectedWallet.id);
      message.success('Transfer successful!');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Transfer failed');
    } finally {
      setTransferLoading(false);
    }
  };

  // Helper to open modals and generate new idempotency key
  const openDepositModal = () => {
    setCurrentIdempotencyKey(generateIdempotencyKey());
    form.resetFields();
    setDepositModal(true);
  };

  const openWithdrawModal = () => {
    setCurrentIdempotencyKey(generateIdempotencyKey());
    form.resetFields();
    setWithdrawModal(true);
  };

  const openTransferModal = () => {
    setCurrentIdempotencyKey(generateIdempotencyKey());
    form.resetFields();
    setTransferModal(true);
  };

  const openCreateWalletModal = () => {
    form.resetFields();
    setCreateWalletModal(true);
  };

  const copyWalletId = (id: string) => {
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    message.success('Wallet ID copied!');
    setTimeout(() => setCopiedId(null), 2000);
  };

  const transactionColumns = [
    {
      title: 'Type',
      dataIndex: 'entryType',
      key: 'entryType',
      width: 130,
      render: (entryType: string, record: TransactionResponse) => {
        // Backend returns DEBIT (money out) or CREDIT (money in)
        const type = entryType || record.type;
        if (!type) return <Tag>Unknown</Tag>;
        const config: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
          CREDIT: { color: 'success', icon: <ArrowDownOutlined />, label: 'Deposit' },
          DEBIT: { color: 'error', icon: <ArrowUpOutlined />, label: 'Withdrawal' },
          DEPOSIT: { color: 'success', icon: <ArrowDownOutlined />, label: 'Deposit' },
          WITHDRAWAL: { color: 'error', icon: <ArrowUpOutlined />, label: 'Withdrawal' },
          TRANSFER_IN: { color: 'processing', icon: <SwapOutlined />, label: 'Received' },
          TRANSFER_OUT: { color: 'warning', icon: <SwapOutlined />, label: 'Sent' },
        };
        const { color, icon, label } = config[type] || { color: 'default', icon: null, label: type };
        return (
          <Tag color={color} icon={icon}>
            {label}
          </Tag>
        );
      },
    },
    {
      title: 'Amount',
      dataIndex: 'amount',
      key: 'amount',
      width: 150,
      render: (amount: number, record: TransactionResponse) => {
        const entryType = record.entryType || record.type;
        const isNegative = ['DEBIT', 'WITHDRAWAL', 'TRANSFER_OUT'].includes(entryType || '');
        const displayAmount = amount ?? record.amountMinorUnits ?? 0;
        return (
          <Text strong style={{ color: isNegative ? '#ef4444' : '#10b981', fontSize: 15 }}>
            {isNegative ? '-' : '+'}
            {formatCurrency(displayAmount, selectedWallet?.currency || 'USD')}
          </Text>
        );
      },
    },
    {
      title: 'Balance After',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      width: 150,
      render: (balance: number, record: TransactionResponse) => {
        const displayBalance = balance ?? record.balanceAfterMinorUnits ?? 0;
        return (
          <Text type="secondary">{formatCurrency(displayBalance, selectedWallet?.currency || 'USD')}</Text>
        );
      },
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (desc: string) => desc || <Text type="secondary">—</Text>,
    },
    {
      title: 'Date',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => (
        <div>
          <div style={{ fontWeight: 500 }}>{new Date(date).toLocaleDateString()}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {new Date(date).toLocaleTimeString()}
          </Text>
        </div>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Row gutter={[24, 24]}>
        {/* Header */}
        <Col span={24}>
          <div className="page-header">
            <div>
              <Title level={2} style={{ marginBottom: 4 }}>
                Dashboard
              </Title>
              <Text type="secondary">Manage your digital wallets and transactions</Text>
            </div>
            <Space>
              <Button icon={<ReloadOutlined />} onClick={loadWallets}>
                Refresh
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={openCreateWalletModal}>
                New Wallet
              </Button>
            </Space>
          </div>
        </Col>

        {/* Quick Stats */}
        {wallets.length > 0 && (
          <Col span={24}>
            <Row gutter={16} className="stats-row">
              <Col xs={24} sm={8}>
                <Card>
                  <Statistic
                    title="Total Wallets"
                    value={wallets.length}
                    prefix={<WalletOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={8}>
                <Card>
                  <Statistic
                    title="Active Wallets"
                    value={wallets.filter(w => w.status === 'ACTIVE').length}
                    prefix={<CheckCircleOutlined />}
                    valueStyle={{ color: '#10b981' }}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={8}>
                <Card>
                  <Statistic
                    title="Recent Transactions"
                    value={transactions.length}
                    prefix={<SwapOutlined />}
                  />
                </Card>
              </Col>
            </Row>
          </Col>
        )}

        {/* Wallet Cards */}
        {wallets.length === 0 ? (
          <Col span={24}>
            <Card>
              <Empty
                description={
                  <div>
                    <Title level={4}>No wallets yet</Title>
                    <Text type="secondary">Create your first wallet to start managing your funds</Text>
                  </div>
                }
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreateWalletModal}>
                  Create Your First Wallet
                </Button>
              </Empty>
            </Card>
          </Col>
        ) : (
          <>
            <Col span={24}>
              <Text strong style={{ fontSize: 16, color: '#374151' }}>
                My Wallets <Text type="secondary" style={{ fontWeight: 400, fontSize: 14 }}>(click to select)</Text>
              </Text>
            </Col>
            {wallets.map((wallet) => {
              const style = WALLET_STYLES[wallet.currency] || WALLET_STYLES.USD;
              const isSelected = selectedWallet?.id === wallet.id;
              return (
                <Col xs={24} sm={12} lg={8} xl={6} key={wallet.id}>
                  <Card
                    hoverable
                    onClick={() => setSelectedWallet(wallet)}
                    style={{
                      background: style.gradient,
                      border: isSelected ? '4px solid #1f2937' : '4px solid transparent',
                      transform: isSelected ? 'scale(1.03)' : 'scale(1)',
                      boxShadow: isSelected
                        ? '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1)'
                        : '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                      position: 'relative',
                    }}
                    styles={{ body: { padding: 20 } }}
                  >
                    {isSelected && (
                      <div style={{
                        position: 'absolute',
                        top: -12,
                        right: 12,
                        background: '#1f2937',
                        color: 'white',
                        padding: '4px 12px',
                        borderRadius: 12,
                        fontSize: 11,
                        fontWeight: 600,
                      }}>
                        SELECTED
                      </div>
                    )}
                    <div style={{ color: 'white' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
                        <div style={{ fontSize: 32, opacity: 0.9 }}>{style.icon}</div>
                        <Tag color={wallet.status === 'ACTIVE' ? 'green' : 'red'} style={{ margin: 0 }}>
                          {wallet.status}
                        </Tag>
                      </div>
                      <Text style={{ color: 'rgba(255,255,255,0.8)', fontSize: 13 }}>
                        {CURRENCIES[wallet.currency]?.name || wallet.currency}
                      </Text>
                      <div style={{ fontSize: 28, fontWeight: 700, marginTop: 4, color: 'white' }}>
                        {formatCurrency(wallet.balance ?? wallet.balanceMinorUnits, wallet.currency)}
                      </div>
                      <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
                        <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 11 }}>
                          ID: {wallet.id.slice(0, 8)}...
                        </Text>
                        <Tooltip title={copiedId === wallet.id ? 'Copied!' : 'Copy ID'}>
                          <Button
                            type="text"
                            size="small"
                            icon={copiedId === wallet.id ? <CheckCircleOutlined /> : <CopyOutlined />}
                            onClick={(e) => { e.stopPropagation(); copyWalletId(wallet.id); }}
                            style={{ color: 'rgba(255,255,255,0.8)', padding: 0 }}
                          />
                        </Tooltip>
                      </div>
                    </div>
                  </Card>
                </Col>
              );
            })}

            {/* Action Buttons for Selected Wallet */}
            {selectedWallet && (
              <Col span={24}>
                <Card>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
                    <div>
                      <Text type="secondary">Selected Wallet</Text>
                      <div>
                        <Text strong style={{ fontSize: 18 }}>
                          {CURRENCIES[selectedWallet.currency]?.name || selectedWallet.currency} ({selectedWallet.currency})
                        </Text>
                        <Text style={{ marginLeft: 12, fontSize: 18, fontWeight: 600, color: '#4f46e5' }}>
                          {formatCurrency(selectedWallet.balance ?? selectedWallet.balanceMinorUnits, selectedWallet.currency)}
                        </Text>
                      </div>
                    </div>
                    <Space size="middle" wrap>
                      <Tooltip title="Add funds to your wallet">
                        <Button
                          type="primary"
                          size="large"
                          icon={<ArrowDownOutlined />}
                          onClick={openDepositModal}
                          style={{ background: '#10b981', borderColor: '#10b981' }}
                        >
                          Deposit
                        </Button>
                      </Tooltip>
                      <Tooltip title="Take money out of your wallet">
                        <Button
                          size="large"
                          danger
                          icon={<ArrowUpOutlined />}
                          onClick={openWithdrawModal}
                        >
                          Withdraw
                        </Button>
                      </Tooltip>
                      <Tooltip title="Send money to another wallet">
                        <Button
                          size="large"
                          icon={<SwapOutlined />}
                          onClick={openTransferModal}
                        >
                          Transfer
                        </Button>
                      </Tooltip>
                    </Space>
                  </div>
                </Card>
              </Col>
            )}

            {/* Transaction History */}
            {selectedWallet && (
              <Col span={24}>
                <Card
                  title={
                    <Space>
                      <HistoryOutlined />
                      <span>Transaction History</span>
                    </Space>
                  }
                  extra={
                    <Text type="secondary">{transactions.length} transactions</Text>
                  }
                >
                  <Table
                    dataSource={transactions}
                    columns={transactionColumns}
                    rowKey="id"
                    loading={transactionsLoading}
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                    locale={{ emptyText: 'No transactions yet' }}
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
        onCancel={() => { setCreateWalletModal(false); form.resetFields(); }}
        footer={null}
        centered
      >
        <Form form={form} onFinish={handleCreateWallet} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="currency"
            label="Select Currency"
            rules={[{ required: true, message: 'Please select a currency' }]}
          >
            <Select placeholder="Choose a currency" size="large">
              {Object.entries(CURRENCIES)
                .filter(([code]) => !wallets.find(w => w.currency === code))
                .map(([code, info]) => (
                  <Option key={code} value={code}>
                    <Space>
                      <span style={{ fontWeight: 500 }}>{info.symbol}</span>
                      <span>{info.name}</span>
                      <Tag>{code}</Tag>
                    </Space>
                  </Option>
                ))}
            </Select>
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
            <Button type="primary" htmlType="submit" block size="large" loading={createWalletLoading} disabled={createWalletLoading}>
              Create Wallet
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Deposit Modal */}
      <Modal
        title={`Deposit to ${selectedWallet?.currency || ''} Wallet`}
        open={depositModal}
        onCancel={() => { setDepositModal(false); form.resetFields(); }}
        footer={null}
        centered
      >
        <Form form={form} onFinish={handleDeposit} layout="vertical" style={{ marginTop: 16 }}>
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
              placeholder="0.00"
            />
          </Form.Item>
          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="e.g., Salary, Savings, etc." size="large" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
            <Button type="primary" htmlType="submit" block size="large" loading={depositLoading} disabled={depositLoading} style={{ background: '#10b981', borderColor: '#10b981' }}>
              Deposit
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Withdraw Modal */}
      <Modal
        title={`Withdraw from ${selectedWallet?.currency} Wallet`}
        open={withdrawModal}
        onCancel={() => { setWithdrawModal(false); form.resetFields(); }}
        footer={null}
        centered
      >
        <div style={{ marginBottom: 16, padding: 12, background: '#fef3c7', borderRadius: 8 }}>
          <Text>
            Available balance: <strong>{formatCurrency(selectedWallet?.balance ?? selectedWallet?.balanceMinorUnits ?? 0, selectedWallet?.currency || 'USD')}</strong>
          </Text>
        </div>
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
              max={(selectedWallet?.balance ?? selectedWallet?.balanceMinorUnits ?? 0) / 100}
              placeholder="0.00"
            />
          </Form.Item>
          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="e.g., ATM withdrawal, etc." size="large" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
            <Button type="primary" htmlType="submit" block size="large" danger loading={withdrawLoading} disabled={withdrawLoading}>
              Withdraw Funds
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* Transfer Modal */}
      <Modal
        title="Transfer Funds"
        open={transferModal}
        onCancel={() => { setTransferModal(false); form.resetFields(); }}
        footer={null}
        centered
        width={480}
      >
        <div style={{ marginBottom: 16, padding: 12, background: '#eff6ff', borderRadius: 8 }}>
          <Text>
            Sending from: <strong>{selectedWallet?.currency} Wallet</strong>
            <br />
            Available: <strong>{formatCurrency(selectedWallet?.balance ?? selectedWallet?.balanceMinorUnits ?? 0, selectedWallet?.currency || 'USD')}</strong>
          </Text>
        </div>
        <Form form={form} onFinish={handleTransfer} layout="vertical">
          <Form.Item
            name="toWalletId"
            label="Recipient Wallet ID"
            rules={[{ required: true, message: 'Please enter recipient wallet ID' }]}
            extra="Enter the full wallet ID of the recipient"
          >
            <Input placeholder="e.g., 550e8400-e29b-41d4-a716-446655440000" size="large" />
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
              max={(selectedWallet?.balance ?? selectedWallet?.balanceMinorUnits ?? 0) / 100}
              placeholder="0.00"
            />
          </Form.Item>
          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="e.g., Payment for services" size="large" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, marginTop: 24 }}>
            <Button type="primary" htmlType="submit" block size="large" loading={transferLoading} disabled={transferLoading}>
              Send Transfer
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

