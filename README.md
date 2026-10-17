# Organizational Digital Library — Complete MVP

A secure, reliable, and functional digital library system designed for organizations. 

- **Administrators** manage, upload, publish, unpublish, and delete documents via a responsive web dashboard.
- **Members** browse, search, and read published documents natively inside an Android application.
- **Shared Backend** runs on Supabase (PostgreSQL, Auth, Storage, and strict Row Level Security policies).

---

## Architecture Overview

```
                      ┌────────────────────────────────────────┐
                      │          Supabase Cloud Backend        │
                      │                                        │
                      │  - Auth (JWT Sessions)                 │
                      │  - PostgreSQL Database (RLS Enforced)  │
                      │    * profiles (admin, member)          │
                      │    * books (pdf, docx, txt)            │
                      │  - Storage Bucket (book-files)         │
                      └──────────────────┬─────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 │                                               │
                 ▼                                               ▼
   ┌───────────────────────────┐                   ┌───────────────────────────┐
   │      Admin Dashboard      │                   │        Android App        │
   │    (React + Vite + TS)    │                   │   (Kotlin + Compose M3)   │
   │                           │                   │                           │
   │ - Full catalog management │                   │ - Browse published books  │
   │ - Multi-format upload     │                   │ - Instant title search    │
   │ - Client-side extraction  │                   │ - Native PDF viewer       │
   │ - Atomic storage delete   │                   │ - Rich text/docx reader   │
   │ - Strict admin route guard│                   │ - Session auto-restore    │
   └───────────────────────────┘                   └───────────────────────────┘
```

---

## Repository Structure

```
├── admin-dashboard/            # React + Vite + TypeScript Admin Dashboard
│   ├── src/
│   │   ├── components/         # BookTable, UploadModal, EditBookModal, LoginView, etc.
│   │   ├── context/            # AuthContext (strict role verification)
│   │   ├── lib/                # Supabase client & documentProcessor (DOCX text extraction)
│   │   ├── types/              # Database models & domain types
│   │   ├── App.tsx             # Main dashboard layout & state
│   │   └── index.css           # Premium Slate & Indigo design system
│   ├── package.json
│   └── vite.config.ts
│
├── android-app/                # Native Android Application (Jetpack Compose)
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml # Internet permission & launcher
│   │   └── java/com/example/digitallibrary/
│   │       ├── data/model/     # Book, Profile, User, Auth models
│   │       ├── data/remote/    # SupabaseConfig & SupabaseClient (REST + Storage)
│   │       ├── data/repository/# AuthRepository & BookRepository (cache & session)
│   │       ├── ui/auth/        # LoginScreen & LoginViewModel
│   │       ├── ui/library/     # LibraryScreen (search & catalog) & LibraryViewModel
│   │       ├── ui/detail/      # BookDetailScreen
│   │       ├── ui/reader/      # ReaderScreen & native PdfViewer (PdfRenderer)
│   │       ├── ui/profile/     # ProfileScreen & logout action
│   │       ├── ui/navigation/  # Navigation Compose graph
│   │       └── MainActivity.kt # Entry point
│   └── build.gradle.kts
│
├── supabase/
│   └── migrations/             # Database schema, bucket definition, & RLS policies
│       └── 20261018000000_create_digital_library_schema.sql
│
├── .env.example                # Template for environment variables
└── README.md                   # Complete documentation & setup instructions
```

---

## 1. Supabase Backend Setup

