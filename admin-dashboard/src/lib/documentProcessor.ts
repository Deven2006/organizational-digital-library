import { supabase } from './supabase';
import mammoth from 'mammoth';
import type { SupportedFileType, Book } from '../types/database';

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
export const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'txt'] as const;

export interface ValidationResult {
  valid: boolean;
  error?: string;
  fileType?: SupportedFileType;
}

export function validateDocumentFile(file: File): ValidationResult {
  if (!file) {
    return { valid: false, error: 'No file selected.' };
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `File size exceeds the 50MB limit (${(file.size / (1024 * 1024)).toFixed(1)}MB).`,
    };
  }

  const extension = file.name.split('.').pop()?.toLowerCase();
  if (!extension || !ALLOWED_EXTENSIONS.includes(extension as SupportedFileType)) {
    return {
      valid: false,
      error: `Unsupported file format (.${extension || 'unknown'}). Allowed formats: .pdf, .docx, .txt`,
    };
  }

  return { valid: true, fileType: extension as SupportedFileType };
}

/**
 * Extracts plain text from DOCX or TXT files client-side.
 * Returns null for PDF files as they are rendered natively by the reader.
 */
export async function extractContentText(file: File, fileType: SupportedFileType): Promise<string | null> {
  if (fileType === 'txt') {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(new Error('Failed to read text file.'));
      reader.readAsText(file);
    });
  }

  if (fileType === 'docx') {
    const arrayBuffer = await file.arrayBuffer();
    const result = await mammoth.extractRawText({ arrayBuffer });
    return result.value.trim();
  }

  return null;
}

/**
 * Uploads a document to the private 'book-files' Supabase storage bucket,
 * extracts text if applicable, and writes the metadata to the 'books' table.
 * If database insertion fails, the uploaded storage file is immediately cleaned up.
 */
export async function uploadBookDocument({
  title,
  author,
  description,
  file,
  isPublished = false,
}: {
  title: string;
  author?: string;
  description?: string;
  file: File;
  isPublished?: boolean;
}): Promise<Book> {
  const validation = validateDocumentFile(file);
  if (!validation.valid || !validation.fileType) {
    throw new Error(validation.error || 'Invalid file.');
  }

  const cleanFilename = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
  const uniqueStoragePath = `${Date.now()}_${crypto.randomUUID().slice(0, 8)}_${cleanFilename}`;

  // 1. Extract readable content (for DOCX and TXT)
  const parsedContent = await extractContentText(file, validation.fileType);

  // 2. Upload file to Supabase Storage private bucket 'book-files'
  const { error: uploadError } = await supabase.storage
    .from('book-files')
    .upload(uniqueStoragePath, file, {
      cacheControl: '3600',
      upsert: false,
    });

  if (uploadError) {
    throw new Error(`Storage upload failed: ${uploadError.message}`);
  }

  // 3. Insert metadata record in 'books' table
  const { data: bookRecord, error: dbError } = await supabase
    .from('books')
    .insert([
      {
        title: title.trim(),
        author: author?.trim() || null,
        description: description?.trim() || null,
        file_type: validation.fileType,
        storage_path: uniqueStoragePath,
        parsed_content: parsedContent,
        is_published: isPublished,
      },
    ])
    .select()
    .single();

  // 4. Rollback storage upload if DB insert failed
  if (dbError) {
    await supabase.storage.from('book-files').remove([uniqueStoragePath]);
    throw new Error(`Database record creation failed: ${dbError.message}`);
  }

  return bookRecord as Book;
}

/**
 * Replaces the document file of an existing book.
 * Uploads new file, updates record, and deletes old file.
 */
export async function replaceBookFile(book: Book, newFile: File): Promise<Book> {
  const validation = validateDocumentFile(newFile);
  if (!validation.valid || !validation.fileType) {
    throw new Error(validation.error || 'Invalid file.');
  }

  const cleanFilename = newFile.name.replace(/[^a-zA-Z0-9._-]/g, '_');
  const newStoragePath = `${Date.now()}_${crypto.randomUUID().slice(0, 8)}_${cleanFilename}`;

  const parsedContent = await extractContentText(newFile, validation.fileType);

  // 1. Upload new file
  const { error: uploadError } = await supabase.storage
    .from('book-files')
    .upload(newStoragePath, newFile, {
      cacheControl: '3600',
      upsert: false,
    });

  if (uploadError) {
    throw new Error(`Storage upload failed: ${uploadError.message}`);
  }

  // 2. Update record in books
  const { data: updatedBook, error: dbError } = await supabase
    .from('books')
    .update({
      file_type: validation.fileType,
      storage_path: newStoragePath,
      parsed_content: parsedContent,
    })
    .eq('id', book.id)
    .select()
    .single();

  if (dbError) {
    // Clean up newly uploaded file if DB update failed
    await supabase.storage.from('book-files').remove([newStoragePath]);
    throw new Error(`Database update failed: ${dbError.message}`);
  }

  // 3. Remove old storage file
  if (book.storage_path) {
    await supabase.storage.from('book-files').remove([book.storage_path]);
  }

  return updatedBook as Book;
}

/**
 * Deletes a book record and removes its corresponding storage object.
 */
export async function deleteBook(book: Book): Promise<void> {
  // 1. Delete database record
  const { error: dbError } = await supabase.from('books').delete().eq('id', book.id);
  if (dbError) {
    throw new Error(`Failed to delete book record: ${dbError.message}`);
  }

  // 2. Delete storage file if present
  if (book.storage_path) {
    const { error: storageError } = await supabase.storage
      .from('book-files')
      .remove([book.storage_path]);
    if (storageError) {
      console.warn(`Could not remove storage object ${book.storage_path}:`, storageError.message);
    }
  }
}
