package com.synex.mirarecorder.di

import android.content.Context
import android.content.SharedPreferences
import com.synex.mirarecorder.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val PREFS_NAME = "mirarecorder_prefs"
    const val KEY_SERVER_URL = "serverURL"
    const val DEFAULT_SERVER_URL = "https://mirrativ-record.saipi1129.com"
    const val KEY_UNLOCKED = "unlocked"
    private const val UNLOCK_PASSWORD_HASH = "6b6302519b6a3b0eccaec5f94e2e2554438d88824fda1c8f32ce11c8001f1222"

    fun verifyPassword(input: String): Boolean {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return hash == UNLOCK_PASSWORD_HASH
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, prefs: SharedPreferences): Retrofit {
        val baseUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
