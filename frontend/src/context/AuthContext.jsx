import React, { createContext, useState, useContext, useEffect } from 'react';
import { authService } from '../services';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  console.log('🔐 AuthProvider initialized');

  useEffect(() => {
    // Check if user is logged in on mount
    const storedUser = authService.getCurrentUser();
    const token = localStorage.getItem('accessToken');
    
    console.log('🔍 Checking stored user:', storedUser ? 'Found' : 'Not found');
    
    if (storedUser && token) {
      setUser(storedUser);
      setIsAuthenticated(true);
    }
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    try {
      const response = await authService.login(credentials);
      
      console.log('🔐 Login response:', response);
      
      // Backend returns: { accessToken, refreshToken, email, name, role, userId }
      // Convert to user object
      const userData = {
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role,
        cityId: response.cityId || null
      };
      
      // Store tokens and user data
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      localStorage.setItem('user', JSON.stringify(userData));
      
      setUser(userData);
      setIsAuthenticated(true);
      
      console.log('✅ Login successful:', userData);
      return { success: true, user: userData };
    } catch (error) {
      console.error('❌ Login failed:', error);
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Login failed. Please try again.';
      return { 
        success: false, 
        error: errorMessage
      };
    }
  };

  const signup = async (userData) => {
    try {
      const response = await authService.signup(userData);
      
      console.log('🔐 Signup response:', response);
      
      // Backend returns: { accessToken, refreshToken, email, name, role, userId }
      // Convert to user object
      const userObj = {
        id: response.userId,
        name: response.name,
        email: response.email,
        role: response.role,
        cityId: response.cityId || null
      };
      
      // Store tokens and user data
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      localStorage.setItem('user', JSON.stringify(userObj));
      
      setUser(userObj);
      setIsAuthenticated(true);
      
      console.log('✅ Signup successful:', userObj);
      return { success: true, user: userObj };
    } catch (error) {
      console.error('❌ Signup failed:', error);
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Signup failed. Please try again.';
      return { 
        success: false, 
        error: errorMessage
      };
    }
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    setIsAuthenticated(false);
  };

  const updateUser = (updatedUserData) => {
    // Merge updated data with existing user data
    const newUserData = { ...user, ...updatedUserData };
    setUser(newUserData);
    
    // Update localStorage
    localStorage.setItem('user', JSON.stringify(newUserData));
    
    console.log('✅ User updated:', newUserData);
  };

  const hasRole = (role) => {
    return user?.role === role;
  };

  const isAdmin = () => hasRole('ADMIN');
  const isTheaterOwner = () => hasRole('THEATER_OWNER');
  const isUser = () => hasRole('USER');

  const value = {
    user,
    loading,
    isAuthenticated,
    login,
    signup,
    logout,
    updateUser,
    hasRole,
    isAdmin,
    isTheaterOwner,
    isUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
