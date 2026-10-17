package com.example.digitallibrary.data.remote

/**
 * Supabase configuration constants for the Android Digital Library client.
 * Replace with your actual project URL and public Anon Key from Supabase Dashboard -> Project Settings -> API.
 */
object SupabaseConfig {
    // Replace with your Supabase Project URL (e.g. "https://abcdefghijklmno.supabase.co")
    const val SUPABASE_URL = "https://ujttmwpvgvhrsxhdrhnl.supabase.co"

    // Replace with your Supabase Project Anon/Public Key (starts with eyJhbG...)
    const val SUPABASE_ANON_KEY = "sb_publishable_VLbLqiRPGjm5tZ5UFywZHw_61hh4xpZ"

    fun isConfigured(): Boolean {
        return SUPABASE_URL.isNotBlank() &&
                SUPABASE_ANON_KEY.isNotBlank() &&
                !SUPABASE_URL.contains("your-project-id") &&
                !SUPABASE_ANON_KEY.contains("your-anon-public-key")
    }
}
