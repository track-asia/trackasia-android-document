package com.trackasia.sample.utils

/**
 * Tiện ích xử lý thời gian
 */
object TimeUtils {
    /**
     * Định dạng số giây thành chuỗi thời gian đọc được
     * VD: 3600 -> 1 giờ
     */
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            hours > 0 -> "$hours giờ ${if (minutes > 0) "$minutes phút" else ""}"
            minutes > 0 -> "$minutes phút"
            else -> "${seconds % 60} giây"
        }
    }
} 