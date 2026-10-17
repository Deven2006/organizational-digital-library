import React, { useState, useRef } from 'react';
import { supabase } from '../lib/supabase';
import { replaceBookFile, validateDocumentFile } from '../lib/documentProcessor';
import type { Book } from '../types/database';
import { X, Edit3, FileText, AlertCircle, CheckCircle } from 'lucide-react';

interface EditBookModalProps {
  book: Book | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedBook: Book) => void;
}

export const EditBookModal: React.FC<EditBookModalProps> = ({
  book,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [title, setTitle] = useState(book?.title || '');
  const [author, setAuthor] = useState(book?.author || '');
  const [description, setDescription] = useState(book?.description || '');
  const [newFile, setNewFile] = useState<File | null>(null);

  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    if (book) {
      setTitle(book.title);
      setAuthor(book.author || '');
      setDescription(book.description || '');
      setNewFile(null);
      setErrorMessage(null);
      setStatusMessage(null);
    }
  }, [book]);

  if (!isOpen || !book) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setErrorMessage(null);
    const file = e.target.files?.[0];
    if (!file) {
      setNewFile(null);
      return;
    }

    const validation = validateDocumentFile(file);
    if (!validation.valid) {
      setErrorMessage(validation.error || 'Invalid file.');
      setNewFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setNewFile(file);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setStatusMessage(null);

    if (!title.trim()) {
      setErrorMessage('Book title cannot be empty.');
      return;
    }

    try {
      setIsSaving(true);
      // 1. If a replacement file was chosen, replace the file in storage and update content
      if (newFile) {
        setStatusMessage('Replacing document file in secure storage...');
        await replaceBookFile(book, newFile);
      }

      // 2. Update metadata fields
      setStatusMessage('Saving metadata updates...');
      const { data: finalRecord, error: updateErr } = await supabase
        .from('books')
        .update({
          title: title.trim(),
          author: author.trim() || null,
          description: description.trim() || null,
        })
        .eq('id', book.id)
        .select()
        .single();

      if (updateErr) {
        throw new Error(updateErr.message);
      }

      setStatusMessage('Changes saved successfully!');
      setTimeout(() => {
        onSuccess(finalRecord as Book);
        onClose();
      }, 500);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to save changes.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-group">
            <Edit3 size={22} className="modal-icon" />
            <h3>Edit Book Details</h3>
          </div>
          <button
            onClick={onClose}
            className="modal-close-btn"
            disabled={isSaving}
            aria-label="Close modal"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          {errorMessage && (
            <div className="alert alert-error">
              <AlertCircle size={18} />
              <span>{errorMessage}</span>
            </div>
          )}

          {statusMessage && (
            <div className="alert alert-info">
              <CheckCircle size={18} />
              <span>{statusMessage}</span>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="edit-title">
              Book Title <span className="required-star">*</span>
            </label>
            <input
              id="edit-title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              disabled={isSaving}
            />
          </div>

          <div className="form-group">
            <label htmlFor="edit-author">Author / Committee</label>
            <input
              id="edit-author"
              type="text"
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
              disabled={isSaving}
            />
          </div>

          <div className="form-group">
            <label htmlFor="edit-description">Description</label>
            <textarea
              id="edit-description"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isSaving}
            />
          </div>

          <div className="form-group">
            <label>Current Document File</label>
            <div className="current-file-badge">
              <FileText size={16} />
              <span>
                Format: <strong>{book.file_type.toUpperCase()}</strong> &bull; Path:{' '}
                <span className="file-path-truncated">{book.storage_path}</span>
              </span>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="replace-file-input">Replace Document File (Optional)</label>
            <input
              ref={fileInputRef}
              id="replace-file-input"
              type="file"
              accept=".pdf,.docx,.txt"
              onChange={handleFileChange}
              disabled={isSaving}
              className="standard-file-input"
            />
            {newFile && (
              <p className="file-replace-hint">
                Selected replacement: <strong>{newFile.name}</strong> (
                {(newFile.size / 1024).toFixed(1)} KB)
              </p>
            )}
          </div>

          <div className="modal-actions">
            <button
              type="button"
              onClick={onClose}
              className="secondary-btn"
              disabled={isSaving}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="primary-btn"
              disabled={isSaving}
            >
              {isSaving ? (
                <span className="spinner-label">
                  <span className="spinner"></span> Saving...
                </span>
              ) : (
                'Save Changes'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
