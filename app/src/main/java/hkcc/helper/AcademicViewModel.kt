package hkcc.helper

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CourseGrade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val credits: Int,
    val grade: String, // A+, A, B+, etc.
    val semester: String,
    val cluster: String = "",
    val compulsoryElective: String = "",
    val geDs: String = "",
    val program: String = ""
)

data class Deadline(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val dueDate: Long,
    val courseCode: String
)

data class GraduationRequirement(
    val category: String, // e.g., "Total", "Cluster", "DS", "GS"
    val required: Int,
    val earned: Int
)

@OptIn(FlowPreview::class)
class AcademicViewModel(application: Application) : AndroidViewModel(application) {

    private val _grades = MutableStateFlow<List<CourseGrade>>(emptyList())
    val grades: StateFlow<List<CourseGrade>> = _grades.asStateFlow()

    private val _deadlines = MutableStateFlow<List<Deadline>>(emptyList())
    val deadlines: StateFlow<List<Deadline>> = _deadlines.asStateFlow()

    private val _targetGpa = MutableStateFlow("3.0")
    val targetGpa = _targetGpa.asStateFlow()

    private val _remainingCredits = MutableStateFlow("30")
    val remainingCredits = _remainingCredits.asStateFlow()

    private var isLoaded = false

    private val gradePoints = mapOf(
        "A+" to 4.3, "A" to 4.0, "A-" to 3.7,
        "B+" to 3.3, "B" to 3.0, "B-" to 2.7,
        "C+" to 2.3, "C" to 2.0, "C-" to 1.7,
        "D+" to 1.3, "D" to 1.0, "F" to 0.0
    )

    init {
        viewModelScope.launch {
            loadFromDisk() // Suspend until data is fully loaded
            isLoaded = true
            
            // Auto-save logic: watch all state changes and save to disk automatically
            // We only start saving AFTER isLoaded is true
            combine(_grades, _deadlines, _targetGpa, _remainingCredits) { g, d, t, r ->
                Unit
            }.debounce(1000) // Give it a second to settle
                .collect {
                    if (isLoaded) {
                        saveToDisk()
                    }
                }
        }
    }

    fun addGrade(course: CourseGrade) {
        _grades.update { list ->
            // If code AND semester already exists, replace it, otherwise add
            val exists = list.any { it.code.equals(course.code, true) && it.semester == course.semester }
            if (exists) {
                list.map { if (it.code.equals(course.code, true) && it.semester == course.semester) course else it }
            } else {
                list + course
            }
        }
    }

    fun removeGrade(id: String) {
        _grades.update { it.filter { g -> g.id != id } }
    }

    fun updateGrade(updatedGrade: CourseGrade) {
        _grades.update { list ->
            list.map { if (it.id == updatedGrade.id) updatedGrade else it }
        }
    }

    fun addDeadline(deadline: Deadline) {
        _deadlines.update { it + deadline }
    }

    fun removeDeadline(id: String) {
        _deadlines.update { it.filter { d -> d.id != id } }
    }

    fun updateTargetGpa(gpa: String) {
        _targetGpa.value = gpa
    }

    fun updateRemainingCredits(credits: String) {
        _remainingCredits.value = credits
    }

    fun calculateGpa(): Double {
        val filteredGrades = _grades.value.filter { gradePoints.containsKey(it.grade) }
        if (filteredGrades.isEmpty()) return 0.0
        val totalPoints = filteredGrades.sumOf { gradePoints[it.grade]!! * it.credits }
        val totalCredits = filteredGrades.sumOf { it.credits }
        return if (totalCredits == 0) 0.0 else totalPoints / totalCredits
    }

    fun getGradeDescription(grade: String): String {
        return when (grade.uppercase()) {
            "A+", "A", "A-" -> "Excellent"
            "B+", "B", "B-" -> "Good"
            "C+", "C", "C-" -> "Satisfactory"
            "D+", "D" -> "Pass"
            "F" -> "Failure"
            else -> ""
        }
    }

