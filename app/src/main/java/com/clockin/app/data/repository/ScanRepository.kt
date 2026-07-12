package com.clockin.app.data.repository

import com.clockin.app.data.local.ScanRecord
import com.clockin.app.data.local.ScanRecordDao
import com.clockin.app.util.CardNumberUtils
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val dao: ScanRecordDao) {

    val allRecords: Flow<List<ScanRecord>> = dao.getAllRecords()

    val allDatesWithRecords: Flow<List<String>> = dao.getAllDatesWithRecords()

    fun getRecordsByDate(date: String): Flow<List<ScanRecord>> = dao.getRecordsByDate(date)

    fun getRecordsByCardNumber(cardNumber: String): Flow<List<ScanRecord>> {
        return dao.getRecordsByCardNumbers(CardNumberUtils.duplicateCandidates(cardNumber))
    }

    fun searchByCardNumber(query: String): Flow<List<ScanRecord>> {
        val normalizedQuery = CardNumberUtils.normalize(query)
        val effectiveQuery = normalizedQuery.ifBlank { query.trim() }
        return dao.searchByCardNumber(escapeLikeWildcards(effectiveQuery))
    }

    private fun escapeLikeWildcards(query: String): String {
        return query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    suspend fun insertRecord(record: ScanRecord) {
        dao.insertRecord(record)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteRecord(id)
    }

    suspend fun updateRecord(record: ScanRecord) {
        dao.updateRecord(
            id = record.id,
            scanDate = record.scanDate,
            cardNumber = CardNumberUtils.normalize(record.cardNumber),
            scanTime = record.scanTime,
            timestamp = record.timestamp
        )
    }

    suspend fun isDuplicateForDay(cardNumber: String, date: String): Boolean {
        return dao.countByCardsAndDate(CardNumberUtils.duplicateCandidates(cardNumber), date) > 0
    }

    suspend fun getRecordCount(): Int = dao.getRecordCount()
}
