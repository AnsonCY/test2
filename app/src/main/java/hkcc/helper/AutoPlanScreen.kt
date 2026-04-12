package hkcc.helper

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPlanScreen(navController: NavController, viewModel: TimetableViewModel) {
    val results by viewModel.generatedSchedules.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val selectedDays = remember { mutableStateListOf<String>() }

    var avoidMorning by remember { mutableStateOf(false) }
    var avoidAfternoon by remember { mutableStateOf(false) }
    var avoidEvening by remember { mutableStateOf(false) }
    var useFixedSections by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PreferenceHeader("Days to AVOID campus", Icons.Default.DateRange)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedDays.remove(day) else selectedDays.add(day) },
                            label = { 
                                Text(
                                    text = day.take(3), 
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                PreferenceHeader("Time Preferences", Icons.Default.Info)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeChip("No Morning", avoidMorning) { avoidMorning = it }
                    TimeChip("No Afternoon", avoidAfternoon) { avoidAfternoon = it }
                    TimeChip("No Evening", avoidEvening) { avoidEvening = it }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = useFixedSections, onCheckedChange = { useFixedSections = it })
                    Column(Modifier.padding(start = 4.dp).weight(1f)) {
                        Text("Prioritize Manual Selection", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Keep current manual selections", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.generateAutoPlan(
                    selectedDays.toSet(),
                    avoidMorning, avoidAfternoon, avoidEvening,
                    useFixedSections
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isGenerating,
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Calculating...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(12.dp))
                Text("Generate Plans", fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (results.isNotEmpty()) {
            Text(
                "Generated Options (${results.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }

        Box(Modifier.weight(1f)) {
            if (results.isEmpty() && !isGenerating) {
                EmptyState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(results) { index, schedule ->
                        PlanResultCard(index, schedule) {
                            viewModel.applySchedule(schedule)
                            navController.navigate("preview_timetable")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Black, 
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label, maxLines = 1, softWrap = false) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text("No plans yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun PlanResultCard(index: Int, schedule: List<hkcc.helper.data.Subject>, onApply: () -> Unit) {
    val scheduleByDay = schedule.sortedBy { it.getDayIndex() }.groupBy { it.dayOfWeek }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Option ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${schedule.size} Classes", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scheduleByDay.forEach { (day, subjects) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = day.take(3),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(44.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            subjects.sortedBy { it.getStartMinutes() }.forEach { sub ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(Modifier.size(8.dp).padding(top = 4.dp).clip(CircleShape).background(sub.color))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${sub.startTime} - ${sub.code}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${sub.getShortTypeAndGroup()} | ${sub.venue}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (sub.lecturer.isNotEmpty()) {
                                            Text(
                                                text = sub.lecturer,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply This Plan")
            }
        }
    }
}
