package com.example.cabinguard.data.cloud

/** Kết quả một lượt đẩy log chưa sync. Lỗi không đổi isSynced. */
sealed interface CloudSyncOutcome {
    data class Success(val count: Int) : CloudSyncOutcome
    data object NothingToSync : CloudSyncOutcome
    data class Failed(val cause: Exception) : CloudSyncOutcome
}
