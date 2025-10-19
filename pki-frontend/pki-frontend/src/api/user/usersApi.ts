import type { AuthAxiosRequestConfig, IApi } from "../data/apiTypes";
import apiClient from "../../interceptor/interceptor";

// User management interfaces (for admin functionality)
export interface User {
  id: number;
  email: string;
  name: string;
  surname: string;
  organizationName: string;
  role: 'USER' | 'CAUSER' | 'ADMIN';
  isActive: boolean;
  suspendedSince: string | null;
}

export interface CreateUserRequest {
  email: string;
  name: string;
  surname: string;
  organizationName: string;
  role: 'USER' | 'CAUSER' | 'ADMIN';
  password: string;
}

export interface UpdateUserRequest {
  name?: string;
  surname?: string;
  organizationName?: string;
  role?: 'USER' | 'CAUSER' | 'ADMIN';
  isActive?: boolean;
}

class UsersApi implements IApi {
    private baseUrl = "/api/users";

    get(requestConfig?: AuthAxiosRequestConfig) {
        const { url, ...config } = requestConfig || {};
        return apiClient.get(url || '', config);
    }
    
    post(requestConfig?: AuthAxiosRequestConfig) {
        const { url, data, ...config } = requestConfig || {};
        return apiClient.post(url || '', data, config);
    }
    
    put(requestConfig?: AuthAxiosRequestConfig) {
        const { url, data, ...config } = requestConfig || {};
        return apiClient.put(url || '', data, config);
    }
    
    delete(requestConfig?: AuthAxiosRequestConfig) {
        const { url, ...config } = requestConfig || {};
        return apiClient.delete(url || '', config);
    }

    // === User Management methods (Mock implementations for now) ===

    // Get all users (Admin only)
    async getAllUsers(): Promise<User[]> {
        // Mock implementation since backend doesn't have this endpoint yet
        return new Promise((resolve) => {
            setTimeout(() => {
                const mockUsers: User[] = [
                    {
                        id: 1,
                        email: 'admin@pki.local',
                        name: 'Administrator',
                        surname: 'System',
                        organizationName: 'PKI Organization',
                        role: 'ADMIN',
                        isActive: true,
                        suspendedSince: null
                    },
                    {
                        id: 2,
                        email: 'operator@pki.local',
                        name: 'CA',
                        surname: 'Operator',
                        organizationName: 'PKI Organization',
                        role: 'CAUSER',
                        isActive: true,
                        suspendedSince: null
                    },
                    {
                        id: 3,
                        email: 'user@pki.local',
                        name: 'Regular',
                        surname: 'User',
                        organizationName: 'External Organization',
                        role: 'USER',
                        isActive: false,
                        suspendedSince: '2024-01-15T10:30:00Z'
                    }
                ];
                resolve(mockUsers);
            }, 500);
        });
    }

    // Create user (Admin only)
    async createUser(userData: CreateUserRequest): Promise<User> {
        // Mock implementation
        return new Promise((resolve) => {
            setTimeout(() => {
                const newUser: User = {
                    id: Math.floor(Math.random() * 1000),
                    email: userData.email,
                    name: userData.name,
                    surname: userData.surname,
                    organizationName: userData.organizationName,
                    role: userData.role,
                    isActive: true,
                    suspendedSince: null
                };
                resolve(newUser);
            }, 500);
        });
    }

    // Update user (Admin only)
    async updateUser(id: number, updates: UpdateUserRequest): Promise<User> {
        // Mock implementation
        return new Promise((resolve) => {
            setTimeout(() => {
                const updatedUser: User = {
                    id,
                    email: `user${id}@pki.local`,
                    name: updates.name || 'Updated',
                    surname: updates.surname || 'User',
                    organizationName: updates.organizationName || 'Updated Organization',
                    role: updates.role || 'USER',
                    isActive: updates.isActive !== undefined ? updates.isActive : true,
                    suspendedSince: updates.isActive === false ? new Date().toISOString() : null
                };
                resolve(updatedUser);
            }, 500);
        });
    }

    // Toggle user status (Admin only)
    async toggleUserStatus(id: number): Promise<User> {
        // Mock implementation
        return new Promise((resolve) => {
            setTimeout(() => {
                const updatedUser: User = {
                    id,
                    email: `user${id}@pki.local`,
                    name: 'Test',
                    surname: 'User',
                    organizationName: 'Test Organization',
                    role: 'USER',
                    isActive: Math.random() > 0.5,
                    suspendedSince: Math.random() > 0.5 ? new Date().toISOString() : null
                };
                resolve(updatedUser);
            }, 500);
        });
    }

    // Get user by ID
    async getUserById(id: number): Promise<User> {
        // Mock implementation
        return new Promise((resolve) => {
            setTimeout(() => {
                const user: User = {
                    id,
                    email: `user${id}@pki.local`,
                    name: 'Test',
                    surname: 'User',
                    organizationName: 'Test Organization',
                    role: 'USER',
                    isActive: true,
                    suspendedSince: null
                };
                resolve(user);
            }, 500);
        });
    }
}

const usersApi = new UsersApi();
export default usersApi;
