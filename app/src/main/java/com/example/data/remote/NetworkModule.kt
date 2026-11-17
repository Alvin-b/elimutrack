package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  fun createService(baseUrl: String, authToken: String = ""): ElimuTrackApiService {
    val normalizedUrl = normalizeBaseUrl(baseUrl)
    val logger = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
      .cookieJar(CookieJar.NO_COOKIES)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(25, TimeUnit.SECONDS)
      .writeTimeout(25, TimeUnit.SECONDS)
      .addInterceptor(logger)
      .addInterceptor { chain ->
        val reqBuilder = chain.request().newBuilder()
          .header("Accept", "application/json")
        if (chain.request().body != null) {
          reqBuilder.header("Content-Type", "application/json")
        }
        if (authToken.isNotBlank()) {
          reqBuilder.header("Authorization", "Bearer $authToken")
        }
        chain.proceed(reqBuilder.build())
      }
      .build()

    return Retrofit.Builder()
      .baseUrl(normalizedUrl)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(ElimuTrackApiService::class.java)
  }

  fun normalizeBaseUrl(raw: String): String {
    val trimmed = raw.trim().ifBlank { "https://site-4457d1964dd64e19ba7fec208a2066ee.freebuff.page/api/" }
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      trimmed
    } else {
      "https://$trimmed"
    }
    val noTrailing = withScheme.trimEnd('/')
    return if (noTrailing.endsWith("/api")) {
      "$noTrailing/"
    } else {
      "$noTrailing/api/"
    }
  }
}
