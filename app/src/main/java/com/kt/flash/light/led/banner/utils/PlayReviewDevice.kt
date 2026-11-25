package com.kt.flash.light.led.banner.utils

import android.content.Context
import android.provider.Settings

/**
 * Nhận diện máy chạy Pre-launch report của Google Play (bot Robo chạy trên hạ tầng Firebase Test Lab).
 * Đây là cách chính thức duy nhất. KHÔNG cố nhận diện người duyệt thật của Google để cho họ thấy
 * trải nghiệm khác người dùng: chính sách Google Play cấm việc đó.
 */
object PlayReviewDevice {

    @Volatile
    private var cached: Boolean? = null

    /** Firebase Test Lab đặt system setting "firebase.test.lab" = "true" trên máy test. */
    fun isFirebaseTestLab(context: Context): Boolean = cached ?: runCatching {
        "true" == Settings.System.getString(context.contentResolver, "firebase.test.lab")
    }.getOrDefault(false).also { cached = it }
}
