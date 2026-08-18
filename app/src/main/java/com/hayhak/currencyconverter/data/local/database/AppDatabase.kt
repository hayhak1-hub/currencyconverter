package com.hayhak.currencyconverter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hayhak.currencyconverter.data.local.dao.ExchangeRateDao
import com.hayhak.currencyconverter.data.local.entity.ExchangeRateEntity

@Database(entities = [ExchangeRateEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao
}
