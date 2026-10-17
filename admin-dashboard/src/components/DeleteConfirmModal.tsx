import React, { useState } from 'react';
import { deleteBook } from '../lib/documentProcessor';
import type { Book } from '../types/database';
import { Trash2, AlertTriangle, X } from 'lucide-react';

interface DeleteConfirmModalProps {
  book: Book | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (deletedBookId: string) => void;
}

export const DeleteConfirmModal: React.FC<DeleteConfirmModalProps> = ({
  book,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen || !book) return null;

  const handleDelete = async () => {
    try {
      setIsDeleting(true);
      setErrorMessage(null);

      await deleteBook(book);

      onSuccess(book.id);
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to delete book.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-dialog modal-danger" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-group danger-title">
            <AlertTriangle size={24} className="danger-icon" />
            <h3>Delete Book</h3>
          </div>
          <button
            onClick={onClose}
            className="modal-close-btn"
            disabled={isDeleting}
            aria-label="Close modal"
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          {errorMessage && (
            <div className="alert alert-error">
              <span>{errorMessage}</span>
            </div>
          )}

          <p className="delete-warning-text">
            Are you sure you want to permanently delete <strong>&ldquo;{book.title}&rdquo;</strong>?
          </p>

          <p className="delete-sub-text">
            This action will delete the database catalog entry and permanently purge the uploaded{' '}
            <strong>.{book.file_type.toUpperCase()}</strong> file from private cloud storage. This cannot be undone.
          </p>
        </div>

        <div className="modal-actions">
          <button
            type="button"
            onClick={onClose}
            className="secondary-btn"
            disabled={isDeleting}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleDelete}
            className="danger-btn"
            disabled={isDeleting}
            id="btn-confirm-delete"
          >
            {isDeleting ? (
              <span className="spinner-label">
                <span className="spinner"></span> Deleting...
              </span>
            ) : (
              <>
                <Trash2 size={16} />
                <span>Delete Permanently</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
