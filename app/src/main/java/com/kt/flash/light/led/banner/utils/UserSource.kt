package com.kt.flash.light.led.banner.utils

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Phân loại user organic / paid theo attribution của Adjust (module ads chỉ trả network dạng String).
 *
 * - Có 3 trạng thái: organic, paid, chưa xác định (lần mở đầu khi attribution chưa về).
 * - Chưa xác định và máy Firebase Test Lab luôn được xử lý như organic; chỉ thành paid khi Adjust xác nhận
 *   network là một network trả phí.
 * - Chốt nhóm 2 lần trong mỗi phiên:
 *   1. [lockForSplash]: trước khi load quảng cáo của Splash (chỉ chờ attribution tối đa vài giây).
 *   2. [lockSession]: lúc rời Splash. Splash còn chờ load quảng cáo nên attribution thường đã kịp về ->
 *      mọi thứ từ màn Language trở đi dùng nhóm này, cố định tới hết phiên.
 *   Attribution về muộn hơn nữa được lưu lại và áp dụng từ lần mở sau.
 */
object UserSource {

    const val ORGANIC = "organic"
    const val PAID = "paid"

    /** User property trên Firebase Analytics: dùng để lọc báo cáo / làm điều kiện Remote Config. */
    private const val USER_PROPERTY = "user_source"

    /** Network của Adjust được tính là organic. Đối chiếu lại với Adjust Dashboard khi thêm tracker mới. */
    val ORGANIC_NETWORKS = setOf("Organic", "Google Organic Search", "Untrusted Devices")

    /** Giá trị đã lưu (null = chưa xác định). */
    private val saved = MutableStateFlow<String?>(null)

    @Volatile
    private var isLoaded = false

    /** Nhóm dùng cho quảng cáo của Splash (lần chốt 1). */
    @Volatile
    private var splashSource: String? = null

    /** Nhóm dùng cho phần còn lại của phiên (lần chốt 2, cuối cùng). */
    @Volatile
    private var sessionSource: String? = null

    /** null nếu chưa có attribution (giữ nguyên trạng thái cũ). */
    fun classify(network: String?, isTestLab: Boolean): String? = when {
        network.isNullOrBlank() -> null
        isTestLab -> ORGANIC
        network in ORGANIC_NETWORKS -> ORGANIC
        else -> PAID
    }

    /** Gọi từ hook attribution của Adjust (main thread). */
    fun onAttribution(context: Context, network: String?) {
        val source = classify(network, PlayReviewDevice.isFirebaseTestLab(context)) ?: return
        ensureLoaded(context)
        if (saved.value == source) return
        SpManager.getInstance(context).putString(Constant.KEY_SP_USER_SOURCE, source)
        saved.value = source
        runCatching { FirebaseAnalytics.getInstance(context).setUserProperty(USER_PROPERTY, source) }
        Logger.e("UserSource: network=$network -> $source")
    }

    /** Nguồn hiện có trên máy (chưa xác định -> organic). Luôn trả về [ORGANIC] hoặc [PAID]. */
    fun effectiveSource(context: Context): String {
        if (PlayReviewDevice.isFirebaseTestLab(context)) return ORGANIC
        if (SpManager.getInstance(context).getPaidUser()) return PAID
        ensureLoaded(context)
        return saved.value ?: ORGANIC
    }

    /** Lần chốt 1: nhóm cho quảng cáo của Splash (chỉ có hiệu lực ở lần gọi đầu tiên). */
    fun lockForSplash(context: Context): String = synchronized(this) {
        sessionSource ?: splashSource ?: effectiveSource(context).also {
            splashSource = it
            Logger.e("UserSource: splash = $it")
        }
    }

    /** Lần chốt 2 (cuối cùng): nhóm cho phần còn lại của phiên (chỉ có hiệu lực ở lần gọi đầu tiên). */
    fun lockSession(context: Context): String = synchronized(this) {
        sessionSource ?: effectiveSource(context).also {
            sessionSource = it
            Logger.e("UserSource: session = $it (splash = $splashSource)")
        }
    }

    /** Nhóm dùng để quyết định hiển thị quảng cáo: chốt lần 2 > chốt lần 1 > nguồn hiện có nếu chưa chốt. */
    fun current(context: Context?): String =
        sessionSource ?: splashSource ?: context?.let { effectiveSource(it) } ?: ORGANIC

    fun isPaid(context: Context?): Boolean = current(context) == PAID

    /**
     * Đợi attribution (lần cài đầu) tối đa [timeoutMs]. Đã có giá trị từ trước thì trả về ngay.
     * Hết giờ thì thôi, phiên này dùng organic.
     */
    suspend fun awaitAttribution(context: Context, timeoutMs: Long) {
        ensureLoaded(context)
        if (saved.value != null || timeoutMs <= 0) return
        withTimeoutOrNull(timeoutMs.milliseconds) { saved.first { it != null } }
    }

    private fun ensureLoaded(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            val stored = SpManager.getInstance(context).getString(Constant.KEY_SP_USER_SOURCE, "")
            if (saved.value == null) saved.value = stored?.takeIf { it == ORGANIC || it == PAID }
            isLoaded = true
        }
    }
}
