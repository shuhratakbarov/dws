// API Response types
export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
}

// Auth types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

// Wallet types
// Wallet types
export interface Wallet {
  id: string;
  userId: string;
  currency: string;
  balance: number;  // Backend returns 'balance'
  balanceMinorUnits?: number;  // Alias for compatibility
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateWalletRequest {
  currency: string;
}

export interface TransactionRequest {
  amountMinorUnits: number;
  idempotencyKey: string;
  description?: string;
}

export interface TransferRequest {
  fromWalletId: string;
  toWalletId: string;
  amountMinorUnits: number;
  idempotencyKey: string;
  description?: string;
}

export interface TransactionResponse {
  id: string;
  transactionId?: string;
  walletId?: string;
  entryType: 'DEBIT' | 'CREDIT';  // Backend uses DEBIT/CREDIT
  type?: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER_IN' | 'TRANSFER_OUT';  // For compatibility
  amount: number;
  amountMinorUnits?: number;  // Alias for compatibility
  balanceAfter: number;
  balanceAfterMinorUnits?: number;  // Alias for compatibility
  description?: string;
  idempotencyKey?: string;
  createdAt: string;
}

// Currency utility
export const CURRENCIES: Record<string, { symbol: string; name: string; decimals: number }> = {
  USD: { symbol: '$', name: 'US Dollar', decimals: 2 },
  EUR: { symbol: '€', name: 'Euro', decimals: 2 },
  GBP: { symbol: '£', name: 'British Pound', decimals: 2 },
  UZS: { symbol: "so'm", name: 'Uzbek Sum', decimals: 2 },
  JPY: { symbol: '¥', name: 'Japanese Yen', decimals: 0 },
};

export function formatCurrency(minorUnits: number | undefined | null, currency: string): string {
  const config = CURRENCIES[currency] || { symbol: currency || '$', decimals: 2 };
  const safeMinorUnits = minorUnits ?? 0;
  const amount = safeMinorUnits / Math.pow(10, config.decimals);
  return `${config.symbol}${amount.toFixed(config.decimals)}`;
}

export function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

