import api from './api';
import { Wallet, CreateWalletRequest, TransactionRequest, TransactionResponse, TransferRequest } from '../types';

export const walletService = {
  // Get all wallets for current user
  async getMyWallets(): Promise<Wallet[]> {
    const response = await api.get<Wallet[]>('/wallets/me');
    return response.data;
  },

  // Get wallet by ID
  async getWallet(walletId: string): Promise<Wallet> {
    const response = await api.get<Wallet>(`/wallets/${walletId}`);
    return response.data;
  },

  // Create new wallet
  async createWallet(data: CreateWalletRequest): Promise<Wallet> {
    const response = await api.post<Wallet>('/wallets/me', data);
    return response.data;
  },

  // Deposit funds
  async deposit(walletId: string, data: TransactionRequest): Promise<TransactionResponse> {
    const response = await api.post<TransactionResponse>(`/wallets/${walletId}/deposit`, data);
    return response.data;
  },

  // Withdraw funds
  async withdraw(walletId: string, data: TransactionRequest): Promise<TransactionResponse> {
    const response = await api.post<TransactionResponse>(`/wallets/${walletId}/withdraw`, data);
    return response.data;
  },

  // Transfer between wallets
  async transfer(data: TransferRequest): Promise<{ source: TransactionResponse; destination: TransactionResponse }> {
    const response = await api.post(`/wallets/transfer`, data);
    return response.data;
  },

  // Get transaction history
  async getTransactions(walletId: string, page = 0, size = 20): Promise<{
    content: TransactionResponse[];
    totalElements: number;
    totalPages: number;
  }> {
    const response = await api.get(`/wallets/${walletId}/transactions`, {
      params: { page, size },
    });
    return response.data;
  },

  // Freeze wallet
  async freezeWallet(walletId: string): Promise<Wallet> {
    const response = await api.post<Wallet>(`/wallets/${walletId}/freeze`);
    return response.data;
  },

  // Unfreeze wallet
  async unfreezeWallet(walletId: string): Promise<Wallet> {
    const response = await api.post<Wallet>(`/wallets/${walletId}/unfreeze`);
    return response.data;
  },
};

// Ledger service for immutable transaction history
export const ledgerService = {
  // Get ledger entries for a wallet
  async getEntries(walletId: string, page = 0, size = 50): Promise<{
    content: any[];
    totalElements: number;
    totalPages: number;
  }> {
    const response = await api.get(`/ledger/wallet/${walletId}`, {
      params: { page, size },
    });
    return response.data;
  },

  // Get all ledger entries for user
  async getUserEntries(page = 0, size = 50): Promise<{
    content: any[];
    totalElements: number;
    totalPages: number;
  }> {
    const response = await api.get('/ledger/me', {
      params: { page, size },
    });
    return response.data;
  },
};

// Notification service
export const notificationService = {
  // Get notification preferences
  async getPreferences(): Promise<any> {
    const response = await api.get('/notifications/preferences');
    return response.data;
  },

  // Update notification preferences
  async updatePreferences(preferences: any): Promise<any> {
    const response = await api.put('/notifications/preferences', preferences);
    return response.data;
  },

  // Get notifications
  async getNotifications(page = 0, size = 20): Promise<{
    content: any[];
    totalElements: number;
    totalPages: number;
  }> {
    const response = await api.get('/notifications', {
      params: { page, size },
    });
    return response.data;
  },

  // Mark notification as read
  async markAsRead(notificationId: string): Promise<void> {
    await api.put(`/notifications/${notificationId}/read`);
  },

  // Mark all notifications as read
  async markAllAsRead(): Promise<void> {
    await api.put('/notifications/read-all');
  },
};

