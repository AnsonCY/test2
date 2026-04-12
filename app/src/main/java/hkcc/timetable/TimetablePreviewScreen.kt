package hkcc.timetable

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.graphics.toColorInt

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
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
        val activeDayIndices = subjects.map { it.getDayIndex() }.filter { it in 0..5 }.distinct().sorted()
        val displayDays = if (activeDayIndices.isEmpty()) (0..4).toList() else activeDayIndices

        val surfaceVariant = "#F5F5F5".toColorInt()
        val gridLineColor = "#EEEEEE".toColorInt()

        val paint = Paint()
        val textPaint = TextPaint().apply { isAntiAlias = true; color = android.graphics.Color.GRAY }
        val boldPaint = TextPaint().apply { isAntiAlias = true; color = android.graphics.Color.BLACK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // Layout Constants - No title, more space for grid
        val startHour = 8
        val endHour = 22
        val totalHours = endHour - startHour + 1
        val margin = 30f
        val timeColWidth = 45f
        val topPadding = 45f 
        val headerHeight = 30f
        
        val gridAreaWidth = pageWidth - (margin * 2) - timeColWidth
        val colWidth = gridAreaWidth / displayDays.size
        val hourHeight = (pageHeight - topPadding - margin) / totalHours

        // 1. Draw Day Headers
        boldPaint.textSize = 10f
        boldPaint.color = "#444444".toColorInt()
        displayDays.forEachIndexed { index, dayIdx ->
            val x = margin + timeColWidth + (index * colWidth)
            val dayName = allDays[dayIdx]
            paint.color = surfaceVariant
            canvas.drawRoundRect(x + 2f, topPadding - headerHeight, x + colWidth - 2f, topPadding - 5f, 6f, 6f, paint)
            val textWidth = boldPaint.measureText(dayName)
            canvas.drawText(dayName, x + (colWidth - textWidth) / 2, topPadding - 15f, boldPaint)
        }

        // 2. Grid & Time Labels - Cleaned up
        textPaint.textSize = 8f
        textPaint.textAlign = Paint.Align.RIGHT
        for (h in 0 until totalHours) {
            val y = topPadding + (h * hourHeight)
            val timeStr = String.format(Locale.US, "%02d:00", h + startHour)
            canvas.drawText(timeStr, margin + timeColWidth - 8f, y + 3f, textPaint)
            
            paint.color = gridLineColor
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin + timeColWidth, y, pageWidth - margin, y, paint)
        }

        for (i in 0..displayDays.size) {
            val x = margin + timeColWidth + (i * colWidth)
            paint.color = gridLineColor
            canvas.drawLine(x, topPadding, x, topPadding + (totalHours - 1) * hourHeight, paint)
        }

        // 3. Draw Subjects - Improved readability and wrapping
        subjects.forEach { sub ->
            val dayIdxInDisplay = displayDays.indexOf(sub.getDayIndex())
            if (dayIdxInDisplay != -1) {
                val startMin = sub.getStartMinutes()
                val endMin = sub.getEndMinutes()
                val startY = topPadding + (startMin - (startHour * 60)) * (hourHeight / 60f)
                val blockHeight = (endMin - startMin) * (hourHeight / 60f)
                val startX = margin + timeColWidth + (dayIdxInDisplay * colWidth)

                paint.color = sub.color.toArgb()
                canvas.drawRoundRect(startX + 2f, startY + 1f, startX + colWidth - 2f, startY + blockHeight - 1f, 8f, 8f, paint)

                boldPaint.color = android.graphics.Color.WHITE
                val isWide = colWidth > 75f
                boldPaint.textSize = if (isWide) 9f else 7.5f
                
                // Smart code wrapping
                val codeToDraw = if (!isWide) {
                    val regex = "([A-Z|a-z]+)(\\d+)".toRegex()
                    regex.replace(sub.code, "$1\n$2")
                } else sub.code
                
                val lines = codeToDraw.split("\n")
                var currentY = startY + 8f + boldPaint.textSize
                lines.forEach { line ->
                    canvas.drawText(line, startX + 6f, currentY, boldPaint)
                    currentY += boldPaint.textSize + 1f
                }

                boldPaint.textSize = if (isWide) 8f else 6.5f
                currentY += 2f
                canvas.drawText(sub.getShortTypeAndGroup(), startX + 6f, currentY, boldPaint)
                
                textPaint.color = android.graphics.Color.WHITE
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.textSize = if (isWide) 7.5f else 6f
                
                if (blockHeight > 40f) {
                    currentY += textPaint.textSize + 3f
                    canvas.drawText(sub.venue, startX + 6f, currentY, textPaint)
                    if (sub.lecturer.isNotEmpty() && blockHeight > 55f) {
                        currentY += textPaint.textSize + 2f
                        canvas.drawText(sub.lecturer, startX + 6f, currentY, textPaint)
                    }
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
            Toast.makeText(context, "PDF Saved to Documents!", Toast.LENGTH_LONG).show()
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
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT")
    val activeDayIndices = remember(solidSubjects, ghostSubjects) {
        val indices = (solidSubjects + ghostSubjects).map { it.getDayIndex() }.filter { it in 0..5 }.toMutableSet()
        val today = LocalDate.now().dayOfWeek.value - 1
        if (today in 0..5) indices.add(today) 
        if (indices.isEmpty()) (0..4).toSet() else indices.toSortedSet()
    }.toList()
    
    val startHour = 8; val endHour = 22; val hourHeight = 84.dp; val headerHeight = 64.dp; val timeColWidth = 64.dp
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
                    activeDayIndices.forEach { dayIdx ->
                        val day = allDays[dayIdx]
                        val isToday = dayIdx == todayIdx
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
                    val totalGridHeight = hourHeight * (endHour - startHour + 1)
                    
                    // Background Grid with alternating column colors
                    Row(Modifier.fillMaxWidth().height(totalGridHeight)) {
                        activeDayIndices.forEachIndexed { index, dayIdx ->
                            val isToday = dayIdx == todayIdx
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

                    // Horizontal Grid Lines
                    Column(Modifier.fillMaxWidth().height(totalGridHeight)) {
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
                                            color = if(isMidday) linePrimary.copy(alpha = 0.3f) else lineOutline.copy(alpha = 0.4f),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = if(isMidday) 2f else 1f
                                        )
                                    }
                            )
                        }
                    }

                    // Subjects rendered in a Row of Columns
                    Row(Modifier.fillMaxWidth().height(totalGridHeight)) {
                        activeDayIndices.forEach { dayIdx ->
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                ghostSubjects.filter { it.getDayIndex() == dayIdx }.forEach { sub ->
                                    TimetableBlock(sub, startHour, hourHeight, isGhost = true, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) })
                                }
                                solidSubjects.filter { it.getDayIndex() == dayIdx }.forEach { sub ->
                                    TimetableBlock(sub, startHour, hourHeight, isGhost = false, isClickable = showGhosting, onClick = { viewModel.selectSubject(sub) })
                                }
                            }
                        }
                    }

                    if (activeDayIndices.contains(todayIdx)) {
                        val activeIdx = activeDayIndices.indexOf(todayIdx)
                        CurrentTimeIndicator(
                            startHour = startHour,
                            hourHeight = hourHeight,
                            todayColumnIdx = activeIdx,
                            totalColumns = activeDayIndices.size
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CurrentTimeIndicator(startHour: Int, hourHeight: Dp, todayColumnIdx: Int, totalColumns: Int) {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute
    val startMinutes = startHour * 60
    val endMinutes = 22 * 60
    
    if (currentMinutes in startMinutes..endMinutes) {
        val minutesSinceStart = currentMinutes - startMinutes
        val topOffset = hourHeight * minutesSinceStart / 60
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            Modifier
                .fillMaxWidth()
                .offset(y = topOffset - 4.dp)
                .height(8.dp)
        ) {
            // Faint full-width guide line
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawLine(
                    color = Color.Red.copy(alpha = 0.15f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // Bold line and dot for today
            Row(Modifier.fillMaxWidth().height(8.dp)) {
                if (todayColumnIdx >= 0) {
                    if (todayColumnIdx > 0) Spacer(Modifier.weight(todayColumnIdx.toFloat()))
                    
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        Canvas(Modifier.fillMaxWidth()) {
                            drawLine(
                                color = Color.Red.copy(alpha = 0.8f),
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        // Pulsing dot at the very start of the day column
                        Box(
                            Modifier
                                .size(10.dp)
                                .offset(x = (-5).dp) // Center the dot on the left edge
                                .shadow(4.dp, CircleShape)
                                .background(Color.Red.copy(alpha = alpha), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                    
                    if (todayColumnIdx < totalColumns - 1) {
                        Spacer(Modifier.weight((totalColumns - 1 - todayColumnIdx).toFloat()))
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableBlock(subject: Subject, startHour: Int, hourHeight: Dp, isGhost: Boolean, isClickable: Boolean = true, onClick: () -> Unit) {
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .offset(y = topOffset)
            .padding(2.dp)
            .then(modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isClickable) { onClick() }
    ) {
        val scope = this
        val availableWidth = scope.maxWidth
        val availableHeight = scope.maxHeight
        
        // Dynamic styling based on horizontal space
        val isWide = availableWidth > 80.dp
        val codeFontSize = if (isWide) 10.sp else 8.sp
        val detailFontSize = if (isWide) 9.sp else 7.sp
        val lineSpacing = if (isWide) 11.sp else 9.sp

        val formattedCode = remember(subject.code, isWide) {
            if (isWide) {
                subject.code 
            } else {
                // Split code into Letters and Digits for better wrapping
                val regex = "([A-Z|a-z]+)(\\d+)".toRegex()
                regex.replace(subject.code, "$1\n$2")
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(if (isWide) 2.dp else 1.dp),
            modifier = Modifier.fillMaxSize().padding(if (isWide) 6.dp else 4.dp)
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
                    fontSize = codeFontSize,
                    lineHeight = lineSpacing,
                    color = textColor,
                    softWrap = true
                )
                if (isGhost) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        modifier = Modifier.size(if (isWide) 12.dp else 10.dp),
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
                    fontSize = detailFontSize,
                    lineHeight = detailFontSize * 1.2f,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isGhost) textColor else Color.White.copy(alpha = 0.95f),
                    softWrap = true
                )
            }

            if (availableHeight > 55.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        modifier = Modifier.size(if (isWide) 11.dp else 9.dp).padding(top = 1.dp),
                        tint = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = subject.venue,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = detailFontSize,
                        lineHeight = detailFontSize * 1.1f,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.8f),
                        softWrap = true
                    )
                }
            }
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
