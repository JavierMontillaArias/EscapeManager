package com.javiermontillaarias.escapemanager.data.network

import com.javiermontillaarias.escapemanager.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Para emulador usa 10.0.2.2; para dispositivo real usa la IP de tu máquina
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private var retrofit: Retrofit? = null

    fun getInstance(sessionManager: SessionManager): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(sessionManager))
                .addInterceptor(logging)
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

    fun getApiService(sessionManager: SessionManager): ApiService =
        getInstance(sessionManager).create(ApiService::class.java)
}