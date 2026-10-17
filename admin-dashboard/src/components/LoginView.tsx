import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { isSupabaseConfigured } from '../lib/supabase';
import { BookOpen, ShieldAlert, KeyRound, Mail, ArrowRight, AlertTriangle } from 'lucide-react';

export const LoginView: React.FC = () => {
  const { login, error: authError } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    if (!email.trim() || !password.trim()) {
      setLocalError('Please enter both email and password.');
      return;
    }

    setIsSubmitting(true);
    const result = await login(email, password);
    setIsSubmitting(false);

    if (!result.success && result.error) {
      setLocalError(result.error);
    }
  };

  const displayError = localError || authError;

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="brand-icon">
            <BookOpen size={32} />
          </div>
          <h1>Organizational Digital Library</h1>
          <p className="login-subtitle">Administrator Access Portal</p>
        </div>

        {!isSupabaseConfigured && (
          <div className="config-alert">
            <div className="config-alert-header">
              <AlertTriangle size={18} />
              <span>Supabase Configuration Required</span>
            </div>
            <p>
              Please create an <code>.env</code> file in <code>admin-dashboard/</code> with your{' '}
              <code>VITE_SUPABASE_URL</code> and <code>VITE_SUPABASE_ANON_KEY</code>.
            </p>
          </div>
        )}

        {displayError && (
          <div className="error-banner">
            <ShieldAlert size={18} />
            <span>{displayError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="email">Administrator Email</label>
            <div className="input-wrapper">
              <Mail size={18} className="input-icon" />
              <input
                id="email"
                type="email"
                placeholder="admin@organization.org"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <div className="input-wrapper">
              <KeyRound size={18} className="input-icon" />
              <input
                id="password"
                type="password"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
                disabled={isSubmitting}
              />
            </div>
          </div>

          <button
            type="submit"
            className="submit-btn"
            disabled={isSubmitting || !isSupabaseConfigured}
          >
            {isSubmitting ? (
              <span className="spinner-label">
                <span className="spinner"></span> Signing In...
              </span>
            ) : (
              <>
                <span>Enter Admin Dashboard</span>
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        <div className="login-footer">
          <h4>First Administrator Setup:</h4>
          <ol>
            <li>Sign up this email address via your Supabase Authentication dashboard or the mobile app.</li>
            <li>In Supabase SQL Editor, execute:
              <pre className="code-snippet">SELECT public.promote_user_to_admin('admin@organization.org');</pre>
            </li>
            <li>Log in with your administrator credentials above.</li>
          </ol>
        </div>
      </div>
    </div>
  );
};
