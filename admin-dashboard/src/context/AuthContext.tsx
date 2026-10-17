import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { User, Session } from '@supabase/supabase-js';
import { supabase, isSupabaseConfigured } from '../lib/supabase';
import type { Profile } from '../types/database';

interface AuthContextType {
  user: User | null;
  profile: Profile | null;
  session: Session | null;
  isAdmin: boolean;
  loading: boolean;
  error: string | null;
  login: (email: string, pass: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchProfile = useCallback(async (userId: string): Promise<Profile | null> => {
    try {
      const { data, error: profileErr } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', userId)
        .single();

      if (profileErr) {
        console.warn('Could not fetch user profile:', profileErr.message);
        return null;
      }
      return data as Profile;
    } catch (err: any) {
      console.warn('Error fetching profile:', err.message);
      return null;
    }
  }, []);

  const refreshProfile = useCallback(async () => {
    if (!user) return;
    const prof = await fetchProfile(user.id);
    setProfile(prof);
  }, [user, fetchProfile]);

  useEffect(() => {
    if (!isSupabaseConfigured) {
      setLoading(false);
      return;
    }

    // Initialize session
    supabase.auth.getSession().then(async ({ data: { session: initialSession } }) => {
      setSession(initialSession);
      const currentUser = initialSession?.user ?? null;
      setUser(currentUser);

      if (currentUser) {
        const prof = await fetchProfile(currentUser.id);
        setProfile(prof);
      }
      setLoading(false);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange(async (_event, newSession) => {
      setSession(newSession);
      const currentUser = newSession?.user ?? null;
      setUser(currentUser);

      if (currentUser) {
        const prof = await fetchProfile(currentUser.id);
        setProfile(prof);
      } else {
        setProfile(null);
      }
      setLoading(false);
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [fetchProfile]);

  const login = async (email: string, pass: string): Promise<{ success: boolean; error?: string }> => {
    setError(null);
    if (!isSupabaseConfigured) {
      const err = 'Supabase is not configured. Please add your credentials in .env.';
      setError(err);
      return { success: false, error: err };
    }

    try {
      const { data, error: signInErr } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password: pass,
      });

      if (signInErr) {
        setError(signInErr.message);
        return { success: false, error: signInErr.message };
      }

      if (!data.user) {
        const err = 'Authentication failed. No user returned.';
        setError(err);
        return { success: false, error: err };
      }

      // Check role authorization on the server
      const prof = await fetchProfile(data.user.id);
      if (!prof || prof.role !== 'admin') {
        // Sign out immediately if not admin
        await supabase.auth.signOut();
        const err = 'Access Denied: Only administrators are permitted to enter this dashboard.';
        setError(err);
        return { success: false, error: err };
      }

      setUser(data.user);
      setSession(data.session);
      setProfile(prof);
      return { success: true };
    } catch (err: any) {
      const msg = err.message || 'An unexpected error occurred during login.';
      setError(msg);
      return { success: false, error: msg };
    }
  };

  const logout = async () => {
    try {
      await supabase.auth.signOut();
    } finally {
      setUser(null);
      setSession(null);
      setProfile(null);
      setError(null);
    }
  };

  const isAdmin = profile?.role === 'admin';

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        session,
        isAdmin,
        loading,
        error,
        login,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
