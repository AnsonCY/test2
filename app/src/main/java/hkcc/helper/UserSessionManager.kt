package hkcc.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.UUID

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    var currentUserId: String
        get() = prefs.getString("user_id", null) ?: generateAndSaveUserId()
        set(value) = prefs.edit { putString("user_id", value) }

    var currentUserName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit { putString("user_name", value) }

    var currentStudentEmail: String
        get() = prefs.getString("student_email", "") ?: ""
        set(value) = prefs.edit { putString("student_email", value) }

    var currentPhoneNumber: String
        get() = prefs.getString("phone_number", "") ?: ""
        set(value) = prefs.edit { putString("phone_number", value) }

    private fun generateAndSaveUserId(): String {
        return UUID.randomUUID().toString().also { newId ->
            prefs.edit { putString("user_id", newId) }
        }
    }

    fun hasCompleteProfile(): Boolean {
        // Name is NOT required, only contact info is required
        return hasContactInfo()
    }

    fun hasContactInfo(): Boolean {
        return currentStudentEmail.isNotBlank() || currentPhoneNumber.isNotBlank()
    }

    fun getDisplayName(): String {
        return currentUserName.ifBlank {
            if (currentStudentEmail.isNotBlank()) {
                currentStudentEmail.substringBefore("@")
            } else {
                "HKCC Student"
            }
        }
    }
}