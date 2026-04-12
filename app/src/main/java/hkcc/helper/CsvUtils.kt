package hkcc.helper

import hkcc.helper.data.Subject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

fun parseCsvStream(inputStream: InputStream): List<Subject> {
    val subjects = mutableListOf<Subject>()
    try {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var curCode = ""; var curName = ""; var curClass = ""
        reader.useLines { lines ->
            lines.forEach { line ->
                var t = line.split("\t").map { it.trim() }
                if (t.size < 5) t = line.split(",").map { it.trim() }
                if (t.size >= 7) {
                    if (t[0].isNotEmpty()) curCode = t[0]; if (t[1].isNotEmpty()) curName = t[1]; if (t[2].isNotEmpty()) curClass = t[2]
                    if (curCode.contains("Subject Code", true)) return@forEach
                    val timeRange = t.getOrElse(6) { "" }
                    if (curCode.isNotEmpty() && timeRange.contains("-")) {
                        val times = timeRange.split("-")
                        subjects.add(Subject(
                            code = curCode, name = curName, classNo = curClass,
                            subGroup = t.getOrElse(3){""}, type = t.getOrElse(4){""}, dayOfWeek = t.getOrElse(5){""},
                            startTime = times[0].trim(), endTime = times[1].trim(), campus = t.getOrElse(7){""},
                            venue = t.getOrElse(8){""}, lecturer = t.getOrElse(9){""},
                            cluster = t.getOrElse(10){""}
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return subjects
}
