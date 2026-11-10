package com.hayhak.currencyconverter.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hayhak.currencyconverter.data.local.dao.ExchangeRateDao
import com.hayhak.currencyconverter.data.local.entity.ExchangeRateEntity

@Database(entities = [ExchangeRateEntity::class, com.hayhak.currencyconverter.data.local.entity.LatestRateEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS latest_rates (baseCurrency TEXT NOT NULL, targetCurrency TEXT NOT NULL, rate REAL NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(baseCurrency, targetCurrency))")
                // Keep all existing history. The latest available snapshot seeds the new cache.
                db.execSQL("INSERT OR REPLACE INTO latest_rates SELECT baseCurrency, targetCurrency, rate, timestamp FROM exchange_rates e WHERE timestamp = (SELECT MAX(timestamp) FROM exchange_rates WHERE baseCurrency = e.baseCurrency) ORDER BY id")
            }
        }
    }
}
