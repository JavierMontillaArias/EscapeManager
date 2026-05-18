package com.javiermontillaarias.escapemanager.data.network

import com.javiermontillaarias.escapemanager.BuildConfig
import com.javiermontillaarias.escapemanager.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL get() = BuildConfig.BASE_URL

    private var retrofit: Retrofit? = null
    // Q-01: cachear ApiService para evitar crear instancias en cada llamada
    private var apiService: ApiService? = null

    @Synchronized
    fun getInstance(sessionManager: SessionManager): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                        else HttpLoggingInterceptor.Level.NONE
                if (BuildConfig.DEBUG) redactHeader("Authorization")
            }

            val noAuthRetrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val noAuthApi = noAuthRetrofit.create(ApiService::class.java)

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(sessionManager))
                .addInterceptor(logging)
                .authenticator(TokenAuthenticator(sessionManager, noAuthApi))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    @Synchronized
    fun getApiService(sessionManager: SessionManager): ApiService {
        return apiService ?: getInstance(sessionManager)
            .create(ApiService::class.java)
            .also { apiService = it }
    }
}
