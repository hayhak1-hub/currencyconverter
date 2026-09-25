package com.hayhak.currencyconverter

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hayhak.currencyconverter.data.local.database.AppDatabase
import com.hayhak.currencyconverter.data.local.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RateCacheTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun historyRefreshCannotDeleteCurrentSnapshot() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.exchangeRateDao()
            val latest = listOf(ExchangeRateEntity(baseCurrency = "USD", targetCurrency = "EUR", rate = 0.9, timestamp = 200),
                ExchangeRateEntity(baseCurrency = "USD", targetCurrency = "TRY", rate = 40.0, timestamp = 200))
            dao.replaceLatest("USD", latest)
            dao.replaceHistory("USD", 0, 300, listOf(latest.first().copy(rate = 0.8, timestamp = 100)))
            val current = dao.getLatestRates("USD").first()
            assertEquals(2, current.size)
            assertEquals(0.9, current.first { it.targetCurrency == "EUR" }.rate, 0.0)
            assertEquals(200L, dao.getLastUpdateTime("USD"))
            dao.replaceLatest("USD", latest)
            dao.replaceLatest("USD", latest)
            assertEquals(1, dao.getHistoricalRates("USD", "EUR", 200).first().size)
        } finally { db.close() }
    }

    @Test fun migrationKeepsHistoryAndSeedsCurrentRates() = runBlocking {
        val name = "migration-test-${UUID.randomUUID()}.db"
        val old = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE exchange_rates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, baseCurrency TEXT NOT NULL, targetCurrency TEXT NOT NULL, rate REAL NOT NULL, timestamp INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO exchange_rates VALUES (1,'USD','EUR',0.8,100), (2,'USD','EUR',0.9,200), (3,'USD','TRY',40.0,200)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        old.writableDatabase
        old.close()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(2, db.exchangeRateDao().getLatestRates("USD").first().size)
            assertEquals(2, db.exchangeRateDao().getHistoricalRates("USD", "EUR", 0).first().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
