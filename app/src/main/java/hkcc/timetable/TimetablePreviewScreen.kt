package hkcc.timetable

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hkcc.timetable.data.Subject
import java.time.LocalTime
import java.util.Locale
import androidx.core.graphics.withTranslation

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetablePreviewScreen(viewModel: TimetableViewModel) {
    val allSubjects by viewModel.subjects.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val ghostSubjects by viewModel.ghostSubjects.collectAsState()
    val showGhosting by viewModel.showGhosting.collectAsState()
    val solidSubjects = remember(allSubjects, selectedIds) { allSubjects.filter { selectedIds.contains(it.id) } }
    var isGridView by remember { mutableStateOf(true) }

    val context = LocalContext.current
    var showShareOptions by remember { mutableStateOf(false) }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("Export & Share") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { generateTimetablePdf(context, solidSubjects); showShareOptions = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export as PDF")
                    }
                    Button(
                        onClick = {
                            val config = viewModel.getConfigString()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Timetable Config", config)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Config code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showShareOptions = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share via Config Code")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShareOptions = false }) { Text("Close") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { showShareOptions = true }) { Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { isGridView = false }) { Icon(Icons.AutoMirrored.Filled.List, "List", tint = if(!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { isGridView = true }) { Icon(Icons.Filled.DateRange, "Grid", tint = if(isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        Box(Modifier.fillMaxSize()) {
            if (isGridView) TimetableGrid(solidSubjects, ghostSubjects, viewModel, showGhosting) else TimetableListView(solidSubjects)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateTimetablePdf(context: Context, subjects: List<Subject>) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
        }

        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        val startHour = 8
        val endHour = 22
        val startY = 40f
        val hourHeight = 50f
        val startX = 50f
        val colWidth = (595f - startX - 20f) / 6

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT_BOLD

        paint.textAlign = Paint.Align.RIGHT
        paint.color = android.graphics.Color.BLACK
        for (h in startHour..endHour) {
            val y = startY + (h - startHour) * hourHeight
            canvas.drawText(String.format(Locale.US, "%02d:00", h), startX - 5f, y + 5f, paint)

            paint.color = android.graphics.Color.LTGRAY
            canvas.drawLine(startX, y, 595f - 20f, y, paint)
            paint.color = android.graphics.Color.BLACK
        }

        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
        paint.color = android.graphics.Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        days.forEachIndexed { index, day ->
            val x = startX + index * colWidth + colWidth / 2
            canvas.drawText(day, x, startY - 10f, paint)
        }

        paint.color = android.graphics.Color.LTGRAY
        for (i in 0..6) {
            val x = startX + i * colWidth
            canvas.drawLine(x, startY, x, startY + (endHour - startHour) * hourHeight, paint)
        }

        subjects.forEach { sub ->
            val dayIdx = sub.getDayIndex()
            if (dayIdx in 0..5) {
                val startMin = sub.getStartMinutes()
                val durationMin = sub.getEndMinutes() - startMin

                val x = startX + dayIdx * colWidth
                val y = startY + (startMin - (startHour * 60)) * (hourHeight / 60f)
                val height = durationMin * (hourHeight / 60f)

                paint.color = sub.color.toArgb()
                canvas.drawRect(x + 1f, y + 1f, x + colWidth - 1f, y + height - 1f, paint)

                val content = "${sub.code}\n${sub.getShortTypeAndGroup()}\n${sub.venue}\n${sub.lecturer}"
                val staticLayout = StaticLayout.Builder.obtain(content, 0, content.length, textPaint, (colWidth - 4f).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build()

                canvas.withTranslation(x + 2f, y + 2f) {
                    staticLayout.draw(this)
                }
            }
        }

        pdfDocument.finishPage(page)

        val filename = "Timetable_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/HKCC_Timetable")
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out -> pdfDocument.writeTo(out) }
            Toast.makeText(context, "PDF Saved!", Toast.LENGTH_LONG).show()
        }
        pdfDocument.close()

    } catch (e: Exception) {
        Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetableGrid(solidSubjects: List<Subject>, ghostSubjects: List<Subject>, viewModel: TimetableViewModel, showGhosting: Boolean) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val startHour = 8; val endHour = 22; val hourHeight = 60.dp; val headerHeight = 40.dp; val timeColWidth = 50.dp
    val scrollState = rememberScrollState()

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(timeColWidth).padding(top = headerHeight).verticalScroll(scrollState)) {
            for (h in startHour..endHour) Text(String.format(Locale.US, "%02d:00", h), style = MaterialTheme.typography.labelSmall, modifier = Modifier.height(hourHeight).fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.height(headerHeight).fillMaxWidth()) {
                days.forEach { day -> Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { Text(day, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) } }
            }
            Box(Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                Column { for (i in startHour..endHour) Box(Modifier.height(hourHeight).fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) }
                ghostSubjects.forEach { sub -> TimetableBlock(sub, startHour, hourHeight, isGhost = true, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) }) }
                solidSubjects.forEach { sub -> TimetableBlock(sub, startHour, hourHeight, isGhost = false, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) }) }
                CurrentTimeIndicator(startHour, hourHeight)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CurrentTimeIndicator(startHour: Int, hourHeight: Dp) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute
    val startMinutes = startHour * 60
    if (currentMinutes in startMinutes..(22 * 60)) {
        val minutesSinceStart = currentMinutes - startMinutes
        val topOffset = hourHeight * minutesSinceStart / 60
        Canvas(Modifier.fillMaxWidth().height(2.dp).offset(y = topOffset)) { drawLine(color = Color.Red, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 4f) }
    }
}

