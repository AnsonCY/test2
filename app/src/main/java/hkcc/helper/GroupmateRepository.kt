package hkcc.helper

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupmateRepository {

    suspend fun createInvitation(invitation: GroupInvitation): Result<GroupInvitation> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseManager.client.postgrest["group_invitations"]
                .insert(invitation) {
                    select()
                }
            val inserted = response.decodeAs<List<GroupInvitation>>().firstOrNull()
            if (inserted != null) {
                Result.success(inserted)
            } else {
                Result.failure(Exception("Failed to create invitation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvitationsForCourse(courseCode: String, classNo: String = ""): Result<List<GroupInvitation>> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val query = SupabaseManager.client.postgrest["group_invitations"]
                .select {
                    filter {
                        eq("course_code", courseCode)
                        gt("expires_at", currentTime)
                    }
                    if (classNo.isNotEmpty()) {
                        filter {
                            eq("class_no", classNo)
                        }
                    }
                }
            val invitations = query.decodeAs<List<GroupInvitation>>()
            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllActiveInvitations(): Result<List<GroupInvitation>> = withContext(Dispatchers.IO) {
        try {
            val invitations = SupabaseManager.client.postgrest["group_invitations"]
                .select {
                    filter {
                        gt("expires_at", System.currentTimeMillis())
                    }
                }
                .decodeAs<List<GroupInvitation>>()
            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInvitation(invitationId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["group_invitations"]
                .delete {
                    filter {
                        eq("id", invitationId)
                        eq("user_id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyInvitations(userId: String): Result<List<GroupInvitation>> = withContext(Dispatchers.IO) {
        try {
            val invitations = SupabaseManager.client.postgrest["group_invitations"]
                .select {
                    filter {
                        eq("user_id", userId)
                        gt("expires_at", System.currentTimeMillis())
                    }
                }
                .decodeAs<List<GroupInvitation>>()
            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateInvitationMessage(invitationId: String, userId: String, newMessage: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["group_invitations"]
                .update(
                    mapOf("message" to newMessage)
                ) {
                    filter {
                        eq("id", invitationId)
                        eq("user_id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}