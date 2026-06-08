package com.example.plzhealth.data

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.data.go.kr/openapi/"

    private class RetryInterceptor(private val maxRetry: Int = 3) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
            val request = chain.request()
            var response: Response? = null
            var exception: IOException? = null
            var tryCount = 0

            while (tryCount < maxRetry) {
                try {
                    tryCount++
                    if (tryCount > 1) {
                        Log.w("RetrofitClient", "공공데이터 서버 에러로 인해 자동 재시도 중... ($tryCount/$maxRetry)")
                        Thread.sleep(300)
                    }
                    response = chain.proceed(request)

                    if (response.isSuccessful) {
                        return response
                    }
                } catch (e: IOException) {
                    exception = e
                    Log.e("RetrofitClient", "통신 중 에러 발생 (시도 $tryCount): ${e.message}")
                }
            }
            if (response != null) return response
            throw exception ?: IOException("알 수 없는 네트워크 오류가 발생했습니다.")
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .protocols(listOf(Protocol.HTTP_1_1))
        .addInterceptor(RetryInterceptor(maxRetry = 4))
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Connection", "close")
                .header("Accept", "application/json")
                .method(originalRequest.method, originalRequest.body)
                .build()
            chain.proceed(newRequest)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: NutriApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NutriApiService::class.java)
    }
}