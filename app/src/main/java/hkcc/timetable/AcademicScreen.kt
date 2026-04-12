package hkcc.timetable

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicScreen(academicViewModel: AcademicViewModel = viewModel(), timetableViewModel: TimetableViewModel = viewModel()) {
    val context = LocalContext.current
    val grades by academicViewModel.grades.collectAsState()
    val targetGpa by academicViewModel.targetGpa.collectAsState()
    val remainingCredits by academicViewModel.remainingCredits.collectAsState()
    val allSubjects by timetableViewModel.subjects.collectAsState()
    val selectedIds by timetableViewModel.selectedIds.collectAsState()
    val totalCreditsGoalStr by timetableViewModel.userCredits.collectAsState()
    val totalCreditsGoal = totalCreditsGoalStr.toIntOrNull() ?: 60

    val mySubjects = remember(allSubjects, selectedIds) {
        allSubjects.filter { selectedIds.contains(it.id) }.distinctBy { it.code }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<CourseGrade?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Academic & GPA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

        Card(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Overall GPA", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(
                    String.format(Locale.US, "%.2f", academicViewModel.calculateGpa()),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "${grades.sumOf { it.credits }} / $totalCreditsGoal Credits",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("Graduation Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val requirements = academicViewModel.getGraduationRequirements(totalCreditsGoal, mapOf("Cluster" to 15, "DS" to 18, "GS" to 9))
                requirements.forEach { req ->
                    val progress = if (req.required > 0) req.earned.toFloat() / req.required else 1f
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(req.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("${req.earned}/${req.required}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("GPA Planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = targetGpa, onValueChange = { academicViewModel.updateTargetGpa(it) }, label = { Text("Target GPA") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = remainingCredits, onValueChange = { academicViewModel.updateRemainingCredits(it) }, label = { Text("Rem. Credits") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                val targetResult = academicViewModel.calculateRequiredGrades(targetGpa.toDoubleOrNull() ?: 0.0, remainingCredits.toIntOrNull() ?: 0)
                Text(targetResult, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Course Grades", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = {
                editingGrade = null
                showAddDialog = true
            }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add Grade")
            }
        }

        if (grades.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("No grades added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        grades.forEach { grade ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(grade.code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        val desc = academicViewModel.getGradeDescription(grade.grade)
                        Text(
                            "${grade.credits} Credits • ${grade.cluster.ifBlank { "No Cluster" }}${if(desc.isNotEmpty()) " • $desc" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        grade.grade,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = {
                        editingGrade = grade
                        showAddDialog = true
                    }) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.outline) }
                    IconButton(onClick = { academicViewModel.removeGrade(grade.id) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Deadlines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            var showDeadlineDialog by remember { mutableStateOf(false) }
            IconButton(onClick = { showDeadlineDialog = true }) { Icon(Icons.Default.Add, null) }

            if (showDeadlineDialog) {
                var title by remember { mutableStateOf("") }
                var dateStr by remember { mutableStateOf("2024-12-31") }
                AlertDialog(
                    onDismissRequest = { showDeadlineDialog = false },
                    title = { Text("Add Deadline") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Due Date (YYYY-MM-DD)") }, shape = RoundedCornerShape(12.dp))
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val date = sdf.parse(dateStr)
                                if (date != null) {
                                    val deadline = Deadline(title = title, dueDate = date.time, courseCode = "")
                                    academicViewModel.addDeadline(deadline)
                                    NotificationHelper.scheduleDeadlineReminder(context, deadline)
                                }
                            } catch (e: Exception) {}
                            showDeadlineDialog = false
                        }, shape = RoundedCornerShape(12.dp)) { Text("Add") }
                    }
                )
            }
        }

        val deadlines by academicViewModel.deadlines.collectAsState()
        deadlines.forEach { deadline ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(deadline.title, fontWeight = FontWeight.Bold)
                        Text("Due: ${java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US).format(java.util.Date(deadline.dueDate))}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { academicViewModel.removeDeadline(deadline.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showAddDialog) {
        var code by remember(editingGrade) { mutableStateOf(editingGrade?.code ?: "") }
        var creds by remember(editingGrade) { mutableStateOf(editingGrade?.credits?.toString() ?: "3") }
        var gradeValue by remember(editingGrade) { mutableStateOf(editingGrade?.grade ?: "A") }
        var cluster by remember(editingGrade) { mutableStateOf(editingGrade?.cluster ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingGrade = null
            },
            title = { Text(if (editingGrade == null) "Add Course Grade" else "Edit Course Grade", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mySubjects.isNotEmpty() && editingGrade == null) {
                        Text("Select Enrolled Course:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mySubjects) { sub ->
                                FilterChip(
                                    selected = code == sub.code,
                                    onClick = {
                                        code = sub.code
                                        cluster = sub.cluster
                                    },
                                    label = { Text(sub.code) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = creds, onValueChange = { creds = it }, label = { Text("Credits") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

                    Text("Select Grade:", style = MaterialTheme.typography.labelSmall)
                    val gradesList = listOf("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F")
                    LazyRow(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gradesList) { g ->
                            FilterChip(
                                selected = gradeValue == g,
                                onClick = { gradeValue = g },
                                label = { Text(g) }
                            )
                        }
                    }

                    OutlinedTextField(value = cluster, onValueChange = { cluster = it }, label = { Text("Cluster (e.g. DS, GS, Area 1)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (code.isNotBlank()) {
                        val newGrade = CourseGrade(
                            id = editingGrade?.id ?: java.util.UUID.randomUUID().toString(),
                            code = code,
                            name = "",
                            credits = creds.toIntOrNull() ?: 3,
                            grade = gradeValue,
                            semester = editingGrade?.semester ?: "Current",
                            cluster = cluster
                        )
                        if (editingGrade == null) {
                            academicViewModel.addGrade(newGrade)
                        } else {
                            academicViewModel.updateGrade(newGrade)
                        }
                        showAddDialog = false
                        editingGrade = null
                    }
                }, shape = RoundedCornerShape(12.dp)) { Text(if (editingGrade == null) "Add" else "Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingGrade = null
                }) { Text("Cancel") }
            }
        )
    }
}
