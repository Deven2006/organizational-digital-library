import React, { useState, useRef, useEffect } from 'react';
import { uploadBookDocument, validateDocumentFile } from '../lib/documentProcessor';
import type { Book } from '../types/database';
import { X, UploadCloud, FileText, CheckCircle, AlertCircle } from 'lucide-react';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newBook: Book) => void;
}

export const UploadModal: React.FC<UploadModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [description, setDescription] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isPublished, setIsPublished] = useState(false);
  const [isTitleManuallyEdited, setIsTitleManuallyEdited] = useState(false);
  const [fileInputKey, setFileInputKey] = useState(0);

  const [isUploading, setIsUploading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const successTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      setTitle('');
      setAuthor('');
      setDescription('');
      setSelectedFile(null);
      setIsPublished(false);
      setIsTitleManuallyEdited(false);
      setStatusMessage(null);
      setErrorMessage(null);
      setFileInputKey((prev) => prev + 1);
    }
  }

  const clearSuccessTimer = () => {
    if (successTimeoutRef.current) {
      clearTimeout(successTimeoutRef.current);
      successTimeoutRef.current = null;
    }
  };

  useEffect(() => {
    return () => {
      clearSuccessTimer();
    };
  }, []);

  const resetForm = () => {
    clearSuccessTimer();
    setTitle('');
    setAuthor('');
    setDescription('');
    setSelectedFile(null);
    setIsPublished(false);
    setIsTitleManuallyEdited(false);
    setStatusMessage(null);
    setErrorMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    setFileInputKey((prev) => prev + 1);
  };

  const handleClose = () => {
    if (isUploading) return;
    resetForm();
    onClose();
  };

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    clearSuccessTimer();
    setErrorMessage(null);
    setStatusMessage(null);
    const file = e.target.files?.[0];
    if (!file) {
      setSelectedFile(null);
      return;
    }

    const validation = validateDocumentFile(file);
    if (!validation.valid) {
      setErrorMessage(validation.error || 'Invalid file.');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setSelectedFile(file);

    // Auto-fill title from filename if title is empty or wasn't manually edited
    if (!title.trim() || !isTitleManuallyEdited) {
      const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '');
      setTitle(nameWithoutExt.replace(/[-_]/g, ' '));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearSuccessTimer();
    setErrorMessage(null);
    setStatusMessage(null);

    if (!title.trim()) {
      setErrorMessage('Book title is required.');
      return;
    }

    if (!selectedFile) {
      setErrorMessage('Please select a valid document (PDF, DOCX, or TXT).');
      return;
    }

    try {
      setIsUploading(true);
      setStatusMessage('Extracting readable content & uploading document to secure storage...');

      const createdBook = await uploadBookDocument({
        title,
        author,
        description,
        file: selectedFile,
        isPublished,
      });

      // Clear the form fields and file input immediately so Book A's data is not retained
      setTitle('');
      setAuthor('');
      setDescription('');
      setSelectedFile(null);
      setIsPublished(false);
      setIsTitleManuallyEdited(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      setFileInputKey((prev) => prev + 1);

      setStatusMessage('Document uploaded and cataloged successfully!');
      onSuccess(createdBook);

      successTimeoutRef.current = setTimeout(() => {
        setStatusMessage(null);
        onClose();
      }, 700);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to upload document.');
      setStatusMessage(null);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleClose}>
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-group">
            <UploadCloud size={22} className="modal-icon" />
            <h3>Upload New Book Document</h3>
          </div>
          <button
            onClick={handleClose}
            className="modal-close-btn"
            disabled={isUploading}
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
            <label htmlFor="book-title">
              Book Title <span className="required-star">*</span>
            </label>
            <input
              id="book-title"
              type="text"
              placeholder="e.g. Organizational Constitution & Bylaws"
              value={title}
              onChange={(e) => {
                clearSuccessTimer();
                setStatusMessage(null);
                setErrorMessage(null);
                setIsTitleManuallyEdited(true);
                setTitle(e.target.value);
              }}
              required
              disabled={isUploading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="book-author">Author / Committee</label>
            <input
              id="book-author"
              type="text"
              placeholder="e.g. Executive Governance Board"
              value={author}
              onChange={(e) => {
                clearSuccessTimer();
                setStatusMessage(null);
                setErrorMessage(null);
                setAuthor(e.target.value);
              }}
              disabled={isUploading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="book-description">Description & Summary</label>
            <textarea
              id="book-description"
              rows={3}
              placeholder="Brief summary of document topics, edition notes, or purpose..."
              value={description}
              onChange={(e) => {
                clearSuccessTimer();
                setStatusMessage(null);
                setErrorMessage(null);
                setDescription(e.target.value);
              }}
              disabled={isUploading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="file-input">
              Document File (.pdf, .docx, .txt) <span className="required-star">*</span>
            </label>
            <div
              className={`file-drop-zone ${selectedFile ? 'has-file' : ''}`}
              onClick={() => {
                clearSuccessTimer();
                fileInputRef.current?.click();
              }}
            >
              <input
                key={fileInputKey}
                ref={fileInputRef}
                id="file-input"
                type="file"
                accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain"
                onChange={handleFileChange}
                style={{ display: 'none' }}
                disabled={isUploading}
              />

              {selectedFile ? (
                <div className="selected-file-preview">
                  <FileText size={32} className="preview-icon" />
                  <div className="selected-file-details">
                    <span className="file-name">{selectedFile.name}</span>
                    <span className="file-size">
                      {(selectedFile.size / 1024).toFixed(1)} KB &bull;{' '}
                      {selectedFile.name.split('.').pop()?.toUpperCase()}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="change-file-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      clearSuccessTimer();
                      fileInputRef.current?.click();
                    }}
                  >
                    Replace
                  </button>
                </div>
              ) : (
                <div className="empty-drop-zone">
                  <UploadCloud size={36} className="cloud-icon" />
                  <p className="primary-text">Click or browse to choose a document file</p>
                  <p className="secondary-text">Supported: PDF, DOCX, TXT (Maximum: 50MB)</p>
                </div>
              )}
            </div>
          </div>

          <div className="checkbox-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={isPublished}
                onChange={(e) => {
                  clearSuccessTimer();
                  setStatusMessage(null);
                  setErrorMessage(null);
                  setIsPublished(e.target.checked);
                }}
                disabled={isUploading}
              />
              <span className="checkbox-custom"></span>
              <span className="checkbox-text">
                Publish immediately (make visible in Android library)
              </span>
            </label>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              onClick={handleClose}
              className="secondary-btn"
              disabled={isUploading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="primary-btn submit-upload-btn"
              disabled={isUploading}
            >
              {isUploading ? (
                <span className="spinner-label">
                  <span className="spinner"></span> Processing...
                </span>
              ) : (
                'Upload & Save Book'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

