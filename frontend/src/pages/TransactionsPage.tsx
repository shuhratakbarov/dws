import { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Tag,
  Typography,
  Select,
  DatePicker,
  Row,
  Col,
  Statistic,
  Space,
  Button,
  Input,
  Empty,
} from 'antd';
import {
  HistoryOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  SearchOutlined,
  DownloadOutlined,
  FilterOutlined,
} from '@ant-design/icons';
import { walletService } from '../services/walletService';
import { Wallet, TransactionResponse, formatCurrency, CURRENCIES } from '../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;

export default function TransactionsPage() {
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [selectedWalletId, setSelectedWalletId] = useState<string>('all');
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [filters, setFilters] = useState({
    type: 'all',
    search: '',
    dateRange: null as [dayjs.Dayjs, dayjs.Dayjs] | null,
  });

  // Stats
  const [stats, setStats] = useState({
    totalDeposits: 0,
    totalWithdrawals: 0,
    totalTransfers: 0,
    transactionCount: 0,
  });

  useEffect(() => {
    loadWallets();
  }, []);

  useEffect(() => {
    loadTransactions();
  }, [selectedWalletId, pagination.current, filters]);

  const loadWallets = async () => {
    try {
      const data = await walletService.getMyWallets();
      setWallets(data);
      if (data.length > 0) {
        setSelectedWalletId(data[0].id);
      }
    } catch (error) {
      console.error('Failed to load wallets');
    }
  };

  const loadTransactions = async () => {
    if (selectedWalletId === 'all' && wallets.length === 0) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      // For simplicity, load from first wallet or selected
      const walletId = selectedWalletId === 'all' ? wallets[0]?.id : selectedWalletId;
      if (!walletId) return;

      const data = await walletService.getTransactions(walletId, pagination.current - 1, pagination.pageSize);
      setTransactions(data.content || []);
      setPagination((prev) => ({ ...prev, total: data.totalElements }));

      // Calculate stats - backend uses entryType (CREDIT/DEBIT) not type
      const credits = data.content?.filter((t: TransactionResponse) =>
        t.entryType === 'CREDIT' || t.type === 'DEPOSIT' || t.type === 'TRANSFER_IN'
      ) || [];
      const debits = data.content?.filter((t: TransactionResponse) =>
        t.entryType === 'DEBIT' || t.type === 'WITHDRAWAL' || t.type === 'TRANSFER_OUT'
      ) || [];

      setStats({
        totalDeposits: credits.reduce((sum: number, t: TransactionResponse) => sum + (t.amount ?? t.amountMinorUnits ?? 0), 0),
        totalWithdrawals: debits.reduce((sum: number, t: TransactionResponse) => sum + (t.amount ?? t.amountMinorUnits ?? 0), 0),
        totalTransfers: data.content?.filter((t: TransactionResponse) =>
          t.type === 'TRANSFER_IN' || t.type === 'TRANSFER_OUT'
        ).length || 0,
        transactionCount: data.totalElements || 0,
      });
    } catch (error) {
      console.error('Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  const getSelectedWallet = () => wallets.find((w) => w.id === selectedWalletId);

  const handleExport = () => {
    // Export to CSV
    const csv = [
      ['Date', 'Type', 'Amount', 'Balance After', 'Description'].join(','),
      ...transactions.map((t) =>
        [
          new Date(t.createdAt).toISOString(),
          t.entryType || t.type || 'Unknown',
          (t.amount ?? t.amountMinorUnits ?? 0) / 100,
          (t.balanceAfter ?? t.balanceAfterMinorUnits ?? 0) / 100,
          t.description || '',
        ].join(',')
      ),
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `transactions-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
  };

  const columns = [
    {
      title: 'Date & Time',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => (
        <div>
          <div>{new Date(date).toLocaleDateString()}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {new Date(date).toLocaleTimeString()}
          </Text>
        </div>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'entryType',
      key: 'entryType',
      width: 140,
      render: (entryType: string, record: TransactionResponse) => {
        const type = entryType || record.type;
        if (!type) return <Tag>Unknown</Tag>;
        const config: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
          CREDIT: { color: 'green', icon: <ArrowDownOutlined />, label: 'Deposit' },
          DEBIT: { color: 'red', icon: <ArrowUpOutlined />, label: 'Withdrawal' },
          DEPOSIT: { color: 'green', icon: <ArrowDownOutlined />, label: 'Deposit' },
          WITHDRAWAL: { color: 'red', icon: <ArrowUpOutlined />, label: 'Withdrawal' },
          TRANSFER_IN: { color: 'blue', icon: <ArrowDownOutlined />, label: 'Received' },
          TRANSFER_OUT: { color: 'orange', icon: <ArrowUpOutlined />, label: 'Sent' },
        };
        const { color, icon, label } = config[type] || { color: 'default', icon: null, label: type || 'Unknown' };
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
      align: 'right' as const,
      render: (amount: number, record: TransactionResponse) => {
        const entryType = record.entryType || record.type;
        const isNegative = ['DEBIT', 'WITHDRAWAL', 'TRANSFER_OUT'].includes(entryType || '');
        const wallet = getSelectedWallet();
        const displayAmount = amount ?? record.amountMinorUnits ?? 0;
        return (
          <Text strong type={isNegative ? 'danger' : 'success'} style={{ fontSize: 16 }}>
            {isNegative ? '-' : '+'}
            {formatCurrency(displayAmount, wallet?.currency || 'USD')}
          </Text>
        );
      },
    },
    {
      title: 'Balance After',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      width: 150,
      align: 'right' as const,
      render: (balance: number, record: TransactionResponse) => {
        const wallet = getSelectedWallet();
        const displayBalance = balance ?? record.balanceAfterMinorUnits ?? 0;
        return formatCurrency(displayBalance, wallet?.currency || 'USD');
      },
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (desc: string) => desc || <Text type="secondary">-</Text>,
    },
    {
      title: 'Transaction ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      render: (id: string) => (
        <Text copyable={{ text: id }} style={{ fontSize: 12 }}>
          {id.substring(0, 8)}...
        </Text>
      ),
    },
  ];

  const filteredTransactions = transactions.filter((t) => {
    if (filters.type !== 'all' && t.type !== filters.type) return false;
    if (filters.search && !t.description?.toLowerCase().includes(filters.search.toLowerCase())) return false;
    return true;
  });

  return (
    <div style={{ padding: 'clamp(12px, 3vw, 24px)' }}>
      <Row gutter={[16, 16]}>
        {/* Header */}
        <Col span={24}>
          <div style={{
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12
          }}>
            <Title level={2} style={{ margin: 0, fontSize: 'clamp(18px, 4vw, 24px)' }}>
              <HistoryOutlined /> Transaction History
            </Title>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>
              Export CSV
            </Button>
          </div>
        </Col>

        {/* Stats */}
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Total Transactions"
              value={stats.transactionCount}
              prefix={<HistoryOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Total Deposits"
              value={formatCurrency(stats.totalDeposits, getSelectedWallet()?.currency || 'USD')}
              valueStyle={{ color: '#3f8600' }}
              prefix={<ArrowDownOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Total Withdrawals"
              value={formatCurrency(stats.totalWithdrawals, getSelectedWallet()?.currency || 'USD')}
              valueStyle={{ color: '#cf1322' }}
              prefix={<ArrowUpOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title="Transfers" value={stats.totalTransfers} />
          </Card>
        </Col>

        {/* Filters */}
        <Col span={24}>
          <Card>
            <Row gutter={16} align="middle">
              <Col xs={24} sm={12} md={6}>
                <Text strong>Wallet:</Text>
                <Select
                  style={{ width: '100%', marginTop: 4 }}
                  value={selectedWalletId}
                  onChange={setSelectedWalletId}
                >
                  {wallets.map((wallet) => (
                    <Option key={wallet.id} value={wallet.id}>
                      {CURRENCIES[wallet.currency]?.symbol} {wallet.currency} - {formatCurrency(wallet.balance ?? wallet.balanceMinorUnits, wallet.currency)}
                    </Option>
                  ))}
                </Select>
              </Col>
              <Col xs={24} sm={12} md={4}>
                <Text strong>Type:</Text>
                <Select
                  style={{ width: '100%', marginTop: 4 }}
                  value={filters.type}
                  onChange={(v) => setFilters({ ...filters, type: v })}
                >
                  <Option value="all">All Types</Option>
                  <Option value="DEPOSIT">Deposits</Option>
                  <Option value="WITHDRAWAL">Withdrawals</Option>
                  <Option value="TRANSFER_IN">Transfers In</Option>
                  <Option value="TRANSFER_OUT">Transfers Out</Option>
                </Select>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Text strong>Search:</Text>
                <Input
                  style={{ marginTop: 4 }}
                  placeholder="Search description..."
                  prefix={<SearchOutlined />}
                  value={filters.search}
                  onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                  allowClear
                />
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Text strong>Date Range:</Text>
                <RangePicker
                  style={{ width: '100%', marginTop: 4 }}
                  onChange={(dates) => setFilters({ ...filters, dateRange: dates as [dayjs.Dayjs, dayjs.Dayjs] | null })}
                />
              </Col>
              <Col>
                <Button
                  icon={<FilterOutlined />}
                  onClick={() => setFilters({ type: 'all', search: '', dateRange: null })}
                  style={{ marginTop: 24 }}
                >
                  Clear
                </Button>
              </Col>
            </Row>
          </Card>
        </Col>

        {/* Transactions Table */}
        <Col span={24}>
          <Card styles={{ body: { padding: 0 } }}>
            {wallets.length === 0 ? (
              <div style={{ padding: 24 }}>
                <Empty description="No wallets found. Create a wallet first." />
              </div>
            ) : (
              <Table
                dataSource={filteredTransactions}
                columns={columns}
                rowKey="id"
                loading={loading}
                scroll={{ x: 800 }}
                size="small"
                pagination={{
                  ...pagination,
                  showSizeChanger: false,
                  size: 'small',
                  showTotal: (total) => `${total} transactions`,
                  onChange: (page, pageSize) => setPagination({ ...pagination, current: page, pageSize }),
                  style: { marginRight: 16, marginBottom: 16 }
                }}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

