package com.trackasia.sample.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    var retrofit: Retrofit? = null

    private fun createRetrofit(baseUrl: String): Retrofit {
        val builder = OkHttpClient.Builder()
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.connectTimeout(5, TimeUnit.SECONDS)
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(logging)

        builder.addInterceptor { chain ->
            val original: Request = chain.request()
            val request: Request = chain.request().newBuilder()
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }

        val client: OkHttpClient = builder.build()

        return Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    fun getInstance(baseUrl: String): Retrofit {
        if (retrofit == null || !retrofit?.baseUrl().toString().equals(baseUrl, ignoreCase = true)) {
            retrofit = createRetrofit(baseUrl)
        }
        return retrofit!!
    }
}