    @SuppressLint("DefaultLocale")
    fun calculateRequiredGrades(targetGpa: Double, remainingCredits: Int): String {
        val currentTotalPoints = _grades.value.filter { gradePoints.containsKey(it.grade) }
            .sumOf { gradePoints[it.grade]!! * it.credits }
        val currentTotalCredits = _grades.value.filter { gradePoints.containsKey(it.grade) }
            .sumOf { it.credits }
        
        val totalNeededCredits = currentTotalCredits + remainingCredits
        val totalNeededPoints = targetGpa * totalNeededCredits
        val pointsNeeded = totalNeededPoints - currentTotalPoints
        
        if (remainingCredits <= 0) return "No remaining credits specified."
        val avgGradePointNeeded = pointsNeeded / remainingCredits
        
        return when {
            avgGradePointNeeded > 4.5 -> "Impossible (Need avg > 4.5)"
            avgGradePointNeeded < 0 -> "Target already achieved!"
            else -> {
                val closestGrade = gradePoints.entries.filter { it.value >= avgGradePointNeeded }
                    .minByOrNull { it.value }?.key ?: "A+"
                "You need an average of ${String.format("%.2f", avgGradePointNeeded)} ($closestGrade) in remaining courses."
            }
        }
    }

    fun getGraduationRequirements(totalRequired: Int, clusterRequired: Map<String, Int>): List<GraduationRequirement> {
        val results = mutableListOf<GraduationRequirement>()
        
        val totalEarned = _grades.value.sumOf { it.credits }
        results.add(GraduationRequirement("Total Credits", totalRequired, totalEarned))
        
        clusterRequired.forEach { (clusterName, req) ->
            val earnedInCluster = _grades.value.filter { it.cluster == clusterName }.sumOf { it.credits }
            results.add(GraduationRequirement(clusterName, req, earnedInCluster))
        }
        
        return results
    }

    private val fileName = "academic_data.json"

    private suspend fun saveToDisk() = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            _grades.value.forEach { g ->
                val obj = JSONObject()
                obj.put("id", g.id)
                obj.put("code", g.code)
                obj.put("name", g.name)
                obj.put("credits", g.credits)
                obj.put("grade", g.grade)
                obj.put("semester", g.semester)
                obj.put("cluster", g.cluster)
                obj.put("compulsoryElective", g.compulsoryElective)
                obj.put("geDs", g.geDs)
                obj.put("program", g.program)
                array.put(obj)
            }
            
            val dArray = JSONArray()
            _deadlines.value.forEach { d ->
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("title", d.title)
                obj.put("dueDate", d.dueDate)
                obj.put("courseCode", d.courseCode)
                dArray.put(obj)
            }

            val root = JSONObject()
            root.put("grades", array)
            root.put("deadlines", dArray)
            root.put("targetGpa", _targetGpa.value)
            root.put("remainingCredits", _remainingCredits.value)

            getApplication<Application>().openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                it.write(root.toString().toByteArray())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        try {
            val file = File(getApplication<Application>().filesDir, fileName)
            if (!file.exists()) return@withContext
            val content = getApplication<Application>().openFileInput(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(content)
            
            val array = root.optJSONArray("grades") ?: JSONArray()
            val list = mutableListOf<CourseGrade>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(CourseGrade(
                    obj.getString("id"), obj.getString("code"), obj.getString("name"),
                    obj.getInt("credits"), obj.getString("grade"), obj.getString("semester"),
                    obj.optString("cluster", ""),
                    obj.optString("compulsoryElective", ""),
                    obj.optString("geDs", ""),
                    obj.optString("program", "")
                ))
            }

            val dArray = root.optJSONArray("deadlines") ?: JSONArray()
            val dList = mutableListOf<Deadline>()
            for (i in 0 until dArray.length()) {
                val obj = dArray.getJSONObject(i)
                dList.add(Deadline(
                    obj.getString("id"), obj.getString("title"),
                    obj.getLong("dueDate"), obj.getString("courseCode")
                ))
            }

            withContext(Dispatchers.Main) {
                _grades.value = list
                _deadlines.value = dList
                _targetGpa.value = root.optString("targetGpa", "3.0")
                _remainingCredits.value = root.optString("remainingCredits", "30")
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
