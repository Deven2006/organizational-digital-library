import React, { useState } from 'react';
import { supabase } from '../lib/supabase';
import type { Book, Series } from '../types/database';
import {
  Layers,
  Plus,
  Edit2,
  Trash2,
  Eye,
  EyeOff,
  BookOpen,
  ArrowUp,
  ArrowDown,
  X,
  AlertTriangle
} from 'lucide-react';

interface SeriesViewProps {
  series: Series[];
  books: Book[];
  loading: boolean;
  onRefresh: () => void;
  onSeriesUpdated: (updated: Series) => void;
  onSeriesDeleted: (seriesId: string) => void;
  onBookUpdated: (updatedBook: Book) => void;
}

export const SeriesView: React.FC<SeriesViewProps> = ({
  series,
  books,
  loading,
  onRefresh,
  onSeriesUpdated,
  onSeriesDeleted,
  onBookUpdated
}) => {
  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingSeries, setEditingSeries] = useState<Series | null>(null);
  const [deletingSeries, setDeletingSeries] = useState<Series | null>(null);
  const [managingSeries, setManagingSeries] = useState<Series | null>(null);

  // Form states for Create/Edit
  const [formTitle, setFormTitle] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formIsPublished, setFormIsPublished] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Open Create Modal
  const openCreateModal = () => {
    setFormTitle('');
    setFormDescription('');
    setFormIsPublished(false);
    setFormError(null);
    setIsCreateModalOpen(true);
  };

  // Open Edit Modal
  const openEditModal = (s: Series) => {
    setEditingSeries(s);
    setFormTitle(s.title);
    setFormDescription(s.description || '');
    setFormIsPublished(s.is_published);
    setFormError(null);
  };

  // Handle Save Series (Create or Edit)
  const handleSaveSeries = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTitle.trim()) {
      setFormError('Series title is required.');
      return;
    }

    try {
      setIsSubmitting(true);
      setFormError(null);

      if (editingSeries) {
        // Update existing
        const { data, error } = await supabase
          .from('series')
          .update({
            title: formTitle.trim(),
            description: formDescription.trim() || null,
            is_published: formIsPublished,
            updated_at: new Date().toISOString()
          })
          .eq('id', editingSeries.id)
          .select()
          .single();

        if (error) throw error;
        onSeriesUpdated(data as Series);
        setEditingSeries(null);
      } else {
        // Create new
        const { error } = await supabase
          .from('series')
          .insert({
            title: formTitle.trim(),
            description: formDescription.trim() || null,
            is_published: formIsPublished
          });

        if (error) throw error;
        onRefresh();
        setIsCreateModalOpen(false);
      }
    } catch (err: any) {
      setFormError(err.message || 'Failed to save series.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Quick Toggle Publish
  const handleTogglePublish = async (s: Series) => {
    try {
      const { data, error } = await supabase
        .from('series')
        .update({
          is_published: !s.is_published,
          updated_at: new Date().toISOString()
        })
        .eq('id', s.id)
        .select()
        .single();

      if (error) throw error;
      onSeriesUpdated(data as Series);
    } catch (err: any) {
      alert(`Could not toggle publication: ${err.message}`);
    }
  };

  // Handle Delete Series
  const handleDeleteSeries = async () => {
    if (!deletingSeries) return;

    try {
      setIsSubmitting(true);
      // Because series_id on books has ON DELETE SET NULL,
      // deleting the series automatically detaches its books safely without deleting them.
      const { error } = await supabase
        .from('series')
        .delete()
        .eq('id', deletingSeries.id);

      if (error) throw error;

      onSeriesDeleted(deletingSeries.id);
      setDeletingSeries(null);
      onRefresh(); // Refresh books state to reflect unassigned books
    } catch (err: any) {
      alert(`Failed to delete series: ${err.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Series Books Helpers
  const getSeriesBooks = (seriesId: string) => {
    return books
      .filter((b) => b.series_id === seriesId)
      .sort((a, b) => (a.series_order ?? 0) - (b.series_order ?? 0));
  };

  const getIndependentBooks = () => {
    return books.filter((b) => !b.series_id);
  };

  // Add Book to Series
  const handleAddBookToSeries = async (bookId: string, seriesId: string) => {
    try {
      const currentSeriesBooks = getSeriesBooks(seriesId);
      const nextOrder = currentSeriesBooks.length > 0
        ? Math.max(...currentSeriesBooks.map((b) => b.series_order ?? 0)) + 1
        : 1;

      const { data, error } = await supabase
        .from('books')
        .update({
          series_id: seriesId,
          series_order: nextOrder,
          updated_at: new Date().toISOString()
        })
        .eq('id', bookId)
        .select()
        .single();

      if (error) throw error;
      onBookUpdated(data as Book);
    } catch (err: any) {
      alert(`Failed to add book to series: ${err.message}`);
    }
  };

  // Remove Book from Series (sets series_id to null)
  const handleRemoveBookFromSeries = async (bookId: string) => {
    try {
      const { data, error } = await supabase
        .from('books')
        .update({
          series_id: null,
          series_order: 0,
          updated_at: new Date().toISOString()
        })
        .eq('id', bookId)
        .select()
        .single();

      if (error) throw error;
      onBookUpdated(data as Book);
    } catch (err: any) {
      alert(`Failed to remove book from series: ${err.message}`);
    }
  };

  // Reorder Book (Move Up or Down)
  const handleReorderBook = async (
    targetBook: Book,
    direction: 'up' | 'down',
    seriesBooks: Book[]
  ) => {
    const currentIndex = seriesBooks.findIndex((b) => b.id === targetBook.id);
    if (currentIndex === -1) return;

    const swapIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
    if (swapIndex < 0 || swapIndex >= seriesBooks.length) return;

    const otherBook = seriesBooks[swapIndex];
    const targetNewOrder = otherBook.series_order ?? swapIndex;
    const otherNewOrder = targetBook.series_order ?? currentIndex;

    try {
      // Update both books with swapped orders
      const [res1, res2] = await Promise.all([
        supabase
          .from('books')
          .update({ series_order: targetNewOrder, updated_at: new Date().toISOString() })
          .eq('id', targetBook.id)
          .select()
          .single(),
        supabase
          .from('books')
          .update({ series_order: otherNewOrder, updated_at: new Date().toISOString() })
          .eq('id', otherBook.id)
          .select()
          .single()
      ]);

      if (res1.error) throw res1.error;
      if (res2.error) throw res2.error;

      onBookUpdated(res1.data as Book);
      onBookUpdated(res2.data as Book);
    } catch (err: any) {
      alert(`Failed to update book order: ${err.message}`);
    }
  };

  return (
    <div className="series-view">
      <div className="section-title-bar" style={{ marginBottom: '1.5rem' }}>
        <div>
          <h2>Book Series & Collections</h2>
          <span className="section-subtitle">
            Group related publications into ordered sequences. Deleting a series does not delete its books.
          </span>
        </div>
        <button
          onClick={openCreateModal}
          className="primary-btn"
          id="btn-create-series"
        >
          <Plus size={16} />
          <span>New Series</span>
        </button>
      </div>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Loading book series...
        </div>
      ) : series.length === 0 ? (
        <div className="empty-state-card" style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-lg)',
          padding: '3rem 2rem',
          textAlign: 'center'
        }}>
          <Layers size={48} style={{ color: 'var(--text-dim)', marginBottom: '1rem' }} />
          <h3>No Series Created Yet</h3>
          <p style={{ color: 'var(--text-muted)', maxWidth: '420px', margin: '0.5rem auto 1.5rem' }}>
            Group multiple publications into serialized collections (e.g. volumes, annual reports, multi-part manuals).
          </p>
          <button onClick={openCreateModal} className="primary-btn">
            <Plus size={16} />
            <span>Create First Series</span>
          </button>
        </div>
      ) : (
        <div className="series-grid" style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
          gap: '1.25rem'
        }}>
          {series.map((s) => {
            const sBooks = getSeriesBooks(s.id);
            return (
              <div
                key={s.id}
                className="series-card"
                style={{
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-lg)',
                  padding: '1.5rem',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  boxShadow: 'var(--shadow-sm)'
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.75rem', marginBottom: '0.75rem' }}>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--text-main)' }}>
                      {s.title}
                    </h3>
                    <span
                      className={`status-pill ${s.is_published ? 'status-published' : 'status-unpublished'}`}
                      style={{ whiteSpace: 'nowrap' }}
                    >
                      {s.is_published ? 'Published' : 'Draft'}
                    </span>
                  </div>

                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1.25rem', lineHeight: 1.5, minHeight: '2.6rem' }}>
                    {s.description || 'No description provided.'}
                  </p>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem', fontSize: '0.85rem', color: 'var(--text-dim)' }}>
                    <BookOpen size={16} />
                    <strong>{sBooks.length}</strong> {sBooks.length === 1 ? 'book' : 'books'} in series
                  </div>
                </div>

                <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <button
                    onClick={() => setManagingSeries(s)}
                    className="secondary-btn"
                    style={{ fontSize: '0.85rem', padding: '0.4rem 0.8rem' }}
                  >
                    <Layers size={14} />
                    <span>Manage Books ({sBooks.length})</span>
                  </button>

                  <div style={{ display: 'flex', gap: '0.4rem' }}>
                    <button
                      onClick={() => handleTogglePublish(s)}
                      className="icon-btn"
                      title={s.is_published ? 'Unpublish Series' : 'Publish Series'}
                    >
                      {s.is_published ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                    <button
                      onClick={() => openEditModal(s)}
                      className="icon-btn"
                      title="Edit Series Details"
                    >
                      <Edit2 size={16} />
                    </button>
                    <button
                      onClick={() => setDeletingSeries(s)}
                      className="icon-btn danger-icon-btn"
                      title="Delete Series"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* CREATE / EDIT MODAL */}
      {(isCreateModalOpen || editingSeries) && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3>{editingSeries ? 'Edit Series' : 'Create New Series'}</h3>
              <button
                onClick={() => {
                  setIsCreateModalOpen(false);
                  setEditingSeries(null);
                }}
                className="close-btn"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSaveSeries} className="modal-body">
              {formError && (
                <div className="alert alert-error" style={{ marginBottom: '1rem' }}>
                  <AlertTriangle size={16} />
                  <span>{formError}</span>
                </div>
              )}

              <div className="form-group">
                <label>Series Title *</label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="e.g. Annual Organizational Reports 2026"
                  required
                  autoFocus
                />
              </div>

              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  placeholder="Summarize the theme or scope of this multi-book collection..."
                  rows={3}
                />
              </div>

              <div className="form-group checkbox-group" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginTop: '0.5rem' }}>
                <input
                  type="checkbox"
                  id="series-is-published"
                  checked={formIsPublished}
                  onChange={(e) => setFormIsPublished(e.target.checked)}
                />
                <label htmlFor="series-is-published" style={{ margin: 0, cursor: 'pointer' }}>
                  Publish this series (visible to organization members)
                </label>
              </div>

              <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
                <button
                  type="button"
                  onClick={() => {
                    setIsCreateModalOpen(false);
                    setEditingSeries(null);
                  }}
                  className="secondary-btn"
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="primary-btn"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Saving...' : editingSeries ? 'Save Changes' : 'Create Series'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE CONFIRM MODAL */}
      {deletingSeries && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '440px' }}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: 'var(--danger)' }}>
                <AlertTriangle size={22} />
                <h3 style={{ margin: 0 }}>Delete Series</h3>
              </div>
              <button onClick={() => setDeletingSeries(null)} className="close-btn">
                <X size={20} />
              </button>
            </div>

            <div className="modal-body">
              <p style={{ marginBottom: '1rem', color: 'var(--text-main)' }}>
                Are you sure you want to delete the series <strong>"{deletingSeries.title}"</strong>?
              </p>
              <div style={{
                background: 'rgba(59, 130, 246, 0.1)',
                border: '1px solid rgba(59, 130, 246, 0.25)',
                borderRadius: 'var(--radius-md)',
                padding: '0.85rem',
                fontSize: '0.85rem',
                color: 'var(--text-muted)'
              }}>
                <strong style={{ color: 'var(--primary)' }}>Safe Deletion Guarantee:</strong> Deleting this series will <strong>NOT</strong> delete any books. All books in this series will simply become independent standalone publications.
              </div>

              <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
                <button
                  type="button"
                  onClick={() => setDeletingSeries(null)}
                  className="secondary-btn"
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleDeleteSeries}
                  className="danger-btn"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Deleting...' : 'Confirm Delete Series'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MANAGE SERIES BOOKS MODAL */}
      {managingSeries && (
        <div className="modal-overlay">
          <div className="modal-card" style={{ maxWidth: '680px', width: '95%' }}>
            <div className="modal-header">
              <div>
                <h3 style={{ margin: 0 }}>Books in "{managingSeries.title}"</h3>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Arrange reading order and add or remove books.
                </span>
              </div>
              <button onClick={() => setManagingSeries(null)} className="close-btn">
                <X size={20} />
              </button>
            </div>

            <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
              {/* CURRENT BOOKS IN SERIES */}
              <div style={{ marginBottom: '1.5rem' }}>
                <h4 style={{ fontSize: '0.95rem', marginBottom: '0.75rem', color: 'var(--text-main)' }}>
                  Current Books Sequence ({getSeriesBooks(managingSeries.id).length})
                </h4>

                {getSeriesBooks(managingSeries.id).length === 0 ? (
                  <p style={{ color: 'var(--text-dim)', fontStyle: 'italic', padding: '1rem', background: 'var(--bg-main)', borderRadius: 'var(--radius-md)' }}>
                    No books assigned to this series yet. Select independent books below to add them.
                  </p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {getSeriesBooks(managingSeries.id).map((book, idx, arr) => (
                      <div
                        key={book.id}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          background: 'var(--bg-main)',
                          border: '1px solid var(--border-subtle)',
                          borderRadius: 'var(--radius-md)',
                          padding: '0.65rem 0.85rem',
                          gap: '0.75rem'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flex: 1, minWidth: 0 }}>
                          <span style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: '26px',
                            height: '26px',
                            borderRadius: '50%',
                            background: 'var(--bg-card-subtle)',
                            color: 'var(--primary)',
                            fontSize: '0.85rem',
                            fontWeight: 700
                          }}>
                            {idx + 1}
                          </span>

                          <div style={{ minWidth: 0, flex: 1 }}>
                            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: 'var(--text-main)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {book.title}
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                              <span>{book.author || 'No author'}</span>
                              <span>•</span>
                              <span style={{ textTransform: 'uppercase', fontWeight: 600 }}>{book.file_type}</span>
                              <span>•</span>
                              <span style={{ color: book.is_published ? 'var(--success)' : 'var(--warning)' }}>
                                {book.is_published ? 'Published' : 'Draft'}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* ORDER & REMOVE CONTROLS */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <button
                            onClick={() => handleReorderBook(book, 'up', arr)}
                            disabled={idx === 0}
                            className="icon-btn"
                            title="Move Up"
                            style={{ opacity: idx === 0 ? 0.35 : 1, padding: '4px' }}
                          >
                            <ArrowUp size={15} />
                          </button>
                          <button
                            onClick={() => handleReorderBook(book, 'down', arr)}
                            disabled={idx === arr.length - 1}
                            className="icon-btn"
                            title="Move Down"
                            style={{ opacity: idx === arr.length - 1 ? 0.35 : 1, padding: '4px' }}
                          >
                            <ArrowDown size={15} />
                          </button>
                          <button
                            onClick={() => handleRemoveBookFromSeries(book.id)}
                            className="icon-btn danger-icon-btn"
                            title="Remove from Series (keep as independent book)"
                            style={{ padding: '4px', marginLeft: '0.35rem' }}
                          >
                            <X size={15} />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* AVAILABLE INDEPENDENT BOOKS TO ADD */}
              <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '1.25rem' }}>
                <h4 style={{ fontSize: '0.95rem', marginBottom: '0.5rem', color: 'var(--text-main)' }}>
                  Add Standalone Books to this Series
                </h4>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginBottom: '0.75rem' }}>
                  Books not currently assigned to any collection:
                </p>

                {getIndependentBooks().length === 0 ? (
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-dim)', fontStyle: 'italic' }}>
                    All existing books are currently assigned to series.
                  </p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', maxHeight: '180px', overflowY: 'auto' }}>
                    {getIndependentBooks().map((b) => (
                      <div
                        key={b.id}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '0.5rem 0.75rem',
                          borderRadius: 'var(--radius-sm)',
                          background: 'var(--bg-card-subtle)',
                          fontSize: '0.85rem'
                        }}
                      >
                        <div style={{ minWidth: 0, flex: 1, marginRight: '0.75rem' }}>
                          <span style={{ fontWeight: 600, color: 'var(--text-main)', display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {b.title}
                          </span>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                            {b.author || 'No author'} ({b.file_type.toUpperCase()})
                          </span>
                        </div>
                        <button
                          onClick={() => handleAddBookToSeries(b.id, managingSeries.id)}
                          className="secondary-btn"
                          style={{ fontSize: '0.75rem', padding: '0.25rem 0.6rem' }}
                        >
                          <Plus size={13} />
                          <span>Add to Series</span>
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="modal-actions" style={{ borderTop: '1px solid var(--border-subtle)', padding: '1rem 1.5rem' }}>
              <button
                type="button"
                onClick={() => setManagingSeries(null)}
                className="primary-btn"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
