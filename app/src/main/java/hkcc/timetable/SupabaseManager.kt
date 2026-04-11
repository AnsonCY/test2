package hkcc.timetable

import android.content.Context
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GroupInvitation(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("user_name")
    val userName: String = "",
    @SerialName("student_email")     // NEW FIELD
    val studentEmail: String = "",    // NEW FIELD
    @SerialName("phone_number")       // NEW FIELD
    val phoneNumber: String = "",     // NEW FIELD
    @SerialName("course_code")
    val courseCode: String = "",
    @SerialName("course_name")
    val courseName: String = "",
    @SerialName("class_no")
    val classNo: String = "",
    @SerialName("sub_group")
    val subGroup: String = "",
    @SerialName("day_of_week")
    val dayOfWeek: String = "",
    @SerialName("start_time")
    val startTime: String = "",
    @SerialName("end_time")
    val endTime: String = "",
    val venue: String = "",
    val message: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("expires_at")
    val expiresAt: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000
)

object SupabaseManager {
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("Supabase client not initialized. Call init() first.")

    fun init(context: Context) {
        Log.d("SupabaseManager", "init() called")
        try {
            _client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(Realtime)
            }
            Log.d("SupabaseManager", "Supabase client created successfully")
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error creating Supabase client", e)
            throw e
        }
    }
}