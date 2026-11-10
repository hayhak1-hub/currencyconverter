package com.hayhak.currencyconverter.di

import android.content.Context
import androidx.room.Room
import com.hayhak.currencyconverter.BuildConfig
import com.hayhak.currencyconverter.data.local.dao.ExchangeRateDao
import com.hayhak.currencyconverter.data.local.database.AppDatabase
import com.hayhak.currencyconverter.data.remote.api.ExchangeRateApi
import com.hayhak.currencyconverter.data.remote.api.FawazCurrencyApi
import com.hayhak.currencyconverter.data.remote.api.GoldPriceApi
import com.hayhak.currencyconverter.data.remote.api.LiveExchangeRateApi
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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/v2/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideExchangeRateApi(retrofit: Retrofit): ExchangeRateApi =
        retrofit.create(ExchangeRateApi::class.java)

    @Provides
    @Singleton
    fun provideLiveExchangeRateApi(client: OkHttpClient): LiveExchangeRateApi =
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LiveExchangeRateApi::class.java)

    @Provides
    @Singleton
    fun provideGoldPriceApi(client: OkHttpClient): GoldPriceApi =
        Retrofit.Builder()
            .baseUrl("https://api.gold-api.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoldPriceApi::class.java)

    @Provides
    @Singleton
    fun provideFawazCurrencyApi(client: OkHttpClient): FawazCurrencyApi =
        Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FawazCurrencyApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "currency_db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()
}
