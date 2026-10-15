package com.douduoyu.app

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

internal enum class BirthdayAnimationQuality {
    Low,
    Standard,
    Refined,
    ;

    fun lower(): BirthdayAnimationQuality =
        when (this) {
            Refined -> Standard
            Standard -> Low
            Low -> Low
        }
}

internal fun BirthdayAnimationQuality.label(): String =
    when (this) {
        BirthdayAnimationQuality.Low -> "低"
        BirthdayAnimationQuality.Standard -> "标准"
        BirthdayAnimationQuality.Refined -> "精致"
    }

internal data class BirthdayAnimationDetail(
    val nightStarCount: Int,
    val twinkleIntervalMillis: Long,
    val passingMeteorTrailCount: Int,
    val giantMeteorTrailCount: Int,
    val meteorImpactDropCount: Int,
    val fishSplashDropCount: Int,
    val pondRippleCount: Int,
    val pondResponseRippleCount: Int,
    val fishSegmentCount: Int,
    val fishWaveHarmonics: Float,
    val giftLightParticleCount: Int,
    val starGlowStride: Int,
    val starRayStride: Int,
    val meteorDebrisCount: Int,
    val pondShimmerCount: Int,
    val fishBubbleCount: Int,
    val giftShineBandCount: Int,
    val cakeShadowStride: Int,
    val cakeSpecularStride: Int,
    val finalStardustCount: Int,
)

internal val BirthdayAnimationQuality.detail: BirthdayAnimationDetail
    get() =
        when (this) {
            BirthdayAnimationQuality.Low ->
                BirthdayAnimationDetail(
                    nightStarCount = 30,
                    twinkleIntervalMillis = 120L,
                    passingMeteorTrailCount = 34,
                    giantMeteorTrailCount = 24,
                    meteorImpactDropCount = 15,
                    fishSplashDropCount = 11,
                    pondRippleCount = 3,
                    pondResponseRippleCount = 3,
                    fishSegmentCount = 9,
                    fishWaveHarmonics = 0f,
                    giftLightParticleCount = 0,
                    starGlowStride = 0,
                    starRayStride = 0,
                    meteorDebrisCount = 0,
                    pondShimmerCount = 0,
                    fishBubbleCount = 0,
                    giftShineBandCount = 0,
                    cakeShadowStride = Int.MAX_VALUE,
                    cakeSpecularStride = Int.MAX_VALUE,
                    finalStardustCount = 12,
                )

            BirthdayAnimationQuality.Standard ->
                BirthdayAnimationDetail(
                    nightStarCount = 54,
                    twinkleIntervalMillis = 100L,
                    passingMeteorTrailCount = 46,
                    giantMeteorTrailCount = 34,
                    meteorImpactDropCount = 21,
                    fishSplashDropCount = 17,
                    pondRippleCount = 4,
                    pondResponseRippleCount = 4,
                    fishSegmentCount = 13,
                    fishWaveHarmonics = 0.14f,
                    giftLightParticleCount = 12,
                    starGlowStride = 9,
                    starRayStride = 15,
                    meteorDebrisCount = 2,
                    pondShimmerCount = 4,
                    fishBubbleCount = 2,
                    giftShineBandCount = 1,
                    cakeShadowStride = 13,
                    cakeSpecularStride = 11,
                    finalStardustCount = 18,
                )

            BirthdayAnimationQuality.Refined ->
                BirthdayAnimationDetail(
                    nightStarCount = 84,
                    twinkleIntervalMillis = 80L,
                    passingMeteorTrailCount = 58,
                    giantMeteorTrailCount = 46,
                    meteorImpactDropCount = 29,
                    fishSplashDropCount = 23,
                    pondRippleCount = 5,
                    pondResponseRippleCount = 5,
                    fishSegmentCount = 17,
                    fishWaveHarmonics = 0.25f,
                    giftLightParticleCount = 20,
                    starGlowStride = 6,
                    starRayStride = 10,
                    meteorDebrisCount = 4,
                    pondShimmerCount = 8,
                    fishBubbleCount = 4,
                    giftShineBandCount = 2,
                    cakeShadowStride = 7,
                    cakeSpecularStride = 6,
                    finalStardustCount = 26,
                )
        }

