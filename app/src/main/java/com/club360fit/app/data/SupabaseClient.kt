package com.club360fit.app.data

import com.club360fit.app.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client singleton. URL uses project ID from Supabase dashboard.
 * Anon key is read from BuildConfig (set via local.properties).
 */
object SupabaseClient {

    private const val SUPABASE_URL = "https://mjkrokpctcieahxtxvxq.supabase.co"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
