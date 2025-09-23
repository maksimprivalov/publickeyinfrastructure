import { useState, useEffect } from 'react';

export type UserRole = 'Admin' | 'CAUser' | 'User';

export interface DecodedToken {
  iss: string;
  sub: string;
  aud: string;
  iat: number;
  role: UserRole;
  userId: number;
  type: string;
  exp: number;
}

function parseJwt(token: string): DecodedToken | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function useUserRole(): UserRole | null {
  const [role, setRole] = useState<UserRole | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (token) {
      const decoded = parseJwt(token);
      setRole(decoded?.role || null);
    } else {
      setRole(null);
    }
  }, []);

  return role;
}

export function useUserId(): number | null {
  const [userId, setUserId] = useState<number | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (token) {
      const decoded = parseJwt(token);
      setUserId(decoded?.userId || null);
    } else {
      setUserId(null);
    }
  }, []);

  return userId;
}