internal data class BirthdayAnimationDeviceProfile(
    val isLowRamDevice: Boolean,
    val totalMemoryMb: Long,
    val memoryClassMb: Int,
    val cpuCoreCount: Int,
    val displayPixelCount: Long,
    val isEmulator: Boolean,
)

internal object BirthdayAnimationQualitySelector {
    private const val FOUR_GB_MB = 4L * 1024L
    private const val EIGHT_GB_MB = 8L * 1024L
    private const val MAX_REFINED_DISPLAY_PIXELS = 5_000_000L

    fun detect(context: Context): BirthdayAnimationQuality {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return BirthdayAnimationQuality.Low
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val metrics = context.resources.displayMetrics
        val buildIdentity =
            "${Build.FINGERPRINT} ${Build.MODEL} ${Build.PRODUCT} ${Build.HARDWARE}".lowercase()
        return select(
            BirthdayAnimationDeviceProfile(
                isLowRamDevice = activityManager.isLowRamDevice,
                totalMemoryMb = memoryInfo.totalMem / (1024L * 1024L),
                memoryClassMb = activityManager.memoryClass,
                cpuCoreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
                displayPixelCount = metrics.widthPixels.toLong() * metrics.heightPixels.toLong(),
                isEmulator =
                    listOf("generic", "emulator", "sdk_gphone", "ranchu", "goldfish")
                        .any(buildIdentity::contains),
            ),
        )
    }

    fun select(profile: BirthdayAnimationDeviceProfile): BirthdayAnimationQuality {
        if (
            profile.isEmulator ||
            profile.isLowRamDevice ||
            profile.totalMemoryMb in 1 until FOUR_GB_MB ||
            profile.memoryClassMb < 192 ||
            profile.cpuCoreCount <= 4
        ) {
            return BirthdayAnimationQuality.Low
        }
        return if (
            profile.totalMemoryMb >= EIGHT_GB_MB &&
            profile.memoryClassMb >= 256 &&
            profile.cpuCoreCount >= 8 &&
            profile.displayPixelCount <= MAX_REFINED_DISPLAY_PIXELS
        ) {
            BirthdayAnimationQuality.Refined
        } else {
            BirthdayAnimationQuality.Standard
        }
    }
}

internal object BirthdayAnimationDeveloperAccess {
    private const val PASSCODE_SHA256 =
        "e11177d09577ab6a61abfd5047c18fe2470455cfd1873ee378a4e9df5b3c2b7a"

    fun matchesPasscode(passcode: String): Boolean {
        val normalized = passcode.trim().lowercase(Locale.ROOT)
        val actual =
            MessageDigest.getInstance("SHA-256")
                .digest(normalized.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xff) }
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            PASSCODE_SHA256.toByteArray(Charsets.US_ASCII),
        )
    }
}

internal class BirthdayAnimationDeveloperStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isUnlocked(): Boolean = preferences.getBoolean(KEY_UNLOCKED, false)

    fun unlock(passcode: String): Boolean {
        if (!BirthdayAnimationDeveloperAccess.matchesPasscode(passcode)) return false
        preferences.edit().putBoolean(KEY_UNLOCKED, true).apply()
        return true
    }

    fun qualityOverride(): BirthdayAnimationQuality? =
        preferences.getString(KEY_QUALITY_OVERRIDE, null)
            ?.let { value -> runCatching { BirthdayAnimationQuality.valueOf(value) }.getOrNull() }

    fun saveQualityOverride(quality: BirthdayAnimationQuality?) {
        preferences.edit().apply {
            if (quality == null) remove(KEY_QUALITY_OVERRIDE) else putString(KEY_QUALITY_OVERRIDE, quality.name)
        }.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "douduoyu-animation-developer"
        const val KEY_UNLOCKED = "unlocked"
        const val KEY_QUALITY_OVERRIDE = "quality_override"
    }
}
