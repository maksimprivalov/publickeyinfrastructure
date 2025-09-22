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
      <form 
        onSubmit={handleSubmit} 
        style={{ 
          width: '100%', 
          maxWidth: 600, 
          margin: '0 auto', 
          display: 'flex', 
          flexDirection: 'column', 
          gap: 20, 
          padding: '32px 24px', 
          background: '#fff', 
          borderRadius: 16, 
          boxShadow: '0 4px 24px rgba(0,0,0,0.08)', 
          border: '1px solid #e5e7eb'
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
          Create Account
        </h2>
        
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', 
          gap: 20 
        }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
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
                  transition: 'border-color 0.2s',
                  outline: 'none'
                }}
              />
              {errors.email && <span style={{ color: '#ef4444', fontSize: 12, fontWeight: 500 }}>{errors.email}</span>}
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label 
                htmlFor="name" 
                style={{ fontSize: 14, fontWeight: 600, color: '#374151', marginBottom: 4 }}
              >
                First Name
              </label>
              <input
                type="text"
                name="name"
                id="name"
                placeholder="Enter your first name"
                value={form.name}
                onChange={handleChange}
                style={{ 
                  padding: '12px 16px', 
                  borderRadius: 8, 
                  border: errors.name ? '2px solid #ef4444' : '1px solid #d1d5db', 
                  fontSize: 15, 
                  color: '#1f2937',
                  transition: 'border-color 0.2s',
                  outline: 'none'
                }}
              />
              {errors.name && <span style={{ color: '#ef4444', fontSize: 12, fontWeight: 500 }}>{errors.name}</span>}
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label 
                htmlFor="surname" 
                style={{ fontSize: 14, fontWeight: 600, color: '#374151', marginBottom: 4 }}
              >
                Last Name
              </label>
              <input
                type="text"
                name="surname"
                id="surname"
                placeholder="Enter your last name"
                value={form.surname}
                onChange={handleChange}
                style={{ 
                  padding: '12px 16px', 
                  borderRadius: 8, 
                  border: errors.surname ? '2px solid #ef4444' : '1px solid #d1d5db', 
                  fontSize: 15, 
                  color: '#1f2937',
                  transition: 'border-color 0.2s',
                  outline: 'none'
                }}
              />
              {errors.surname && <span style={{ color: '#ef4444', fontSize: 12, fontWeight: 500 }}>{errors.surname}</span>}
            </div>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label 
                htmlFor="organization" 
                style={{ fontSize: 14, fontWeight: 600, color: '#374151', marginBottom: 4 }}
              >
                Organization
              </label>
              <input
                type="text"
                name="organization"
                id="organization"
                placeholder="Enter organization name"
                value={form.organization}
                onChange={handleChange}
                style={{ 
                  padding: '12px 16px', 
                  borderRadius: 8, 
                  border: errors.organization ? '2px solid #ef4444' : '1px solid #d1d5db', 
                  fontSize: 15, 
                  color: '#1f2937',
                  transition: 'border-color 0.2s',
                  outline: 'none'
                }}
              />
              {errors.organization && <span style={{ color: '#ef4444', fontSize: 12, fontWeight: 500 }}>{errors.organization}</span>}
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label 
                htmlFor="password" 
                style={{ fontSize: 14, fontWeight: 600, color: '#374151', marginBottom: 4 }}
              >
                Password
              </label>
              <input
                type="password"
                name="password"
                id="password"
                placeholder="Enter password"
                value={form.password}
                onChange={handleChange}
                style={{ 
                  padding: '12px 16px', 
                  borderRadius: 8, 
                  border: errors.password ? '2px solid #ef4444' : '1px solid #d1d5db', 
                  fontSize: 15, 
                  color: '#1f2937',
                  transition: 'border-color 0.2s',
                  outline: 'none'
                }}
              />
              {errors.password && <span style={{ color: '#ef4444', fontSize: 12, fontWeight: 500 }}>{errors.password}</span>}
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label 
                htmlFor="confirmPassword" 
                style={{ fontSize: 14, fontWeight: 600, color: '#374151', marginBottom: 4 }}
              >
                Confirm Password
              </label>
              <input
                type="password"
                name="confirmPassword"
                id="confirmPassword"
                placeholder="Confirm your password"
                value={form.confirmPassword}
                onChange={handleChange}
                style={{ 
                  padding: '12px 16px', 
                  borderRadius: 8, 
                  border: errors.confirmPassword ? '2px solid #ef4444' : '1px solid #d1d5db', 
                  fontSize: 15, 
                  color: '#1f2937',
                  transition: 'border-color 0.2s',
                  outline: 'none'
                }}
              />
              {errors.confirmPassword && <span style={{ color: '#ef4444', fontSize: 12, fontWeight: 500 }}>{errors.confirmPassword}</span>}
            </div>
          </div>
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
          {mutation.status === 'pending' ? 'Creating account...' : 'Create Account'}
        </button>
      </form>

      <Toast.Root 
        open={open} 
        onOpenChange={setOpen} 
        duration={4000} 
        style={{ 
          background: '#fff', 
          border: mutation.status === 'success' ? '1px solid #22c55e' : '1px solid #ef4444', 
          borderRadius: 8, 
          padding: 16, 
          minWidth: 280, 
          color: mutation.status === 'success' ? '#22c55e' : '#ef4444', 
          fontWeight: 500, 
          fontSize: 14, 
          boxShadow: mutation.status === 'success' 
            ? '0 4px 12px rgba(34, 197, 94, 0.15)' 
            : '0 4px 12px rgba(239, 68, 68, 0.15)',
          zIndex: 1000
        }}
      >
        <Toast.Title style={{ 
          fontWeight: 700, 
          marginBottom: 4,
          fontSize: 15
        }}>
          {mutation.status === 'success' ? 'Registration Successful' : 'Registration Error'}
        </Toast.Title>
        <Toast.Description style={{ fontSize: 13, lineHeight: 1.4 }}>
          {toastMsg}
        </Toast.Description>
      </Toast.Root>
    </>
  );
};
