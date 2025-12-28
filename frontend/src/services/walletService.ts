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
};

