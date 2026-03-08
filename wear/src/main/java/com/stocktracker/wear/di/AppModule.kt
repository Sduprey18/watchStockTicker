package com.stocktracker.wear.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.stocktracker.wear.data.ConnectivityObserver
import com.stocktracker.wear.data.StockRepository
import com.stocktracker.wear.data.local.AppDatabase
import com.stocktracker.wear.data.remote.AlphaVantageApi
import com.stocktracker.wear.data.remote.RateLimitInterceptor
import com.stocktracker.wear.data.remote.RequestQueue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.stocktracker.wear.BuildConfig
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://www.alphavantage.co/"

    @Provides
    @Singleton
    fun provideOkHttpClient(rateLimitInterceptor: RateLimitInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(HttpLoggingInterceptor { message ->
                Timber.tag("OkHttp").d(message)
            }.apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAlphaVantageApi(retrofit: Retrofit): AlphaVantageApi =
        retrofit.create(AlphaVantageApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(application: Application): AppDatabase =
        Room.databaseBuilder(application, AppDatabase::class.java, "stock_tracker_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideWatchlistDao(db: AppDatabase) = db.watchlistDao()

    @Provides
    @Singleton
    fun provideQuoteDao(db: AppDatabase) = db.quoteDao()

    @Provides
    @Singleton
    fun provideRequestQueue(): RequestQueue = RequestQueue()

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context
    ): ConnectivityObserver = ConnectivityObserver(context)
}

