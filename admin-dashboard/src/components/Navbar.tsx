import React from 'react';
import { useAuth } from '../context/AuthContext';
import { BookOpen, LogOut, ShieldCheck, Layers, FileText } from 'lucide-react';

interface NavbarProps {
  onOpenUpload: () => void;
  activeTab: 'books' | 'series';
  onTabChange: (tab: 'books' | 'series') => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenUpload, activeTab, onTabChange }) => {
  const { user, profile, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-container">
        <div className="navbar-brand" style={{ cursor: 'pointer' }} onClick={() => onTabChange('books')}>
          <div className="logo-badge">
            <BookOpen size={24} />
          </div>
          <div className="brand-text">
            <h2>Digital Library</h2>
            <span className="brand-tag">Administration</span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="nav-tabs" style={{ display: 'flex', gap: '0.5rem', background: 'var(--bg-card-subtle)', padding: '4px', borderRadius: 'var(--radius-md)' }}>
          <button
            onClick={() => onTabChange('books')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.45rem 0.9rem',
              borderRadius: 'var(--radius-sm)',
              border: 'none',
              fontSize: '0.875rem',
              fontWeight: 600,
              cursor: 'pointer',
              background: activeTab === 'books' ? 'var(--primary)' : 'transparent',
              color: activeTab === 'books' ? '#ffffff' : 'var(--text-muted)',
              transition: 'all 0.15s ease'
            }}
          >
            <FileText size={16} />
            <span>Catalog & Documents</span>
          </button>
          <button
            onClick={() => onTabChange('series')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.45rem 0.9rem',
              borderRadius: 'var(--radius-sm)',
              border: 'none',
              fontSize: '0.875rem',
              fontWeight: 600,
              cursor: 'pointer',
              background: activeTab === 'series' ? 'var(--primary)' : 'transparent',
              color: activeTab === 'series' ? '#ffffff' : 'var(--text-muted)',
              transition: 'all 0.15s ease'
            }}
          >
            <Layers size={16} />
            <span>Series Collections</span>
          </button>
        </div>

        <div className="navbar-actions">
          <button
            onClick={onOpenUpload}
            className="primary-btn upload-trigger-btn"
            id="btn-add-book-header"
          >
            + Add New Book
          </button>

          <div className="user-profile-widget">
            <div className="role-chip">
              <ShieldCheck size={14} />
              <span>{profile?.role || 'Admin'}</span>
            </div>
            <span className="user-email" title={user?.email || ''}>
              {user?.email}
            </span>
            <button
              onClick={logout}
              className="logout-btn"
              title="Log Out"
              id="btn-logout"
            >
              <LogOut size={16} />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
