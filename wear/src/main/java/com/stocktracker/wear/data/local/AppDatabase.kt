package com.stocktracker.wear.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stocktracker.wear.data.local.dao.QuoteDao
import com.stocktracker.wear.data.local.dao.WatchlistDao
import com.stocktracker.wear.data.local.entity.CachedQuoteEntity
import com.stocktracker.wear.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class, CachedQuoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun quoteDao(): QuoteDao
}
