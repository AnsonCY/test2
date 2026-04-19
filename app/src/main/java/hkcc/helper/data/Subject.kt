package hkcc.helper.data

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.util.UUID

data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val classNo: String,
    val subGroup: String,
    val type: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val campus: String,
    val venue: String,
    val lecturer: String = "",
    val cluster: String = "", // e.g. "Cluster area 1", "DS", "GS"
    val clusterArea: String = "",
    val compulsoryElective: String = "",
    val geDs: String = "",
    val program: String = "",
    val credits: Int = 3,
    val color: Color = generateColor(code)
) {
    fun isLecture(): Boolean = type.contains("Lect", ignoreCase = true)

    fun getFullTime(): String = "$dayOfWeek $startTime - $endTime"

    fun getStartMinutes(): Int = parseTimeToMinutes(startTime)
    fun getEndMinutes(): Int = parseTimeToMinutes(endTime)

    fun getShortTypeAndGroup(): String {
        return if (isLecture()) "Lec $classNo"
        else "Tut $classNo$subGroup"
    }

    fun getCampusType(): String {
        val loc = (campus + venue).uppercase()
        return when {
            loc.contains("ONLINE") -> "ONLINE"
            loc.contains("HHB") -> "HHB"
            loc.contains("WK") || loc.contains("WEST KOWLOON") ||
                    loc.contains("-S") || loc.contains("-N") || loc.contains("UG") -> "WK"
            else -> "OTHER"
        }
    }

    fun isOnCampus(): Boolean = getCampusType() != "ONLINE"

    fun getDayIndex(): Int {
        return when (dayOfWeek.trim().uppercase().take(3)) {
            "MON" -> 0; "TUE" -> 1; "WED" -> 2; "THU" -> 3; "FRI" -> 4; "SAT" -> 5; else -> -1
        }
    }

    fun toUniqueSignature(): String = "$code|$classNo|$subGroup|$type|$dayOfWeek|$startTime"

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("code", code)
            put("name", name)
            put("classNo", classNo)
            put("subGroup", subGroup)
            put("type", type)
            put("dayOfWeek", dayOfWeek)
            put("startTime", startTime)
            put("endTime", endTime)
            put("campus", campus)
            put("venue", venue)
            put("lecturer", lecturer)
            put("cluster", cluster)
            put("clusterArea", clusterArea)
            put("compulsoryElective", compulsoryElective)
            put("geDs", geDs)
            put("program", program)
            put("credits", credits)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Subject {
            return Subject(
                id = json.optString("id", UUID.randomUUID().toString()),
                code = json.getString("code"),
                name = json.getString("name"),
                classNo = json.getString("classNo"),
                subGroup = json.getString("subGroup"),
                type = json.getString("type"),
                dayOfWeek = json.getString("dayOfWeek"),
                startTime = json.getString("startTime"),
                endTime = json.getString("endTime"),
                campus = json.getString("campus"),
                venue = json.getString("venue"),
                lecturer = json.optString("lecturer", ""),
                cluster = json.optString("cluster", ""),
                clusterArea = json.optString("clusterArea", ""),
                compulsoryElective = json.optString("compulsoryElective", ""),
                geDs = json.optString("geDs", ""),
                program = json.optString("program", ""),
                credits = json.optInt("credits", 3)
            )
        }
    }

    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":").map { it.trim().toInt() }
            parts[0] * 60 + parts[1]
        } catch (e: Exception) { 0 }
    }
}

fun generateColor(seed: String): Color {
    val hash = seed.hashCode()
    val r = ((hash and 0xFF0000) shr 16) / 255f
    val g = ((hash and 0x00FF00) shr 8) / 255f
    val b = (hash and 0x0000FF) / 255f
    return Color(0.4f + 0.4f * r, 0.45f + 0.4f * g, 0.5f + 0.4f * b, 1f)
}