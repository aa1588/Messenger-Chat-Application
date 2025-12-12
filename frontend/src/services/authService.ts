import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

interface LoginResponse {
  accessToken: string;
  tokenType: string;
  id: number;
  username: string;
  email: string;
}

class AuthService {
  async login(username: string, password: string): Promise<LoginResponse> {
    const response = await axios.post(`${API_URL}/api/auth/signin`, {
      username,
      password
    });
    return response.data;
  }

  async register(username: string, email: string, password: string): Promise<string> {
    const response = await axios.post(`${API_URL}/api/auth/signup`, {
      username,
      email,
      password
    });
    return response.data;
  }

  getAuthHeader() {
    const token = localStorage.getItem('token');
    if (token) {
      return { Authorization: `Bearer ${token}` };
    }
    return {};
  }
}

export const authService = new AuthService();