export type UserRole = 'admin' | 'member';

export interface Profile {
  id: string;
  full_name: string | null;
  role: UserRole;
  created_at: string;
}

export type SupportedFileType = 'pdf' | 'docx' | 'txt';

export interface Series {
  id: string;
  title: string;
  description: string | null;
  cover_image_url: string | null;
  is_published: boolean;
  created_at: string;
  updated_at: string;
}

export interface Book {
  id: string;
  title: string;
  author: string | null;
  description: string | null;
  file_type: SupportedFileType;
  storage_path: string;
  parsed_content: string | null;
  is_published: boolean;
  series_id: string | null;
  series_order: number;
  version: number;
  content_hash: string | null;
  created_at: string;
  updated_at: string;
}

export interface DashboardStats {
  total: number;
  published: number;
  unpublished: number;
}