### Step 1: Create a Supabase Project
1. Log in to [Supabase](https://supabase.com) and click **New Project**.
2. Note your **Project URL** and **Project API Anon Key** under **Project Settings -> API**.

### Step 2: Apply Database Migrations
1. In the Supabase Dashboard, open the **SQL Editor**.
2. Click **New query** and copy the entire contents of:
   ```
   supabase/migrations/20261018000000_create_digital_library_schema.sql
   ```
3. Click **Run**. This will create:
   - `profiles` table with automatic user signup trigger and role lock.
   - `books` table with publication status and file type validations.
   - `book-files` private storage bucket.
   - Server-side Row Level Security (RLS) policies for tables and storage objects.
   - The `public.promote_user_to_admin(...)` provisioning function.

---

## 2. Secure First Administrator Provisioning

To prevent unauthorized privilege escalation, public admin registration is disabled. Follow this procedure:

1. Create a user account via the Supabase Auth UI or by signing up through the Android app / web dashboard with your organization email (e.g., `admin@organization.org`).
2. Go to the **Supabase Dashboard -> SQL Editor**.
3. Run the following command:
   ```sql
   SELECT public.promote_user_to_admin('admin@organization.org');
   ```
4. This securely elevates the user's role to `'admin'` in the `public.profiles` table. The user can now log in to the Admin Dashboard.

---

## 3. Running the Admin Web Dashboard

### Prerequisites
- Node.js 18+ and npm

### Local Development
1. Navigate to `admin-dashboard/`:
   ```bash
   cd admin-dashboard
   ```
2. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
3. Edit `.env` and fill in your Supabase credentials:
   ```env
   VITE_SUPABASE_URL=https://your-project-id.supabase.co
   VITE_SUPABASE_ANON_KEY=eyJhbGciOi...
   ```
4. Install dependencies:
   ```bash
   npm install
   ```
5. Start the Vite development server:
   ```bash
   npm run dev
   ```
6. Open `http://localhost:5173` in your browser.

### Production Build
To create an optimized production bundle:
```bash
npm run build
```
The output will be generated in `admin-dashboard/dist/`.

---

## 4. Deploying the Admin Website to Vercel

1. Push your repository to GitHub, GitLab, or Bitbucket.
2. Log in to [Vercel](https://vercel.com) and click **Add New -> Project**.
3. Select your repository.
4. Set the **Root Directory** to `admin-dashboard`.
5. Under **Environment Variables**, add:
   - `VITE_SUPABASE_URL` = your Supabase project URL
   - `VITE_SUPABASE_ANON_KEY` = your Supabase public anon key
6. Click **Deploy**. Vercel will automatically run `npm run build` and publish your site with a custom SSL domain.

---

## 5. Running & Building the Android Application

### Prerequisites
- Android Studio Ladybug / Meerkat or Android SDK 34+ with command-line tools
- JDK 17 or higher

### Configuration
1. Open `android-app/app/src/main/java/com/example/digitallibrary/data/remote/SupabaseConfig.kt`.
2. Enter your project's URL and anon key:
   ```kotlin
   const val SUPABASE_URL = "https://your-project-id.supabase.co"
   const val SUPABASE_ANON_KEY = "your-anon-public-key-here"
   ```

### Running on Device or Emulator
From the `android-app/` directory:
```bash
./gradlew installDebug
```
Or open the `android-app/` folder in **Android Studio** and click **Run (Shift+F10)**.

### Building the Release APK
```bash
./gradlew assembleRelease
```
The output APK will be placed in:
`android-app/app/build/outputs/apk/release/`

---

## 6. Document Upload & Reading Workflow

### Supported Document Formats
| Format | Admin Processing | Android Reader Experience |
|---|---|---|
| **PDF** (`.pdf`) | Uploaded to private `book-files` bucket | Rendered in-app page-by-page via Android's native `PdfRenderer` with vertical scrolling |
| **DOCX** (`.docx`) | Client-side text extraction via `mammoth.js` during upload; stored in `parsed_content` | Rendered with clean typography, spacing, and vertical scrolling |
| **TXT** (`.txt`) | Content read into `parsed_content` and file stored in bucket | Rendered directly in comfortable reader view |

### Atomic Upload & Deletion Guarantees
- If a database insert fails during upload, the uploaded file in cloud storage is deleted automatically to prevent orphaned files.
- When an administrator deletes a book, the database record is removed and its corresponding storage object is purged from `book-files`.

---

## 7. Security Model & Row Level Security (RLS)

All security rules are enforced at the PostgreSQL database and storage levels:

1. **Role Tampering Protection**:
   A database trigger (`trg_profile_role_guard`) blocks members from modifying their own `role` field. Only an administrator can update roles.
2. **Catalog Visibility**:
   Members can only query books where `is_published = TRUE`. Administrators can query both draft and published books.
3. **Storage Access Policies**:
   Members cannot download an unpublished document even if they know its storage path. Storage RLS verifies that an active, published book record exists for that path before allowing reads.
4. **No Service-Role Key Exposure**:
   Client applications strictly use the public `anon` key combined with user JWT tokens. The `service_role` key is never bundled in frontend or mobile source code.

---

## 8. Verification & Testing Checklist

- [x] **Database Schema**: Tables, indexes, triggers, and RLS policies tested for role enforcement.
- [x] **Admin Web Dashboard**: TypeScript typecheck and Vite production build verified with 0 errors.
- [x] **Authentication**: Login, session persistence via SharedPreferences / Supabase Auth, and explicit logout.
- [x] **Document Management**: Multi-format upload (PDF, DOCX, TXT), title search, one-click publish toggle, and deletion.
- [x] **Android Application**: Jetpack Compose screens for Login, Library catalog, Details, in-app PDF rendering, and typography reader.
