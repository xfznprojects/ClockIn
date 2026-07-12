package com.clockin.app.ui.screens.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockin.app.data.local.ScanRecord
import com.clockin.app.data.repository.ScanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseViewModel(private val repository: ScanRepository) : ViewModel() {

    // Calendar state
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Filter: show only days with records
    private val _showOnlyRecordDays = MutableStateFlow(false)
    val showOnlyRecordDays: StateFlow<Boolean> = _showOnlyRecordDays.asStateFlow()

    // All dates that have records (for calendar dots)
    val datesWithRecords: StateFlow<Set<String>> = repository.allDatesWithRecords
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Records for selected date
    val selectedDateRecords: StateFlow<List<ScanRecord>> = _selectedDate
        .flatMapLatest { date ->
            if (date != null) repository.getRecordsByDate(date)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search results
    val searchResults: StateFlow<List<ScanRecord>> = _searchQuery
        .flatMapLatest { query ->
            if (query.length >= 1) repository.searchByCardNumber(query)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All records (for showing filtered days list)
    val allRecords: StateFlow<List<ScanRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun selectDate(date: String?) {
        _selectedDate.value = date
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun setSearching(searching: Boolean) {
        _isSearching.value = searching
        if (!searching) {
            _searchQuery.value = ""
        }
    }

    fun toggleShowOnlyRecordDays() {
        _showOnlyRecordDays.value = !_showOnlyRecordDays.value
    }
}
