import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from './context/AuthContext';
import { supabase, isSupabaseConfigured } from './lib/supabase';
import type { Book, Series, DashboardStats } from './types/database';
import { LoginView } from './components/LoginView';
import { Navbar } from './components/Navbar';
import { StatsCards } from './components/StatsCards';
import { BookTable } from './components/BookTable';
import { SeriesView } from './components/SeriesView';
import { UploadModal } from './components/UploadModal';
import { EditBookModal } from './components/EditBookModal';
import { DeleteConfirmModal } from './components/DeleteConfirmModal';
import { AlertCircle, BookOpen } from 'lucide-react';

export const AppContent: React.FC = () => {
  const { user, isAdmin, loading: authLoading } = useAuth();

  const [activeTab, setActiveTab] = useState<'books' | 'series'>('books');
  const [books, setBooks] = useState<Book[]>([]);
  const [series, setSeries] = useState<Series[]>([]);
  const [loadingBooks, setLoadingBooks] = useState<boolean>(true);
  const [loadingSeries, setLoadingSeries] = useState<boolean>(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // Modals state
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [editingBook, setEditingBook] = useState<Book | null>(null);
  const [deletingBook, setDeletingBook] = useState<Book | null>(null);

  const fetchBooks = useCallback(async () => {
    if (!isAdmin) return;
    try {
      setLoadingBooks(true);
      setFetchError(null);

      const { data, error } = await supabase
        .from('books')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) {
        throw error;
      }

      setBooks((data as Book[]) || []);
    } catch (err: any) {
      console.error('Error fetching books:', err);
      setFetchError(err.message || 'Could not load library books from database.');
    } finally {
      setLoadingBooks(false);
    }
  }, [isAdmin]);

  const fetchSeries = useCallback(async () => {
    if (!isAdmin) return;
    try {
      setLoadingSeries(true);
      const { data, error } = await supabase
        .from('series')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) {
        console.warn('Could not fetch series (table might not exist yet):', error);
        return;
      }

      setSeries((data as Series[]) || []);
    } catch (err: any) {
      console.error('Error fetching series:', err);
    } finally {
      setLoadingSeries(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    if (isAdmin) {
      fetchBooks();
      fetchSeries();
    }
  }, [isAdmin, fetchBooks, fetchSeries]);

  const stats: DashboardStats = {
    total: books.length,
    published: books.filter((b) => b.is_published).length,
    unpublished: books.filter((b) => !b.is_published).length,
  };

  const handleBookCreated = (newBook: Book) => {
    setBooks((prev) => [newBook, ...prev]);
  };

  const handleBookUpdated = (updatedBook: Book) => {
    setBooks((prev) => prev.map((b) => (b.id === updatedBook.id ? updatedBook : b)));
  };

  const handleBookDeleted = (deletedBookId: string) => {
    setBooks((prev) => prev.filter((b) => b.id !== deletedBookId));
  };

  const handleSeriesUpdated = (updatedSeries: Series) => {
    setSeries((prev) => prev.map((s) => (s.id === updatedSeries.id ? updatedSeries : s)));
  };

  const handleSeriesDeleted = (deletedSeriesId: string) => {
    setSeries((prev) => prev.filter((s) => s.id !== deletedSeriesId));
  };

  if (authLoading) {
    return (
      <div className="full-screen-loader">
        <div className="loader-box">
          <BookOpen size={48} className="loader-brand-icon" />
          <span className="spinner large-spinner"></span>
          <p>Verifying administrator credentials...</p>
        </div>
      </div>
    );
  }

  if (!user || !isAdmin) {
    return <LoginView />;
  }

  return (
    <div className="dashboard-layout">
      <Navbar
        onOpenUpload={() => setIsUploadOpen(true)}
        activeTab={activeTab}
        onTabChange={(tab) => setActiveTab(tab)}
      />

      <main className="main-content">
        <div className="content-container">
          <div className="page-header">
            <div>
              <h1 className="page-title">
                {activeTab === 'books' ? 'Catalog & Documents' : 'Series Collections'}
              </h1>
              <p className="page-description">
                {activeTab === 'books'
                  ? 'Manage, publish, and upload official organizational library documents.'
                  : 'Organize multi-part publications and annual volumes into ordered series.'}
              </p>
            </div>
            <div className="header-badge">
              <span>Cloud Status:</span>
              <strong className={isSupabaseConfigured ? 'status-connected' : 'status-disconnected'}>
                {isSupabaseConfigured ? 'Supabase Connected' : 'Configuration Missing'}
              </strong>
            </div>
          </div>

          {fetchError && (
            <div className="alert alert-error">
              <AlertCircle size={20} />
              <span>{fetchError}</span>
            </div>
          )}

          {activeTab === 'books' ? (
            <>
              <StatsCards stats={stats} loading={loadingBooks} />

              <section className="catalog-section">
                <div className="section-title-bar">
                  <h2>Library Documents</h2>
                  <span className="section-subtitle">
                    Members on Android see all <strong>Published</strong> items in real-time.
                  </span>
                </div>

                <BookTable
                  books={books}
                  series={series}
                  loading={loadingBooks}
                  onRefresh={fetchBooks}
                  onOpenUpload={() => setIsUploadOpen(true)}
                  onEditBook={(book) => setEditingBook(book)}
                  onDeleteBook={(book) => setDeletingBook(book)}
                  onBookUpdated={handleBookUpdated}
                />
              </section>
            </>
          ) : (
            <section className="catalog-section">
              <SeriesView
                series={series}
                books={books}
                loading={loadingSeries}
                onRefresh={() => {
                  fetchBooks();
                  fetchSeries();
                }}
                onSeriesUpdated={handleSeriesUpdated}
                onSeriesDeleted={handleSeriesDeleted}
                onBookUpdated={handleBookUpdated}
              />
            </section>
          )}
        </div>
      </main>

      {/* Modals */}
      <UploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onSuccess={handleBookCreated}
      />

      <EditBookModal
        book={editingBook}
        isOpen={Boolean(editingBook)}
        onClose={() => setEditingBook(null)}
        onSuccess={handleBookUpdated}
      />

      <DeleteConfirmModal
        book={deletingBook}
        isOpen={Boolean(deletingBook)}
        onClose={() => setDeletingBook(null)}
        onSuccess={handleBookDeleted}
      />
    </div>
  );
};

export default function App() {
  return <AppContent />;
}
