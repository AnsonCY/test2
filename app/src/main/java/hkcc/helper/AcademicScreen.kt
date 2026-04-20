package hkcc.helper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

val ColorScheme.infoContainer: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) Color(0xFFE1F5FE) else Color(0xFF01579B)

@Composable
fun GradeItem(grade: CourseGrade, viewModel: AcademicViewModel, onEdit: (CourseGrade) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(grade.code, fontWeight = FontWeight.Bold)
                Text("${grade.credits} Credits • ${grade.cluster}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(grade.grade, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { onEdit(grade) }) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { viewModel.removeGrade(grade.id) }) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicScreen(academicViewModel: AcademicViewModel = viewModel(), timetableViewModel: TimetableViewModel = viewModel()) {
    val grades by academicViewModel.grades.collectAsState()
    val targetGpa by academicViewModel.targetGpa.collectAsState()
    val remainingCredits by academicViewModel.remainingCredits.collectAsState()
    val allSubjects by timetableViewModel.subjects.collectAsState()
    val selectedIds by timetableViewModel.selectedIds.collectAsState()
    val selectedSubjectCodes by timetableViewModel.selectedSubjectCodes.collectAsState()
    val totalCreditsGoalStr by timetableViewModel.userCredits.collectAsState()
    val totalCreditsGoal = totalCreditsGoalStr.toIntOrNull() ?: 60

    val selectedStudyPattern by timetableViewModel.selectedStudyPattern.collectAsState()
    val studyPatterns by timetableViewModel.studyPatterns.collectAsState()
    val subjectSpecs by timetableViewModel.subjectSpecs.collectAsState()

    val mySubjects = remember(allSubjects, selectedIds, selectedSubjectCodes) {
        val fullySelected = allSubjects.filter { selectedIds.contains(it.id) }.map { it.code }.toSet()
        val allInterested = fullySelected + selectedSubjectCodes
        allSubjects.filter { allInterested.contains(it.code) }.distinctBy { it.code }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<CourseGrade?>(null) }
    var prefilledSemester by remember { mutableStateOf("Current") }
    var prefilledCode by remember { mutableStateOf("") }

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
                val currentPattern = if (selectedStudyPattern.isNotEmpty()) {
                    studyPatterns.find { 
                        it.programCode == selectedStudyPattern["program"] &&
                        it.studyPattern == selectedStudyPattern["pattern"] &&
                        it.cantonesePutonghua == selectedStudyPattern["cantonese"] &&
                        it.engLevel == selectedStudyPattern["eng"]
                    }
                } else null

                val dsElectiveReq = (currentPattern?.totalDs?.toIntOrNull() ?: 0) * 3
                val geElectiveReq = (currentPattern?.totalGe?.toIntOrNull() ?: 0) * 3
                
                val clusterAreas = listOf("A", "M", "N", "E", "D")
                
                // Only count credits for grades that are NOT "F"
                val passingGrades = grades.filter { it.grade != "F" }
                val totalEarned = passingGrades.sumOf { it.credits }
                
                // Graduation requirements matching user criteria
                // Only count Electives for both DS and GE credits. F grades don't count.
                // Cluster Area is null or Compulsory/Elective is Compulsory should not be count as a elective credit
                val earnedDS = passingGrades.filter { 
                    it.geDs == "DS" && 
                    it.compulsoryElective == "Elective" && 
                    it.cluster.isNotBlank() 
                }.sumOf { it.credits }

                val earnedGEElectives = passingGrades.filter { 
                    it.geDs == "GE" && 
                    it.compulsoryElective == "Elective" && 
                    it.cluster.isNotBlank() 
                }.sumOf { it.credits }
                
                // Clusters completed (unique areas). Only Electives count for clusters. F grades don't count.
                // Also ensures Cluster Area is not null/blank and status is Elective.
                val clusterGrades = passingGrades.filter { 
                    it.cluster in clusterAreas && 
                    it.cluster.isNotBlank() && 
                    it.compulsoryElective == "Elective" 
                }
                val completedClusterNames = clusterGrades.map { it.cluster }.distinct()
                val completedClusters = completedClusterNames.size
                val hasClusterM = completedClusterNames.contains("M")

                // Graduation rule: 5 areas total, OR 4 areas if it includes Cluster M
                val requiredClusters = if (hasClusterM && completedClusters >= 4) 4 else 5

                val displayReqs = mutableListOf(
                    GraduationRequirement("Total Credits", totalCreditsGoal, totalEarned),
                    GraduationRequirement("Cluster Areas", requiredClusters, completedClusters)
                )
                
                if (dsElectiveReq > 0) displayReqs.add(GraduationRequirement("DS Electives", dsElectiveReq, earnedDS))
                if (geElectiveReq > 0) displayReqs.add(GraduationRequirement("GE Electives", geElectiveReq, earnedGEElectives))
                
                displayReqs.forEach { req ->
                    val progress = if (req.required > 0) req.earned.toFloat() / req.required else 1f
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(req.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val displayEarned = if (req.category == "Cluster Areas") "${req.earned}" else "${req.earned}"
                            Text("$displayEarned/${req.required}${if (req.category != "Cluster Areas") " Cr" else ""}", 
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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

                // Graduation Status Notice
                val missingCredits = (totalCreditsGoal - totalEarned).coerceAtLeast(0)
                val missingDS = (dsElectiveReq - earnedDS).coerceAtLeast(0)
                val missingGE = (geElectiveReq - earnedGEElectives).coerceAtLeast(0)
                val missingClusters = clusterAreas.filter { it !in completedClusterNames }
                
                val isGraduated = missingCredits == 0 && (completedClusters >= requiredClusters) && missingDS == 0 && missingGE == 0

                if (!isGraduated) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Graduation Checklist:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            if (missingCredits > 0) Text("• $missingCredits more credits required", style = MaterialTheme.typography.bodySmall)
                            if (missingDS > 0) Text("• $missingDS credits of DS Electives missing", style = MaterialTheme.typography.bodySmall)
                            if (missingGE > 0) Text("• $missingGE credits of GE Electives missing", style = MaterialTheme.typography.bodySmall)
                            if (missingClusters.isNotEmpty() && completedClusters < requiredClusters) {
                                Text("• Missing Areas: ${missingClusters.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF2E7D32)) // Placeholder for check icon
                            Spacer(Modifier.width(8.dp))
                            Text("Requirements Met! You are ready to graduate.", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
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
                prefilledCode = ""
                prefilledSemester = "Current"
                showAddDialog = true
            }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Add Extra")
            }
        }

        var selectedSemester by remember { mutableStateOf<String?>(null) }
        val bioLevel by timetableViewModel.bioLevel.collectAsState()
        val chemLevel by timetableViewModel.chemLevel.collectAsState()
        val phyLevel by timetableViewModel.phyLevel.collectAsState()

        val patternRows = remember(studyPatterns, selectedStudyPattern, bioLevel, chemLevel, phyLevel) {
            val baseRows = studyPatterns.filter {
                it.programCode == selectedStudyPattern["program"] &&
                it.studyPattern == selectedStudyPattern["pattern"] &&
                it.cantonesePutonghua == selectedStudyPattern["cantonese"] &&
                it.engLevel == selectedStudyPattern["eng"] &&
                (it.programCode != "8C112-AS" || (
                    (it.bioLevel.isBlank() || it.bioLevel == bioLevel) &&
                    (it.chemLevel.isBlank() || it.chemLevel == chemLevel) &&
                    (it.phyLevel.isBlank() || it.phyLevel == phyLevel)
                ))
            }
            var lastSem = ""
            baseRows.map { row ->
                if (row.semester.isNotBlank()) lastSem = row.semester
                row.copy(semester = lastSem)
            }
        }
        val programTitle = selectedStudyPattern["programTitle"] ?: ""
        val semesters = patternRows.map { it.semester }.distinct().sortedBy { it.toIntOrNull() ?: 0 }

        // EXCLUDE any grade assigned to a specific semester from "Other Courses"
        val consumedGradeIds = remember(grades) {
            grades.filter { it.semester.startsWith("Sem ") }.map { it.id }.toSet()
        }

        if (selectedStudyPattern.isEmpty()) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Select your program in Profile to see semester classification.", 
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(semesters) { sem ->
                    val isSelected = selectedSemester == sem
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSemester = if (isSelected) null else sem },
                        label = { Text("Sem $sem", fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            selectedSemester?.let { sem ->
                val semRows = patternRows.filter { it.semester == sem }
                val semGrades = grades.filter { it.semester == "Sem $sem" }
                val matchedGradeIdsForThisSem = mutableSetOf<String>()
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Semester $sem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            FilledTonalButton(
                                onClick = {
                                    editingGrade = null
                                    prefilledCode = ""
                                    prefilledSemester = "Sem $sem"
                                    showAddDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Course", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                        semRows.forEach { row ->
                            val spec = subjectSpecs.find { 
                                it.code == row.subjectCode && (it.programme.isBlank() || it.programme.equals(programTitle, true))
                            } ?: subjectSpecs.find { it.code == row.subjectCode }
                            
                            val matchingGrade = semGrades.find { it.code == row.subjectCode }
                            if (matchingGrade != null) matchedGradeIdsForThisSem.add(matchingGrade.id)

                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.subjectCode, fontWeight = FontWeight.Bold)
                                    if (spec != null) Text(spec.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (matchingGrade != null) {
                                    Text(matchingGrade.grade, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = {
                                        editingGrade = matchingGrade
                                        showAddDialog = true
                                    }) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                                } else {
                                    TextButton(onClick = {
                                        editingGrade = null
                                        prefilledCode = row.subjectCode
                                        prefilledSemester = "Sem $sem"
                                        showAddDialog = true
                                    }) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Grade")
                                    }
                                }
                            }
                        }

                        val extraSemGrades = semGrades.filter { !matchedGradeIdsForThisSem.contains(it.id) }
                        if (extraSemGrades.isNotEmpty()) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Text("Additional Courses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp))
                            extraSemGrades.forEach { grade ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(grade.code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${grade.credits} Credits • ${grade.cluster}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(grade.grade, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = {
                                        editingGrade = grade
                                        showAddDialog = true
                                    }) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { academicViewModel.removeGrade(grade.id) }) { 
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) 
                                    }
                                }
                            }
                        }

                        val geElectives = semRows.map { it.geElective }.firstOrNull { it.isNotBlank() }
                        val geDsElectives = semRows.map { it.geDsElective }.firstOrNull { it.isNotBlank() }
                        val dsElectives = semRows.map { it.dsElective }.firstOrNull { it.isNotBlank() }

                        if (!geElectives.isNullOrBlank() || !geDsElectives.isNullOrBlank() || !dsElectives.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.infoContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (!geElectives.isNullOrBlank()) Text("• You need to add $geElectives more GE Elective in this semester", style = MaterialTheme.typography.labelSmall)
                                    if (!geDsElectives.isNullOrBlank()) Text("• You need to add $geDsElectives more GE or DS Elective in this semester", style = MaterialTheme.typography.labelSmall)
                                    if (!dsElectives.isNullOrBlank()) Text("• You need to add $dsElectives more DS Elective in this semester", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        val otherGrades = grades.filter { !consumedGradeIds.contains(it.id) }
        if (otherGrades.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Other Courses", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            otherGrades.forEach { grade ->
                GradeItem(grade, academicViewModel) { editingGrade = it; showAddDialog = true }
            }
        }
    }

    if (showAddDialog) {
        var code by remember(editingGrade, prefilledCode) { mutableStateOf(prefilledCode) }
        var creds by remember(editingGrade) { mutableStateOf("3") }
        var gradeValue by remember(editingGrade) { mutableStateOf("A") }
        var cluster by remember(editingGrade) { mutableStateOf("") }
        var semester by remember(editingGrade, prefilledSemester) { mutableStateOf(prefilledSemester) }

        LaunchedEffect(editingGrade, prefilledSemester) {
            if (editingGrade != null) {
                code = editingGrade?.code ?: ""
                creds = editingGrade?.credits?.toString() ?: "3"
                gradeValue = editingGrade?.grade ?: "A"
                cluster = editingGrade?.cluster ?: ""
                semester = editingGrade?.semester ?: "Current"
            } else {
                semester = prefilledSemester
                code = prefilledCode
            }
        }

        val matchedSubject = remember(code, allSubjects, subjectSpecs) { 
            allSubjects.find { it.code.equals(code.trim(), true) } 
            ?: subjectSpecs.find { it.code.equals(code.trim(), true) }?.let { spec ->
                hkcc.helper.data.Subject(
                    code = spec.code, name = spec.title, credits = spec.credit.toIntOrNull() ?: 3,
                    cluster = spec.clusterArea, clusterArea = spec.clusterArea, 
                    compulsoryElective = spec.compulsoryElective, geDs = spec.geDs,
                    program = spec.programme, classNo = "", subGroup = "", type = "", 
                    dayOfWeek = "", startTime = "", endTime = "", campus = "", venue = "", lecturer = ""
                )
            }
        }

        LaunchedEffect(matchedSubject) {
            if (matchedSubject != null && editingGrade == null) {
                val autoValue = when {
                    matchedSubject.geDs == "DS" -> "DS"
                    matchedSubject.geDs == "GE" && matchedSubject.clusterArea.isNotBlank() -> matchedSubject.clusterArea
                    matchedSubject.geDs == "GE" && matchedSubject.clusterArea.isBlank() -> "GS"
                    else -> matchedSubject.clusterArea.ifBlank { matchedSubject.cluster }
                }
                if (autoValue.isNotBlank()) cluster = autoValue
                creds = matchedSubject.credits.toString()
            }
        }

        val isClusterLocked = matchedSubject != null && (
            matchedSubject.geDs.isNotBlank() || matchedSubject.clusterArea.isNotBlank() || matchedSubject.cluster.isNotBlank()
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingGrade = null },
            title = { Text(if (editingGrade == null) "Add Course Grade" else "Edit Course Grade", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mySubjects.isNotEmpty() && editingGrade == null) {
                        Text("Select Enrolled Course:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mySubjects) { sub ->
                                val isEntered = grades.any { it.code == sub.code }
                                FilterChip(
                                    selected = code == sub.code,
                                    onClick = {
                                        code = sub.code
                                        cluster = when (sub.geDs) {
                                            "DS" -> "DS"
                                            "GE" if sub.clusterArea.isNotBlank() -> sub.clusterArea
                                            "GE" if sub.clusterArea.isBlank() -> "GS"
                                            else -> sub.clusterArea.ifBlank { sub.cluster }
                                        }
                                        creds = sub.credits.toString()
                                    },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sub.code)
                                            if (isEntered) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(
                        value = creds, onValueChange = { if (matchedSubject == null) creds = it }, 
                        label = { Text("Credits") }, modifier = Modifier.fillMaxWidth(), 
                        singleLine = true, shape = RoundedCornerShape(12.dp), enabled = matchedSubject == null
                    )
                    Text("Select Grade:", style = MaterialTheme.typography.labelSmall)
                    val gradesList = listOf("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F", "P")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gradesList) { g ->
                            FilterChip(selected = gradeValue == g, onClick = { gradeValue = g }, label = { Text(g) })
                        }
                    }
                    OutlinedTextField(
                        value = cluster, onValueChange = { if (!isClusterLocked) cluster = it }, 
                        label = { Text("Cluster or DS") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), enabled = !isClusterLocked
                    )
                    OutlinedTextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (code.isNotBlank()) {
                        // Find spec from either current subjects or the full specification list
                        val specFromAll = allSubjects.find { it.code.equals(code, true) }
                        val specFromList = subjectSpecs.find { it.code.equals(code, true) }
                        
                        val finalCompulsoryStatus = specFromAll?.compulsoryElective 
                            ?: specFromList?.compulsoryElective 
                            ?: if (cluster.isBlank()) "Compulsory" else "Elective"
                            
                        val finalGeDs = specFromAll?.geDs 
                            ?: specFromList?.geDs 
                            ?: when {
                                cluster.uppercase() == "DS" -> "DS"
                                cluster.uppercase() in listOf("A", "M", "N", "E", "D", "GS") -> "GE"
                                else -> ""
                            }

                        val newGrade = CourseGrade(
                            id = editingGrade?.id ?: java.util.UUID.randomUUID().toString(),
                            code = code, 
                            name = specFromList?.title ?: specFromAll?.name ?: "", 
                            credits = creds.toIntOrNull() ?: 3,
                            grade = gradeValue, 
                            semester = semester,
                            cluster = cluster.uppercase(),
                            compulsoryElective = finalCompulsoryStatus,
                            geDs = finalGeDs,
                            program = specFromList?.programme ?: specFromAll?.program ?: ""
                        )
                        if (editingGrade == null) academicViewModel.addGrade(newGrade) else academicViewModel.updateGrade(newGrade)
                        showAddDialog = false; editingGrade = null
                    }
                }, shape = RoundedCornerShape(12.dp)) { Text(if (editingGrade == null) "Add" else "Save Changes") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; editingGrade = null }) { Text("Cancel") } }
        )
    }
}
