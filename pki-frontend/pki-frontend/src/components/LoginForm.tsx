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
      <form onSubmit={handleSubmit} style={{ width: '100%', maxWidth: 400, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20, padding: 32, background: '#fff', borderRadius: 16, boxShadow: '0 4px 24px rgba(0,0,0,0.08)', border: '1px solid #e5e7eb' }}>
        <h2 style={{ fontSize: 24, fontWeight: 700, textAlign: 'center', marginBottom: 8, color: '#222' }}>Sign In</h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label htmlFor="email" style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>Email</label>
          <input
            type="email"
            name="email"
            id="email"
            placeholder="Enter your email"
            value={form.email}
            onChange={handleChange}
            style={{ padding: 12, borderRadius: 8, border: '1px solid #d1d5db', fontSize: 15, color: 'black' }}
          />
          {errors.email && <span style={{ color: '#e53e3e', fontSize: 12 }}>{errors.email}</span>}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <label htmlFor="password" style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>Password</label>
          <input
            type="password"
            name="password"
            id="password"
            placeholder="Enter your password"
            value={form.password}
            onChange={handleChange}
            style={{ padding: 12, borderRadius: 8, border: '1px solid #d1d5db', fontSize: 15, color: 'black' }}
          />
          {errors.password && <span style={{ color: '#e53e3e', fontSize: 12 }}>{errors.password}</span>}
        </div>
        <button type="submit" style={{ marginTop: 10, background: '#2563eb', color: '#fff', fontWeight: 700, fontSize: 16, padding: 12, borderRadius: 8, border: 'none', boxShadow: '0 2px 8px rgba(37,99,235,0.08)', cursor: 'pointer' }}>
          {mutation.status === 'pending' ? 'Logging in...' : 'Login'}
        </button>
      </form>
        <Toast.Root open={open} onOpenChange={setOpen} duration={4000} style={{ background: '#fff', border: '1px solid #e53e3e', borderRadius: 8, padding: 16, minWidth: 220, color: '#e53e3e', fontWeight: 600, fontSize: 15, boxShadow: '0 2px 8px rgba(229,62,62,0.08)' }}>
          <Toast.Title style={{ fontWeight: 700, marginBottom: 4 }}>Login Error</Toast.Title>
          <Toast.Description>{toastMsg}</Toast.Description>
        </Toast.Root>
    </>
  );
};
