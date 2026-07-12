package com.clockin.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clockin.app.data.local.PreferencesManager
import com.clockin.app.data.local.ScanRecord
import com.clockin.app.data.repository.ScanRepository
import com.clockin.app.ui.components.EditableRecordListItem
import com.clockin.app.ui.screens.database.CalendarView
import com.clockin.app.ui.screens.database.DatabaseViewModel
import com.clockin.app.ui.theme.GreenAccent
import com.clockin.app.ui.theme.ScanFailRed
import com.clockin.app.util.CardNumberUtils
import com.clockin.app.util.DateTimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    preferencesManager: PreferencesManager,
    repository: ScanRepository,
    onNavigateBack: () -> Unit,
    onNavigateToWriteTag: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val preventDuplicates by preferencesManager.preventDuplicates.collectAsState()

    val viewModel = remember { DatabaseViewModel(repository) }
    val currentMonth by viewModel.currentMonth.collectAsState()
    val datesWithRecords by viewModel.datesWithRecords.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDateRecords by viewModel.selectedDateRecords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Manual add dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var addDate by remember { mutableStateOf(DateTimeUtils.getCurrentDisplayDate()) }
    var addCardNumber by remember { mutableStateOf("") }
    var addTime by remember { mutableStateOf(DateTimeUtils.getCurrentTime()) }
    var addErrorMessage by remember { mutableStateOf<String?>(null) }

    // Delete confirmation
    var deleteRecordId by remember { mutableStateOf<Long?>(null) }
    var editRecord by remember { mutableStateOf<ScanRecord?>(null) }
    var editDate by remember { mutableStateOf("") }
    var editCardNumber by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }

    // Add dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                addErrorMessage = null
            },
            title = { Text("Add Record") },
            text = {
                Column {
                    OutlinedTextField(
                        value = addDate,
                        onValueChange = {
                            addDate = it
                            addErrorMessage = null
                        },
                        label = { Text("Date (dd/mm/yyyy)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addCardNumber,
                        onValueChange = {
                            addCardNumber = it
                            addErrorMessage = null
                        },
                        label = { Text("Badge Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addTime,
                        onValueChange = {
                            addTime = it
                            addErrorMessage = null
                        },
                        label = { Text("Time (HH:mm:ss)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (addErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = addErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ScanFailRed
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalizedCardNumber = CardNumberUtils.normalize(addCardNumber)
                        val storageDate = DateTimeUtils.parseDateInputToStorage(addDate)
                        if (storageDate == null) {
                            addErrorMessage = "Date must be dd/mm/yyyy"
                            return@TextButton
                        }
                        if (normalizedCardNumber.isBlank() || !CardNumberUtils.isValidCardNumber(normalizedCardNumber)) {
                            addErrorMessage = "Badge number must contain only digits"
                            return@TextButton
                        }
                        val timestamp = DateTimeUtils.parseDateTimeToTimestamp(storageDate, addTime)
                        if (timestamp == null) {
                            addErrorMessage = "Time must be HH:mm:ss"
                            return@TextButton
                        }
                        scope.launch {
                            repository.insertRecord(
                                ScanRecord(
                                    scanDate = storageDate,
                                    cardNumber = normalizedCardNumber,
                                    scanTime = addTime.trim(),
                                    timestamp = timestamp
                                )
                            )
                            showAddDialog = false
                            addCardNumber = ""
                            addDate = DateTimeUtils.getCurrentDisplayDate()
                            addTime = DateTimeUtils.getCurrentTime()
                            addErrorMessage = null
                            snackbarHostState.showSnackbar("Record saved")
                        }
                    },
                    enabled = CardNumberUtils.normalize(addCardNumber).isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    addErrorMessage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editRecord != null) {
        AlertDialog(
            onDismissRequest = {
                editRecord = null
                editErrorMessage = null
            },
            title = { Text("Edit Record") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = {
                            editDate = it
                            editErrorMessage = null
                        },
                        label = { Text("Date (dd/mm/yyyy)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCardNumber,
                        onValueChange = {
                            editCardNumber = it
                            editErrorMessage = null
                        },
                        label = { Text("Badge Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editTime,
                        onValueChange = {
                            editTime = it
                            editErrorMessage = null
                        },
                        label = { Text("Time (HH:mm:ss)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = editErrorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ScanFailRed
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val record = editRecord ?: return@TextButton
                        val normalizedCardNumber = CardNumberUtils.normalize(editCardNumber)
                        val storageDate = DateTimeUtils.parseDateInputToStorage(editDate)
                        if (storageDate == null) {
                            editErrorMessage = "Date must be dd/mm/yyyy"
                            return@TextButton
                        }
                        if (normalizedCardNumber.isBlank() || !CardNumberUtils.isValidCardNumber(normalizedCardNumber)) {
                            editErrorMessage = "Badge number must contain only digits"
                            return@TextButton
                        }
                        val timestamp = DateTimeUtils.parseDateTimeToTimestamp(storageDate, editTime)
                        if (timestamp == null) {
                            editErrorMessage = "Time must be HH:mm:ss"
                            return@TextButton
                        }
                        scope.launch {
                            repository.updateRecord(
                                record.copy(
                                    scanDate = storageDate,
                                    cardNumber = normalizedCardNumber,
                                    scanTime = editTime.trim(),
                                    timestamp = timestamp
                                )
                            )
                            editRecord = null
                            editErrorMessage = null
                            snackbarHostState.showSnackbar("Record updated")
                        }
                    },
                    enabled = editRecord != null
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editRecord = null
                    editErrorMessage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (deleteRecordId != null) {
        AlertDialog(
            onDismissRequest = { deleteRecordId = null },
            title = { Text("Delete Record") },
            text = { Text("Delete this record?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteRecord(deleteRecordId!!)
                            deleteRecordId = null
                            snackbarHostState.showSnackbar("Record deleted")
                        }
                    }
                ) {
                    Text("Delete", color = ScanFailRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecordId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bottom sheet for day detail (admin - editable)
    if (selectedDate != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectDate(null) },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = GreenAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateTimeUtils.formatDisplayDate(selectedDate!!),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GreenAccent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${selectedDateRecords.size} scans",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        addDate = DateTimeUtils.formatDisplayDate(selectedDate!!)
                        addTime = DateTimeUtils.getCurrentTime()
                        addErrorMessage = null
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, "Add", tint = GreenAccent)
                    }
                }

                if (selectedDateRecords.isEmpty()) {
                    Text(
                        text = "No scans on this day",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            selectedDateRecords,
                            key = { _, r -> r.id }
                        ) { index, record ->
                            EditableRecordListItem(
                                record = record,
                                onEdit = { selected ->
                                    editRecord = selected
                                    editDate = DateTimeUtils.formatDisplayDate(selected.scanDate)
                                    editCardNumber = CardNumberUtils.normalize(selected.cardNumber)
                                    editTime = selected.scanTime
                                    editErrorMessage = null
                                },
                                onDelete = { id -> deleteRecordId = id },
                                positionLabel = "${index + 1}/${selectedDateRecords.size}",
                                showDate = true
                            )
                        }
                        item(key = "records-count-footer-$selectedDate") {
                            Text(
                                text = "${selectedDateRecords.size}/${selectedDateRecords.size} scans shown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setSearching(!isSearching) }) {
                        Icon(
                            if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearching) "Close" else "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Action buttons - two rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToWriteTag,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Nfc, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write Badge", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                Button(
                    onClick = {
                        addDate = DateTimeUtils.getCurrentDisplayDate()
                        addTime = DateTimeUtils.getCurrentTime()
                        addErrorMessage = null
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToExport,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            AnimatedVisibility(
                visible = isSearching,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    placeholder = { Text("Search by badge number...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (isSearching && searchQuery.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    Text(
                        text = "No results for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    Text(
                        text = "${searchResults.size} results",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    // Cap eagerly composed rows; this Column is not lazy
                    val displayedResults = searchResults.take(100)
                    displayedResults.forEachIndexed { index, record ->
                        EditableRecordListItem(
                            record = record,
                            onEdit = { selected ->
                                editRecord = selected
                                editDate = DateTimeUtils.formatDisplayDate(selected.scanDate)
                                editCardNumber = CardNumberUtils.normalize(selected.cardNumber)
                                editTime = selected.scanTime
                                editErrorMessage = null
                            },
                            onDelete = { id -> deleteRecordId = id },
                            positionLabel = "${index + 1}/${searchResults.size}",
                            showDate = true
                        )
                    }
                    if (searchResults.size > displayedResults.size) {
                        Text(
                            text = "Showing first ${displayedResults.size} of ${searchResults.size} results",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Calendar
                CalendarView(
                    yearMonth = currentMonth,
                    datesWithRecords = datesWithRecords,
                    selectedDate = null,
                    onDateSelected = { date ->
                        viewModel.selectDate(date.toString())
                    },
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Settings section
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Prevent duplicate scans",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Blocks the same badge only on the same day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preventDuplicates,
                                onCheckedChange = { preferencesManager.setPreventDuplicates(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GreenAccent,
                                    checkedTrackColor = GreenAccent.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
