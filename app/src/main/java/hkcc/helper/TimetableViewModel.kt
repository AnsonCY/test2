package hkcc.helper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hkcc.helper.data.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TimetableViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    private val _allSubjects = MutableStateFlow<List<Subject>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _clusterFilter = MutableStateFlow("All")
    val clusterFilter = _clusterFilter.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    private val _selectedSubjectCodes = MutableStateFlow<Set<String>>(emptySet())
    val selectedSubjectCodes = _selectedSubjectCodes.asStateFlow()

    private val _userCredits = MutableStateFlow("15")
    val userCredits = _userCredits.asStateFlow()

    private val _isCreditsLocked = MutableStateFlow(false)
    val isCreditsLocked = _isCreditsLocked.asStateFlow()

    private val _studyPatterns = MutableStateFlow<List<StudyPatternRow>>(emptyList())
    val studyPatterns = _studyPatterns.asStateFlow()

    private val _subjectSpecs = MutableStateFlow<List<SubjectSpec>>(emptyList())
    val subjectSpecs = _subjectSpecs.asStateFlow()

    private val _showGhosting = MutableStateFlow(true)
    val showGhosting = _showGhosting.asStateFlow()

    private val _themeMode = MutableStateFlow(THEME_SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    private val _reminderMinutes = MutableStateFlow(15)
    val reminderMinutes = _reminderMinutes.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _selectedStudyPattern = MutableStateFlow<Map<String, String>>(emptyMap())
    val selectedStudyPattern = _selectedStudyPattern.asStateFlow()

    private val _bioLevel = MutableStateFlow("")
    val bioLevel = _bioLevel.asStateFlow()

    private val _chemLevel = MutableStateFlow("")
    val chemLevel = _chemLevel.asStateFlow()

    private val _phyLevel = MutableStateFlow("")
    val phyLevel = _phyLevel.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    val subjects: StateFlow<List<Subject>> = _allSubjects.asStateFlow()

    val ghostSubjects: StateFlow<List<Subject>> = combine(
        _allSubjects, _selectedIds, _selectedSubjectCodes, _showGhosting
    ) { all, ids, codes, showGhosts ->
        if (!showGhosts) return@combine emptyList()
        val ghosts = mutableListOf<Subject>()
        for (code in codes) {
            val subjectsForCode = all.filter { it.code == code }
            val solidSubjects = subjectsForCode.filter { ids.contains(it.id) }
            val hasSolidLecture = solidSubjects.any { it.isLecture() }
            val hasSolidTutorial = solidSubjects.any { !it.isLecture() }

            ghosts.addAll(subjectsForCode.filter { candidate ->
                if (ids.contains(candidate.id)) return@filter false
                if (candidate.isLecture()) !hasSolidLecture
                else if (hasSolidTutorial) false
                else if (hasSolidLecture) candidate.classNo == solidSubjects.first { it.isLecture() }.classNo
                else true
            })
        }
        ghosts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var isLoaded = false

    init {
        viewModelScope.launch {
            loadFromDisk()
            isLoaded = true
            loadStudyPatterns()
        }
    }

    private fun loadStudyPatterns() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                
                // Load Subject Specs first
                val specList = context.assets.open("subjectspec.csv").use { parseSubjectSpecCsv(it) }
                _subjectSpecs.value = specList
                
                // Load Subjects with spec info - link by program title
                val programTitle = _selectedStudyPattern.value["programTitle"] ?: ""
                context.assets.open("subjects.csv").use { stream ->
                    val subjectsFromCsv = parseCsvStream(stream, specList, programTitle)
                    addSubjects(subjectsFromCsv)
                }

                // Update existing subjects with spec info
                _allSubjects.update { currentList ->
                    currentList.map { sub ->
                        val spec = specList.find { 
                            it.code == sub.code && (it.programme.isBlank() || it.programme.equals(programTitle, true))
                        } ?: specList.find { it.code == sub.code }
                        
                        if (spec != null) {
                            sub.copy(
                                cluster = spec.clusterArea,
                                clusterArea = spec.clusterArea,
                                compulsoryElective = spec.compulsoryElective,
                                geDs = spec.geDs,
                                program = spec.programme,
                                credits = spec.credit.toIntOrNull() ?: 3
                            )
                        } else sub
                    }
                }

                context.assets.open("studypattern.csv").use { stream ->
                    _studyPatterns.value = parseStudyPatternCsv(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applyStudyPattern(
        programCode: String, 
        programTitle: String, 
        pattern: String, 
        semester: String, 
        cantonesePutonghua: String, 
        engLevel: String,
        bioLevel: String = "",
        chemLevel: String = "",
        phyLevel: String = ""
    ) {
        _selectedStudyPattern.value = mapOf(
            "program" to programCode,
            "programTitle" to programTitle,
            "pattern" to pattern,
            "semester" to semester,
            "cantonese" to cantonesePutonghua,
            "eng" to engLevel,
            "bio" to bioLevel,
            "chem" to chemLevel,
            "phy" to phyLevel
        )
        _bioLevel.value = bioLevel
        _chemLevel.value = chemLevel
        _phyLevel.value = phyLevel

        val baseRows = _studyPatterns.value.filter {
            it.programCode == programCode && it.studyPattern == pattern
        }

        val langFilteredRows = baseRows.filter {
            (cantonesePutonghua.isBlank() || cantonesePutonghua == "Not set" || it.cantonesePutonghua.isBlank() || it.cantonesePutonghua == cantonesePutonghua) &&
                    (engLevel.isBlank() || engLevel == "Not set" || it.engLevel.isBlank() || it.engLevel == engLevel)
        }

        val rowsAfterLang = langFilteredRows.ifEmpty { baseRows }

        val scienceFilteredRows = rowsAfterLang.filter {
            (bioLevel.isBlank() || bioLevel == "Not set" || it.bioLevel.isBlank() || it.bioLevel == bioLevel) &&
                    (chemLevel.isBlank() || chemLevel == "Not set" || it.chemLevel.isBlank() || it.chemLevel == chemLevel) &&
                    (phyLevel.isBlank() || phyLevel == "Not set" || it.phyLevel.isBlank() || it.phyLevel == phyLevel)
        }

        val rows = scienceFilteredRows.ifEmpty { rowsAfterLang }

        if (rows.isNotEmpty()) {
            // Clear existing selections before applying new pattern
            _selectedIds.value = emptySet()
            
            val credits = rows[0].requiredCredit
            _userCredits.value = credits
            _isCreditsLocked.value = true

            val subjectCodes = rows
                .filter { it.semester == semester }
                .map { it.subjectCode }
                .toSet()
            _selectedSubjectCodes.value = subjectCodes
            saveToDisk()

            // Reload subjects to apply program-specific specs
            loadStudyPatterns()
        }
    }

    fun unlockCredits() {
        _isCreditsLocked.value = false
        _selectedStudyPattern.value = emptyMap()
        saveToDisk()
    }

    fun updateSearch(q: String) { _searchQuery.value = q }
    fun updateUserCredits(c: String) { _userCredits.value = c; saveToDisk() }
    fun toggleGhosting(v: Boolean) { _showGhosting.value = v; saveToDisk() }

    fun updateReminderMinutes(m: Int) { _reminderMinutes.value = m; saveToDisk() }
    fun toggleNotifications(enabled: Boolean) { _notificationsEnabled.value = enabled; saveToDisk() }

    fun updateClusterFilter(f: String) { _clusterFilter.value = f }

    fun setThemeMode(mode: Int) {
        _themeMode.value = mode
        saveToDisk()
    }

    fun toggleSubjectInterest(code: String) {
        _selectedSubjectCodes.update { if (it.contains(code)) it - code else it + code }
        if (!_selectedSubjectCodes.value.contains(code)) {
            val idsToRemove = _allSubjects.value.filter { it.code == code }.map { it.id }.toSet()
            _selectedIds.update { it - idsToRemove }
        }
        saveToDisk()
    }

    fun selectSubject(subject: Subject) {
        _selectedSubjectCodes.update { it + subject.code }
        _selectedIds.update { currentIds ->
            val newIds = currentIds.toMutableSet()
            if (currentIds.contains(subject.id)) {
                newIds.remove(subject.id)
                return@update newIds
            }
            val sameSubjectEntries = _allSubjects.value.filter { currentIds.contains(it.id) && it.code == subject.code }
            if (subject.isLecture()) {
                newIds.removeAll(sameSubjectEntries.map { it.id }.toSet())
            } else {
                newIds.removeAll(sameSubjectEntries.filter { !it.isLecture() }.map { it.id }.toSet())
            }

            val currentList = _allSubjects.value.filter { newIds.contains(it.id) }
            val error = findClash(subject, currentList)
            if (error != null) {
                viewModelScope.launch { _uiEvent.emit(error) }
                return@update currentIds
            }

            if (!subject.isLecture()) {
                val match = _allSubjects.value.find { it.code == subject.code && it.isLecture() && it.classNo == subject.classNo }
                if (match != null && !newIds.contains(match.id)) {
                    if (findClash(match, currentList) == null) newIds.add(match.id)
                }
            }
            newIds.add(subject.id)
            newIds
        }
        saveToDisk()
    }

    private fun findClash(candidate: Subject, currentList: List<Subject>): String? {
        val sameDay = currentList.filter { it.dayOfWeek.equals(candidate.dayOfWeek, true) }
        for (existing in sameDay) {
            val sA = candidate.getStartMinutes(); val eA = candidate.getEndMinutes()
            val sB = existing.getStartMinutes(); val eB = existing.getEndMinutes()
            if (sA < eB && sB < eA) return "Time Clash: ${candidate.code} vs ${existing.code}"

            val cA = candidate.getCampusType(); val cB = existing.getCampusType()
            if (cA == cB) continue
            val (first, second) = if (sA < sB) candidate to existing else existing to candidate
            if (second.getStartMinutes() - first.getEndMinutes() < 60) return "Travel Clash: ${first.venue} -> ${second.venue}"
        }
        return null
    }

    private val _generatedSchedules = MutableStateFlow<List<List<Subject>>>(emptyList())
    val generatedSchedules = _generatedSchedules.asStateFlow()

    private var generationJob: Job? = null

    fun generateAutoPlan(
        daysToAvoidCampus: Set<String>,
        avoidMorning: Boolean,
        avoidAfternoon: Boolean,
        avoidEvening: Boolean,
        useFixedSections: Boolean
    ) {
        generationJob?.cancel()

        generationJob = viewModelScope.launch(Dispatchers.Default) {
            _isGenerating.value = true
            val interestedCodes = _selectedSubjectCodes.value.toList()
            if (interestedCodes.isEmpty()) {
                _uiEvent.emit("Please select subjects in 'Select Subjects' first.")
                _isGenerating.value = false
                return@launch
            }

            val currentSelectedIds = _selectedIds.value

            val subjectOptions = mutableListOf<List<List<Subject>>>()
            for (code in interestedCodes) {
                val subItems = _allSubjects.value.filter { it.code == code }
                val lectures = subItems.filter { it.isLecture() }
                val tutorials = subItems.filter { !it.isLecture() }
                
                // If prioritize manual selection is ON, and user has already selected something for this code
                val userSelectedForThisCode = subItems.filter { currentSelectedIds.contains(it.id) }
                
                val validCombos = mutableListOf<List<Subject>>()

                if (useFixedSections && userSelectedForThisCode.isNotEmpty()) {
                    // Force the combination that matches user selection
                    validCombos.add(userSelectedForThisCode)
                } else {
                    if (lectures.isEmpty() && tutorials.isNotEmpty()) {
                        tutorials.forEach { validCombos.add(listOf(it)) }
                    } else if (tutorials.isEmpty()) {
                        lectures.forEach { validCombos.add(listOf(it)) }
                    } else {
                        lectures.forEach { lec ->
                            val matchingTuts = tutorials.filter { it.classNo == lec.classNo }
                            if (matchingTuts.isNotEmpty()) {
                                matchingTuts.forEach { tut -> validCombos.add(listOf(lec, tut)) }
                            } else {
                                validCombos.add(listOf(lec))
                            }
                        }
                    }
                }
                if (validCombos.isNotEmpty()) subjectOptions.add(validCombos)
            }

            val results = mutableListOf<List<Subject>>()
            // Limit to 10000 schedules
            solveSchedule(
                0, subjectOptions, emptyList(),
                daysToAvoidCampus, avoidMorning, avoidAfternoon, avoidEvening,
                results, limit = 10000
            )

            if (isActive) {
                // Remove duplicates logic
                val uniqueResults = results.distinctBy { schedule ->
                    schedule.map { it.id }.sorted().joinToString(",")
                }

                _generatedSchedules.value = uniqueResults
                _isGenerating.value = false

                withContext(Dispatchers.Main) {
                    if (uniqueResults.isEmpty()) {
                        _uiEvent.emit("No valid schedules found. Try relaxing constraints.")
                    } else {
                        _uiEvent.emit("Found ${uniqueResults.size} schedules!")
                    }
                }
            }
        }
    }

    private fun solveSchedule(
        index: Int,
        allOptions: List<List<List<Subject>>>,
        currentSchedule: List<Subject>,
        daysToAvoid: Set<String>,
        avoidMorning: Boolean,
        avoidAfternoon: Boolean,
        avoidEvening: Boolean,
        results: MutableList<List<Subject>>,
        limit: Int
    ) {
        if (results.size >= limit) return
        if (index == allOptions.size) {
            results.add(currentSchedule)
            return
        }

        val myOptions = allOptions[index]
        for (option in myOptions) {
            if (results.size >= limit) return

            var isSafe = true
            for (newItem in option) {
                // 1. Check time clash
                if (findClash(newItem, currentSchedule) != null) { isSafe = false; break }

                // 2. Check days to avoid
                val dayKey = newItem.dayOfWeek.trim().uppercase().take(3)
                if (daysToAvoid.contains(dayKey) && newItem.isOnCampus()) {
                    isSafe = false; break
                }

                // 3. Check time preferences
                val start = newItem.getStartMinutes()
                if (avoidMorning && start < 720) { isSafe = false; break } // Before 12:00
                if (avoidAfternoon && start >= 720 && start < 1080) { isSafe = false; break } // 12:00 - 18:00
                if (avoidEvening && start >= 1080) { isSafe = false; break } // After 18:00
            }
            if (isSafe) solveSchedule(
                index + 1, allOptions, currentSchedule + option,
                daysToAvoid, avoidMorning, avoidAfternoon, avoidEvening,
                results, limit
            )
        }
    }

    fun applySchedule(schedule: List<Subject>) {
        _selectedIds.value = schedule.map { it.id }.toSet()
        _selectedSubjectCodes.update { it + schedule.map { s -> s.code }.toSet() }
        saveToDisk()
    }

    fun getConfigString(): String {
        val sigs = _allSubjects.value.filter { _selectedIds.value.contains(it.id) }
            .joinToString(";;") { it.toUniqueSignature() }
        return android.util.Base64.encodeToString("${_userCredits.value}###$sigs".toByteArray(), android.util.Base64.NO_WRAP)
    }

    fun loadConfigString(config: String) {
        try {
            val parts = String(android.util.Base64.decode(config, android.util.Base64.NO_WRAP)).split("###")
            if (parts.isNotEmpty()) {
                _userCredits.value = parts[0]
                if (parts.size > 1) {
                    val sigs = parts[1].split(";;").toSet()
                    val ids = _allSubjects.value.filter { sigs.contains(it.toUniqueSignature()) }.map { it.id }.toSet()
                    _selectedIds.value = ids
                    _selectedSubjectCodes.update { it + _allSubjects.value.filter { s -> ids.contains(s.id) }.map { s -> s.code }.toSet() }
                    saveToDisk()
                    viewModelScope.launch { _uiEvent.emit("Config loaded!") }
                }
            }
        } catch (e: Exception) { viewModelScope.launch { _uiEvent.emit("Invalid Config") } }
    }

    fun addSubjects(newSubjects: List<Subject>) {
        _allSubjects.update { (it + newSubjects).distinctBy { s -> s.toUniqueSignature() } }
        saveToDisk()
    }

    fun clearAll() {
        _allSubjects.value = emptyList()
        _selectedIds.value = emptySet()
        _selectedSubjectCodes.value = emptySet()
        _generatedSchedules.value = emptyList()
        saveToDisk()
    }

    private val fileName = "timetable_data_v2.json"

    private fun saveToDisk() {
        if (!isLoaded) return // Prevent saving empty state before load completes
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject()
                root.put("credits", _userCredits.value)
                root.put("showGhosting", _showGhosting.value)
                root.put("themeMode", _themeMode.value)
                root.put("reminderMinutes", _reminderMinutes.value)
                root.put("notificationsEnabled", _notificationsEnabled.value)
                root.put("isCreditsLocked", _isCreditsLocked.value)
                root.put("bioLevel", _bioLevel.value)
                root.put("chemLevel", _chemLevel.value)
                root.put("phyLevel", _phyLevel.value)
                
                val patternObj = JSONObject()
                _selectedStudyPattern.value.forEach { (k, v) -> patternObj.put(k, v) }
                root.put("selectedStudyPattern", patternObj)

                val idsArray = JSONArray()
                _selectedIds.value.forEach { idsArray.put(it) }
                root.put("selectedIds", idsArray)

                val codesArray = JSONArray()
                _selectedSubjectCodes.value.forEach { codesArray.put(it) }
                root.put("selectedCodes", codesArray)

                val subjectsArray = JSONArray()
                _allSubjects.value.forEach { subjectsArray.put(it.toJson()) }
                root.put("subjects", subjectsArray)

                getApplication<Application>().openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                    it.write(root.toString().toByteArray())
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val file = File(context.filesDir, fileName)

            if (!file.exists()) {
                withContext(Dispatchers.Main) {
                    _themeMode.value = THEME_SYSTEM
                }
                return@withContext
            }

            context.openFileInput(fileName).use { stream ->
                val root = JSONObject(stream.bufferedReader().use { it.readText() })
                
                val credits = root.optString("credits", "15")
                val showGhosting = root.optBoolean("showGhosting", true)
                val themeMode = if (root.has("themeMode")) {
                    root.getInt("themeMode")
                } else if (root.has("isDarkMode")) {
                    if(root.getBoolean("isDarkMode")) THEME_DARK else THEME_LIGHT
                } else {
                    THEME_SYSTEM
                }

                val reminderMinutes = root.optInt("reminderMinutes", 15)
                val notificationsEnabled = root.optBoolean("notificationsEnabled", true)
                val isCreditsLocked = root.optBoolean("isCreditsLocked", false)
                val bio = root.optString("bioLevel", "")
                val chem = root.optString("chemLevel", "")
                val phy = root.optString("phyLevel", "")

                val patternMap = mutableMapOf<String, String>()
                val patternObj = root.optJSONObject("selectedStudyPattern")
                patternObj?.keys()?.forEach { patternMap[it] = patternObj.getString(it) }

                val loadedIds = mutableSetOf<String>()
                val idsArray = root.optJSONArray("selectedIds")
                if (idsArray != null) for (i in 0 until idsArray.length()) loadedIds.add(idsArray.getString(i))

                val loadedCodes = mutableSetOf<String>()
                val codesArray = root.optJSONArray("selectedCodes")
                if (codesArray != null) for (i in 0 until codesArray.length()) loadedCodes.add(codesArray.getString(i))

                val loadedSubjects = mutableListOf<Subject>()
                val subArray = root.optJSONArray("subjects")
                if (subArray != null) for (i in 0 until subArray.length()) loadedSubjects.add(
                    Subject.fromJson(subArray.getJSONObject(i)))
                
                withContext(Dispatchers.Main) {
                    _userCredits.value = credits
                    _showGhosting.value = showGhosting
                    _themeMode.value = themeMode
                    _reminderMinutes.value = reminderMinutes
                    _notificationsEnabled.value = true // simplified check
                    _notificationsEnabled.value = notificationsEnabled
                    _isCreditsLocked.value = isCreditsLocked
                    _bioLevel.value = bio
                    _chemLevel.value = chem
                    _phyLevel.value = phy
                    _selectedStudyPattern.value = patternMap
                    _selectedIds.value = loadedIds
                    _selectedSubjectCodes.value = loadedCodes
                    _allSubjects.value = loadedSubjects
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}