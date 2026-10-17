import React, { useState, useMemo } from 'react';
import type { Book, Series } from '../types/database';
import { supabase } from '../lib/supabase';
import {
  Search,
  FileText,
  CheckCircle,
  EyeOff,
  Edit2,
  Trash2,
  Globe,
  RefreshCw,
  FolderOpen,
  Layers
} from 'lucide-react';

interface BookTableProps {
  books: Book[];
  series?: Series[];
  loading: boolean;
  onRefresh: () => void;
  onOpenUpload: () => void;
  onEditBook: (book: Book) => void;
  onDeleteBook: (book: Book) => void;
  onBookUpdated: (book: Book) => void;
}

export const BookTable: React.FC<BookTableProps> = ({
  books,
  series = [],
  loading,
  onRefresh,
  onOpenUpload,
  onEditBook,
  onDeleteBook,
  onBookUpdated,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'published' | 'draft'>('all');
  const [togglingBookId, setTogglingBookId] = useState<string | null>(null);

  const filteredBooks = useMemo(() => {
    return books.filter((b) => {
      const matchesSearch =
        b.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (b.author && b.author.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus =
        statusFilter === 'all'
          ? true
          : statusFilter === 'published'
          ? b.is_published
          : !b.is_published;

      return matchesSearch && matchesStatus;
    });
  }, [books, searchQuery, statusFilter]);

  const handleTogglePublish = async (book: Book) => {
    try {
      setTogglingBookId(book.id);
      const newStatus = !book.is_published;

      const { data, error } = await supabase
        .from('books')
        .update({
          is_published: newStatus,
        })
        .eq('id', book.id)
        .select()
        .single();

      if (error) {
        alert(`Failed to update publication status: ${error.message}`);
        return;
      }

      onBookUpdated(data as Book);
    } catch (err: any) {
      alert(`Error: ${err.message}`);
    } finally {
      setTogglingBookId(null);
    }
  };

  const getFormatBadgeClass = (format: string) => {
    switch (format.toLowerCase()) {
      case 'pdf':
        return 'badge-format badge-pdf';
      case 'docx':
        return 'badge-format badge-docx';
      case 'txt':
        return 'badge-format badge-txt';
      default:
        return 'badge-format';
    }
  };

  const formatDate = (isoString: string) => {
    if (!isoString) return '—';
    const date = new Date(isoString);
    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="table-wrapper-card">
      <div className="table-controls-bar">
        <div className="search-filter-group">
          <div className="search-box">
            <Search size={18} className="search-icon" />
            <input
              type="text"
              placeholder="Search books by title or author..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              id="input-search-books"
            />
          </div>

          <div className="filter-buttons">
            <button
              className={`filter-btn ${statusFilter === 'all' ? 'active' : ''}`}
              onClick={() => setStatusFilter('all')}
            >
              All ({books.length})
            </button>
            <button
              className={`filter-btn ${statusFilter === 'published' ? 'active' : ''}`}
              onClick={() => setStatusFilter('published')}
            >
              Published ({books.filter((b) => b.is_published).length})
            </button>
            <button
              className={`filter-btn ${statusFilter === 'draft' ? 'active' : ''}`}
              onClick={() => setStatusFilter('draft')}
            >
              Drafts ({books.filter((b) => !b.is_published).length})
            </button>
          </div>
        </div>

        <div className="table-actions">
          <button
            onClick={onRefresh}
            className="secondary-btn refresh-btn"
            title="Refresh Catalog"
            disabled={loading}
          >
            <RefreshCw size={16} className={loading ? 'spin' : ''} />
            <span>Refresh</span>
          </button>
          <button
            onClick={onOpenUpload}
            className="primary-btn"
            id="btn-add-book-table"
          >
            + Add Book
          </button>
        </div>
      </div>

      <div className="table-responsive">
        <table className="books-table">
          <thead>
            <tr>
              <th>Title & Author</th>
              <th>Format</th>
              <th>Status</th>
              <th>Uploaded Date</th>
              <th className="th-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading && books.length === 0 ? (
              <tr>
                <td colSpan={5} className="table-status-cell">
                  <div className="table-loader">
                    <span className="spinner"></span>
                    <span>Loading library documents from database...</span>
                  </div>
                </td>
              </tr>
            ) : filteredBooks.length === 0 ? (
              <tr>
                <td colSpan={5} className="table-status-cell">
                  <div className="empty-catalog-state">
                    <FolderOpen size={40} className="empty-icon" />
                    <h4>No books found</h4>
                    <p>
                      {searchQuery
                        ? 'No books match your current search query.'
                        : 'Your library is empty. Click "+ Add Book" to upload your first document.'}
                    </p>
                    {searchQuery ? (
                      <button
                        onClick={() => setSearchQuery('')}
                        className="secondary-btn small-btn"
                      >
                        Clear search
                      </button>
                    ) : (
                      <button onClick={onOpenUpload} className="primary-btn small-btn">
                        Upload Document
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ) : (
              filteredBooks.map((book) => {
                const isToggling = togglingBookId === book.id;
                return (
                  <tr key={book.id}>
                    <td className="td-book-info">
                      <div className="book-title-cell">
                        <div className="book-mini-icon">
                          <FileText size={18} />
                        </div>
                        <div className="book-text-meta">
                          <span className="book-title">{book.title}</span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                            <span className="book-author">
                              {book.author ? book.author : 'Author not specified'}
                            </span>
                            {book.series_id && (
                              <span style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.25rem',
                                fontSize: '0.725rem',
                                background: 'rgba(59, 130, 246, 0.12)',
                                color: 'var(--primary)',
                                border: '1px solid rgba(59, 130, 246, 0.25)',
                                padding: '1px 6px',
                                borderRadius: 'var(--radius-sm)',
                                fontWeight: 600
                              }}>
                                <Layers size={11} />
                                <span>
                                  {series.find((s) => s.id === book.series_id)?.title || 'Series'} #{book.series_order}
                                </span>
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className={getFormatBadgeClass(book.file_type)}>
                        {book.file_type.toUpperCase()}
                      </span>
                    </td>

                    <td>
                      <button
                        onClick={() => handleTogglePublish(book)}
                        disabled={isToggling}
                        className={`status-toggle-badge ${
                          book.is_published ? 'published' : 'draft'
                        }`}
                        title={
                          book.is_published
                            ? 'Click to unpublish (hide from members)'
                            : 'Click to publish (make visible to members)'
                        }
                      >
                        {isToggling ? (
                          <span className="spinner small-spinner"></span>
                        ) : book.is_published ? (
                          <>
                            <Globe size={13} />
                            <span>Published</span>
                          </>
                        ) : (
                          <>
                            <EyeOff size={13} />
                            <span>Draft</span>
                          </>
                        )}
                      </button>
                    </td>

                    <td className="td-date">{formatDate(book.created_at)}</td>

                    <td className="td-actions">
                      <div className="action-button-group">
                        <button
                          onClick={() => handleTogglePublish(book)}
                          disabled={isToggling}
                          className="action-icon-btn toggle-action-btn"
                          title={book.is_published ? 'Unpublish' : 'Publish'}
                        >
                          {book.is_published ? (
                            <EyeOff size={16} />
                          ) : (
                            <CheckCircle size={16} />
                          )}
                        </button>
                        <button
                          onClick={() => onEditBook(book)}
                          className="action-icon-btn edit-action-btn"
                          title="Edit Book Details / Replace Document"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          onClick={() => onDeleteBook(book)}
                          className="action-icon-btn delete-action-btn"
                          title="Delete Book"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="table-footer">
        <span>
          Showing <strong>{filteredBooks.length}</strong> of <strong>{books.length}</strong>{' '}
          total books
        </span>
      </div>
    </div>
  );
};
