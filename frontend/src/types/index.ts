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

// Tax rates for different providers (slightly higher than actual provider rates)
export const TAX_RATES: Record<string, { rate: number; name: string }> = {
  VISA: { rate: 0.025, name: 'Visa' },           // 2.5%
  MASTERCARD: { rate: 0.025, name: 'Mastercard' }, // 2.5%
  UZCARD: { rate: 0.015, name: 'UzCard' },       // 1.5%
  HUMO: { rate: 0.015, name: 'Humo' },           // 1.5%
};

export function formatCurrency(minorUnits: number | undefined | null, currency: string): string {
  const config = CURRENCIES[currency] || { symbol: currency || '$', decimals: 2 };
  const safeMinorUnits = minorUnits ?? 0;
  const amount = safeMinorUnits / Math.pow(10, config.decimals);

  // Format with thousand separators (space)
  const formatted = amount.toFixed(config.decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  return `${config.symbol}${formatted}`;
}

export function formatNumber(value: number): string {
  // Format number with thousand separators (space)
  return value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

export function calculateTax(amount: number, cardType: string): { tax: number; total: number; rate: number } {
  const taxInfo = TAX_RATES[cardType] || { rate: 0.02, name: 'Standard' };
  const tax = Math.ceil(amount * taxInfo.rate);
  return {
    tax,
    total: amount + tax,
    rate: taxInfo.rate * 100,
  };
}

export function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

