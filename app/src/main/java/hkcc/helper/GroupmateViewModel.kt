package hkcc.helper

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupmateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GroupmateRepository()
    private val sessionManager = UserSessionManager(application)

    private val _uiState = MutableStateFlow<GroupmateUiState>(GroupmateUiState.Idle)
    val uiState: StateFlow<GroupmateUiState> = _uiState.asStateFlow()

    private val _myInvitations = MutableStateFlow<List<GroupInvitation>>(emptyList())
    val myInvitations: StateFlow<List<GroupInvitation>> = _myInvitations.asStateFlow()

    private val _courseInvitations = MutableStateFlow<List<GroupInvitation>>(emptyList())
    val courseInvitations: StateFlow<List<GroupInvitation>> = _courseInvitations.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    init {
        Log.d("GroupmateViewModel", "ViewModel initialized")
        loadMyInvitations()
    }
    fun getDisplayName(): String {
        return sessionManager.getDisplayName()
    }

    fun hasContactInfo(): Boolean {
        return sessionManager.hasContactInfo()
    }
    // User profile methods
    fun setUserName(name: String) {
        sessionManager.currentUserName = name
        viewModelScope.launch {
            _successMessage.emit("Name saved: $name")
        }
    }

    fun setStudentEmail(email: String) {
        sessionManager.currentStudentEmail = email
        viewModelScope.launch {
            _successMessage.emit("Email saved: $email")
        }
    }

    fun setPhoneNumber(phone: String) {
        sessionManager.currentPhoneNumber = phone
        viewModelScope.launch {
            _successMessage.emit("Phone number saved: $phone")
        }
    }

    fun getUserName(): String = sessionManager.currentUserName
    fun getUserId(): String = sessionManager.currentUserId
    fun getStudentEmail(): String = sessionManager.currentStudentEmail
    fun getPhoneNumber(): String = sessionManager.currentPhoneNumber
    fun hasCompleteProfile(): Boolean = sessionManager.hasCompleteProfile()

    fun createInvitation(
        courseCode: String,
        courseName: String,
        classNo: String,
        subGroup: String,
        dayOfWeek: String,
        startTime: String,
        endTime: String,
        venue: String,
        message: String
    ) {
        viewModelScope.launch {
            _uiState.value = GroupmateUiState.Loading
            try {
                val invitation = GroupInvitation(
                    userId = sessionManager.currentUserId,
                    userName = sessionManager.currentUserName,
                    studentEmail = sessionManager.currentStudentEmail,
                    phoneNumber = sessionManager.currentPhoneNumber,
                    courseCode = courseCode,
                    courseName = courseName,
                    classNo = classNo,
                    subGroup = subGroup,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    venue = venue,
                    message = message,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000
                )

                val result = repository.createInvitation(invitation)
                result.fold(
                    onSuccess = {
                        _uiState.value = GroupmateUiState.Success("Invitation created!")
                        _successMessage.emit("Invitation posted successfully!")
                        loadMyInvitations()
                    },
                    onFailure = {
                        _uiState.value = GroupmateUiState.Error(it.message ?: "Failed to create invitation")
                        _errorMessage.emit(it.message ?: "Failed to create invitation")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = GroupmateUiState.Error(e.message ?: "Unknown error")
                _errorMessage.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun loadInvitationsForCourse(courseCode: String, classNo: String = "") {
        viewModelScope.launch {
            _uiState.value = GroupmateUiState.Loading
            try {
                val result = repository.getInvitationsForCourse(courseCode, classNo)
                result.fold(
                    onSuccess = { invitations ->
                        _courseInvitations.value = invitations
                        _uiState.value = GroupmateUiState.Success("Loaded ${invitations.size} invitations")
                        if (invitations.isEmpty()) {
                            _successMessage.emit("No groupmates found for this course")
                        } else {
                            _successMessage.emit("Found ${invitations.size} groupmate(s)!")
                        }
                    },
                    onFailure = {
                        _uiState.value = GroupmateUiState.Error(it.message ?: "Failed to load invitations")
                        _errorMessage.emit(it.message ?: "Failed to load invitations")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = GroupmateUiState.Error(e.message ?: "Unknown error")
                _errorMessage.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMyInvitations() {
        viewModelScope.launch {
            try {
                val result = repository.getMyInvitations(sessionManager.currentUserId)
                result.fold(
                    onSuccess = {
                        _myInvitations.value = it
                    },
                    onFailure = {
                        _errorMessage.emit(it.message ?: "Failed to load invitations")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteInvitation(invitationId: String) {
        viewModelScope.launch {
            try {
                val result = repository.deleteInvitation(invitationId, sessionManager.currentUserId)
                result.fold(
                    onSuccess = {
                        _successMessage.emit("Invitation deleted!")
                        loadMyInvitations()
                    },
                    onFailure = {
                        _errorMessage.emit(it.message ?: "Failed to delete invitation")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun clearCourseInvitations() {
        _courseInvitations.value = emptyList()
    }
}

sealed class GroupmateUiState {
    object Idle : GroupmateUiState()
    object Loading : GroupmateUiState()
    data class Success(val message: String) : GroupmateUiState()
    data class Error(val message: String) : GroupmateUiState()
}