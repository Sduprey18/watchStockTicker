package com.stocktracker.wear.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stocktracker.wear.data.local.entity.CachedQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    @Query("SELECT * FROM cached_quotes WHERE symbol = :symbol")
    fun quoteFlow(symbol: String): Flow<CachedQuoteEntity?>

    @Query("SELECT * FROM cached_quotes")
    fun allQuotesFlow(): Flow<List<CachedQuoteEntity>>

    @Query("SELECT * FROM cached_quotes WHERE symbol IN (:symbols)")
    fun quotesForSymbolsFlow(symbols: List<String>): Flow<List<CachedQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedQuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CachedQuoteEntity>)

    @Query("SELECT * FROM cached_quotes WHERE symbol = :symbol")
    suspend fun getQuote(symbol: String): CachedQuoteEntity?

    @Query("DELETE FROM cached_quotes WHERE fetchedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
