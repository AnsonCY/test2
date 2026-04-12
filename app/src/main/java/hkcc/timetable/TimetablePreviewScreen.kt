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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hkcc.timetable.data.Subject
import java.time.LocalDate
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

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isGridView) "Weekly Schedule" else "Class List",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${solidSubjects.size} sessions confirmed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { isGridView = !isGridView }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Filled.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (isGridView) "List" else "Grid",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { showShareOptions = true }) {
                            Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (solidSubjects.isEmpty() && !showGhosting) {
                EmptyTimetableState()
            } else {
                if (isGridView) {
                    TimetableGrid(solidSubjects, ghostSubjects, viewModel, showGhosting)
                } else {
                    TimetableListView(solidSubjects)
                }
            }
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
    val startHour = 8; val endHour = 22; val hourHeight = 80.dp; val headerHeight = 64.dp; val timeColWidth = 60.dp
    val scrollState = rememberScrollState()
    val todayIdx = LocalDate.now().dayOfWeek.value - 1 // 0=Mon, 6=Sun

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxSize()) {
            // Time Column
            Column(
                Modifier
                    .width(timeColWidth)
                    .padding(top = headerHeight)
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .drawBehind {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
            ) {
                for (h in startHour..endHour) {
                    Box(
                        Modifier
                            .height(hourHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d:00", h),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Grid Content
            Column(Modifier.weight(1f)) {
                // Day Headers
                Row(
                    Modifier
                        .height(headerHeight)
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    days.forEachIndexed { index, day ->
                        val isToday = index == todayIdx
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day,
                                    fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                                if (isToday) {
                                    Box(
                                        Modifier
                                            .padding(top = 4.dp)
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    // Background Grid with alternating column colors
                    Row(Modifier.fillMaxWidth().height(hourHeight * (endHour - startHour + 1))) {
                        days.forEachIndexed { index, _ ->
                            val isToday = index == todayIdx
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                            index % 2 == 0 -> Color.Transparent
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                        }
                                    )
                                    .drawBehind {
                                        drawLine(
                                            color = Color.LightGray.copy(alpha = 0.15f),
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 1f
                                        )
                                    }
                            )
                        }
                    }

                    // Horizontal Grid Lines with thicker lines for midday
                    Column {
                        for (i in startHour..endHour) {
                            val linePrimary = MaterialTheme.colorScheme.primary
                            val lineOutline = MaterialTheme.colorScheme.outlineVariant
                            Box(
                                Modifier
                                    .height(hourHeight)
                                    .fillMaxWidth()
                                    .drawBehind {
                                        val isMidday = i == 13
                                        drawLine(
                                            color = if(isMidday) linePrimary.copy(alpha = 0.3f) else lineOutline.copy(alpha = 0.5f),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = if(isMidday) 2f else 1f
                                        )
                                    }
                            )
                        }
                    }

                    ghostSubjects.forEach { sub ->
                        TimetableBlock(sub, startHour, hourHeight, isGhost = true, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) })
                    }
                    solidSubjects.forEach { sub ->
                        TimetableBlock(sub, startHour, hourHeight, isGhost = false, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) })
                    }
                    CurrentTimeIndicator(startHour, hourHeight)
                }
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
        Box(
            Modifier
                .fillMaxWidth()
                .offset(y = topOffset - 4.dp)
                .height(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(Modifier.fillMaxWidth()) {
                drawLine(
                    color = Color.Red.copy(alpha = 0.6f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2f
                )
                drawCircle(
                    color = Color.Red,
                    radius = 4.dp.toPx(),
                    center = Offset(0f, size.height / 2)
                )
            }
        }
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

        val textColor = if (isGhost) MaterialTheme.colorScheme.primary else Color.White
        val containerColor = if (isGhost) MaterialTheme.colorScheme.surface else subject.color
        
        val modifier = if (isGhost) {
            Modifier
                .dashedBorder(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), 12.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        } else {
            Modifier
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                .background(containerColor, RoundedCornerShape(12.dp))
                .drawBehind {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.15f),
                        topLeft = Offset(0f, 0f),
                        size = size.copy(height = 4.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
        }

        val formattedCode = remember(subject.code) {
            // Split code into Letters and Digits for better wrapping, e.g., SEHH1234 -> SEHH\n1234
            val regex = "([A-Z|a-z]+)(\\d+)".toRegex()
            regex.replace(subject.code, "$1\n$2")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = topOffset)
        ) {
            if (dayIdx > 0) Spacer(Modifier.weight(dayIdx.toFloat()))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(2.dp)
                    .then(modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isClickable) { onClick() }
                    .padding(4.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = formattedCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            color = textColor,
                            softWrap = true
                        )
                        if (isGhost) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Surface(
                        color = if (isGhost) Color.Transparent else Color.Black.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = subject.getShortTypeAndGroup(),
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.sp,
                            lineHeight = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGhost) textColor else Color.White.copy(alpha = 0.95f),
                            softWrap = true
                        )
                    }

                    if (height > 55.dp) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                modifier = Modifier.size(8.dp).padding(top = 1.dp),
                                tint = textColor.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(1.dp))
                            Text(
                                text = subject.venue,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 7.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.8f),
                                softWrap = true
                            )
                        }
                    }
                }
            }
            if (dayIdx < 5) Spacer(Modifier.weight((5 - dayIdx).toFloat()))
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

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedByDay.forEach { (day, dailySubjects) ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                }
            }

            items(dailySubjects) { subject ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            Modifier
                                .width(10.dp)
                                .fillMaxHeight()
                                .background(subject.color)
                        )
                        Column(Modifier.padding(20.dp).weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject.code,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = subject.getShortTypeAndGroup(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                InfoTag(Icons.Default.DateRange, "${subject.startTime} - ${subject.endTime}")
                                InfoTag(Icons.Default.Info, subject.venue)
                            }
                            
                            if (subject.lecturer.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = subject.lecturer,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
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
}

@Composable
fun InfoTag(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyTimetableState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.DateRange,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Empty Timetable",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            "Go to 'Select Subjects' or 'Auto Plan' to build your schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
