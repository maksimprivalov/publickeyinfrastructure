import React, { useState } from 'react';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import userApi from '../api/user/userApi';
import * as Toast from '@radix-ui/react-toast';
import { history } from '../services/history';

const loginSchema = z.object({
  email: z.email(),
  password: z.string().min(6)
});

export const LoginForm = () => {
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [open, setOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');

  const mutation = useMutation({
    mutationFn: async (data: { email: string; password: string }) => {
      return userApi.post({
        url: '/api/users/login',
        data,
      });
    },
    onSuccess: (response: any) => {
      const { accessToken, refreshToken } = response.data;
      if (accessToken && refreshToken) {
        console.log(accessToken)
        localStorage.setItem('access_token', accessToken);
        localStorage.setItem('refresh_token', refreshToken);
        history.navigate("/"); // Redirect to home
      } else {
        setToastMsg("Invalid server response: tokens missing.");
        setOpen(true);
      }
    },
    onError: (error: any) => {
      console.log(error)
      setToastMsg(error.response.data);
      setOpen(true);
    },
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const result = loginSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: { email?: string; password?: string } = {};
      result.error.issues.forEach((err: any) => {
        if (err.path[0]) fieldErrors[err.path[0] as 'email' | 'password'] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }
    setErrors({});
    mutation.mutate(form);
  };

  return (
    <>
      <form 
        onSubmit={handleSubmit} 
        style={{ 
          width: '100%', 
          maxWidth: 400, 
          margin: '0 auto', 
          display: 'flex', 
          flexDirection: 'column', 
          gap: 20, 
          padding: '32px 24px', 
          background: '#fff', 
          borderRadius: 16, 
          boxShadow: '0 4px 24px rgba(0,0,0,0.08)', 
          border: '1px solid #e5e7eb',
          '@media (max-width: 480px)': {
            padding: '24px 16px',
            gap: 16
          }
        }}
      >
        <h2 style={{ 
          fontSize: 24, 
          fontWeight: 700, 
          textAlign: 'center', 
          marginBottom: 8, 
          color: '#1f2937',
          letterSpacing: '-0.025em'
        }}>
          Sign In
        </h2>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label 
            htmlFor="email" 
            style={{ 
              fontSize: 14, 
              fontWeight: 600, 
              color: '#374151',
              marginBottom: 4
            }}
          >
            Email
          </label>
          <input
            type="email"
            name="email"
            id="email"
            placeholder="Enter your email"
            value={form.email}
            onChange={handleChange}
            style={{ 
              padding: '12px 16px', 
              borderRadius: 8, 
              border: errors.email ? '2px solid #ef4444' : '1px solid #d1d5db', 
              fontSize: 15, 
              color: '#1f2937',
              transition: 'border-color 0.2s, box-shadow 0.2s',
              outline: 'none',
              '&:focus': {
                borderColor: '#2563eb',
                boxShadow: '0 0 0 3px rgba(37, 99, 235, 0.1)'
              }
            }}
          />
          {errors.email && (
            <span style={{ 
              color: '#ef4444', 
              fontSize: 12, 
              marginTop: 2,
              fontWeight: 500 
            }}>
              {errors.email}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label 
            htmlFor="password" 
            style={{ 
              fontSize: 14, 
              fontWeight: 600, 
              color: '#374151',
              marginBottom: 4
            }}
          >
            Password
          </label>
          <input
            type="password"
            name="password"
            id="password"
            placeholder="Enter your password"
            value={form.password}
            onChange={handleChange}
            style={{ 
              padding: '12px 16px', 
              borderRadius: 8, 
              border: errors.password ? '2px solid #ef4444' : '1px solid #d1d5db', 
              fontSize: 15, 
              color: '#1f2937',
              transition: 'border-color 0.2s, box-shadow 0.2s',
              outline: 'none'
            }}
          />
          {errors.password && (
            <span style={{ 
              color: '#ef4444', 
              fontSize: 12, 
              marginTop: 2,
              fontWeight: 500 
            }}>
              {errors.password}
            </span>
          )}
        </div>

        <button 
          type="submit" 
          disabled={mutation.status === 'pending'}
          style={{ 
            marginTop: 10, 
            background: mutation.status === 'pending' ? '#9ca3af' : '#2563eb', 
            color: '#fff', 
            fontWeight: 700, 
            fontSize: 16, 
            padding: '12px 24px', 
            borderRadius: 8, 
            border: 'none', 
            boxShadow: '0 2px 8px rgba(37,99,235,0.2)', 
            cursor: mutation.status === 'pending' ? 'not-allowed' : 'pointer',
            transition: 'all 0.2s ease',
            outline: 'none'
          }}
          onMouseOver={(e) => {
            if (mutation.status !== 'pending') {
              e.currentTarget.style.background = '#1d4ed8';
              e.currentTarget.style.transform = 'translateY(-1px)';
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(37,99,235,0.3)';
            }
          }}
          onMouseOut={(e) => {
            if (mutation.status !== 'pending') {
              e.currentTarget.style.background = '#2563eb';
              e.currentTarget.style.transform = 'translateY(0)';
              e.currentTarget.style.boxShadow = '0 2px 8px rgba(37,99,235,0.2)';
            }
          }}
        >
          {mutation.status === 'pending' ? 'Signing in...' : 'Sign In'}
        </button>
      </form>

      <Toast.Root 
        open={open} 
        onOpenChange={setOpen} 
        duration={4000} 
        style={{ 
          background: '#fff', 
          border: '1px solid #ef4444', 
          borderRadius: 8, 
          padding: 16, 
          minWidth: 280, 
          color: '#ef4444', 
          fontWeight: 500, 
          fontSize: 14, 
          boxShadow: '0 4px 12px rgba(239, 68, 68, 0.15)',
          zIndex: 1000
        }}
      >
        <Toast.Title style={{ 
          fontWeight: 700, 
          marginBottom: 4,
          fontSize: 15
        }}>
          Sign In Error
        </Toast.Title>
        <Toast.Description style={{ fontSize: 13, lineHeight: 1.4 }}>
          {toastMsg}
        </Toast.Description>
      </Toast.Root>
    </>
  );
};
