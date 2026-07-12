package com.clockin.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE scan_date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: String): Flow<List<ScanRecord>>

    @Query("SELECT DISTINCT scan_date FROM scan_records ORDER BY scan_date DESC")
    fun getAllDatesWithRecords(): Flow<List<String>>

    @Query("SELECT * FROM scan_records WHERE card_number LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY timestamp DESC")
    fun searchByCardNumber(query: String): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE card_number IN (:cardNumbers) ORDER BY timestamp DESC")
    fun getRecordsByCardNumbers(cardNumbers: List<String>): Flow<List<ScanRecord>>

    @Query("SELECT COUNT(*) FROM scan_records WHERE card_number IN (:cardNumbers) AND scan_date = :date")
    suspend fun countByCardsAndDate(cardNumbers: List<String>, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecord)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("UPDATE scan_records SET scan_date = :scanDate, card_number = :cardNumber, scan_time = :scanTime, timestamp = :timestamp WHERE id = :id")
    suspend fun updateRecord(id: Long, scanDate: String, cardNumber: String, scanTime: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM scan_records")
    suspend fun getRecordCount(): Int
}
