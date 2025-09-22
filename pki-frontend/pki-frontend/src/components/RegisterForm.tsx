import React, { useState } from 'react';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import userApi from '../api/user/userApi';
import * as Toast from '@radix-ui/react-toast';
// import { history } from '../services/history';

const registerSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
  confirmPassword: z.string().min(6),
  name: z.string().min(1, "Name is required"),
  surname: z.string().min(1, "Surname is required"),
  organization: z.string().min(1, "Organization is required")
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});
 

export const RegisterForm = () => {
  const [form, setForm] = useState({ email: '', password: '', confirmPassword: '', name: '', surname: '', organization: '' });
  const [errors, setErrors] = useState<{ email?: string; password?: string; confirmPassword?: string; name?: string; surname?: string; organization?: string }>({});
  const [open, setOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState('');

  const mutation = useMutation({
    mutationFn: async (data: { email: string; password: string; name: string; surname: string; organization: string }) => {
      return userApi.post({
        url: '/api/users/register',
        data,
      });
    },
    onSuccess: (response: any) => {
      if (response) {
        setToastMsg("User created successfuly");
        setOpen(true);
      }

    },
    onError: (error: any) => {
      console.log(error)
      setToastMsg(error.response.data);
      setOpen(true);
    }
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const result = registerSchema.safeParse(form);
    if (!result.success) {
      const fieldErrors: { email?: string; password?: string; confirmPassword?: string; name?: string; surname?: string; organization?: string } = {};
      result.error.issues.forEach((err: any) => {
        if (err.path[0]) fieldErrors[err.path[0] as keyof typeof fieldErrors] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }
    setErrors({});
    mutation.mutate({ email: form.email, password: form.password, name: form.name, surname: form.surname, organization: form.organization });
  };

  return (
    <>
    <form onSubmit={handleSubmit} style={{ width: '100%', maxWidth: 500, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20, padding: 32, background: '#fff', borderRadius: 16, boxShadow: '0 4px 24px rgba(0,0,0,0.08)', border: '1px solid #e5e7eb' }}>
      <h2 style={{ fontSize: 24, fontWeight: 700, textAlign: 'center', marginBottom: 8, color: '#222' }}>Create Account</h2>
      <div style={{ display: 'flex', gap: 20 }}>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
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
            <label htmlFor="name" style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>Name</label>
            <input
              type="text"
              name="name"
              id="name"
              placeholder="Enter your name"
              value={form.name}
              onChange={handleChange}
              style={{ padding: 12, borderRadius: 8, border: '1px solid #d1d5db', fontSize: 15, color: 'black' }}
            />
            {errors.name && <span style={{ color: '#e53e3e', fontSize: 12 }}>{errors.name}</span>}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label htmlFor="surname" style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>Surname</label>
            <input
              type="text"
              name="surname"
              id="surname"
              placeholder="Enter your surname"
              value={form.surname}
              onChange={handleChange}
              style={{ padding: 12, borderRadius: 8, border: '1px solid #d1d5db', fontSize: 15, color: 'black' }}
            />
            {errors.surname && <span style={{ color: '#e53e3e', fontSize: 12 }}>{errors.surname}</span>}
          </div>
        </div>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label htmlFor="organization" style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>Organization</label>
            <input
              type="text"
              name="organization"
              id="organization"
              placeholder="Enter your organization"
              value={form.organization}
              onChange={handleChange}
              style={{ padding: 12, borderRadius: 8, border: '1px solid #d1d5db', fontSize: 15, color: 'black' }}
            />
            {errors.organization && <span style={{ color: '#e53e3e', fontSize: 12 }}>{errors.organization}</span>}
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
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label htmlFor="confirmPassword" style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>Confirm Password</label>
            <input
              type="password"
              name="confirmPassword"
              id="confirmPassword"
              placeholder="Confirm your password"
              value={form.confirmPassword}
              onChange={handleChange}
              style={{ padding: 12, borderRadius: 8, border: '1px solid #d1d5db', fontSize: 15, color: 'black' }}
            />
            {errors.confirmPassword && <span style={{ color: '#e53e3e', fontSize: 12 }}>{errors.confirmPassword}</span>}
          </div>
        </div>
      </div>
      <button type="submit" style={{ marginTop: 10, background: '#2563eb', color: '#fff', fontWeight: 700, fontSize: 16, padding: 12, borderRadius: 8, border: 'none', boxShadow: '0 2px 8px rgba(37,99,235,0.08)', cursor: 'pointer' }}>
        {mutation.status === 'pending' ? 'Registering...' : 'Register'}
      </button>
      {mutation.status === 'error' && <span style={{ color: '#e53e3e', fontSize: 12, textAlign: 'center', marginTop: 8 }}>Registration failed</span>}
    </form>
    <Toast.Root open={open} onOpenChange={setOpen} duration={4000} style={{ background: '#fff', border: '1px solid #e53e3e', borderRadius: 8, padding: 16, minWidth: 220, color: '#e53e3e', fontWeight: 600, fontSize: 15, boxShadow: '0 2px 8px rgba(229,62,62,0.08)' }}>
      <Toast.Title style={{ fontWeight: 700, marginBottom: 4 }}>Register Error</Toast.Title>
      <Toast.Description>{toastMsg}</Toast.Description>
    </Toast.Root>
    </>
  );
};
