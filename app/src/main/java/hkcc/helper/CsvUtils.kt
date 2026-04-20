package hkcc.helper

import hkcc.helper.data.Subject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        when (val c = line[i]) {
            '\"' -> {
                inQuotes = !inQuotes
            }
            ',' if !inQuotes -> {
                result.add(current.toString().trim())
                current = StringBuilder()
            }
            else -> {
                current.append(c)
            }
        }
        i++
    }
    result.add(current.toString().trim())
    return result
}

fun parseCsvStream(inputStream: InputStream, specList: List<SubjectSpec> = emptyList(), programTitle: String = ""): List<Subject> {
    val subjects = mutableListOf<Subject>()
    try {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var curCode = ""; var curName = ""; var curClass = ""
        reader.useLines { lines ->
            lines.forEach { line ->
                val t = if (line.contains("\t")) line.split("\t").map { it.trim() } else parseCsvLine(line)
                if (t.size >= 7) {
                    if (t[0].isNotEmpty()) curCode = t[0]; if (t[1].isNotEmpty()) curName = t[1]; if (t[2].isNotEmpty()) curClass = t[2]
                    if (curCode.contains("Subject Code", true)) return@forEach
                    val timeRange = t.getOrElse(6) { "" }
                    if (curCode.isNotEmpty() && timeRange.contains("-")) {
                        val times = timeRange.split("-")
                        // Find spec matching code AND program title (if title matches)
                        // Note: Some specs have empty programme (apply to all)
                        val spec = specList.find { 
                            it.code == curCode && (it.programme.isBlank() || it.programme.equals(programTitle, true))
                        } ?: specList.find { it.code == curCode } // fallback to first matching code if program specific one not found

                        subjects.add(Subject(
                            code = curCode, name = curName, classNo = curClass,
                            subGroup = t.getOrElse(3){""}, type = t.getOrElse(4){""}, dayOfWeek = t.getOrElse(5){""},
                            startTime = times[0].trim(), endTime = times[1].trim(), campus = t.getOrElse(7){""},
                            venue = t.getOrElse(8){""}, lecturer = t.getOrElse(9){""},
                            cluster = spec?.clusterArea ?: t.getOrElse(10){""},
                            clusterArea = spec?.clusterArea ?: "",
                            compulsoryElective = spec?.compulsoryElective ?: "",
                            geDs = spec?.geDs ?: "",
                            program = spec?.programme ?: "",
                            credits = spec?.credit?.toIntOrNull() ?: 3
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return subjects
}

data class SubjectSpec(
    val code: String,
    val title: String,
    val clusterArea: String,
    val credit: String,
    val compulsoryElective: String,
    val geDs: String,
    val programme: String
)

fun parseSubjectSpecCsv(inputStream: InputStream): List<SubjectSpec> {
    val specs = mutableListOf<SubjectSpec>()
    try {
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.useLines { lines ->
            lines.drop(1).forEach { line ->
                val t = parseCsvLine(line)
                if (t.size >= 7) {
                    val code = t[0]
                    if (code.isNotEmpty()) {
                        specs.add(SubjectSpec(
                            code = code,
                            title = t[1],
                            clusterArea = t[2],
                            credit = t[3],
                            compulsoryElective = t[4],
                            geDs = t[5],
                            programme = t[6]
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return specs
}

data class StudyPatternRow(
    val programCode: String,
    val programTitle: String,
    val semester: String,
    val requiredCredit: String,
    val studyPattern: String,
    val subjectCode: String,
    val geElective: String,
    val geDsElective: String,
    val dsElective: String,
    val cantonesePutonghua: String,
    val engLevel: String,
    val totalDs: String,
    val totalGe: String
)

fun parseStudyPatternCsv(inputStream: InputStream): List<StudyPatternRow> {
    val rows = mutableListOf<StudyPatternRow>()
    try {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var lastProgramCode = ""
        var lastProgramTitle = ""
        var lastSemester = ""
        var lastRequiredCredit = ""
        var lastStudyPattern = ""
        var lastGeElective = ""
        var lastGeDsElective = ""
        var lastDsElective = ""
        var lastCantonesePutonghua = ""
        var lastEngLevel = ""
        var lastTotalDs = ""
        var lastTotalGe = ""

        reader.useLines { lines ->
            lines.drop(1).forEach { line ->
                val t = parseCsvLine(line)
                if (t.size >= 13) {
                    if (t[0].isNotEmpty()) lastProgramCode = t[0]
                    if (t[1].isNotEmpty()) lastProgramTitle = t[1]
                    if (t[2].isNotEmpty()) lastSemester = t[2]
                    if (t[3].isNotEmpty()) lastRequiredCredit = t[3]
                    if (t[4].isNotEmpty()) lastStudyPattern = t[4]
                    
                    // Subject code is at index 5
                    val subjectCode = t[5]
                    
                    // Electives at 6, 7, 8
                    if (t[6].isNotEmpty()) lastGeElective = t[6] else if (t[0].isNotEmpty()) lastGeElective = ""
                    if (t[7].isNotEmpty()) lastGeDsElective = t[7] else if (t[0].isNotEmpty()) lastGeDsElective = ""
                    if (t[8].isNotEmpty()) lastDsElective = t[8] else if (t[0].isNotEmpty()) lastDsElective = ""

                    // Cantonese/Putonghua is at index 9, Eng Level at 10
                    if (t[9].isNotEmpty()) lastCantonesePutonghua = t[9]
                    if (t[10].isNotEmpty()) lastEngLevel = t[10]
                    
                    // Total DS at 11, Total GE at 12
                    if (t[11].isNotEmpty()) lastTotalDs = t[11]
                    if (t[12].isNotEmpty()) lastTotalGe = t[12]

                    if (subjectCode.isNotEmpty()) {
                        rows.add(StudyPatternRow(
                            programCode = lastProgramCode,
                            programTitle = lastProgramTitle,
                            semester = lastSemester,
                            requiredCredit = lastRequiredCredit,
                            studyPattern = lastStudyPattern,
                            subjectCode = subjectCode,
                            geElective = lastGeElective,
                            geDsElective = lastGeDsElective,
                            dsElective = lastDsElective,
                            cantonesePutonghua = lastCantonesePutonghua,
                            engLevel = lastEngLevel,
                            totalDs = lastTotalDs,
                            totalGe = lastTotalGe
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return rows
}
