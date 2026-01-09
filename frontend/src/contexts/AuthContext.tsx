import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authService } from '../services/authService';
import { AuthResponse, User } from '../types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check if user is already logged in
    const token = authService.getToken();
    if (token) {
      // Decode token to get user info (simplified - in production use proper JWT decode)
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({
          id: payload.userId,
          email: payload.email || payload.sub,
          firstName: payload.firstName || '',
          lastName: payload.lastName || '',
        });
      } catch {
        authService.logout();
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    const response: AuthResponse = await authService.login({ email, password });
    authService.saveTokens(response);

    // Decode token to get user info
    const payload = JSON.parse(atob(response.accessToken.split('.')[1]));
    setUser({
      id: payload.userId,
      email: payload.email || payload.sub,
      firstName: payload.firstName || '',
      lastName: payload.lastName || '',
    });
  };

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    const response: AuthResponse = await authService.register({ email, password, firstName, lastName });
    authService.saveTokens(response);

    const payload = JSON.parse(atob(response.accessToken.split('.')[1]));
    setUser({
      id: payload.userId,
      email: payload.email || payload.sub,
      firstName,
      lastName,
    });
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

