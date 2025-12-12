import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authService } from '../services/authService';

interface User {
  id: number;
  username: string;
  email: string;
}

interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    
    if (token && userData) {
      setUser(JSON.parse(userData));
    }
    setLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    const response = await authService.login(username, password);
    localStorage.setItem('token', response.accessToken);
    localStorage.setItem('user', JSON.stringify({
      id: response.id,
      username: response.username,
      email: response.email
    }));
    setUser({
      id: response.id,
      username: response.username,
      email: response.email
    });
  };

  const register = async (username: string, email: string, password: string) => {
    await authService.register(username, email, password);
  };

  const logout = () => {
    console.log('Logout function called');
    // Simply clear local storage and state - no API calls
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    console.log('Logout completed - user should be null:', null);
  };

  const value = {
    user,
    login,
    register,
    logout,
    loading
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};