@Composable
fun TimetableBlock(subject: Subject, startHour: Int, hourHeight: Dp, isGhost: Boolean, isClickable: Boolean = true, onClick: () -> Unit) {
    val dayIdx = subject.getDayIndex()
    if (dayIdx in 0..5) {
        val startMin = subject.getStartMinutes()
        val durationMin = subject.getEndMinutes() - startMin
        val topOffset = hourHeight * (startMin - startHour * 60) / 60
        val height = hourHeight * durationMin / 60

        val textColor = if (isGhost) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else Color.Black
        val modifier = if (isGhost) Modifier.dashedBorder(1.dp, MaterialTheme.colorScheme.primary, 4.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.1f)) else Modifier.background(subject.color, RoundedCornerShape(4.dp))

        Row(Modifier.fillMaxWidth().height(height).offset(y = topOffset)) {
            if(dayIdx > 0) Spacer(Modifier.weight(dayIdx.toFloat()))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp).then(modifier).clickable(enabled = isClickable) { onClick() }.padding(2.dp)) {
                Column {
                    Text(subject.code, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = textColor, maxLines = 1)
                    Text(subject.getShortTypeAndGroup(), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
                    Text(subject.venue, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = textColor, maxLines = 1)
                    if(subject.lecturer.isNotEmpty()) Text(subject.lecturer, style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = textColor, lineHeight = 8.sp, maxLines = 1)
                }
            }
            if(dayIdx < 5) Spacer(Modifier.weight((5 - dayIdx).toFloat()))
        }
    }
}

fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp) = drawBehind {
    val stroke = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(radius.toPx()))
}

@Composable
fun TimetableListView(subjects: List<Subject>) {
    val sorted = subjects.sortedWith(compareBy({ it.getDayIndex() }, { it.getStartMinutes() }))
    val groupedByDay = sorted.groupBy { it.dayOfWeek }

    LazyColumn(Modifier.fillMaxSize()) {
        groupedByDay.forEach { (day, dailySubjects) ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            items(dailySubjects) { subject ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Box(
                        Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .background(subject.color, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    )
                    Column(Modifier.padding(12.dp).weight(1f)) {
                        Text(subject.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(subject.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(
                            "${subject.startTime} - ${subject.endTime} @ ${subject.venue}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (subject.lecturer.isNotEmpty()) {
                            Text("Lecturer: ${subject.lecturer}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Group: ${subject.classNo} ${subject.subGroup} (${subject.type})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
