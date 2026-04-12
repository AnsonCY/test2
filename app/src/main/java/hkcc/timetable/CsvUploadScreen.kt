package hkcc.timetable

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hkcc.timetable.data.Subject

@Composable
fun CsvUploadScreen(viewModel: TimetableViewModel) {
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }
    var showAddManual by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val subjects = parseCsvStream(stream)
                if (subjects.isNotEmpty()) {
                    viewModel.addSubjects(subjects)
                    Toast.makeText(context, "Added ${subjects.size} entries", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will delete all subjects and selections.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showConfirm = false
                }) { Text("Confirm", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            icon = { Icon(Icons.Filled.Warning, null) }
        )
    }

    if (showAddManual) {
        var code by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var day by remember { mutableStateOf("MON") }
        var start by remember { mutableStateOf("09:00") }
        var end by remember { mutableStateOf("11:00") }
        var venue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddManual = false },
            title = { Text("Add Custom Course") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code") })
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") })
                    OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day (MON, TUE...)") })
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start Time (HH:mm)") })
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End Time (HH:mm)") })
                    OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        viewModel.addSubjects(listOf(Subject(
                            code = code, name = name, classNo = "CUSTOM", subGroup = "", type = "Lec",
                            dayOfWeek = day, startTime = start, endTime = end, campus = "Other", venue = venue
                        )))
                        showAddManual = false
                    }
                }, shape = RoundedCornerShape(12.dp)) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddManual = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AddCircle, "", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Import CSV File")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showAddManual = true },
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Edit, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Manually")
        }
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = { showConfirm = true }) {
            Icon(Icons.Filled.Delete, "")
            Spacer(Modifier.width(4.dp))
            Text("Clear All Data", color = MaterialTheme.colorScheme.error)
        }
    }
}
