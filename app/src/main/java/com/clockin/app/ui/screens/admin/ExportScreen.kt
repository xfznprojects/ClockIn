package com.clockin.app.ui.screens.admin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.clockin.app.data.local.ScanRecord
import com.clockin.app.data.repository.ScanRepository
import com.clockin.app.ui.theme.GreenAccent
import com.clockin.app.util.CardNumberUtils
import com.clockin.app.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    repository: ScanRepository,
    activity: Activity,
    onNavigateBack: () -> Unit
) {
    val allRecords by repository.allRecords.collectAsState(initial = emptyList())
    var emailAddress by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Export Database") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = GreenAccent,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${allRecords.size} records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GreenAccent
            )
            Text(
                text = "will be attached as a CSV file",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = emailAddress,
                onValueChange = { emailAddress = it },
                label = { Text("Email address (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Leave empty to pick an email app yourself",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        sendDatabaseEmail(activity, allRecords, emailAddress)
                    }
                },
                enabled = allRecords.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Send via Email",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (allRecords.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No records to export",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun sendDatabaseEmail(
    activity: Activity,
    records: List<ScanRecord>,
    emailAddress: String
) {
    // Full export goes into a CSV file; only the short summary rides along as the email body.
    val exportFile = try {
        withContext(Dispatchers.IO) { writeCsvFile(activity, records) }
    } catch (_: Exception) {
        Toast.makeText(activity, "Could not create the export file", Toast.LENGTH_LONG).show()
        return
    }

    val uri = FileProvider.getUriForFile(
        activity,
        "${activity.packageName}.fileprovider",
        exportFile
    )

    val byDate = records.groupBy { it.scanDate }
    val summary = byDate.entries.sortedByDescending { it.key }.joinToString("\n") { (date, recs) ->
        val badgeNumbers = recs
            .map { CardNumberUtils.normalize(it.cardNumber) }
            .distinct()
            .joinToString(", ")
        "  ${DateTimeUtils.formatDisplayDate(date)}: ${recs.size} scans (badges: $badgeNumbers)"
    }

    val body = """
ClockIn - Attendance Export
====================================

Total: ${records.size} records
Days with scans: ${byDate.size}

Summary per day:
$summary

The full list is attached as a CSV file.
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "ClockIn - Database Export")
        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (emailAddress.isNotBlank()) {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
        }
    }

    try {
        activity.startActivity(Intent.createChooser(intent, "Export via..."))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(activity, "No app available to share the export", Toast.LENGTH_LONG).show()
    }
}

private fun writeCsvFile(activity: Activity, records: List<ScanRecord>): File {
    val exportsDir = File(activity.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportsDir, "clockin_export_${DateTimeUtils.getCurrentDate()}.csv")
    file.bufferedWriter().use { writer ->
        writer.appendLine("date,badge,time")
        records.forEach { record ->
            writer.appendLine(
                "${record.scanDate},${CardNumberUtils.normalize(record.cardNumber)},${record.scanTime}"
            )
        }
    }
    return file
}
