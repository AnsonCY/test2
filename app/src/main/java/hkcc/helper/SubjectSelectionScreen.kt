package hkcc.helper

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hkcc.helper.data.Subject

@Composable
fun SubjectSelectionScreen(viewModel: TimetableViewModel) {
    val allSubjects by viewModel.subjects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectedCodes by viewModel.selectedSubjectCodes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val clusterFilter by viewModel.clusterFilter.collectAsState()

    val filtered = remember(allSubjects, searchQuery, clusterFilter) {
        allSubjects.filter {
            (it.code.contains(searchQuery, true) || it.name.contains(searchQuery, true) || it.lecturer.contains(searchQuery, true)) &&
            (clusterFilter == "All" || it.cluster == clusterFilter)
        }
    }

    val clusters = remember(allSubjects) { listOf("All") + allSubjects.map { it.cluster }.filter { it.isNotEmpty() }.distinct().sorted() }

    val grouped = remember(filtered) { filtered.groupBy { it.code } }
    val sortedGroupKeys = remember(grouped, selectedCodes) { grouped.keys.sortedWith(compareByDescending<String> { selectedCodes.contains(it) }.thenBy { it }) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = searchQuery, onValueChange = { viewModel.updateSearch(it) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), placeholder = { Text("Search Code, Lecturer...") }, leadingIcon = { Icon(Icons.Filled.Search, "") }, singleLine = true, shape = RoundedCornerShape(12.dp))

        LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(clusters) { cluster ->
                FilterChip(
                    selected = clusterFilter == cluster,
                    onClick = { viewModel.updateClusterFilter(cluster) },
                    label = { Text(cluster) }
                )
            }
        }

        LazyColumn {
            items(sortedGroupKeys) { code ->
                SubjectGroupCard(
                    code = code,
                    subjects = grouped[code]!!,
                    selectedIds = selectedIds,
                    isInterested = selectedCodes.contains(code),
                    onToggleInterest = { viewModel.toggleSubjectInterest(code) },
                    onSelectClass = { subject -> viewModel.selectSubject(subject) }
                )
            }
        }
    }
}

@Composable
fun SubjectGroupCard(code: String, subjects: List<Subject>, selectedIds: Set<String>, isInterested: Boolean, onToggleInterest: () -> Unit, onSelectClass: (Subject) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = subjects.firstOrNull()?.name ?: ""
    val containerColor = if (isInterested) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isInterested) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = containerColor), border = BorderStroke(1.dp, borderColor), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isInterested, onCheckedChange = { onToggleInterest() })
                Column(Modifier.weight(1f).clickable { expanded = !expanded }) { Text(code, fontWeight = FontWeight.Bold); Text(name, style = MaterialTheme.typography.bodySmall) }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, "") }
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    val classes = subjects.groupBy { it.classNo }.toSortedMap()
                    classes.forEach { (cls, list) ->
                        val lecture = list.find { it.isLecture() }
                        val tuts = list.filter { !it.isLecture() }.sortedBy { it.subGroup }
                        if (lecture != null) {
                            val isSel = selectedIds.contains(lecture.id)
                            Row(Modifier.clickable { onSelectClass(lecture) }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(isSel, { onSelectClass(lecture) })
                                Column {
                                    Text("Class $cls (Lect) @ ${lecture.venue}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if(lecture.lecturer.isNotEmpty()) Text("Lecturer: ${lecture.lecturer}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (isSel && tuts.isNotEmpty()) {
                                Column(Modifier.padding(start = 32.dp)) {
                                    tuts.forEach { tut ->
                                        Row(Modifier.clickable { onSelectClass(tut) }, verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(selectedIds.contains(tut.id), { onSelectClass(tut) })
                                            Column {
                                                Text("Group ${tut.subGroup} @ ${tut.venue}", style = MaterialTheme.typography.bodySmall)
                                                if(tut.lecturer.isNotEmpty()) Text("Tutor: ${tut.lecturer}", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
