package com.clockin.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "scan_date")
    val scanDate: String,

    @ColumnInfo(name = "card_number")
    val cardNumber: String,

    @ColumnInfo(name = "scan_time")
    val scanTime: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
